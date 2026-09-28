package com.kingofxitter.app.ui.board

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.kingofxitter.app.ui.theme.XitterTheme
import com.kingofxitter.app.ui.theme.nightBackground
import com.kingofxitter.engine.GameFixtures
import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.CardId
import com.kingofxitter.engine.model.DecisionId
import com.kingofxitter.engine.model.DecisionKind
import com.kingofxitter.engine.model.DecisionOption
import com.kingofxitter.engine.model.DecisionOptionId
import com.kingofxitter.engine.model.DiceFace
import com.kingofxitter.engine.model.Die
import com.kingofxitter.engine.model.DieId
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.PendingDecision
import com.kingofxitter.engine.model.PlayerId
import com.kingofxitter.engine.model.TurnPhase

/** Incoming-tweet states for previews and the debug design gallery. */
internal object IncomingSamples {
    private val trumpet = PlayerId("trumpet")

    private fun stayOrLogOff(player: PlayerId) =
        PendingDecision(
            id = DecisionId("stay-or-log-off"),
            playerId = player,
            kind = DecisionKind.STAY_OR_LOG_OFF,
            options =
                listOf(
                    DecisionOption(DecisionOptionId("stay"), Action.StayInXitter(player)),
                    DecisionOption(DecisionOptionId("log-off"), Action.LogOffXitter(player)),
                ),
        )

    /** `incoming-tweet-stay-or-log-off.html`: Trumpet's two tweets plus CAPS LOCK STUCK take you from 7M to 4M. */
    val design: GameState =
        BoardSamples.design.copy(
            players =
                BoardSamples.design.players.map { player ->
                    when (player.id) {
                        BoardSamples.you -> player.copy(followers = 4)
                        trumpet -> player.copy(policyIds = listOf(CardId("caps-lock-stuck")))
                        else -> player
                    }
                },
            currentPlayerId = trumpet,
            phase = TurnPhase.DECISION,
            turnNumber = 8,
            xitterPlayerId = BoardSamples.you,
            dice =
                listOf(DiceFace.TWEET, DiceFace.TWEET, DiceFace.ONE, DiceFace.LIKE, DiceFace.TWO, DiceFace.CHARACTERS)
                    .mapIndexed { index, face -> Die(DieId(index), face) },
            rollNumber = 3,
            pendingDecisions = listOf(stayOrLogOff(BoardSamples.you)),
            phaseAfterDecisions = TurnPhase.SHOP,
        )

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
