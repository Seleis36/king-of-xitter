package com.kingofxitter.bots

import com.kingofxitter.engine.GameFixtures
import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.CardId
import com.kingofxitter.engine.model.TurnPhase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BotMovesTest {
    private val market =
        CardMarket { id ->
            when (id.value) {
                "quote-tweet" -> CardAppraisal(cost = 40, isPolicy = true)
                "home-turf" -> CardAppraisal(cost = 50, isPolicy = true)
                else -> null
            }
        }

    @Test
    fun `a pending decision is the only thing on offer`() {
        val state = GameFixtures.incomingTweet
        val decision = state.pendingDecisions.first()
        val actions = BotMoves.legalActions(state, market = market)
        assertEquals(decision.options.size, actions.size)
        assertTrue(actions.all { it is Action.ChooseDecisionOption && it.decisionId == decision.id })
        assertEquals(decision.options.map { it.id }, actions.map { (it as Action.ChooseDecisionOption).optionId })
        assertEquals(decision.playerId, BotMoves.actingPlayerId(state))
    }

    @Test
    fun `a turn start only rolls`() {
        val state = GameFixtures.yourRoll.copy(phase = TurnPhase.TURN_START, rollNumber = 0, turnRollLimit = null)
        assertEquals(listOf(Action.RollDice(state.currentPlayerId)), BotMoves.legalActions(state, market = market))
    }

    @Test
    fun `rolling offers resolve plus one keep set per face subset`() {
        val state = GameFixtures.yourRoll
        val actions = BotMoves.legalActions(state, market = market)
        assertTrue(actions.first() is Action.ResolveRoll)
        val keeps = actions.filterIsInstance<Action.RollDice>().map { it.keptDice }
        // Five distinct faces are on the table, so 2^5 subsets minus "keep everything".
        assertEquals(31, keeps.size)
        assertEquals(keeps.size, keeps.toSet().size)
        assertTrue(keeps.first().isEmpty())
        assertTrue(keeps.none { it.size == state.dice.size })
    }

    @Test
    fun `the last roll can only resolve`() {
        val state = GameFixtures.yourRoll.copy(rollNumber = 3, turnRollLimit = 3)
        assertEquals(listOf(Action.ResolveRoll(state.currentPlayerId)), BotMoves.legalActions(state, market = market))
    }

    @Test
    fun `the shop offers affordable known cards, a sweep and ending the turn`() {
        val state = GameFixtures.shop
        val you = state.players.first { it.id == state.currentPlayerId }
        assertEquals(30, you.characters)
        val actions = BotMoves.legalActions(state, market = market)
        // Quote Tweet costs 40 and Home Turf 50, so only the sweep and EndTurn are affordable.
        assertEquals(listOf(Action.SweepShop(you.id), Action.EndTurn(you.id)), actions)
        val rich = state.copy(players = state.players.map { if (it.id == you.id) it.copy(characters = 60) else it })
        assertTrue(Action.BuyPowerUp(you.id, CardId("quote-tweet")) in BotMoves.legalActions(rich, market = market))
    }

    @Test
    fun `an unknown market switches shopping off so the base reducer never rejects a bot`() {
        val actions = BotMoves.legalActions(GameFixtures.shop)
        assertEquals(listOf(Action.EndTurn(GameFixtures.shop.currentPlayerId)), actions)
    }

    @Test
    fun `a finished match offers nothing`() {
        assertEquals(emptyList<Action>(), BotMoves.legalActions(GameFixtures.victory, market = market))
        assertNull(BotMoves.actingPlayerId(GameFixtures.victory))
    }

    @Test
    fun `the bot stream is reproducible and leaves the reducer's stream alone`() {
        val state = GameFixtures.yourRoll
        val first = botRandom(state).let { stream -> List(5) { stream.nextInt(100) } }
        val second = botRandom(state).let { stream -> List(5) { stream.nextInt(100) } }
        assertEquals(first, second)
        assertEquals(42L, state.randomState)
    }
}
