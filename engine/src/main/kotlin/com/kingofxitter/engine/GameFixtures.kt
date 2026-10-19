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
import com.kingofxitter.engine.model.MatchStats
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
                    if (player.id == kim) player.copy(policyIds = listOf(CardId("c03_alpha_poster"))) else player
                },
        )

    /** Compose-attack screen: your roll with three tweets at the Main Character. */
    val composeTweet get() = Design.compose

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
            shopCardIds = listOf(CardId("c15_quote_tweet"), CardId("c06_home_turf"), CardId("c09_press_office")),
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

    /** Victory by survival can occur below twenty mandates. */
    val survivorVictory =
        victory.copy(
            result = MatchResult.Victory(you, VictoryReason.LAST_ACCOUNT_STANDING),
            players = roster.map { if (it.id == you) it.copy(mandates = 4) else it.copy(followers = 0) },
        )

    /** A living player can lose when an opponent reaches twenty mandates. */
    val scoreDefeat = defeat.copy(players = defeat.players.map { if (it.id == you) it.copy(followers = 7) else it })

    /** Simultaneous elimination leaves no winner. */
    val noWinner =
        defeat.copy(
            result = MatchResult.NoWinner,
            players = roster.map { it.copy(followers = 0) },
        )

    /** In-match standings: `design/screens/standings.html`. */
    val liveStandings = Design.standings

    /** In-progress snapshot used by the match timeline. */
    val timeline = yourRoll.copy(phase = TurnPhase.END_TURN, xitterPlayerId = kim)

    /**
     * S6: exactly what each file in `design/screens/` draws, so a screen's preview can be checked
     * against its design side by side. You are Macaron, against Trumpet, Poutine and Kim, and every
     * card is a real catalogue card. The generic fixtures above stay as they are for the tests.
     */
    object Design {
        private val trumpet = PlayerId("trumpet")

        /** The design's line-up on turn 7 (`game-board-your-roll.html`). */
        private val leaders =
            listOf(
                PlayerState(
                    you,
                    LeaderId("macaron"),
                    followers = 7,
                    characters = 60,
                    mandates = 8,
                    extraRerolls = 1,
                    policyIds = listOf(CardId("c18_speechwriter"), CardId("c09_press_office")),
                ),
                PlayerState(trumpet, LeaderId("trumpet"), followers = 5, characters = 30, mandates = 13),
                PlayerState(poutine, LeaderId("poutine"), followers = 9, characters = 120, mandates = 4),
                PlayerState(kim, LeaderId("kim"), followers = 3, characters = 20, mandates = 16),
            )

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

        private fun dice(vararg faces: DiceFace) = faces.mapIndexed { index, face -> Die(DieId(index), face) }

        /** `game-board-your-roll.html`: roll 2 of 4 with four dice kept and Trumpet on Xitter. */
        val board =
            GameState(
                players = leaders,
                currentPlayerId = you,
                phase = TurnPhase.ROLLING,
                turnNumber = 7,
                xitterPlayerId = trumpet,
                dice =
                    dice(DiceFace.TWEET, DiceFace.TWEET, DiceFace.THREE, DiceFace.THREE, DiceFace.TWEET, DiceFace.CHARACTERS)
                        .mapIndexed { index, die -> die.copy(isKept = index < 4) },
                rollNumber = 2,
                turnRollLimit = 4,
                shopCardIds = listOf(CardId("c12_bot_farm"), CardId("c59_buy_followers"), CardId("c54_leaked_group_chat")),
                randomState = 7L,
            )

        /** `compose-your-attack-tweet.html`: the same roll's three tweets, aimed at Trumpet. */
        val compose = board

        /** `incoming-tweet-stay-or-log-off.html`: Trumpet's tweets and CAPS LOCK STUCK take you from 7M to 4M on Xitter. */
        val incoming =
            board.copy(
                players =
                    leaders.map { player ->
                        when (player.id) {
                            you -> player.copy(followers = 4)
                            trumpet -> player.copy(policyIds = listOf(CardId("c43_caps_lock_stuck")))
                            else -> player
                        }
                    },
                currentPlayerId = trumpet,
                phase = TurnPhase.DECISION,
                turnNumber = 8,
                xitterPlayerId = you,
                dice = dice(DiceFace.TWEET, DiceFace.TWEET, DiceFace.ONE, DiceFace.LIKE, DiceFace.TWO, DiceFace.CHARACTERS),
                rollNumber = 3,
                turnRollLimit = 3,
                pendingDecisions = listOf(stayOrLogOff(you)),
                phaseAfterDecisions = TurnPhase.SHOP,
            )

        /** `power-up-shop.html`: 60 characters, Speechwriter and Press Office held, three cards on the shelf. */
        val shop = board.copy(phase = TurnPhase.SHOP)

        /** `match-timeline.html`: turn 9, Macaron now on Xitter. */
        val timeline = board.copy(phase = TurnPhase.END_TURN, turnNumber = 9, xitterPlayerId = you)

        /** `standings.html`: turn 8, you on Xitter in third. */
        val standings =
            board.copy(
                turnNumber = 8,
                xitterPlayerId = you,
                players =
                    leaders.map { player ->
                        when (player.id) {
                            you -> player.copy(followers = 4, mandates = 12)
                            trumpet -> player.copy(followers = 2)
                            else -> player
                        }
                    },
                matchStats = mapOf(you to MatchStats(19, 23, 4)),
            )

        /** `victory.html`: you reach 20; Kim 18, Poutine 7, Trumpet suspended on 11. */
        val victory =
            board.copy(
                phase = TurnPhase.FINISHED,
                turnNumber = 14,
                xitterPlayerId = you,
                dice = emptyList(),
                result = MatchResult.Victory(you, VictoryReason.TWENTY_MANDATES),
                players =
                    leaders.map { player ->
                        when (player.id) {
                            you -> player.copy(mandates = 20)
                            kim -> player.copy(mandates = 18)
                            poutine -> player.copy(mandates = 7)
                            else -> player.copy(followers = 0, mandates = 11)
                        }
                    },
                matchStats = mapOf(you to MatchStats(19, 23, 4)),
            )

        /** `defeat-account-suspended.html`: you hit 0 followers on turn 12, and Kim wins on 20. */
        val defeat =
            board.copy(
                phase = TurnPhase.FINISHED,
                turnNumber = 12,
                xitterPlayerId = kim,
                dice = emptyList(),
                result = MatchResult.Victory(kim, VictoryReason.TWENTY_MANDATES),
                players =
                    leaders.map { player ->
                        when (player.id) {
                            you -> player.copy(followers = 0)
                            kim -> player.copy(mandates = 20)
                            else -> player
                        }
                    },
            )
    }
}
