package com.kingofxitter.cards

import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.CardEffectIntent
import com.kingofxitter.engine.model.CardHookContext
import com.kingofxitter.engine.model.CardHookWindow
import com.kingofxitter.engine.model.CardId
import com.kingofxitter.engine.model.DecisionOptionId
import com.kingofxitter.engine.model.DiceFace
import com.kingofxitter.engine.model.Die
import com.kingofxitter.engine.model.DieId
import com.kingofxitter.engine.model.PlayerId
import com.kingofxitter.engine.model.PlayerResource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ComplexPoliciesTest {
    private val parodyAccount = ComplexPolicies.effects.first { it.cardId == CardId("c07_parody_account") }
    private val retweetForRetweet = ComplexPolicies.effects.first { it.cardId == CardId("c20_retweet_for_retweet") }
    private val burnerAccount = ComplexPolicies.effects.first { it.cardId == CardId("c23_burner_account") }
    private val leakedMemo = ComplexPolicies.effects.first { it.cardId == CardId("c25_leaked_memo") }
    private val pivot = ComplexPolicies.effects.first { it.cardId == CardId("c26_pivot") }
    private val hostileTakeover = ComplexPolicies.effects.first { it.cardId == CardId("c32_hostile_takeover") }

    class FixedRandom(val value: Int) : com.kingofxitter.engine.model.HookRandom {
        override fun nextInt(bound: Int): Int = value
    }

    @Test
    fun `burner account triggers on elimination`() {
        val p1 = TestGame.HUMAN
        val state = TestGame()
            .withFollowers(p1, 0)
            .withMandates(p1, 15)
            .withPolicies(p1, listOf(CardId("c23_burner_account"), CardId("c01_doomscroll_drip")))
            .build()
        val context = CardHookContext(
            state = state,
            ownerId = p1,
            sourceCardId = CardId("c23_burner_account"),
            window = CardHookWindow.ELIMINATED,
            random = FixedRandom(0)
        )
        val intents = burnerAccount.onEliminated(context)
        
        assertTrue(intents.contains(CardEffectIntent.DiscardPolicy(p1, CardId("c23_burner_account"))))
        assertTrue(intents.contains(CardEffectIntent.DiscardPolicy(p1, CardId("c01_doomscroll_drip"))))
        assertTrue(intents.contains(CardEffectIntent.ChangeResource(p1, PlayerResource.MANDATES, -15)))
        assertTrue(intents.contains(CardEffectIntent.ChangeResource(p1, PlayerResource.FOLLOWERS, 10)))
    }

    @Test
    fun `pivot allows discarding policy`() {
        val p1 = TestGame.HUMAN
        val state = TestGame()
            .withPolicies(p1, listOf(CardId("c26_pivot"), CardId("c02_lobbyist")))
            .build()
        val context = CardHookContext(
            state = state,
            ownerId = p1,
            sourceCardId = CardId("c26_pivot"),
            window = CardHookWindow.TURN_END,
            random = FixedRandom(0)
        )
        
        val offers = pivot.activatedAbilities(context)
        assertEquals(1, offers.size)
        val offer = offers.first()
        assertEquals("discard_policy", offer.abilityId)
        assertEquals(1, offer.options.size)
        
        val action = offer.options.first().action as Action.ActivateAbility
        assertEquals("discard_c02_lobbyist", action.abilityId)
        
        val intents = pivot.activate(context, action)
        assertTrue(intents.contains(CardEffectIntent.DiscardPolicy(p1, CardId("c02_lobbyist"))))
        assertTrue(intents.contains(CardEffectIntent.ChangeResource(p1, PlayerResource.CHARACTERS, 30)))
    }

    @Test
    fun `hostile takeover allows buying policies`() {
        val p1 = TestGame.HUMAN
        val p2 = TestGame.BOT
        val state = TestGame()
            .withCharacters(p1, 50)
            .withPolicies(p1, listOf(CardId("c32_hostile_takeover")))
            .withPolicies(p2, listOf(CardId("c04_thick_skin_pr")))
            .build()
        val context = CardHookContext(
            state = state,
            ownerId = p1,
            sourceCardId = CardId("c32_hostile_takeover"),
            window = CardHookWindow.AFTER_RESOLVE,
            random = FixedRandom(0)
        )
        
        val offers = hostileTakeover.activatedAbilities(context)
        assertEquals(1, offers.size)
        val offer = offers.first()
        assertEquals(1, offer.options.size)
        
        val action = offer.options.first().action as Action.ActivateAbility
        assertEquals("buy_p2_c04_thick_skin_pr", action.abilityId)
        
        val intents = hostileTakeover.activate(context, action)
        assertTrue(intents.contains(CardEffectIntent.TransferPolicy(p2, p1, CardId("c04_thick_skin_pr"))))
        assertTrue(intents.contains(CardEffectIntent.ChangeResource(p1, PlayerResource.CHARACTERS, -40)))
        assertTrue(intents.contains(CardEffectIntent.ChangeResource(p2, PlayerResource.CHARACTERS, 40)))
    }
}
