package com.kingofxitter.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kingofxitter.app.game.GameViewModel
import com.kingofxitter.bots.BotDifficulty
import com.kingofxitter.bots.LeaderBots
import com.kingofxitter.bots.asBotAction
import com.kingofxitter.cards.CardCatalog
import com.kingofxitter.cards.CardEffects
import com.kingofxitter.cards.ShopLogic
import com.kingofxitter.cards.ShopReducer
import com.kingofxitter.content.LeaderRoster
import com.kingofxitter.engine.DefaultGameReducer
import com.kingofxitter.engine.MatchSaveFile
import com.kingofxitter.engine.MatchSetup
import com.kingofxitter.engine.RollOffRound
import com.kingofxitter.engine.model.CardEffectRegistry
import com.kingofxitter.engine.model.GameEvent
import com.kingofxitter.engine.model.LeaderId
import com.kingofxitter.engine.model.PlayerId
import com.kingofxitter.engine.model.PlayerResource
import com.kingofxitter.engine.model.PlayerState
import com.kingofxitter.engine.model.ResourceChangeReason
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.random.Random

/** Your leader until V4's Pick your leader screen supplies one: the design's Macaron. */
val DefaultLeader = LeaderRoster.macaron.id

/**
 * What a new match is set up with. The defaults are the base rules at the controller's own
 * pacing; V5's Settings screen and V4's Pick your leader screen replace them.
 *
 * [characterCap] only reaches a new match: a resumed one keeps the cap it was started with, which
 * is why the Settings screen says rules changes apply next match.
 */
data class MatchOptions(
    val leader: LeaderId = DefaultLeader,
    val difficulty: BotDifficulty = BotDifficulty.REPLY_GUY,
    val botDelayMillis: Long = DEFAULT_BOT_DELAY_MILLIS,
    val characterCap: Int? = null,
    /** Fixed seed for a reproducible match; null draws a new one. */
    val seed: Long? = null,
)

/**
 * What the Versus intro shows: who was drawn, who won the roll-off, and the dice that decided it
 * (ROADMAP §1, "First player"). It is only produced for a match that actually started here; a
 * resumed save has no roll-off to replay.
 */
data class MatchIntro(
    val humanPlayerId: PlayerId,
    val firstPlayerId: PlayerId,
    val players: List<PlayerState>,
    val rollOff: List<RollOffRound>,
    /** The number on the Versus screen's ticket, from the match's own seed. */
    val matchNumber: Int,
)

/** A started match, plus the intro when it is a fresh one. */
data class StartedMatch(
    val game: GameViewModel,
    val intro: MatchIntro?,
)

/** `RealGameController`'s own default pause between bot moves. */
const val DEFAULT_BOT_DELAY_MILLIS = 400L

/** The current match, or why there isn't one. */
sealed interface MatchSession {
    /** Restoring the save or setting up a new match. */
    data object Loading : MatchSession

    /** A live match. Use [game]'s `humanPlayerId`, which a restored save may have changed. */
    data class Ready(
        val game: GameViewModel,
    ) : MatchSession

    /** The save couldn't be read or written. The file is left on disk. */
    data class Failed(
        val error: Throwable,
    ) : MatchSession
}

/**
 * Hosts the match for the navigation graph. [GameViewModel] lives in a match-scoped
 * [ViewModelStore], so it is cleared (and its bots stop) when the match is replaced or the
 * activity finishes for good, and survives configuration changes in between. The match is saved
 * to [saveFile]; the app uses [Factory], which keeps it in the app's private files.
 */
