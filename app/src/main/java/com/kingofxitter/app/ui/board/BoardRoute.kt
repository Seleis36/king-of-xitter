package com.kingofxitter.app.ui.board

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.kingofxitter.engine.GameController
import com.kingofxitter.engine.model.PlayerId

/** Board bound to a [GameController]: renders its latest state and submits the human's actions. */
@Composable
fun BoardRoute(
    controller: GameController,
    humanId: PlayerId,
    callbacks: BoardCallbacks,
    modifier: Modifier = Modifier,
) {
    val state by controller.state.collectAsState()
    BoardScreen(state, humanId, controller::submit, modifier, callbacks)
}
