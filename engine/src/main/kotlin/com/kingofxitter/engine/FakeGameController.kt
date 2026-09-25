package com.kingofxitter.engine

import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.GameEvent
import com.kingofxitter.engine.model.GameState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/** Synchronous in-memory controller for previews and fixture-driven screens. */
class FakeGameController(
    initialState: GameState,
    private val reducer: GameReducer = DefaultGameReducer(),
) : GameController {
    private val mutableState = MutableStateFlow(initialState)
    private val mutableEvents = MutableSharedFlow<GameEvent>(extraBufferCapacity = EVENT_BUFFER_CAPACITY)

    override val state: StateFlow<GameState> = mutableState.asStateFlow()
    override val events: Flow<GameEvent> = mutableEvents.asSharedFlow()

    /** Apply the action immediately and publish its resulting snapshot and events. */
    override fun submit(action: Action) {
        val transition = reducer.reduce(mutableState.value, action)
        mutableState.value = transition.state
        transition.events.forEach(mutableEvents::tryEmit)
    }

    private companion object {
        const val EVENT_BUFFER_CAPACITY = 64
    }
}
