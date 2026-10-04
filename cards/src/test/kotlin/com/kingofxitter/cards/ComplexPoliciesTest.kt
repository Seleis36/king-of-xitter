package com.kingofxitter.cards

import com.kingofxitter.engine.DefaultGameReducer
import com.kingofxitter.engine.GameTransition
import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.CardEffectIntent
import com.kingofxitter.engine.model.CardEffectRegistry
import com.kingofxitter.engine.model.CardHookContext
import com.kingofxitter.engine.model.CardHookWindow
import com.kingofxitter.engine.model.CardId
import com.kingofxitter.engine.model.DecisionKind
import com.kingofxitter.engine.model.DecisionOptionId
import com.kingofxitter.engine.model.DiceFace
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.HookRandom
import com.kingofxitter.engine.model.PlayerId
import com.kingofxitter.engine.model.PlayerResource
import com.kingofxitter.engine.model.TurnPhase
import com.kingofxitter.engine.model.effectivePolicyIds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ComplexPoliciesTest {
    private val reducer =
        ShopReducer(DefaultGameReducer(CardEffectRegistry(ComplexPolicies.effects + PassivePolicies.effects)))

    private class FixedRandom(
        val value: Int,
    ) : HookRandom {
        override fun nextInt(bound: Int): Int = value
    }

    private fun effect(id: String) = ComplexPolicies.effects.first { it.cardId == CardId(id) }

    private fun GameState.player(id: PlayerId) = players.first { it.id == id }

    /** A rolled turn with no scoring dice, so resolving it only reaches the shop-entry offers. */
    private fun resolveOffers(game: TestGame): GameTransition {
        val state = game.withDice(DiceFace.ONE, DiceFace.ONE, DiceFace.TWO, DiceFace.TWO, DiceFace.LIKE, DiceFace.LIKE).build()
        return reducer.reduce(state, Action.ResolveRoll(TestGame.HUMAN))
    }

    private fun choose(
        transition: GameTransition,
        optionId: String,
    ): GameTransition {
        val decision = transition.state.pendingDecisions.first()
        return reducer.reduce(transition.state, Action.ChooseDecisionOption(decision.playerId, decision.id, DecisionOptionId(optionId)))
    }

    @Test
    fun `parody account cancels one hit per like it rolls`() {
        val state = TestGame().withPolicy(TestGame.HUMAN, CardId("c07_parody_account")).build()
        val context =
            CardHookContext(
                state,
                TestGame.HUMAN,
                CardId("c07_parody_account"),
                CardHookWindow.INCOMING_DAMAGE,
                random = FixedRandom(DiceFace.LIKE.ordinal),
                damage = 3,
            )

        assertEquals(3, effect("c07_parody_account").modifyIncomingDamage(context).damageReduction)
    }

    @Test
    fun `retweet for retweet sells a like to a hurt leader and only on your own roll`() {
        val state =
            TestGame()
                .withPolicy(TestGame.HUMAN, CardId("c20_retweet_for_retweet"))
                .withFollowers(TestGame.BOT, 5)
                .withCharacters(TestGame.BOT, 40)
                .withDice(DiceFace.LIKE, DiceFace.ONE)
                .build()
        val own =
            CardHookContext(
                state,
                TestGame.HUMAN,
                CardId("c20_retweet_for_retweet"),
                CardHookWindow.AFTER_DICE_ROLL,
                random = FixedRandom(0),
            )
        val offers = effect("c20_retweet_for_retweet").activatedAbilities(own)
        assertEquals(1, offers.size)

        val action =
            offers
                .first()
                .options
                .first()
                .action as Action.ActivateAbility
        val intents = effect("c20_retweet_for_retweet").activate(own, action)
        assertTrue(CardEffectIntent.ChangeResource(TestGame.HUMAN, PlayerResource.CHARACTERS, 20) in intents)
        assertTrue(CardEffectIntent.ChangeResource(TestGame.BOT, PlayerResource.FOLLOWERS, 1) in intents)

        val other = own.copy(state = state.copy(currentPlayerId = TestGame.BOT))
        assertTrue(effect("c20_retweet_for_retweet").activatedAbilities(other).isEmpty())
    }

    @Test
    fun `selling a like removes the die and the roll can still be rerolled`() {
        val state =
            TestGame()
                .withPhase(TurnPhase.ROLLING)
                .withRollNumber(0)
                .withPolicy(TestGame.HUMAN, CardId("c20_retweet_for_retweet"))
                .withFollowers(TestGame.BOT, 5)
                .withCharacters(TestGame.BOT, 40)
                .build()
        val rolled =
            (0L..300L)
                .map { reducer.reduce(state.copy(randomState = it), Action.RollDice(TestGame.HUMAN, emptySet())) }
                .first { r -> r.state.dice.any { it.face == DiceFace.LIKE } && r.state.pendingDecisions.isNotEmpty() }
        val option =
            rolled.state.pendingDecisions
                .first()
                .options
                .first { it.id.value != "decline" }

        val sold = choose(rolled, option.id.value)
        assertEquals(5, sold.state.dice.size)
        assertEquals(6, sold.state.player(TestGame.BOT).followers)

        val again = reducer.reduce(sold.state, Action.RollDice(TestGame.HUMAN, emptySet()))
        assertTrue(again.events.none { it is com.kingofxitter.engine.model.GameEvent.ActionRejected })
    }

    @Test
    fun `burner account brings a suspended leader back with 10M and nothing else`() {
        val state =
            TestGame()
                .withPolicies(TestGame.BOT, listOf(CardId("c23_burner_account"), CardId("c01_doomscroll_drip")))
                .withFollowers(TestGame.BOT, 1)
                .withMandates(TestGame.BOT, 15)
                .withXitter(TestGame.BOT)
                .withDice(DiceFace.TWEET, DiceFace.ONE, DiceFace.TWO, DiceFace.TWO, DiceFace.LIKE, DiceFace.LIKE)
                .build()

        val bot = reducer.reduce(state, Action.ResolveRoll(TestGame.HUMAN)).state.player(TestGame.BOT)

        assertEquals(10, bot.followers)
        assertEquals(0, bot.mandates)
        assertTrue(bot.policyIds.isEmpty())
    }

    @Test
    fun `leaked memo buys the top card of the deck`() {
        val top = CardId("c01_doomscroll_drip")
        val offer =
            resolveOffers(
                TestGame()
                    .withPhase(TurnPhase.ROLLING)
                    .withPolicy(TestGame.HUMAN, CardId("c25_leaked_memo"))
                    .withCharacters(TestGame.HUMAN, 100)
                    .withDrawPile(listOf(top, CardId("c02_lobbyist"))),
            )
        assertEquals(
            DecisionKind.ACTIVATE_ABILITY,
            offer.state.pendingDecisions
                .first()
                .kind,
        )

        val bought = choose(offer, "buy").state.player(TestGame.HUMAN)

        assertTrue(top in bought.policyIds)
        assertEquals(40, bought.characters)
    }

    @Test
    fun `pivot discards a policy for its full cost`() {
        val offer =
            resolveOffers(
                TestGame()
                    .withPhase(TurnPhase.ROLLING)
                    .withPolicies(TestGame.HUMAN, listOf(CardId("c26_pivot"), CardId("c02_lobbyist")))
                    .withCharacters(TestGame.HUMAN, 0),
            )

        val after = choose(offer, "discard_c02_lobbyist").state

        assertEquals(30, after.player(TestGame.HUMAN).characters)
        assertTrue(CardId("c02_lobbyist") !in after.player(TestGame.HUMAN).policyIds)
        assertTrue(CardId("c02_lobbyist") in after.discardPileCardIds)
    }

    @Test
    fun `hostile takeover buys another leader's policy and pays them`() {
        val offer =
            resolveOffers(
                TestGame()
                    .withPhase(TurnPhase.ROLLING)
                    .withCharacters(TestGame.HUMAN, 50)
                    .withCharacters(TestGame.BOT, 0)
                    .withPolicy(TestGame.HUMAN, CardId("c32_hostile_takeover"))
                    .withPolicy(TestGame.BOT, CardId("c04_thick_skin_pr")),
            )

        val after = choose(offer, "buy_${TestGame.BOT.value}_c04_thick_skin_pr").state

        assertTrue(CardId("c04_thick_skin_pr") in after.player(TestGame.HUMAN).policyIds)
        assertTrue(after.player(TestGame.BOT).policyIds.isEmpty())
        assertEquals(10, after.player(TestGame.HUMAN).characters)
        assertEquals(40, after.player(TestGame.BOT).characters)
    }

    @Test
    fun `hostile takeover is not offered when the owner cannot pay`() {
        val offer =
            resolveOffers(
                TestGame()
                    .withPhase(TurnPhase.ROLLING)
                    .withCharacters(TestGame.HUMAN, 30)
                    .withPolicy(TestGame.HUMAN, CardId("c32_hostile_takeover"))
                    .withPolicy(TestGame.BOT, CardId("c04_thick_skin_pr")),
            )

        assertTrue(offer.state.pendingDecisions.isEmpty())
    }

    @Test
    fun `copycat copies another leader's policy, and stops when they lose it`() {
        val offer =
            resolveOffers(
                TestGame()
                    .withPhase(TurnPhase.ROLLING)
                    .withPolicy(TestGame.HUMAN, CardId("c27_copycat_policy"))
                    .withPolicy(TestGame.BOT, CardId("c18_speechwriter")),
            )

        val copied = choose(offer, "copy_c18_speechwriter").state
        val human = copied.player(TestGame.HUMAN)

        assertEquals(CardId("c18_speechwriter"), human.copiedPolicyId)
        assertTrue(CardId("c18_speechwriter") in copied.effectivePolicyIds(human))

        val lost = copied.copy(players = copied.players.map { if (it.id == TestGame.BOT) it.copy(policyIds = emptyList()) else it })
        assertTrue(CardId("c18_speechwriter") !in lost.effectivePolicyIds(lost.player(TestGame.HUMAN)))
    }

    @Test
    fun `copied speechwriter gives an extra reroll`() {
        val base =
            TestGame()
                .withPhase(TurnPhase.ROLLING)
                .withRollNumber(0)
                .withPolicy(TestGame.HUMAN, CardId("c27_copycat_policy"))
                .withPolicy(TestGame.BOT, CardId("c18_speechwriter"))
                .build()
        val state =
            base.copy(
                players =
                    base.players.map {
                        if (it.id ==
                            TestGame.HUMAN
                        ) {
                            it.copy(copiedPolicyId = CardId("c18_speechwriter"))
                        } else {
                            it
                        }
                    },
            )

        val rolled = reducer.reduce(state, Action.RollDice(TestGame.HUMAN, emptySet())).state

        assertEquals(4, rolled.turnRollLimit)
    }

    @Test
    fun `switching a copy costs 10 chars`() {
        val base =
            TestGame()
                .withPhase(TurnPhase.ROLLING)
                .withPolicy(TestGame.HUMAN, CardId("c27_copycat_policy"))
                .withPolicy(TestGame.BOT, CardId("c18_speechwriter"))
                .withPolicy(TestGame.BOT, CardId("c04_thick_skin_pr"))
                .withCharacters(TestGame.HUMAN, 30)
        val state = base.build()
        val withCopy =
            state.copy(
                players =
                    state.players.map {
                        if (it.id ==
                            TestGame.HUMAN
                        ) {
                            it.copy(copiedPolicyId = CardId("c18_speechwriter"))
                        } else {
                            it
                        }
                    },
            )
        val resolved =
            reducer.reduce(
                withCopy.copy(
                    dice =
                        listOf(DiceFace.ONE, DiceFace.ONE, DiceFace.TWO, DiceFace.TWO, DiceFace.LIKE, DiceFace.LIKE).mapIndexed {
                            i,
                            f,
                            ->
                            com.kingofxitter.engine.model
                                .Die(
                                    com.kingofxitter.engine.model
                                        .DieId(i),
                                    f,
                                )
                        },
                ),
                Action.ResolveRoll(TestGame.HUMAN),
            )

        val switched = choose(resolved, "copy_c04_thick_skin_pr").state.player(TestGame.HUMAN)

        assertEquals(CardId("c04_thick_skin_pr"), switched.copiedPolicyId)
        assertEquals(20, switched.characters)
    }

    @Test
    fun `breaking news lets its owner buy a newly revealed card out of turn`() {
        val shown = CardId("c02_lobbyist")
        val revealed = CardId("c25_leaked_memo")
        val state =
            TestGame()
                .withPhase(TurnPhase.SHOP)
                .withShop(listOf(shown))
                .withDrawPile(listOf(revealed))
                .withCharacters(TestGame.HUMAN, 100)
                .withCharacters(TestGame.BOT, 50)
                .withPolicy(TestGame.BOT, CardId("c31_breaking_news_alerts"))
                .build()

        val bought = reducer.reduce(state, Action.BuyPowerUp(TestGame.HUMAN, shown))
        val decision = bought.state.pendingDecisions.first()
        assertEquals(TestGame.BOT, decision.playerId)
        assertEquals(DecisionKind.BUY_POWER_UP, decision.kind)

        val botBuys = choose(bought, "buy_${revealed.value}")

        assertTrue(botBuys.events.none { it is com.kingofxitter.engine.model.GameEvent.ActionRejected })
        assertTrue(revealed in botBuys.state.player(TestGame.BOT).policyIds)
        assertEquals(30, botBuys.state.player(TestGame.BOT).characters)
        assertEquals(TurnPhase.SHOP, botBuys.state.phase)
        assertTrue(botBuys.state.pendingDecisions.isEmpty())
    }

    @Test
    fun `breaking news can be declined and is not offered to the buyer`() {
        val state =
            TestGame()
                .withPhase(TurnPhase.SHOP)
                .withShop(listOf(CardId("c02_lobbyist")))
                .withDrawPile(listOf(CardId("c25_leaked_memo")))
                .withCharacters(TestGame.HUMAN, 100)
                .withCharacters(TestGame.BOT, 50)
                .withPolicy(TestGame.HUMAN, CardId("c31_breaking_news_alerts"))
                .withPolicy(TestGame.BOT, CardId("c31_breaking_news_alerts"))
                .build()

        val bought = reducer.reduce(state, Action.BuyPowerUp(TestGame.HUMAN, CardId("c02_lobbyist")))
        assertEquals(listOf(TestGame.BOT), bought.state.pendingDecisions.map { it.playerId })

        val declined = choose(bought, "decline")

        assertEquals(TurnPhase.SHOP, declined.state.phase)
        assertNotNull(declined.state.shopCardIds.firstOrNull())
    }
}