class MatchHost(
    private val saveFile: File,
    private val gameFactory: suspend (MatchOptions) -> StartedMatch = { options ->
        val seed = options.seed ?: Random.nextLong()
        val setup = MatchSetup.create(options.leader, LeaderRoster.ids, seed)
        val initial = ShopLogic.setupDeck(setup.state.copy(characterCap = options.characterCap), CardCatalog.standardDeck, seed)
        val game =
            GameViewModel.restore(
                initialState = initial,
                humanPlayerId = setup.humanPlayerId,
                saveFile = MatchSaveFile(saveFile.toPath()),
                reducer = ShopReducer(DefaultGameReducer(CardEffectRegistry(CardEffects.all))),
                // V6 gives each seat its own leader's personality at the chosen difficulty,
                // and the catalogue lets the bots price what is in the shop.
                botAction = LeaderBots(options.difficulty, CatalogMarket).asBotAction(),
                botDelayMillis = options.botDelayMillis,
            )
        // A restored save is a different match, so it has no roll-off for the Versus screen.
        val fresh = game.humanPlayerId == setup.humanPlayerId && game.state.value == initial
        StartedMatch(
            game,
            if (!fresh) {
                null
            } else {
                MatchIntro(
                    humanPlayerId = setup.humanPlayerId,
                    firstPlayerId = setup.state.currentPlayerId,
                    players = setup.state.players,
                    rollOff = setup.rollOff,
                    matchNumber = Math.floorMod(seed, MATCH_NUMBERS),
                )
            },
        )
    },
) : ViewModel() {
    private val store = ViewModelStore()
    private val mutableSession = MutableStateFlow<MatchSession?>(null)
    private val mutableLastHit = MutableStateFlow<Pair<PlayerId, Int>?>(null)
    private val mutableIntro = MutableStateFlow<MatchIntro?>(null)
    private var starting: Job? = null
    private var hitWatcher: Job? = null

    /** Null until [resumeOrStart] or [startNew] is called. */
    val session: StateFlow<MatchSession?> = mutableSession.asStateFlow()

    /**
     * The attacker and damage of the latest tweet that hit the human, from the engine's events,
     * for the incoming-tweet screen.
     */
    val lastHitOnHuman: StateFlow<Pair<PlayerId, Int>?> = mutableLastHit.asStateFlow()

    /** The roll-off of the current match, or null when it was resumed from a save. */
    val intro: StateFlow<MatchIntro?> = mutableIntro.asStateFlow()

    /** Continues the saved match, or starts a new one when there is no save. */
    fun resumeOrStart(options: MatchOptions = MatchOptions()) {
        if (mutableSession.value is MatchSession.Ready || starting?.isActive == true) return
        start(options, discardSave = false)
    }

    /** Throws away any saved match and starts a fresh one. */
    fun startNew(options: MatchOptions = MatchOptions()) = start(options, discardSave = true)

    private fun start(
        options: MatchOptions,
        discardSave: Boolean,
    ) {
        starting?.cancel()
        hitWatcher?.cancel()
        // Stop the old match first, so its bots can't write to the save while it's replaced.
        store.clear()
        mutableIntro.value = null
        mutableSession.value = MatchSession.Loading
        starting =
            viewModelScope.launch {
                val startupJob = currentCoroutineContext()[Job]
                var unregisteredGame: GameViewModel? = null
                try {
                    if (discardSave) saveFile.delete()
                    val started = gameFactory(options)
                    val game = started.game
                    unregisteredGame = game
                    currentCoroutineContext().ensureActive()
                    if (starting !== startupJob) {
                        game.close()
                        return@launch
                    }
                    val registered = ViewModelProvider(store, SingleInstanceFactory(game))[GameViewModel::class.java]
                    unregisteredGame = null
                    watchHits(registered)
                    mutableIntro.value = started.intro
                    mutableSession.value = MatchSession.Ready(registered)
                } catch (cancelled: CancellationException) {
                    unregisteredGame?.close()
                    throw cancelled
                } catch (error: Exception) {
                    unregisteredGame?.close()
                    if (currentCoroutineContext().isActive && starting === startupJob) {
                        mutableSession.value = MatchSession.Failed(error)
                    }
                }
            }
    }

    private fun watchHits(game: GameViewModel) {
        hitWatcher?.cancel()
        mutableLastHit.value = null
        hitWatcher =
            viewModelScope.launch {
                game.events.collect { event ->
                    if (event is GameEvent.ResourceChanged &&
                        event.playerId == game.humanPlayerId &&
                        event.resource == PlayerResource.FOLLOWERS &&
                        event.reason == ResourceChangeReason.TWEET &&
                        event.amount < 0
                    ) {
                        mutableLastHit.value = game.state.value.currentPlayerId to -event.amount
                    }
                }
            }
    }

    override fun onCleared() {
        store.clear()
    }

    /** Hands the already-restored instance to the store, which only knows how to own it. */
    private class SingleInstanceFactory(
        private val instance: GameViewModel,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = instance as T
    }

    companion object {
        private const val SAVE_NAME = "match.json"

        /** The Versus screen's ticket is four digits, as in the design's "MATCH #0427". */
        private const val MATCH_NUMBERS = 10_000

        /** Saves the match as `match.json` in the app's private files. */
        val Factory =
            viewModelFactory {
                initializer { MatchHost(File(checkNotNull(this[APPLICATION_KEY]).filesDir, SAVE_NAME)) }
            }
    }
}
