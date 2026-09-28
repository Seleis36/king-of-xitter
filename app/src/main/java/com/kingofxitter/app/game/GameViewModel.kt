package com.kingofxitter.app.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingofxitter.engine.DefaultGameReducer
import com.kingofxitter.engine.GameController
import com.kingofxitter.engine.GameReducer
import com.kingofxitter.engine.MatchSaveFile
import com.kingofxitter.engine.RealGameController
import com.kingofxitter.engine.SavedMatch
import com.kingofxitter.engine.firstAvailableAction
import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.PlayerId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Lifecycle owner for the real match loop; destinations observe the stable GameController seam. */
class GameViewModel private constructor(
    initialState: GameState,
    val humanPlayerId: PlayerId,
    saveFile: MatchSaveFile,
    reducer: GameReducer,
    botAction: (GameState) -> Action?,
    botDelayMillis: Long,
) : ViewModel(),
    GameController,
    AutoCloseable {
    private val controller =
        RealGameController(
            initialState = initialState,
            scope = viewModelScope,
            botPlayers = initialState.players.map { it.id }.toSet() - humanPlayerId,
            reducer = reducer,
            botAction = botAction,
            botDelayMillis = botDelayMillis,
            save = { state -> withContext(Dispatchers.IO) { saveFile.save(state, humanPlayerId) } },
        )
    override val state = controller.state
    override val events = controller.events

    /** UI can present persistence failures without replacing or deleting the user's save. */
    val failure = controller.failure

    override fun submit(action: Action) = controller.submit(action)

    /** Retry after a temporary disk or bot-provider failure. */
    fun retry() = controller.retry()

    override fun close() = controller.close()

    override fun onCleared() = close()

    companion object {
        /** Restore before starting bots. The app shell supplies an app-private save path and its chosen leader. */
        suspend fun restore(
            initialState: GameState,
            humanPlayerId: PlayerId,
            saveFile: MatchSaveFile,
            reducer: GameReducer = DefaultGameReducer(),
            botAction: (GameState) -> Action? = ::firstAvailableAction,
            botDelayMillis: Long = 400,
        ): GameViewModel {
            val restored =
                withContext(Dispatchers.IO) {
                    saveFile.loadMatch() ?: SavedMatch(initialState, humanPlayerId).also { saveFile.save(initialState, humanPlayerId) }
                }
            val restoredHuman = requireNotNull(restored.humanPlayerId) { "Saved match is missing human ownership" }
            require(restored.state.players.any { it.id == restoredHuman })
            return GameViewModel(restored.state, restoredHuman, saveFile, reducer, botAction, botDelayMillis)
        }
    }
}
