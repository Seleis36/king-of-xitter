package com.kingofxitter.cards

import com.kingofxitter.engine.DefaultGameReducer
import com.kingofxitter.engine.GameTransition
import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.ActionRejectionReason
import com.kingofxitter.engine.model.CardId
import com.kingofxitter.engine.model.GameEvent
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.PlayerResource
import com.kingofxitter.engine.model.ResourceChangeReason
import com.kingofxitter.engine.model.TurnPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShopReducerTest {

    private val reducer = ShopReducer(DefaultGameReducer())
    
    // Some cards from CardCatalog for testing
    private val policyCard = CardId("c01_doomscroll_drip") // Doomscroll Drip, cost 60, POLICY
    private val executiveOrderCard = CardId("c49_op_ed") // Op-Ed, cost 50, EXECUTIVE_ORDER
    private val cheapCard = CardId("c25_leaked_memo") // Leaked Memo, cost 20, POLICY
    private val expensiveCard = CardId("c27_copycat_policy") // Copycat Policy, cost 80, POLICY

    @Test
    fun `setupDeck shuffles and deals 3 to shop`() {
        val allCards = listOf(policyCard, executiveOrderCard, cheapCard, expensiveCard)
        val state = TestGame().build()
        val newState = ShopLogic.setupDeck(state, allCards, seed = 42L)

        assertEquals(3, newState.shopCardIds.size)
        assertEquals(1, newState.drawPileCardIds.size)
        assertEquals(0, newState.discardPileCardIds.size)
        
        // Assert all cards are accounted for
        val total = newState.shopCardIds + newState.drawPileCardIds
        assertTrue(total.containsAll(allCards))
    }

    @Test
    fun `buyPowerUp rejects if not SHOP phase`() {
        val state = TestGame()
            .withPhase(TurnPhase.ROLLING)
            .withShop(policyCard)
            .withCharacters(TestGame.HUMAN, 100)
            .build()
        val action = Action.BuyPowerUp(TestGame.HUMAN, policyCard)
        
        val transition = reducer.reduce(state, action)
        assertEquals(1, transition.events.size)
        assertEquals(GameEvent.ActionRejected(TestGame.HUMAN, ActionRejectionReason.WRONG_PHASE), transition.events[0])
    }
    
    @Test
    fun `buyPowerUp rejects if not enough characters`() {
        val state = TestGame()
            .withPhase(TurnPhase.SHOP)
            .withShop(policyCard) // cost 60
            .withCharacters(TestGame.HUMAN, 50)
            .build()
        val action = Action.BuyPowerUp(TestGame.HUMAN, policyCard)
        
        val transition = reducer.reduce(state, action)
        assertEquals(1, transition.events.size)
        assertEquals(GameEvent.ActionRejected(TestGame.HUMAN, ActionRejectionReason.INVALID_SELECTION), transition.events[0])
    }
    
    @Test
    fun `buyPowerUp rejects if card not in shop`() {
        val state = TestGame()
            .withPhase(TurnPhase.SHOP)
            .withShop(policyCard)
            .withCharacters(TestGame.HUMAN, 100)
            .build()
        val action = Action.BuyPowerUp(TestGame.HUMAN, expensiveCard) // Not in shop
        
        val transition = reducer.reduce(state, action)
        assertEquals(1, transition.events.size)
        assertEquals(GameEvent.ActionRejected(TestGame.HUMAN, ActionRejectionReason.INVALID_SELECTION), transition.events[0])
    }

    @Test
    fun `buyPowerUp policy subtracts cost, adds to policies, and refills shop`() {
        val state = TestGame()
            .withPhase(TurnPhase.SHOP)
            .withShop(policyCard, cheapCard)
            .withDrawPile(expensiveCard, executiveOrderCard)
            .withCharacters(TestGame.HUMAN, 100)
            .build()
        val action = Action.BuyPowerUp(TestGame.HUMAN, policyCard) // Cost 60
        
        val transition = reducer.reduce(state, action)
        val newState = transition.state
        
        val human = newState.players.first { it.id == TestGame.HUMAN }
        assertEquals(40, human.characters)
        assertTrue(human.policyIds.contains(policyCard))
        
        // Shop should be refilled with expensiveCard
        assertEquals(listOf(cheapCard, expensiveCard), newState.shopCardIds)
        assertEquals(listOf(executiveOrderCard), newState.drawPileCardIds)
        
        val events = transition.events
        assertTrue(events.any { it is GameEvent.ResourceChanged && it.amount == -60 })
        assertTrue(events.any { it is GameEvent.PowerUpBought && it.cardId == policyCard })
        assertTrue(events.any { it is GameEvent.ShopChanged })
    }

    @Test
    fun `buyPowerUp executive order goes to discard pile`() {
        val state = TestGame()
            .withPhase(TurnPhase.SHOP)
            .withShop(executiveOrderCard)
            .withCharacters(TestGame.HUMAN, 100)
            .build()
        val action = Action.BuyPowerUp(TestGame.HUMAN, executiveOrderCard)
        
        val transition = reducer.reduce(state, action)
        val newState = transition.state
        
        val human = newState.players.first { it.id == TestGame.HUMAN }
        assertEquals(50, human.characters)
        // Executive order does NOT go to policyIds
        assertTrue(!human.policyIds.contains(executiveOrderCard))
        // It goes to discard pile
        assertEquals(listOf(executiveOrderCard), newState.discardPileCardIds)
    }

    @Test
    fun `sweepShop rejects if not enough characters`() {
        val state = TestGame()
            .withPhase(TurnPhase.SHOP)
            .withCharacters(TestGame.HUMAN, 10)
            .build()
        val action = Action.SweepShop(TestGame.HUMAN)
        
        val transition = reducer.reduce(state, action)
        assertEquals(GameEvent.ActionRejected(TestGame.HUMAN, ActionRejectionReason.INVALID_SELECTION), transition.events[0])
    }

    @Test
    fun `sweepShop sweeps all cards to discard and refills shop`() {
        val state = TestGame()
            .withPhase(TurnPhase.SHOP)
            .withShop(policyCard, cheapCard)
            .withDrawPile(executiveOrderCard, expensiveCard)
            .withCharacters(TestGame.HUMAN, 20)
            .build()
        val action = Action.SweepShop(TestGame.HUMAN)
        
        val transition = reducer.reduce(state, action)
        val newState = transition.state
        
        val human = newState.players.first { it.id == TestGame.HUMAN }
        assertEquals(0, human.characters) // Cost is 20
        
        // Old shop goes to discard
        assertEquals(listOf(policyCard, cheapCard), newState.discardPileCardIds)
        
        // New shop is from draw pile
        assertEquals(listOf(executiveOrderCard, expensiveCard), newState.shopCardIds)
        assertEquals(0, newState.drawPileCardIds.size)
        
        val events = transition.events
        assertTrue(events.any { it is GameEvent.ResourceChanged && it.amount == -20 })
        assertTrue(events.any { it is GameEvent.ShopChanged })
    }

    @Test
    fun `refillShop shuffles discard pile when draw pile is empty`() {
        val state = TestGame()
            .withPhase(TurnPhase.SHOP)
            .withShop(policyCard) // One card in shop, so it will try to refill 2 more
            .withDrawPile() // Empty draw pile
            .withDiscardPile(cheapCard, expensiveCard, executiveOrderCard)
            .withCharacters(TestGame.HUMAN, 100)
            .build()
        val action = Action.BuyPowerUp(TestGame.HUMAN, policyCard)
        
        val transition = reducer.reduce(state, action)
        val newState = transition.state
        
        // Shop should be refilled with 3 cards total (it bought 1, so it needed 3 total to be full)
        assertEquals(3, newState.shopCardIds.size)
        // Discard pile was shuffled into draw pile, and 3 cards were taken
        assertEquals(0, newState.drawPileCardIds.size)
        assertEquals(0, newState.discardPileCardIds.size)
    }

}
