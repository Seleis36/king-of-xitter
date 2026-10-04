package com.kingofxitter.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kingofxitter.app.game.GameViewModel
import com.kingofxitter.bots.InternBot
import com.kingofxitter.bots.asBotAction
import com.kingofxitter.content.LeaderRoster
import com.kingofxitter.engine.MatchSaveFile
import com.kingofxitter.engine.MatchSetup
import com.kingofxitter.engine.model.GameEvent
import com.kingofxitter.engine.model.LeaderId
import com.kingofxitter.engine.model.PlayerId
import com.kingofxitter.engine.model.PlayerResource
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
    private val gameFactory: suspend (LeaderId) -> GameViewModel = { leader ->
        val setup = MatchSetup.create(leader, LeaderRoster.ids, Random.nextLong())
        GameViewModel.restore(
            initialState = setup.state,
            humanPlayerId = setup.humanPlayerId,
            saveFile = MatchSaveFile(saveFile.toPath()),
            // B1 replaces the engine's always-first-option stand-in. The deck is still empty, so
            // the bots play base rules with CardMarket.Unknown until C2 wires the shop in.
            botAction = InternBot().asBotAction(),
        )
    },
) : ViewModel() {
    private val store = ViewModelStore()
    private val mutableSession = MutableStateFlow<MatchSession?>(null)
    private val mutableLastHit = MutableStateFlow<Pair<PlayerId, Int>?>(null)
    private var starting: Job? = null
    private var hitWatcher: Job? = null

    /** Null until [resumeOrStart] or [startNew] is called. */
    val session: StateFlow<MatchSession?> = mutableSession.asStateFlow()

    /**
     * The attacker and damage of the latest tweet that hit the human, from the engine's events,
     * for the incoming-tweet screen.
     */
    val lastHitOnHuman: StateFlow<Pair<PlayerId, Int>?> = mutableLastHit.asStateFlow()

    /** Continues the saved match, or starts a new one when there is no save. */
    fun resumeOrStart(leader: LeaderId = DefaultLeader) {
        if (mutableSession.value is MatchSession.Ready || starting?.isActive == true) return
        start(leader, discardSave = false)
    }

    /** Throws away any saved match and starts a fresh one. */
    fun startNew(leader: LeaderId = DefaultLeader) = start(leader, discardSave = true)

    private fun start(
        leader: LeaderId,
        discardSave: Boolean,
    ) {
        starting?.cancel()
        hitWatcher?.cancel()
        // Stop the old match first, so its bots can't write to the save while it's replaced.
        store.clear()
        mutableSession.value = MatchSession.Loading
        starting =
            viewModelScope.launch {
                val startupJob = currentCoroutineContext()[Job]
                var unregisteredGame: GameViewModel? = null
                try {
                    if (discardSave) saveFile.delete()
                    val game = gameFactory(leader)
                    unregisteredGame = game
                    currentCoroutineContext().ensureActive()
                    if (starting !== startupJob) {
                        game.close()
                        return@launch
                    }
                    val registered = ViewModelProvider(store, SingleInstanceFactory(game))[GameViewModel::class.java]
                    unregisteredGame = null
                    watchHits(registered)
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

        /** Saves the match as `match.json` in the app's private files. */
        val Factory =
            viewModelFactory {
                initializer { MatchHost(File(checkNotNull(this[APPLICATION_KEY]).filesDir, SAVE_NAME)) }
            }
    }
}
