package com.kingofxitter.cards

import com.kingofxitter.engine.DefaultGameReducer
import com.kingofxitter.engine.GameTransition
import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.ActionRejectionReason
import com.kingofxitter.engine.model.CardEffect
import com.kingofxitter.engine.model.CardEffectIntent
import com.kingofxitter.engine.model.CardEffectRegistry
import com.kingofxitter.engine.model.CardHookContext
import com.kingofxitter.engine.model.CardId
import com.kingofxitter.engine.model.DecisionId
import com.kingofxitter.engine.model.DecisionKind
import com.kingofxitter.engine.model.GameEvent
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.MatchResult
import com.kingofxitter.engine.model.PendingDecision
import com.kingofxitter.engine.model.PlayerResource
import com.kingofxitter.engine.model.TurnPhase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ShopReducerTest {
    private val reducer = ShopReducer(DefaultGameReducer())
    private val policyCard = CardId("c01_doomscroll_drip")
    private val executiveOrderCard = CardId("c49_op_ed")
    private val cheapCard = CardId("c25_leaked_memo")
    private val expensiveCard = CardId("c27_copycat_policy")

    @Test
    fun `setupDeck shuffles deterministically and preserves every physical copy`() {
        val state = TestGame().build()
        val result = ShopLogic.setupDeck(state, CardCatalog.standardDeck, 42L)
        assertEquals(result, ShopLogic.setupDeck(state, CardCatalog.standardDeck, 42L))
        assertNotEquals(result, ShopLogic.setupDeck(state, CardCatalog.standardDeck, 43L))
        assertEquals(3, result.shopCardIds.size)
        assertEquals(63, result.drawPileCardIds.size)
        assertEquals(emptyList(), result.discardPileCardIds)
        assertEquals(counts(CardCatalog.standardDeck), counts(result.shopCardIds + result.drawPileCardIds))
        assertEquals(emptyList(), state.shopCardIds)
        assertNotEquals(42L, result.randomState)
    }

    @Test
    fun `setupDeck handles empty and undersized decks`() {
        val state = TestGame().build()
        assertEquals(emptyList(), ShopLogic.setupDeck(state, emptyList(), 42L).shopCardIds)
        val shortDeck = listOf(policyCard, cheapCard)
        val result = ShopLogic.setupDeck(state, shortDeck, 42L)
        assertEquals(counts(shortDeck), counts(result.shopCardIds))
        assertEquals(emptyList(), result.drawPileCardIds)
    }

    @Test
    fun `buyPowerUp rejects if not SHOP phase`() {
        assertRejected(
            shopState().copy(phase = TurnPhase.ROLLING),
            Action.BuyPowerUp(TestGame.HUMAN, policyCard),
            ActionRejectionReason.WRONG_PHASE,
        )
    }

    @Test
    fun `buyPowerUp rejects if not enough characters`() {
        val state =
            TestGame()
                .withPhase(TurnPhase.SHOP)
                .withShop(listOf(policyCard))
                .withCharacters(TestGame.HUMAN, 50)
                .build()
        assertRejected(state, Action.BuyPowerUp(TestGame.HUMAN, policyCard), ActionRejectionReason.INVALID_SELECTION)
    }

    @Test
    fun `buyPowerUp rejects if card not in shop`() {
        assertRejected(shopState(), Action.BuyPowerUp(TestGame.HUMAN, expensiveCard), ActionRejectionReason.INVALID_SELECTION)
    }

    @Test
    fun `buyPowerUp rejects an unknown definition even if it is in the shop`() {
        val unknown = CardId("unknown")
        assertRejected(
            shopState().copy(shopCardIds = listOf(unknown)),
            Action.BuyPowerUp(TestGame.HUMAN, unknown),
            ActionRejectionReason.INVALID_SELECTION,
        )
    }

    @Test
    fun `buyPowerUp policy subtracts cost adds ownership and fills all three slots`() {
        val state =
            TestGame()
                .withPhase(TurnPhase.SHOP)
                .withShop(listOf(policyCard, cheapCard))
                .withDrawPile(listOf(expensiveCard, executiveOrderCard))
                .withCharacters(TestGame.HUMAN, 100)
                .build()
        val transition = reducer.reduce(state, Action.BuyPowerUp(TestGame.HUMAN, policyCard))
        val human = transition.state.players.first { it.id == TestGame.HUMAN }
        assertEquals(40, human.characters)
        assertEquals(listOf(policyCard), human.policyIds)
        assertEquals(listOf(cheapCard, expensiveCard, executiveOrderCard), transition.state.shopCardIds)
        assertEquals(emptyList(), transition.state.drawPileCardIds)
        assertEquals(state.randomState, transition.state.randomState)
        assertTrue(transition.events.any { it is GameEvent.ResourceChanged && it.amount == -60 && it.totalAfter == 40 })
        assertTrue(transition.events.any { it == GameEvent.PowerUpBought(TestGame.HUMAN, policyCard) })
        assertEquals(GameEvent.ShopChanged(transition.state.shopCardIds), transition.events.last())
        assertEquals(100, state.players.first { it.id == TestGame.HUMAN }.characters)
    }

    @Test
    fun `buyPowerUp executive order goes to discard after its effect`() {
        val state =
            shopState().copy(
                shopCardIds = listOf(executiveOrderCard),
                drawPileCardIds = listOf(cheapCard, expensiveCard, policyCard),
            )
        val transition = reducer.reduce(state, Action.BuyPowerUp(TestGame.HUMAN, executiveOrderCard))
        val human = transition.state.players.first { it.id == TestGame.HUMAN }
        assertEquals(50, human.characters)
        assertFalse(executiveOrderCard in human.policyIds)
        assertEquals(listOf(executiveOrderCard), transition.state.discardPileCardIds)
        assertEquals(listOf(cheapCard, expensiveCard, policyCard), transition.state.shopCardIds)
    }

    @Test
    fun `purchase hook observes payment before discard or refill and runs once`() {
        var observed: CardHookContext? = null
        var calls = 0
        val effect =
            object : CardEffect {
                override val cardId = executiveOrderCard

                override fun onBuy(context: CardHookContext): List<CardEffectIntent> {
                    observed = context
                    calls += 1
                    return listOf(CardEffectIntent.ChangeResource(context.ownerId, PlayerResource.CHARACTERS, 20))
                }
            }
        val hooked = ShopReducer(DefaultGameReducer(CardEffectRegistry(listOf(effect))))
        val state = shopState().copy(shopCardIds = listOf(executiveOrderCard))
        val transition = hooked.reduce(state, Action.BuyPowerUp(TestGame.HUMAN, executiveOrderCard))
        val snapshot = observed!!.state
        assertEquals(1, calls)
        assertEquals(executiveOrderCard, observed!!.purchasedCardId)
        assertEquals(50, snapshot.players.first { it.id == TestGame.HUMAN }.characters)
        assertEquals(emptyList(), snapshot.discardPileCardIds)
        assertEquals(emptyList(), snapshot.shopCardIds)
        assertEquals(
            70,
            transition.state.players
                .first { it.id == TestGame.HUMAN }
                .characters,
        )
        // With no other cards left, refill may recycle the order only after its hook.
        assertEquals(listOf(executiveOrderCard), transition.state.shopCardIds)
    }

    @Test
    fun `new policy and already owned policies receive the purchase hook`() {
        val observedIds = mutableListOf<CardId>()
        val effects =
            listOf(policyCard, cheapCard).map { id ->
                object : CardEffect {
                    override val cardId = id

                    override fun onBuy(context: CardHookContext): List<CardEffectIntent> {
                        observedIds += context.sourceCardId
                        assertEquals(
                            listOf(cheapCard, policyCard),
                            context.state.players
                                .first { it.id == TestGame.HUMAN }
                                .policyIds,
                        )
                        return emptyList()
                    }
                }
            }
        val hooked = ShopReducer(DefaultGameReducer(CardEffectRegistry(effects)))
        val state =
            TestGame()
                .withPhase(TurnPhase.SHOP)
                .withPolicy(TestGame.HUMAN, cheapCard)
                .withShop(listOf(policyCard))
                .withCharacters(TestGame.HUMAN, 100)
                .build()
        hooked.reduce(state, Action.BuyPowerUp(TestGame.HUMAN, policyCard))
        assertEquals(listOf(policyCard, cheapCard), observedIds)
    }

    @Test
    fun `sweepShop rejects if not enough characters`() {
        val state =
            TestGame()
                .withPhase(TurnPhase.SHOP)
                .withShop(listOf(policyCard))
                .withCharacters(TestGame.HUMAN, 10)
                .build()
        assertRejected(state, Action.SweepShop(TestGame.HUMAN), ActionRejectionReason.INVALID_SELECTION)
    }

    @Test
    fun `sweepShop sweeps all visible cards and deals three replacements`() {
        val replacements = listOf(executiveOrderCard, CardId("c50_photo_op"), CardId("c51_baby_kissing"))
        val oldShop = listOf(policyCard, cheapCard, expensiveCard)
        val state =
            TestGame()
                .withPhase(TurnPhase.SHOP)
                .withShop(oldShop)
                .withDrawPile(replacements)
                .withCharacters(TestGame.HUMAN, 20)
                .build()
        val transition = reducer.reduce(state, Action.SweepShop(TestGame.HUMAN))
        assertEquals(
            0,
            transition.state.players
                .first { it.id == TestGame.HUMAN }
                .characters,
        )
        assertEquals(oldShop, transition.state.discardPileCardIds)
        assertEquals(replacements, transition.state.shopCardIds)
        assertEquals(emptyList(), transition.state.drawPileCardIds)
        assertEquals(GameEvent.ShopChanged(replacements), transition.events.last())
    }

    @Test
    fun `refillShop shuffles discard pile when draw pile is empty`() {
        val state = shopState().copy(discardPileCardIds = listOf(cheapCard, expensiveCard, executiveOrderCard))
        val result = reducer.reduce(state, Action.BuyPowerUp(TestGame.HUMAN, policyCard))
        assertEquals(3, result.state.shopCardIds.size)
        assertEquals(counts(state.discardPileCardIds), counts(result.state.shopCardIds))
        assertEquals(emptyList(), result.state.drawPileCardIds)
        assertEquals(emptyList(), result.state.discardPileCardIds)
        assertNotEquals(state.randomState, result.state.randomState)
        assertEquals(result, reducer.reduce(state, Action.BuyPowerUp(TestGame.HUMAN, policyCard)))
    }

    @Test
    fun `sweep refills across deck exhaustion without losing physical cards`() {
        val state =
            shopState().copy(
                shopCardIds = listOf(policyCard, cheapCard),
                drawPileCardIds = listOf(expensiveCard, executiveOrderCard),
            )
        val result = reducer.reduce(state, Action.SweepShop(TestGame.HUMAN)).state
        assertEquals(3, result.shopCardIds.size)
        assertEquals(1, result.drawPileCardIds.size)
        assertEquals(emptyList(), result.discardPileCardIds)
        assertEquals(counts(state.shopCardIds + state.drawPileCardIds), counts(result.shopCardIds + result.drawPileCardIds))
    }

    @Test
    fun `buying final policy emits an empty shop when both piles are exhausted`() {
        val transition = reducer.reduce(shopState(), Action.BuyPowerUp(TestGame.HUMAN, policyCard))
        assertEquals(emptyList(), transition.state.shopCardIds)
        assertEquals(GameEvent.ShopChanged(emptyList()), transition.events.last())
    }

    @Test
    fun `shop actions reject finished matches even if their phase remains SHOP`() {
        val actions = listOf(Action.BuyPowerUp(TestGame.HUMAN, policyCard), Action.SweepShop(TestGame.HUMAN))
        for (state in listOf(shopState().copy(result = MatchResult.NoWinner), shopState().copy(phase = TurnPhase.FINISHED))) {
            actions.forEach { assertRejected(state, it, ActionRejectionReason.MATCH_FINISHED) }
        }
    }

    @Test
    fun `shop actions reject suspended players and missing players`() {
        val state = shopState()
        val suspended = state.copy(players = state.players.map { if (it.id == TestGame.HUMAN) it.copy(followers = 0) else it })
        for (action in listOf(Action.BuyPowerUp(TestGame.HUMAN, policyCard), Action.SweepShop(TestGame.HUMAN))) {
            assertRejected(suspended, action, ActionRejectionReason.INVALID_SELECTION)
            assertRejected(
                state.copy(players = state.players.filter { it.id != TestGame.HUMAN }),
                action,
                ActionRejectionReason.INVALID_SELECTION,
            )
        }
    }

    @Test
    fun `shop actions reject another player and unresolved decisions`() {
        val state = shopState()
        val waiting =
            state.copy(
                pendingDecisions = listOf(PendingDecision(DecisionId("pending"), TestGame.HUMAN, DecisionKind.CHOOSE_TARGET, emptyList())),
            )
        assertRejected(state, Action.BuyPowerUp(TestGame.BOT, policyCard), ActionRejectionReason.WRONG_PLAYER)
        assertRejected(state, Action.SweepShop(TestGame.BOT), ActionRejectionReason.WRONG_PLAYER)
        assertRejected(waiting, Action.BuyPowerUp(TestGame.HUMAN, policyCard), ActionRejectionReason.WRONG_PHASE)
        assertRejected(waiting, Action.SweepShop(TestGame.HUMAN), ActionRejectionReason.WRONG_PHASE)
        assertRejected(state.copy(phase = TurnPhase.ROLLING), Action.SweepShop(TestGame.HUMAN), ActionRejectionReason.WRONG_PHASE)
    }

    @Test
    fun `empty shop cannot be swept for a charge with no cards`() {
        assertRejected(
            shopState().copy(shopCardIds = emptyList()),
            Action.SweepShop(TestGame.HUMAN),
            ActionRejectionReason.INVALID_SELECTION,
        )
    }

    @Test
    fun `purchase effects can end the match and subsequent purchases are rejected`() {
        val effect =
            object : CardEffect {
                override val cardId = executiveOrderCard

                override fun onBuy(context: CardHookContext): List<CardEffectIntent> =
                    listOf(CardEffectIntent.ChangeResource(context.ownerId, PlayerResource.MANDATES, 20))
            }
        val hooked = ShopReducer(DefaultGameReducer(CardEffectRegistry(listOf(effect))))
        val state = shopState().copy(shopCardIds = listOf(executiveOrderCard))
        val won = hooked.reduce(state, Action.BuyPowerUp(TestGame.HUMAN, executiveOrderCard)).state
        assertTrue(won.result is MatchResult.Victory)
        val rejected = hooked.reduce(won, Action.BuyPowerUp(TestGame.HUMAN, executiveOrderCard))
        assertEquals(won, rejected.state)
        assertEquals(listOf(GameEvent.ActionRejected(TestGame.HUMAN, ActionRejectionReason.MATCH_FINISHED)), rejected.events)
    }

    @Test
    fun `duplicate policy copies are retained so their effect can count both`() {
        val phone = CardId("c13_second_phone")
        val effect =
            object : CardEffect {
                override val cardId = phone

                override fun diceCount(context: CardHookContext): Int =
                    context.state.players
                        .first { it.id == context.ownerId }
                        .policyIds
                        .count { it == cardId }
            }
        val hooked = ShopReducer(DefaultGameReducer(CardEffectRegistry(listOf(effect))))
        val state =
            TestGame()
                .withPhase(TurnPhase.SHOP)
                .withShop(listOf(phone, phone))
                .withCharacters(TestGame.HUMAN, 140)
                .build()
        val first = hooked.reduce(state, Action.BuyPowerUp(TestGame.HUMAN, phone)).state
        val second = hooked.reduce(first, Action.BuyPowerUp(TestGame.HUMAN, phone)).state
        assertEquals(listOf(phone, phone), second.players.first { it.id == TestGame.HUMAN }.policyIds)
        val rolled = hooked.reduce(second.copy(phase = TurnPhase.TURN_START, rollNumber = 0), Action.RollDice(TestGame.HUMAN))
        assertEquals(8, rolled.state.dice.size)
    }

    @Test
    fun `non shop actions delegate to the base engine unchanged`() {
        val state = shopState()
        val action = Action.EndTurn(TestGame.HUMAN)
        assertEquals(DefaultGameReducer().reduce(state, action), reducer.reduce(state, action))
    }

    private fun shopState(): GameState =
        TestGame()
            .withPhase(TurnPhase.SHOP)
            .withShop(listOf(policyCard))
            .withCharacters(TestGame.HUMAN, 100)
            .build()

    private fun counts(ids: List<CardId>): Map<CardId, Int> = ids.groupingBy { it }.eachCount()

    private fun assertRejected(
        state: GameState,
        action: Action,
        reason: ActionRejectionReason,
    ) {
        val transition: GameTransition = reducer.reduce(state, action)
        assertEquals(state, transition.state)
        assertEquals(listOf(GameEvent.ActionRejected(action.playerId, reason)), transition.events)
    }
}
