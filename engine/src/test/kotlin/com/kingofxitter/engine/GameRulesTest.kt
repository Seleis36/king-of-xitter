package com.kingofxitter.engine

import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.CardEffect
import com.kingofxitter.engine.model.CardEffectIntent
import com.kingofxitter.engine.model.CardEffectRegistry
import com.kingofxitter.engine.model.CardHookContext
import com.kingofxitter.engine.model.CardId
import com.kingofxitter.engine.model.DecisionOptionId
import com.kingofxitter.engine.model.DiceFace
import com.kingofxitter.engine.model.Die
import com.kingofxitter.engine.model.DieId
import com.kingofxitter.engine.model.GameEvent
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.LeaderId
import com.kingofxitter.engine.model.MatchResult
import com.kingofxitter.engine.model.MatchStats
import com.kingofxitter.engine.model.PlayerId
import com.kingofxitter.engine.model.PlayerResource
import com.kingofxitter.engine.model.PlayerState
import com.kingofxitter.engine.model.ResourceChangeReason
import com.kingofxitter.engine.model.TurnPhase
import com.kingofxitter.engine.model.VictoryReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GameRulesTest {
    private val first = PlayerId("first")
    private val second = PlayerId("second")
    private val third = PlayerId("third")

    @Test
    fun `seeded roll creates six dice reproducibly and allows only three rolls`() {
        val initial = game(TurnPhase.TURN_START)
        val reducer = DefaultGameReducer()

        val firstRun = reducer.reduce(initial, Action.RollDice(first))
        val repeatRun = reducer.reduce(initial, Action.RollDice(first))

        assertEquals(firstRun, repeatRun)
        assertEquals(6, firstRun.state.dice.size)
        var current = firstRun.state
        repeat(2) {
            current = reducer.reduce(current, Action.RollDice(first, current.dice.map { it.id }.toSet())).state
        }
        val rejected = reducer.reduce(current, Action.RollDice(first, current.dice.map { it.id }.toSet()))
        assertEquals(current, rejected.state)
        assertIs<GameEvent.ActionRejected>(rejected.events.single())
    }

    @Test
    fun `four of a kind scores one plus each die beyond three`() {
        val transition =
            DefaultGameReducer().reduce(
                game(
                    TurnPhase.ROLLING,
                    xitter = second,
                    dice =
                        listOf(
                            DiceFace.ONE,
                            DiceFace.ONE,
                            DiceFace.ONE,
                            DiceFace.ONE,
                            DiceFace.LIKE,
                            DiceFace.CHARACTERS,
                        ),
                ),
                Action.ResolveRoll(first),
            )

        assertEquals(
            2,
            transition.state.players
                .first { it.id == first }
                .mandates,
        )
        assertEquals(
            10,
            transition.state.players
                .first { it.id == first }
                .characters,
        )
    }

    @Test
    fun `three of a kind scores its face value for twos and threes`() {
        val transition =
            DefaultGameReducer().reduce(
                game(
                    TurnPhase.ROLLING,
                    xitter = second,
                    dice = listOf(DiceFace.TWO, DiceFace.TWO, DiceFace.TWO, DiceFace.THREE, DiceFace.THREE, DiceFace.THREE),
                ),
                Action.ResolveRoll(first),
            )

        assertEquals(
            5,
            transition.state.players
                .first { it.id == first }
                .mandates,
        )
    }

    @Test
    fun `dice resolution applies numbers then likes then characters then tweets`() {
        val transition =
            DefaultGameReducer().reduce(
                game(
                    TurnPhase.ROLLING,
                    xitter = second,
                    followers = 8,
                    dice =
                        listOf(
                            DiceFace.ONE,
                            DiceFace.ONE,
                            DiceFace.ONE,
                            DiceFace.LIKE,
                            DiceFace.CHARACTERS,
                            DiceFace.TWEET,
                        ),
                ),
                Action.ResolveRoll(first),
            )
        val resourceEvents = transition.events.filterIsInstance<GameEvent.ResourceChanged>()

        assertEquals(
            listOf(
                ResourceChangeReason.OTHER,
                ResourceChangeReason.LIKE,
                ResourceChangeReason.CHARACTERS_FACE,
                ResourceChangeReason.TWEET,
            ),
            resourceEvents.map { it.reason },
        )
        assertEquals(1, transition.state.matchStats[first]?.tweetsSent)
        assertEquals(1, transition.state.matchStats[first]?.unfollowsDealt)
    }

    @Test
    fun `likes heal to the cap but do not heal the Main Character`() {
        val dice = listOf(DiceFace.LIKE, DiceFace.LIKE, DiceFace.ONE, DiceFace.ONE, DiceFace.ONE, DiceFace.TWEET)
        val injured = game(TurnPhase.ROLLING, dice = dice, followers = 9)
        val healed = DefaultGameReducer().reduce(injured, Action.ResolveRoll(first))
        assertEquals(
            10,
            healed.state.players
                .first { it.id == first }
                .followers,
        )

        val mainCharacter = injured.copy(xitterPlayerId = first)
        val unchanged = DefaultGameReducer().reduce(mainCharacter, Action.ResolveRoll(first))
        assertEquals(
            9,
            unchanged.state.players
                .first { it.id == first }
                .followers,
        )
    }

    @Test
    fun `Main Character tweets hit all opponents and entry scores one mandate`() {
        val attackers =
            game(
                TurnPhase.ROLLING,
                dice = listOf(DiceFace.TWEET, DiceFace.TWEET, DiceFace.ONE, DiceFace.ONE, DiceFace.ONE, DiceFace.LIKE),
                xitter = first,
            )
        val attacked = DefaultGameReducer().reduce(attackers, Action.ResolveRoll(first))
        assertEquals(
            8,
            attacked.state.players
                .first { it.id == second }
                .followers,
        )
        assertEquals(
            8,
            attacked.state.players
                .first { it.id == third }
                .followers,
        )

        val emptyXitter =
            game(
                TurnPhase.ROLLING,
                dice = listOf(DiceFace.ONE, DiceFace.ONE, DiceFace.ONE, DiceFace.CHARACTERS, DiceFace.CHARACTERS, DiceFace.CHARACTERS),
            )
        val entered = DefaultGameReducer().reduce(emptyXitter, Action.ResolveRoll(first))
        assertEquals(first, entered.state.xitterPlayerId)
        assertEquals(
            2,
            entered.state.players
                .first { it.id == first }
                .mandates,
        )
        assertTrue(entered.events.any { it == GameEvent.MainCharacterChanged(null, first) })
    }

    @Test
    fun `non Main Character tweets only hit the Main Character`() {
        val initial =
            game(
                TurnPhase.ROLLING,
                current = second,
                xitter = first,
                dice = listOf(DiceFace.TWEET, DiceFace.TWEET, DiceFace.ONE, DiceFace.ONE, DiceFace.ONE, DiceFace.LIKE),
            )

        val transition = DefaultGameReducer().reduce(initial, Action.ResolveRoll(second))

        assertEquals(
            8,
            transition.state.players
                .first { it.id == first }
                .followers,
        )
        assertEquals(
            10,
            transition.state.players
                .first { it.id == third }
                .followers,
        )
        assertEquals(1, transition.state.pendingDecisions.size)
    }

    @Test
    fun `logging off after a hit transfers Xitter to the attacker`() {
        val initial =
            game(
                TurnPhase.ROLLING,
                current = second,
                xitter = first,
                dice = listOf(DiceFace.TWEET, DiceFace.ONE, DiceFace.ONE, DiceFace.ONE, DiceFace.LIKE, DiceFace.LIKE),
            )
        val reducer = DefaultGameReducer()
        val hit = reducer.reduce(initial, Action.ResolveRoll(second))
        val decision = hit.state.pendingDecisions.single()

        val loggedOff =
            reducer.reduce(
                hit.state,
                Action.ChooseDecisionOption(first, decision.id, DecisionOptionId("log-off")),
            )

        assertEquals(second, loggedOff.state.xitterPlayerId)
        assertTrue(loggedOff.events.any { it == GameEvent.MainCharacterChanged(first, second) })
    }

    @Test
    fun `starting on Xitter gains two mandates and suspended players are skipped`() {
        val start = game(TurnPhase.TURN_START, current = first, xitter = first, mandates = 4)
        val reducer = DefaultGameReducer()
        val started = reducer.reduce(start, Action.RollDice(first))
        assertEquals(
            6,
            started.state.players
                .first { it.id == first }
                .mandates,
        )

        val ending =
            game(TurnPhase.END_TURN).copy(
                players =
                    listOf(
                        PlayerState(first, LeaderId("first")),
                        PlayerState(second, LeaderId("second"), followers = 0),
                        PlayerState(third, LeaderId("third")),
                    ),
            )
        val next = reducer.reduce(ending, Action.EndTurn(first))
        assertEquals(third, next.state.currentPlayerId)
    }

    @Test
    fun `reach twenty mandates to win`() {
        val initial =
            game(
                TurnPhase.ROLLING,
                mandates = 18,
                dice = List(6) { DiceFace.ONE },
            )

        val transition = DefaultGameReducer().reduce(initial, Action.ResolveRoll(first))

        assertEquals(MatchResult.Victory(first, VictoryReason.TWENTY_MANDATES), transition.state.result)
        assertEquals(TurnPhase.FINISHED, transition.state.phase)
    }

    @Test
    fun `last account standing wins when the Main Character is suspended`() {
        val initial =
            game(
                TurnPhase.ROLLING,
                current = second,
                xitter = first,
                secondFollowers = 10,
                firstFollowers = 1,
                thirdFollowers = 0,
                dice = listOf(DiceFace.TWEET, DiceFace.ONE, DiceFace.ONE, DiceFace.ONE, DiceFace.LIKE, DiceFace.LIKE),
            )

        val transition = DefaultGameReducer().reduce(initial, Action.ResolveRoll(second))

        assertEquals(MatchResult.Victory(second, VictoryReason.LAST_ACCOUNT_STANDING), transition.state.result)
        assertEquals(second, transition.state.xitterPlayerId)
    }

    @Test
    fun `simultaneous elimination produces no winner`() {
        val cardId = CardId("self-elimination")
        val selfEliminatingEffect =
            object : CardEffect {
                override val cardId = cardId

                override fun onResolve(context: CardHookContext) =
                    listOf(
                        CardEffectIntent.ChangeResource(
                            second,
                            PlayerResource.FOLLOWERS,
                            -10,
                            ResourceChangeReason.POWER_UP,
                        ),
                        CardEffectIntent.ChangeResource(
                            first,
                            PlayerResource.FOLLOWERS,
                            -1,
                            ResourceChangeReason.POWER_UP,
                        ),
                    )
            }
        val initial =
            game(
                TurnPhase.ROLLING,
                current = first,
                xitter = first,
                firstFollowers = 1,
                secondFollowers = 1,
                thirdFollowers = 0,
                dice = listOf(DiceFace.TWEET, DiceFace.ONE, DiceFace.ONE, DiceFace.ONE, DiceFace.LIKE, DiceFace.LIKE),
                policies = listOf(cardId),
            )

        val transition =
            DefaultGameReducer(CardEffectRegistry(listOf(selfEliminatingEffect))).reduce(initial, Action.ResolveRoll(first))

        assertEquals(MatchResult.NoWinner, transition.state.result)
    }

    @Test
    fun `recorded seeds complete reproducible four player games without purchases`() {
        val firstRun = playGame(seed = 20260926L)
        val repeatRun = playGame(seed = 20260926L)
        val secondRun = playGame(seed = 20260927L)
        val thirdRun = playGame(seed = 20260928L)

        assertEquals(firstRun, repeatRun)
        assertGoldenGame(
            firstRun,
            MatchResult.Victory(second, VictoryReason.TWENTY_MANDATES),
            31,
            mapOf(
                first to MatchStats(tweetsSent = 4, unfollowsDealt = 12, turnsAsMainCharacter = 3),
                second to MatchStats(tweetsSent = 12, unfollowsDealt = 17, turnsAsMainCharacter = 6),
                third to MatchStats(tweetsSent = 11, unfollowsDealt = 11),
                PlayerId("fourth") to MatchStats(tweetsSent = 1, unfollowsDealt = 1),
            ),
        )
        assertGoldenGame(
            secondRun,
            MatchResult.Victory(PlayerId("fourth"), VictoryReason.TWENTY_MANDATES),
            29,
            mapOf(
                first to MatchStats(turnsAsMainCharacter = 2),
                second to MatchStats(tweetsSent = 5, unfollowsDealt = 5),
                third to MatchStats(tweetsSent = 8, unfollowsDealt = 8),
                PlayerId("fourth") to MatchStats(tweetsSent = 7, unfollowsDealt = 9, turnsAsMainCharacter = 6),
            ),
        )
        assertGoldenGame(
            thirdRun,
            MatchResult.Victory(third, VictoryReason.TWENTY_MANDATES),
            44,
            mapOf(
                first to MatchStats(tweetsSent = 2, unfollowsDealt = 6, turnsAsMainCharacter = 3),
                second to MatchStats(tweetsSent = 10, unfollowsDealt = 15, turnsAsMainCharacter = 4),
                third to MatchStats(tweetsSent = 19, unfollowsDealt = 19, turnsAsMainCharacter = 9),
                PlayerId("fourth") to MatchStats(tweetsSent = 13, unfollowsDealt = 13),
            ),
        )
    }

    private fun playGame(seed: Long): GameState {
        val ids = listOf(first, second, third, PlayerId("fourth"))
        val reducer = DefaultGameReducer()
        var state =
            GameState(
                players = ids.map { id -> PlayerState(id, LeaderId(id.value)) },
                currentPlayerId = first,
                phase = TurnPhase.TURN_START,
                randomState = seed,
            )
        var actionCount = 0
        while (state.result == null && actionCount++ < 10_000) {
            val transition =
                when (state.phase) {
                    TurnPhase.TURN_START -> reducer.reduce(state, Action.RollDice(state.currentPlayerId))
                    TurnPhase.ROLLING -> {
                        if (state.rollNumber < 3) {
                            reducer.reduce(state, Action.RollDice(state.currentPlayerId))
                        } else {
                            reducer.reduce(state, Action.ResolveRoll(state.currentPlayerId))
                        }
                    }
                    TurnPhase.DECISION -> {
                        val decision = state.pendingDecisions.first()
                        val option = decision.options.first()
                        reducer.reduce(
                            state,
                            Action.ChooseDecisionOption(decision.playerId, decision.id, option.id),
                        )
                    }
                    TurnPhase.SHOP -> GameTransition(state.copy(phase = TurnPhase.END_TURN), emptyList())
                    TurnPhase.END_TURN -> reducer.reduce(state, Action.EndTurn(state.currentPlayerId))
                    TurnPhase.RESOLVING,
                    TurnPhase.SETUP,
                    TurnPhase.FINISHED,
                    -> error("Unexpected phase ${state.phase} in the recorded game")
                }
            state = transition.state
        }
        check(state.result != null) { "Recorded game did not finish within the action limit" }
        return state
    }

    private fun assertGoldenGame(
        state: GameState,
        result: MatchResult,
        turnNumber: Int,
        stats: Map<PlayerId, MatchStats>,
    ) {
        assertEquals(result, state.result)
        assertEquals(turnNumber, state.turnNumber)
        assertEquals(stats, state.matchStats)
    }

    private fun resolve(faces: List<DiceFace>) =
        DefaultGameReducer().reduce(game(TurnPhase.ROLLING, dice = faces), Action.ResolveRoll(first))

    private fun game(
        phase: TurnPhase,
        current: PlayerId = first,
        xitter: PlayerId? = null,
        dice: List<DiceFace> = emptyList(),
        followers: Int = 10,
        firstFollowers: Int = followers,
        secondFollowers: Int = 10,
        thirdFollowers: Int = 10,
        mandates: Int = 0,
        policies: List<CardId> = emptyList(),
    ) = GameState(
        players =
            listOf(
                PlayerState(first, LeaderId("first"), followers = firstFollowers, mandates = mandates, policyIds = policies),
                PlayerState(second, LeaderId("second"), followers = secondFollowers),
                PlayerState(third, LeaderId("third"), followers = thirdFollowers),
            ),
        currentPlayerId = current,
        phase = phase,
        turnNumber = 1,
        xitterPlayerId = xitter,
        dice = dice.mapIndexed { index, face -> Die(DieId(index), face) },
        rollNumber = if (phase == TurnPhase.ROLLING) 1 else 0,
        randomState = 20260926L,
    )
}
