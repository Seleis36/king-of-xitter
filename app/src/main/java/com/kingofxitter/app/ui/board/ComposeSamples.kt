package com.kingofxitter.app.ui.board

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.kingofxitter.app.ui.theme.XitterTheme
import com.kingofxitter.app.ui.theme.nightBackground
import com.kingofxitter.engine.GameFixtures
import com.kingofxitter.engine.model.GameState

/** Compose-screen states for previews and the debug design gallery. */
internal object ComposeSamples {
    /** `compose-your-attack-tweet.html`: three tweets at Trumpet, the Main Character. */
    val design = BoardSamples.design

    /** You hold Xitter, so your tweet hits everyone. */
    val asMainCharacter = GameFixtures.yourRoll

    val all: Map<String, GameState> = mapOf("design" to design, "mainCharacter" to asMainCharacter)
}

@Composable
private fun ComposePreview(state: GameState) {
    XitterTheme {
        ComposeTweetScreen(state, BoardSamples.you, onSend = {}, onCancel = {}, modifier = Modifier.nightBackground())
    }
}

@Preview(name = "Design: hitting the Main Character", widthDp = 412, heightDp = 915)
@Composable
private fun ComposeDesignPreview() = ComposePreview(ComposeSamples.design)

@Preview(name = "Fixture: yourRoll, hitting everyone", widthDp = 412, heightDp = 915)
@Composable
private fun ComposeMainCharacterPreview() = ComposePreview(ComposeSamples.asMainCharacter)
