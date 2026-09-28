package com.kingofxitter.app.ui.board

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.kingofxitter.engine.GameController
import com.kingofxitter.engine.model.PlayerId

/**
 * Board bound to a [GameController]: renders its latest state and submits the human's actions.
 * Opens the incoming-tweet screen when the human must decide whether to stay on Xitter.
 */
@Composable
fun BoardRoute(
    controller: GameController,
    humanId: PlayerId,
    callbacks: BoardCallbacks,
    modifier: Modifier = Modifier,
) {
    val state by controller.state.collectAsState()
    // Being hit as Main Character interrupts the board: open the incoming tweet once per decision.
    val incoming = (BoardMode.of(state, humanId) as? BoardMode.IncomingTweet)?.decision?.id
    LaunchedEffect(incoming) { if (incoming != null) callbacks.onIncomingTweet() }
    BoardScreen(state, humanId, controller::submit, modifier, callbacks)
}
