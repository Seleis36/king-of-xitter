package com.kingofxitter.app.ui.board

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.kingofxitter.app.ui.motion.EffectQueue
import com.kingofxitter.engine.GameController
import com.kingofxitter.engine.model.PlayerId

/**
 * Board bound to a [GameController]: renders its latest state and submits the human's actions.
 * Opens the incoming-tweet screen when the human must decide whether to stay on Xitter, and
 * draws [effects] (damage numbers, shakes, stamps).
 */
@Composable
fun BoardRoute(
    controller: GameController,
    humanId: PlayerId,
    callbacks: BoardCallbacks,
    modifier: Modifier = Modifier,
    effects: EffectQueue? = null,
) {
    val state by controller.state.collectAsState()
    val active =
        effects
            ?.active
            ?.collectAsState()
            ?.value
            .orEmpty()
    // Being hit as Main Character interrupts the board: open the incoming tweet once per decision.
    val incoming = (BoardMode.of(state, humanId) as? BoardMode.IncomingTweet)?.decision?.id
    LaunchedEffect(incoming) { if (incoming != null) callbacks.onIncomingTweet() }
    BoardScreen(state, humanId, controller::submit, modifier, callbacks, effects = active)
}

/**
 * An [EffectQueue] fed by [controller]'s events for as long as the caller is composed. Hoist it
 * above navigation so effects from the compose and incoming screens still play on the board.
 */
@Composable
fun rememberEffectQueue(controller: GameController): EffectQueue {
    val scope = rememberCoroutineScope()
    val queue = remember(controller) { EffectQueue(scope) }
    LaunchedEffect(controller) { controller.events.collect(queue::offer) }
    return queue
}
