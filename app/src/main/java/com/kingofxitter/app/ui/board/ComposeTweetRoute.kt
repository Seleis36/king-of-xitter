package com.kingofxitter.app.ui.board

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.kingofxitter.engine.GameController
import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.PlayerId

/**
 * Compose screen bound to a [GameController]. SEND IT resolves the human's roll, then [onDone]
 * returns to the board, which shows the result. [onSent] receives the chosen zinger before the
 * roll resolves, so the voice log can post it as the attack.
 */
@Composable
fun ComposeTweetRoute(
    controller: GameController,
    humanId: PlayerId,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    onSent: (String) -> Unit = {},
) {
    val state by controller.state.collectAsState()
    ComposeTweetScreen(
        state = state,
        humanId = humanId,
        onSend = { zinger ->
            onSent(zinger)
            controller.submit(Action.ResolveRoll(humanId))
            onDone()
        },
        onCancel = onDone,
        modifier = modifier,
    )
}
