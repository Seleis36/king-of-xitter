package com.kingofxitter.engine

import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.GameEvent
import com.kingofxitter.engine.model.GameState

/**
 * Pure rules seam: identical state and action inputs produce the same transition.
 * Randomness is derived from and written back to [GameState.randomState].
 * Implementations do not read clocks or perform I/O. An illegal action leaves
 * the state unchanged and returns an [com.kingofxitter.engine.model.GameEvent.ActionRejected] event.
 */
fun interface GameReducer {
    /** Reduces [action] against [state] and returns the next state and ordered events. */
    fun reduce(
        state: GameState,
        action: Action,
    ): GameTransition
}

/** State and occurrence-ordered domain events produced by one reducer invocation. */
data class GameTransition(
    val state: GameState,
    val events: List<GameEvent>,
)
