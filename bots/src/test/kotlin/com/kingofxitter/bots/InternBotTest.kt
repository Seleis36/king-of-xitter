package com.kingofxitter.bots

import com.kingofxitter.engine.DefaultGameReducer
import com.kingofxitter.engine.GameFixtures
import com.kingofxitter.engine.MatchSetup
import com.kingofxitter.engine.SplitMix64
import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.GameEvent
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.LeaderId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InternBotTest {
    private val roster = listOf("trumpet", "poutine", "kim", "macaron", "xi", "mi-leon").map(::LeaderId)
    private val bot = InternBot()

    @Test
    fun `it only ever picks a legal move`() {
        val state = GameFixtures.yourRoll
        val legal = BotMoves.legalActions(state).toSet()
        val stream = SplitMix64(1)
        repeat(200) { assertTrue(bot.decide(state, null, stream) in legal) }
    }

    @Test
    fun `it answers the decision it is given`() {
        val state = GameFixtures.incomingTweet
        val decision = state.pendingDecisions.first()
        val action = bot.decide(state, decision, SplitMix64(3))
        assertTrue(action is Action.ChooseDecisionOption)
        assertEquals(decision.id, action.decisionId)
        assertTrue(action.optionId in decision.options.map { it.id })
    }

    @Test
    fun `a finished match gives it nothing to do`() {
        assertNull(bot.decide(GameFixtures.victory, null, SplitMix64(1)))
    }

    @Test
    fun `the same position and stream always give the same move`() {
        val state = GameFixtures.yourRoll
        assertEquals(
            List(20) { bot.decide(state, null, SplitMix64(9)) },
            List(20) { bot.decide(state, null, SplitMix64(9)) },
        )
    }

    @Test
    fun `it never shops without a market, and shops with one`() {
        val shop = GameFixtures.shop
        val stream = SplitMix64(5)
        repeat(50) { assertTrue(bot.decide(shop, null, stream) is Action.EndTurn) }
        val rich = shop.copy(players = shop.players.map { it.copy(characters = 100) })
        val shopper = InternBot(CardMarket { CardAppraisal(cost = 40, isPolicy = true) })
        val moves = List(60) { shopper.decide(rich, null, SplitMix64(it.toLong())) }
        assertTrue(moves.any { it is Action.BuyPowerUp })
        assertTrue(moves.any { it is Action.SweepShop })
    }

    @Test
    fun `four interns play a whole match to a result without an illegal move`() {
        // The walking skeleton of ROADMAP M1: base rules only, no power-ups in the deck.
        val reducer = DefaultGameReducer()
        var state = MatchSetup.create(roster.first(), roster, seed = 20_260_104L).state
        var steps = 0
        while (state.result == null) {
            val action = assertNotNull(bot.decide(state, state.pendingDecisions.firstOrNull(), botRandom(state)), describe(state))
            val transition = reducer.reduce(state, action)
            assertTrue(
                transition.events.none { it is GameEvent.ActionRejected },
                "$action rejected in ${describe(state)}: " +
                    transition.events.filterIsInstance<GameEvent.ActionRejected>().map(GameEvent.ActionRejected::reason),
            )
            assertTrue(transition.state != state, "$action changed nothing in ${describe(state)}")
            state = transition.state
            assertTrue(++steps < 20_000, "the match did not finish")
        }
        assertTrue(state.players.count { !it.isSuspended } <= 4)
    }

    @Test
    fun `interns finish every match across many seeds`() {
        val reducer = DefaultGameReducer()
        repeat(40) { seed ->
            var state = MatchSetup.create(roster[seed % roster.size], roster, seed.toLong()).state
            var steps = 0
            while (state.result == null && steps < 20_000) {
                val action = bot.decide(state, state.pendingDecisions.firstOrNull(), botRandom(state)) ?: break
                state = reducer.reduce(state, action).state
                steps++
            }
            assertNotNull(state.result, "seed $seed stalled after $steps steps in ${describe(state)}")
        }
    }

    private fun describe(state: GameState) =
        "phase=${state.phase} roll=${state.rollNumber} player=${state.currentPlayerId.value} " +
            "decisions=${state.pendingDecisions.map { it.kind }}"
}
