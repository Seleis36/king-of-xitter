package com.kingofxitter.app.ui.board

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.kingofxitter.app.ui.theme.XitterTheme
import com.kingofxitter.app.ui.theme.nightBackground
import com.kingofxitter.engine.GameFixtures
import com.kingofxitter.engine.model.CardId
import com.kingofxitter.engine.model.DiceFace
import com.kingofxitter.engine.model.Die
import com.kingofxitter.engine.model.DieId
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.LeaderId
import com.kingofxitter.engine.model.PlayerId
import com.kingofxitter.engine.model.PlayerState
import com.kingofxitter.engine.model.TurnPhase

/** Board states for previews and the debug design gallery. */
internal object BoardSamples {
    val you = PlayerId("you")

    /** The exact situation drawn in `game-board-your-roll.html`. */
    val design =
        GameState(
            players =
                listOf(
                    PlayerState(
                        you,
                        LeaderId("macaron"),
                        followers = 7,
                        characters = 60,
                        mandates = 8,
                        extraRerolls = 1,
                        policyIds = listOf(CardId("speechwriter"), CardId("press-office")),
                    ),
                    PlayerState(PlayerId("trumpet"), LeaderId("trumpet"), followers = 5, characters = 30, mandates = 13),
                    PlayerState(PlayerId("poutine"), LeaderId("poutine"), followers = 9, characters = 120, mandates = 4),
                    PlayerState(PlayerId("kim"), LeaderId("kim"), followers = 3, characters = 20, mandates = 16),
                ),
            currentPlayerId = you,
            phase = TurnPhase.ROLLING,
            turnNumber = 7,
            xitterPlayerId = PlayerId("trumpet"),
            dice =
                listOf(DiceFace.TWEET, DiceFace.TWEET, DiceFace.THREE, DiceFace.THREE, DiceFace.TWEET, DiceFace.CHARACTERS)
                    .mapIndexed { index, face -> Die(DieId(index), face, isKept = index < 4) },
            rollNumber = 2,
            turnRollLimit = 4,
            shopCardIds = listOf(CardId("quote-tweet"), CardId("home-turf"), CardId("press-office")),
            randomState = 7L,
        )

    /** Start of your turn with an empty Xitter. */
    val turnStart =
        design.copy(phase = TurnPhase.TURN_START, xitterPlayerId = null, dice = emptyList(), rollNumber = 0, turnRollLimit = null)

    /** Named states shown by the gallery: design plus the shared engine fixtures. */
    val all: Map<String, GameState> =
        mapOf(
            "design" to design,
            "start" to turnStart,
            "yourRoll" to GameFixtures.yourRoll,
            "opponentTurn" to GameFixtures.opponentTurn,
            "incomingTweet" to GameFixtures.incomingTweet,
            "shop" to GameFixtures.shop,
            "victory" to GameFixtures.victory,
        )
}

@Composable
private fun BoardPreview(state: GameState) {
    XitterTheme { BoardScreen(state, BoardSamples.you, onAction = {}, modifier = Modifier.nightBackground()) }
}

@Preview(name = "Design", widthDp = 412, heightDp = 915)
@Composable
private fun BoardDesignPreview() = BoardPreview(BoardSamples.design)

@Preview(name = "Turn start, empty Xitter", widthDp = 412, heightDp = 915)
@Composable
private fun BoardTurnStartPreview() = BoardPreview(BoardSamples.turnStart)

@Preview(name = "Fixture: yourRoll", widthDp = 412, heightDp = 915)
@Composable
private fun BoardYourRollPreview() = BoardPreview(GameFixtures.yourRoll)

@Preview(name = "Fixture: opponentTurn", widthDp = 412, heightDp = 915)
@Composable
private fun BoardOpponentPreview() = BoardPreview(GameFixtures.opponentTurn)

@Preview(name = "Fixture: incomingTweet", widthDp = 412, heightDp = 915)
@Composable
private fun BoardIncomingPreview() = BoardPreview(GameFixtures.incomingTweet)

@Preview(name = "Fixture: shop", widthDp = 412, heightDp = 915)
@Composable
private fun BoardShopPreview() = BoardPreview(GameFixtures.shop)

@Preview(name = "Fixture: victory", widthDp = 412, heightDp = 915)
@Composable
private fun BoardVictoryPreview() = BoardPreview(GameFixtures.victory)
