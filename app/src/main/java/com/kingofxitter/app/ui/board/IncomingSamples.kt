package com.kingofxitter.app.ui.board

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.kingofxitter.app.ui.theme.XitterTheme
import com.kingofxitter.app.ui.theme.nightBackground
import com.kingofxitter.engine.GameFixtures
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.PlayerId

/** Incoming-tweet states for previews and the debug design gallery. */
internal object IncomingSamples {
    private val trumpet = PlayerId("trumpet")

    /** `incoming-tweet-stay-or-log-off.html`: Trumpet's two tweets plus CAPS LOCK STUCK take you from 7M to 4M. */
    val design: GameState = GameFixtures.Design.incoming

    /** The design's hit, with CAPS LOCK STUCK's extra unfollow. */
    val designHit = IncomingHit(trumpet, damage = 3)

    val fixture: GameState = GameFixtures.incomingTweet
}

@Preview(name = "Design", widthDp = 412, heightDp = 915)
@Composable
private fun IncomingDesignPreview() {
    XitterTheme {
        val state = IncomingSamples.design
        IncomingTweetScreen(
            state,
            BoardSamples.you,
            state.pendingDecisions.first(),
            onAction = {},
            modifier = Modifier.nightBackground(),
            hit = IncomingSamples.designHit,
        )
    }
}

@Preview(name = "Fixture: incomingTweet", widthDp = 412, heightDp = 915)
@Composable
private fun IncomingFixturePreview() {
    XitterTheme {
        val state = IncomingSamples.fixture
        IncomingTweetScreen(state, BoardSamples.you, state.pendingDecisions.first(), onAction = {}, modifier = Modifier.nightBackground())
    }
}
