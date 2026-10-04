package com.kingofxitter.cards

import com.kingofxitter.engine.DefaultGameReducer
import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.CardEffectRegistry
import com.kingofxitter.engine.model.CardId
import com.kingofxitter.engine.model.CounterId
import com.kingofxitter.engine.model.DiceFace
import com.kingofxitter.engine.model.EXTRA_TURN_DICE_PENALTY
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.PlayerId
import com.kingofxitter.engine.model.TurnPhase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CountersAndTurnsTest {
    private val reducer = DefaultGameReducer(CardEffectRegistry(CountersAndTurns.effects))
    private val shopReducer = ShopReducer(reducer)

    private fun GameState.player(id: PlayerId) = players.first { it.id == id }

    private fun tweetState(policy: String) =
        TestGame()
            .withPolicy(TestGame.HUMAN, CardId(policy))
            .withXitter(TestGame.BOT)
            .withDice(DiceFace.TWEET, DiceFace.ONE, DiceFace.TWO, DiceFace.TWO, DiceFace.THREE, DiceFace.THREE)
            .build()

    @Test
    fun `Pinned Tweet gives an extra turn with one fewer die, then the penalty ends`() {
        val state =
            TestGame()
                .withPolicy(TestGame.HUMAN, CardId("c16_pinned_tweet"))
                .withDice(DiceFace.ONE, DiceFace.ONE, DiceFace.ONE, DiceFace.LIKE, DiceFace.LIKE, DiceFace.LIKE)
                .build()

        val resolved = reducer.reduce(state, Action.ResolveRoll(TestGame.HUMAN)).state
        assertEquals(1, resolved.player(TestGame.HUMAN).extraTurns)

        val extraTurn = reducer.reduce(resolved.copy(phase = TurnPhase.END_TURN), Action.EndTurn(TestGame.HUMAN)).state
        assertEquals(TestGame.HUMAN, extraTurn.currentPlayerId)
        assertEquals(0, extraTurn.player(TestGame.HUMAN).extraTurns)

        val rolled = reducer.reduce(extraTurn, Action.RollDice(TestGame.HUMAN, emptySet())).state
        assertEquals(5, rolled.dice.size)

        val afterExtra = reducer.reduce(rolled.copy(phase = TurnPhase.END_TURN), Action.EndTurn(TestGame.HUMAN)).state
        assertEquals(TestGame.BOT, afterExtra.currentPlayerId)
        assertNull(afterExtra.player(TestGame.HUMAN).counters[CounterId(EXTRA_TURN_DICE_PENALTY)])
    }

    @Test
    fun `Pinned Tweet does nothing without three 1s`() {
        val state =
            TestGame()
                .withPolicy(TestGame.HUMAN, CardId("c16_pinned_tweet"))
                .withDice(DiceFace.ONE, DiceFace.ONE, DiceFace.TWO, DiceFace.LIKE, DiceFace.LIKE, DiceFace.LIKE)
                .build()

        val resolved = reducer.reduce(state, Action.ResolveRoll(TestGame.HUMAN)).state

        assertEquals(0, resolved.player(TestGame.HUMAN).extraTurns)
    }

    @Test
    fun `Super PAC loads 60 chars when bought`() {
        val state =
            TestGame()
                .withPhase(TurnPhase.SHOP)
                .withShop(listOf(CardId("c28_super_pac")))
                .withCharacters(TestGame.HUMAN, 100)
                .build()

        val bought = shopReducer.reduce(state, Action.BuyPowerUp(TestGame.HUMAN, CardId("c28_super_pac"))).state

        assertEquals(60, bought.player(TestGame.HUMAN).counters[CounterId("super_pac")])
    }

    @Test
    fun `Super PAC pays 20 chars at the start of each turn and discards itself when empty`() {
        var state =
            TestGame()
                .withPhase(TurnPhase.TURN_START)
                .withRollNumber(0)
                .withPolicy(TestGame.HUMAN, CardId("c28_super_pac"))
                .withCharacters(TestGame.HUMAN, 0)
                .build()
        state =
            state.copy(
                players =
                    state.players.map {
                        if (it.id == TestGame.HUMAN) it.copy(counters = mapOf(CounterId("super_pac") to 40)) else it
                    },
            )

        val first = reducer.reduce(state, Action.RollDice(TestGame.HUMAN, emptySet())).state
        assertEquals(20, first.player(TestGame.HUMAN).characters)
        assertEquals(20, first.player(TestGame.HUMAN).counters[CounterId("super_pac")])
        assertTrue(CardId("c28_super_pac") in first.player(TestGame.HUMAN).policyIds)

        val secondStart =
            first.copy(
                phase = TurnPhase.TURN_START,
                rollNumber = 0,
                dice = emptyList(),
                turnStartHooksApplied = false,
                turnDiceCount = null,
                turnRollLimit = null,
            )
        val second = reducer.reduce(secondStart, Action.RollDice(TestGame.HUMAN, emptySet())).state
        assertEquals(40, second.player(TestGame.HUMAN).characters)
        assertTrue(CardId("c28_super_pac") !in second.player(TestGame.HUMAN).policyIds)
    }

    @Test
    fun `Cancel Campaign gives a cancelled counter to a leader it hurts`() {
        val result = reducer.reduce(tweetState("c35_cancel_campaign"), Action.ResolveRoll(TestGame.HUMAN))

        assertEquals(1, result.state.player(TestGame.BOT).counters[CounterId("cancelled")])
    }

    @Test
    fun `A cancelled leader loses 1M per counter at the end of their turn`() {
        val base =
            TestGame()
                .withPhase(TurnPhase.END_TURN)
                .withFollowers(TestGame.BOT, 10)
                .withCurrentPlayer(TestGame.BOT)
                .build()
        val state =
            base.copy(
                players =
                    base.players.map {
                        if (it.id == TestGame.BOT) it.copy(counters = mapOf(CounterId("cancelled") to 2)) else it
                    },
            )

        val result = reducer.reduce(state, Action.EndTurn(TestGame.BOT)).state

        assertEquals(8, result.player(TestGame.BOT).followers)
    }

    @Test
    fun `Shadowban gives a shadowban counter to a leader it hurts`() {
        val result = reducer.reduce(tweetState("c40_shadowban"), Action.ResolveRoll(TestGame.HUMAN))

        assertEquals(1, result.state.player(TestGame.BOT).counters[CounterId("shadowban")])
    }

    @Test
    fun `A shadowbanned leader rolls one fewer die per counter`() {
        val base =
            TestGame()
                .withPhase(TurnPhase.ROLLING)
                .withRollNumber(0)
                .withCurrentPlayer(TestGame.BOT)
                .build()
        val state =
            base.copy(
                players =
                    base.players.map {
                        if (it.id == TestGame.BOT) it.copy(counters = mapOf(CounterId("shadowban") to 2)) else it
                    },
            )

        val rolled = reducer.reduce(state, Action.RollDice(TestGame.BOT, emptySet())).state

        assertEquals(4, rolled.dice.size)
    }

    @Test
    fun `Smoke and Mirrors starts with 3 counters`() {
        val state =
            TestGame()
                .withPhase(TurnPhase.SHOP)
                .withShop(listOf(CardId("c41_smoke_and_mirrors")))
                .withCharacters(TestGame.HUMAN, 100)
                .build()

        val bought = shopReducer.reduce(state, Action.BuyPowerUp(TestGame.HUMAN, CardId("c41_smoke_and_mirrors"))).state

        assertEquals(3, bought.player(TestGame.HUMAN).counters[CounterId("smoke_cloud")])
    }

    @Test
    fun `Smoke and Mirrors spends a counter for one extra reroll this turn only`() {
        val base =
            TestGame()
                .withPhase(TurnPhase.ROLLING)
                .withRollNumber(0)
                .withPolicy(TestGame.HUMAN, CardId("c41_smoke_and_mirrors"))
                .build()
        val state =
            base.copy(
                players =
                    base.players.map {
                        if (it.id == TestGame.HUMAN) it.copy(counters = mapOf(CounterId("smoke_cloud") to 3)) else it
                    },
            )
        val rolled = reducer.reduce(state, Action.RollDice(TestGame.HUMAN, emptySet())).state
        val decision = rolled.pendingDecisions.first { d -> d.options.any { it.id.value == "spend" } }
        val limitBefore = rolled.turnRollLimit

        val spent =
            reducer
                .reduce(
                    rolled,
                    Action.ChooseDecisionOption(TestGame.HUMAN, decision.id, decision.options.first { it.id.value == "spend" }.id),
                ).state

        assertEquals(2, spent.player(TestGame.HUMAN).counters[CounterId("smoke_cloud")])
        assertEquals(limitBefore!! + 1, spent.turnRollLimit)
        assertEquals(0, spent.player(TestGame.HUMAN).extraRerolls)
        assertNotNull(spent)
    }
}
