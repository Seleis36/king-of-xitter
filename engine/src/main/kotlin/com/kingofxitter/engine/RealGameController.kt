package com.kingofxitter.engine

import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.ActionRejectionReason
import com.kingofxitter.engine.model.GameEvent
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.PlayerId
import com.kingofxitter.engine.model.TurnPhase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Serializes UI actions and paced bot turns. Closing cancels only this controller's work. */
class RealGameController(
    initialState: GameState,
    scope: CoroutineScope,
    private val botPlayers: Set<PlayerId>,
    private val reducer: GameReducer = DefaultGameReducer(),
    private val botDelayMillis: Long = 400,
    private val botAction: (GameState) -> Action? = ::firstAvailableAction,
    private val save: suspend (GameState) -> Unit = {},
) : GameController,
    AutoCloseable {
    private val ownedScope = CoroutineScope(scope.coroutineContext + SupervisorJob(scope.coroutineContext[Job]))
    private val requests = Channel<Request>(Channel.UNLIMITED)
    private val mutableState = MutableStateFlow(initialState)
    private val mutableEvents = MutableSharedFlow<GameEvent>(extraBufferCapacity = 64)
    private val mutableFailure = MutableStateFlow<Throwable?>(null)
    private var scheduledBot: Job? = null
    private var revision = 0L
    private var failedRequest: Request? = null
    override val state = mutableState.asStateFlow()
    override val events = mutableEvents.asSharedFlow()

    /** Persistence or bot-provider failure, exposed without silently discarding a turn. */
    val failure = mutableFailure.asStateFlow()

    init {
        require(botDelayMillis >= 0)
        require(botPlayers.all { id -> initialState.players.any { it.id == id } })
        ownedScope.launch {
            scheduleBot()
            for (request in requests) {
                if (request.retry) {
                    failedRequest?.let { requests.send(it) }
                    continue
                }
                if (request.revision != null && request.revision != revision) continue
                try {
                    val snapshot = mutableState.value
                    val action = request.action ?: botAction(snapshot) ?: continue
                    val transition =
                        if (request.revision == null && action.playerId in botPlayers) {
                            GameTransition(snapshot, listOf(GameEvent.ActionRejected(action.playerId, ActionRejectionReason.WRONG_PLAYER)))
                        } else {
                            reducer.reduce(snapshot, action)
                        }
                    if (transition.state != snapshot) {
                        save(transition.state)
                        mutableState.value = transition.state
                        revision++
                        mutableFailure.value = null
                        failedRequest = null
                        scheduleBot()
                    }
                    transition.events.forEach { mutableEvents.emit(it) }
                    if (request.revision != null && transition.events.any { it is GameEvent.ActionRejected }) {
                        val decision = snapshot.pendingDecisions.firstOrNull()
                        val decline = decision?.options?.firstOrNull { it.action is Action.DeclineAbility }
                        if (decision != null && decline != null) {
                            requests.send(Request(Action.ChooseDecisionOption(decision.playerId, decision.id, decline.id), revision))
                        } else {
                            error("Bot supplied a rejected action without a decline option")
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    mutableFailure.value = error
                    failedRequest = request
                }
            }
        }
    }

    override fun submit(action: Action) {
        check(requests.trySend(Request(action)).isSuccess) { "Controller is closed" }
    }

    /** Retry the failed action after the UI resolves a transient persistence or bot-provider error. */
    fun retry() {
        check(requests.trySend(Request(retry = true)).isSuccess) { "Controller is closed" }
    }

    private fun scheduleBot() {
        scheduledBot?.cancel()
        val snapshot = mutableState.value
        val actor = snapshot.pendingDecisions.firstOrNull()?.playerId ?: snapshot.currentPlayerId
        if (snapshot.result != null || actor !in botPlayers) return
        val observedRevision = revision
        scheduledBot =
            ownedScope.launch {
                delay(botDelayMillis)
                requests.send(Request(revision = observedRevision))
            }
    }

    override fun close() {
        requests.close()
        ownedScope.cancel()
    }

    private data class Request(
        val action: Action? = null,
        val revision: Long? = null,
        val retry: Boolean = false,
    )
}

/** Documented stand-in until B1: choose the first offered option, resolve one roll, skip shopping. */
fun firstAvailableAction(state: GameState): Action? {
    if (state.result != null) return null
    val decision = state.pendingDecisions.firstOrNull()
    if (decision != null) {
        val option = decision.options.firstOrNull() ?: return null
        return Action.ChooseDecisionOption(decision.playerId, decision.id, option.id)
    }
    return when (state.phase) {
        TurnPhase.TURN_START -> Action.RollDice(state.currentPlayerId)
        TurnPhase.ROLLING -> Action.ResolveRoll(state.currentPlayerId)
        TurnPhase.SHOP, TurnPhase.END_TURN -> Action.EndTurn(state.currentPlayerId)
        else -> null
    }
}
