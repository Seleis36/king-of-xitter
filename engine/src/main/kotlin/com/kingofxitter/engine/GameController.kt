package com.kingofxitter.engine

import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.GameEvent
import com.kingofxitter.engine.model.GameState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Observable match seam used by Android screens, bots, and previews.
 * State always exposes the latest snapshot; events describe ordered changes.
 */
interface GameController {
    /** Current immutable match snapshot. New collectors immediately receive its latest value. */
    val state: StateFlow<GameState>

    /**
     * Ordered transient events. Active collectors receive events from submitted actions;
     * collectors should subscribe before submitting actions because events are not replayed.
     */
    val events: Flow<GameEvent>

    /** Submit a player intent for processing without blocking the caller. */
    fun submit(action: Action)
}
