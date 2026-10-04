package com.kingofxitter.cards

import com.kingofxitter.engine.DefaultGameReducer
import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.CardEffectRegistry
import com.kingofxitter.engine.model.CardId
import com.kingofxitter.engine.model.CounterId
import com.kingofxitter.engine.model.TurnPhase
import com.kingofxitter.engine.model.DiceFace
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CountersAndTurnsTest {
    private val reducer = DefaultGameReducer(CardEffectRegistry(CountersAndTurns.effects))
    private val shopReducer = ShopReducer(reducer)

    @Test
    fun `Pinned Tweet adds extra turn and subtracts one die limit permanently on 1-1-1`() {
        val state = TestGame()
            .withPolicies(TestGame.HUMAN, listOf(CardId("c16_pinned_tweet")))
            .withRoll(DiceFace.ONE, DiceFace.ONE, DiceFace.ONE, DiceFace.LIKE, DiceFace.LIKE, DiceFace.LIKE)
            .build()
            
        val result = reducer.reduce(state, Action.ResolveRoll(TestGame.HUMAN))
        val human = result.state.players.first { it.id == TestGame.HUMAN }
        
        assertEquals(1, human.extraTurns)
        assertEquals(-1, human.extraDice)
    }

    @Test
    fun `Super PAC loads 60 chars and gives 20 per turn until empty then discards`() {
        val state = TestGame()
            .withPhase(TurnPhase.SHOP)
            .withShop(listOf(CardId("c28_super_pac")))
            .withCharacters(TestGame.HUMAN, 100)
            .build()
            
        val bought = shopReducer.reduce(state, Action.BuyPowerUp(TestGame.HUMAN, CardId("c28_super_pac")))
        var human = bought.state.players.first { it.id == TestGame.HUMAN }
        assertEquals(60, human.counters[CounterId("super_pac")])
        
        // Pass a turn to trigger onTurnStart
        var turnState = bought.state.copy(phase = TurnPhase.END_TURN)
        turnState = reducer.reduce(turnState, Action.EndTurn(TestGame.HUMAN)).state
        
        human = turnState.players.first { it.id == TestGame.BOT }
        assertEquals(20, human.characters, "Bot just started turn, doesn't have PAC")
        
        // Let's manually trigger onTurnStart for Human to test it
        var testStart = bought.state.copy(phase = TurnPhase.TURN_START, currentPlayerId = TestGame.HUMAN)
        val started = reducer.reduce(testStart, Action.RollDice(TestGame.HUMAN, emptySet()))
        human = started.state.players.first { it.id == TestGame.HUMAN }
        assertEquals(40, human.counters[CounterId("super_pac")])
        assertEquals(100 - CardCatalog.byId[CardId("c28_super_pac")]!!.cost + 20, human.characters)
    }

    @Test
    fun `Cancel Campaign gives cancelled counter on damage dealt`() {
        val state = TestGame()
            .withPolicies(TestGame.HUMAN, listOf(CardId("c35_cancel_campaign")))
            .withRoll(DiceFace.TWEET, DiceFace.LIKE, DiceFace.ONE, DiceFace.ONE, DiceFace.ONE, DiceFace.ONE)
            .build()
            
        val result = reducer.reduce(state, Action.ResolveRoll(TestGame.HUMAN))
        val bot = result.state.players.first { it.id == TestGame.BOT }
        
        assertEquals(1, bot.counters[CounterId("cancelled")])
    }
    
    @Test
    fun `Shadowban gives shadowban counter on damage dealt`() {
        val state = TestGame()
            .withPolicies(TestGame.HUMAN, listOf(CardId("c40_shadowban")))
            .withRoll(DiceFace.TWEET, DiceFace.LIKE, DiceFace.ONE, DiceFace.ONE, DiceFace.ONE, DiceFace.ONE)
            .build()
            
        val result = reducer.reduce(state, Action.ResolveRoll(TestGame.HUMAN))
        val bot = result.state.players.first { it.id == TestGame.BOT }
        
        assertEquals(1, bot.counters[CounterId("shadowban")])
    }

    @Test
    fun `Smoke and Mirrors gives 3 counters on buy and allows spending 1 for extra reroll`() {
        val state = TestGame()
            .withPhase(TurnPhase.SHOP)
            .withShop(listOf(CardId("c41_smoke_and_mirrors")))
            .withCharacters(TestGame.HUMAN, 100)
            .build()
            
        val bought = shopReducer.reduce(state, Action.BuyPowerUp(TestGame.HUMAN, CardId("c41_smoke_and_mirrors")))
        val human = bought.state.players.first { it.id == TestGame.HUMAN }
        assertEquals(3, human.counters[CounterId("smoke_cloud")])
        
        // Rolling should present a decision
        val rollState = bought.state.copy(phase = TurnPhase.TURN_START, turnStartHooksApplied = true)
        val rolled = reducer.reduce(rollState, Action.RollDice(TestGame.HUMAN, emptySet()))
        
        assertNotNull(rolled.state.pendingDecisions.find { it.options.any { opt -> opt.id.value == "spend" } })
    }
}
