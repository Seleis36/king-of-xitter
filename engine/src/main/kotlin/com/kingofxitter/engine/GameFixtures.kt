package com.kingofxitter.engine

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
import com.kingofxitter.engine.model.MatchResult
import com.kingofxitter.engine.model.PendingDecision
import com.kingofxitter.engine.model.PlayerId
import com.kingofxitter.engine.model.PlayerState
import com.kingofxitter.engine.model.TurnPhase
import com.kingofxitter.engine.model.VictoryReason

/** Named match snapshots for screen previews and UI integration. */
object GameFixtures {
    private val you = PlayerId("you")
    private val kim = PlayerId("kim")
    private val macaron = PlayerId("macaron")
    private val poutine = PlayerId("poutine")

    private val roster =
        listOf(
            PlayerState(you, LeaderId("macaron"), followers = 7, mandates = 6, characters = 30),
            PlayerState(kim, LeaderId("kim"), followers = 10, mandates = 8, characters = 60),
            PlayerState(macaron, LeaderId("poutine"), followers = 5, mandates = 3, characters = 10),
            PlayerState(poutine, LeaderId("mi-leon"), followers = 8, mandates = 5, characters = 40),
        )

    /** Active turn with a mixed roll and the player Main Character. */
    val yourRoll =
        GameState(
            players = roster,
            currentPlayerId = you,
            phase = TurnPhase.ROLLING,
            turnNumber = 4,
            xitterPlayerId = you,
            dice =
                listOf(
                    Die(DieId(0), DiceFace.THREE),
                    Die(DieId(1), DiceFace.THREE),
                    Die(DieId(2), DiceFace.LIKE),
                    Die(DieId(3), DiceFace.TWEET),
                    Die(DieId(4), DiceFace.CHARACTERS),
                    Die(DieId(5), DiceFace.ONE),
                ),
            rollNumber = 2,
            randomState = 42L,
        )

    /** Opponent roll with a policy in play and an available reroll. */
    val opponentTurn =
        yourRoll.copy(
            currentPlayerId = kim,
            xitterPlayerId = you,
            dice = yourRoll.dice.map { it.copy(isKept = it.face == DiceFace.THREE) },
            players =
                roster.map { player ->
                    if (player.id == kim) player.copy(policyIds = listOf(CardId("alpha-poster"))) else player
                },
        )

    /** Compose-attack screen after the current player rolled tweets. */
    val composeTweet = opponentTurn

    /** Decision prompt after the player is hit while Main Character. */
    val incomingTweet =
        yourRoll.copy(
            currentPlayerId = kim,
            phase = TurnPhase.DECISION,
            pendingDecisions =
                listOf(
                    PendingDecision(
                        id = DecisionId("stay-or-log-off"),
                        playerId = you,
                        kind = DecisionKind.STAY_OR_LOG_OFF,
                        options =
                            listOf(
                                DecisionOption(
                                    DecisionOptionId("stay"),
                                    Action.StayInXitter(you),
                                ),
                                DecisionOption(
                                    DecisionOptionId("log-off"),
                                    Action.LogOffXitter(you),
                                ),
                            ),
                    ),
                ),
            phaseAfterDecisions = TurnPhase.SHOP,
        )

    /** Post-roll shop with a three-card row. */
    val shop =
        yourRoll.copy(
            phase = TurnPhase.SHOP,
            shopCardIds = listOf(CardId("quote-tweet"), CardId("home-turf"), CardId("press-office")),
        )

    /** Victory screen state for the current player. */
    val victory =
        yourRoll.copy(
            phase = TurnPhase.FINISHED,
            result = MatchResult.Victory(you, VictoryReason.TWENTY_MANDATES),
            players = roster.map { if (it.id == you) it.copy(mandates = 20) else it },
        )

    /** Defeat screen state after the player's account is suspended. */
    val defeat =
        yourRoll.copy(
            phase = TurnPhase.FINISHED,
            result = MatchResult.Victory(kim, VictoryReason.TWENTY_MANDATES),
            players =
                roster.map {
                    when (it.id) {
                        you -> it.copy(followers = 0)
                        kim -> it.copy(mandates = 20)
                        else -> it
                    }
                },
        )

    /** Finished match used by the standings screen. */
    val standings =
        yourRoll.copy(
            phase = TurnPhase.FINISHED,
            result = MatchResult.Victory(kim, VictoryReason.TWENTY_MANDATES),
            players = roster.map { if (it.id == kim) it.copy(mandates = 20) else it },
        )

    /** In-progress snapshot used by the match timeline. */
    val timeline = yourRoll.copy(phase = TurnPhase.END_TURN, xitterPlayerId = kim)
}
