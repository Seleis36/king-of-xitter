package com.kingofxitter.bots

import com.kingofxitter.engine.GameReducer
import com.kingofxitter.engine.GameTransition
import com.kingofxitter.engine.model.ActionRejectionReason
import com.kingofxitter.engine.model.GameEvent
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SimulatorTest {
    private val fast = listOf(BotDifficulty.INTERN, BotDifficulty.REPLY_GUY)

    @Test
    fun `a run completes every match and the totals add up`() {
        val report = Simulator.run(SimulationConfig(games = 30, difficulties = fast))
        assertEquals(emptyList(), report.failures)
        assertEquals(30, report.games)
        // Four seats a match, each one counted once.
        assertEquals(120, report.seatsByDifficulty.values.sum())
        assertEquals(120, report.seatsByLeader.values.sum())
        assertEquals(report.games - report.noWinner, report.winsByDifficulty.values.sum())
        assertEquals(report.winsByDifficulty.values.sum(), report.winsByLeader.values.sum())
        assertTrue(report.averageTurns > 1, "${report.averageTurns}")
    }

    @Test
    fun `the same configuration always produces the same report`() {
        val config = SimulationConfig(games = 15, seed = 404, difficulties = fast)
        assertEquals(Simulator.run(config), Simulator.run(config))
    }

    @Test
    fun `win rates are per seat, so turn order cannot skew a difficulty`() {
        val report = Simulator.run(SimulationConfig(games = 40, difficulties = fast))
        // With two brains over four seats each takes exactly two seats a match.
        assertEquals(80, report.seatsByDifficulty[BotDifficulty.INTERN])
        assertEquals(80, report.seatsByDifficulty[BotDifficulty.REPLY_GUY])
        assertTrue(report.winRate(BotDifficulty.REPLY_GUY) > report.winRate(BotDifficulty.INTERN))
        assertTrue(BotDifficulty.entries.all { report.winRate(it) in 0.0..1.0 })
    }

    @Test
    fun `the difficulty ladder really is a ladder`() {
        val report = Simulator.run(SimulationConfig(games = 12, difficulties = BotDifficulty.entries))
        assertEquals(emptyList(), report.failures)
        assertTrue(
            report.winRate(BotDifficulty.TROLL_FARM) > report.winRate(BotDifficulty.INTERN),
            "troll ${report.winRate(BotDifficulty.TROLL_FARM)} vs intern ${report.winRate(BotDifficulty.INTERN)}",
        )
    }

    @Test
    fun `a broken engine is reported with the seed that reproduces it, not thrown`() {
        val rejecting =
            GameReducer {
                state,
                action,
                ->
                GameTransition(state, listOf(GameEvent.ActionRejected(action.playerId, ActionRejectionReason.WRONG_PHASE)))
            }
        val report = Simulator.run(SimulationConfig(games = 3, seed = 77, difficulties = fast)) { rejecting }
        assertEquals(0, report.games)
        assertEquals(3, report.failures.size)
        assertEquals(listOf(77L, 78L, 79L), report.failures.map { it.seed })
        assertContains(report.failures.first().reason, "rejected")
    }

    @Test
    fun `a match that never finishes is reported as stuck instead of looping`() {
        val stalling = GameReducer { state, _ -> GameTransition(state, emptyList()) }
        val report = Simulator.run(SimulationConfig(games = 1, difficulties = fast, actionLimit = 5)) { stalling }
        assertEquals(1, report.failures.size)
        assertContains(report.failures.single().reason, "no progress")
    }

    @Test
    fun `a brain that throws is caught and reported`() {
        val throwing = GameReducer { _, _ -> error("engine exploded") }
        val report = Simulator.run(SimulationConfig(games = 1, difficulties = fast)) { throwing }
        assertContains(report.failures.single().reason, "engine exploded")
    }

    @Test
    fun `the config refuses anything but the six leaders`() {
        assertFailsWith<IllegalArgumentException> { SimulationConfig(leaders = DEFAULT_LEADERS.take(5)) }
        assertFailsWith<IllegalArgumentException> { SimulationConfig(games = 0) }
        assertFailsWith<IllegalArgumentException> { SimulationConfig(difficulties = emptyList()) }
    }

    @Test
    fun `every difficulty maps to the brain that implements it`() {
        BotDifficulty.entries.forEach { difficulty ->
            assertEquals(difficulty, LeaderBots(difficulty).brain(DEFAULT_LEADERS.first()).difficulty)
        }
    }

    @Test
    fun `the report reads as the CLI prints it`() {
        val config = SimulationConfig(games = 6, difficulties = fast)
        val text = format(config, Simulator.run(config), seconds = 1.5)
        assertContains(text, "games        6 of 6 completed")
        assertContains(text, "reply_guy")
        assertContains(text, "trumpet")
        assertContains(text, "no failures")
    }

    @Test
    fun `failures are printed with their seeds`() {
        val rejecting =
            GameReducer {
                state,
                action,
                ->
                GameTransition(state, listOf(GameEvent.ActionRejected(action.playerId, ActionRejectionReason.WRONG_PHASE)))
            }
        val config = SimulationConfig(games = 2, seed = 9, difficulties = fast)
        val text = format(config, Simulator.run(config) { rejecting }, seconds = 0.1)
        assertContains(text, "FAILURES (2)")
        assertContains(text, "seed 9:")
    }
}
