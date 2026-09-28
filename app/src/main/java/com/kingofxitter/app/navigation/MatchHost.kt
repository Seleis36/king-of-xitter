package com.kingofxitter.app.navigation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import com.kingofxitter.app.game.GameViewModel
import com.kingofxitter.engine.MatchSaveFile
import com.kingofxitter.engine.MatchSetup
import com.kingofxitter.engine.model.GameEvent
import com.kingofxitter.engine.model.LeaderId
import com.kingofxitter.engine.model.PlayerId
import com.kingofxitter.engine.model.PlayerResource
import com.kingofxitter.engine.model.ResourceChangeReason
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import kotlin.random.Random

/** The six leaders `MatchSetup` draws from. Stand-in for S5's roster. */
val LeaderRoster = listOf("trumpet", "poutine", "kim", "macaron", "xi", "mi-leon").map(::LeaderId)

/** Your leader until V4's Pick your leader screen supplies one: the design's Macaron. */
val DefaultLeader = LeaderId("macaron")

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
 * activity finishes for good, and survives configuration changes in between.
 */
class MatchHost(
    application: Application,
) : AndroidViewModel(application) {
    private val store = ViewModelStore()
    private val saveFile = File(application.filesDir, SAVE_NAME)
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
        mutableSession.value = MatchSession.Loading
        starting =
            viewModelScope.launch {
                runCatching {
                    if (discardSave) saveFile.delete()
                    val setup = MatchSetup.create(leader, LeaderRoster, Random.nextLong())
                    GameViewModel.restore(setup.state, setup.humanPlayerId, MatchSaveFile(saveFile.toPath()))
                }.onSuccess { game ->
                    store.clear()
                    val registered = ViewModelProvider(store, SingleInstanceFactory(game))[GameViewModel::class.java]
                    watchHits(registered)
                    mutableSession.value = MatchSession.Ready(registered)
                }.onFailure { error ->
                    mutableSession.value = MatchSession.Failed(error)
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

    private companion object {
        const val SAVE_NAME = "match.json"
    }
}
