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
import com.kingofxitter.engine.model.LeaderId
import com.kingofxitter.engine.model.PendingDecision
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

    /** C9: the design's roll, paused on a power-up of yours asking a question. */
    private fun asking(
        cardId: String,
        promptKey: String,
        options: List<DecisionOption>,
    ): GameState {
        val owned = design.players.map { if (it.id == you) it.copy(policyIds = it.policyIds + CardId(cardId)) else it }
        val decision =
            PendingDecision(
                id = DecisionId("ability-$cardId"),
                playerId = you,
                kind = DecisionKind.ACTIVATE_ABILITY,
                options = options + DecisionOption(DecisionOptionId("decline"), Action.DeclineAbility(you)),
                promptKey = promptKey,
            )
        return design.copy(
            players = owned,
            phase = TurnPhase.DECISION,
            phaseAfterDecisions = TurnPhase.ROLLING,
            pendingDecisions = listOf(decision),
        )
    }

    private fun use(
        cardId: String,
        abilityId: String,
        dieIds: Set<DieId> = emptySet(),
        targetPlayerId: PlayerId? = null,
    ) = Action.ActivateAbility(you, CardId(cardId), abilityId, targetPlayerId = targetPlayerId, dieIds = dieIds)

    /** Poll Rigger: pick one of your dice on the tray to turn into a 1. */
    val pickDie =
        asking(
            "c22_poll_rigger",
            "card_c22_prompt",
            design.dice.map { die ->
                DecisionOption(
                    DecisionOptionId("${die.id.value}"),
                    use("c22_poll_rigger", "use_poll_rigger", setOf(die.id)),
                    "card_c22_option",
                )
            },
        )

    /** Spin Doctor: pick a die on the tray, then its new face. */
    val pickFace =
        asking(
            "c44_spin_doctor",
            "card_c44_prompt",
            design.dice.flatMap { die ->
                DiceFace.entries.filter { it != die.face }.map { face ->
                    DecisionOption(
                        DecisionOptionId("${die.id.value}_${face.name}"),
                        use("c44_spin_doctor", "use_spin_doctor:${face.name}", setOf(die.id)),
                        "card_c44_option",
                    )
                }
            },
        )

    /** Retweet for Retweet: pick who buys your like. */
    val pickLeader =
        asking(
            "c20_retweet_for_retweet",
            "ability_sell_like_prompt",
            listOf(PlayerId("trumpet"), PlayerId("poutine")).map { target ->
                DecisionOption(
                    DecisionOptionId("sell_like_${target.value}"),
                    use("c20_retweet_for_retweet", "sell_like", setOf(DieId(5)), target),
                )
            },
        )

    /** Paid Promotion: one way to use it, or skip. */
    val useOrSkip =
        asking(
            "c37_paid_promotion",
            "card_c37_prompt",
            listOf(DecisionOption(DecisionOptionId("buy_followers"), use("c37_paid_promotion", "use_paid_promotion"), "card_c37_option")),
        )

    /** Pivot: one button per policy you could discard. */
    val chooseCard =
        asking(
            "c26_pivot",
            "ability_discard_policy_prompt",
            listOf("c18_speechwriter", "c09_press_office").map { policy ->
                DecisionOption(DecisionOptionId("discard_$policy"), use("c26_pivot", "discard_policy:$policy"))
            },
        )

    /** Named states shown by the gallery: design plus the shared engine fixtures. */
    val all: Map<String, GameState> =
        mapOf(
            "design" to design,
            "start" to turnStart,
            "pickDie" to pickDie,
            "pickFace" to pickFace,
            "pickLeader" to pickLeader,
            "useOrSkip" to useOrSkip,
            "chooseCard" to chooseCard,
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

@Preview(name = "C9: pick a die", widthDp = 412, heightDp = 915)
@Composable
private fun BoardPickDiePreview() = BoardPreview(BoardSamples.pickDie)

@Preview(name = "C9: pick a die, then a face", widthDp = 412, heightDp = 915)
@Composable
private fun BoardPickFacePreview() = BoardPreview(BoardSamples.pickFace)

@Preview(name = "C9: pick a leader", widthDp = 412, heightDp = 915)
@Composable
private fun BoardPickLeaderPreview() = BoardPreview(BoardSamples.pickLeader)

@Preview(name = "C9: use it or skip", widthDp = 320, heightDp = 640, fontScale = 1.5f)
@Composable
private fun BoardUseOrSkipPreview() = BoardPreview(BoardSamples.useOrSkip)

@Preview(name = "C9: choose a card", widthDp = 412, heightDp = 915)
@Composable
private fun BoardChooseCardPreview() = BoardPreview(BoardSamples.chooseCard)
