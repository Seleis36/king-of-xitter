package com.kingofxitter.cards

import com.kingofxitter.engine.DefaultGameReducer
import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.CardEffectRegistry
import com.kingofxitter.engine.model.CardId
import com.kingofxitter.engine.model.DiceFace
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.LeaderId
import com.kingofxitter.engine.model.PlayerId
import com.kingofxitter.engine.model.PlayerState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BurnerAccountScopeTest {
    private val reducer = ShopReducer(DefaultGameReducer(CardEffectRegistry(CardEffects.all)))
    private val burner = CardId("c23_burner_account")
    private val carol = PlayerId("carol")

    private fun GameState.player(id: PlayerId) = players.first { it.id == id }

    /** Human holds Burner Account and kills the Xitter holder (the bot) with a single tweet. */
    private fun humanSuspendsBot(): GameState {
        val state =
            TestGame()
                .withPlayer(PlayerState(id = carol, leaderId = LeaderId("macaron")))
                .withPolicies(TestGame.HUMAN, listOf(burner, CardId("c02_lobbyist")))
                .withMandates(TestGame.HUMAN, 7)
                .withFollowers(TestGame.BOT, 1)
                .withXitter(TestGame.BOT)
                .withDice(DiceFace.TWEET, DiceFace.ONE, DiceFace.TWO, DiceFace.TWO, DiceFace.LIKE, DiceFace.LIKE)
                .build()
        return reducer.reduce(state, Action.ResolveRoll(TestGame.HUMAN)).state
    }

    @Test
    fun `Burner Account does nothing when another leader is suspended`() {
        val resolved = humanSuspendsBot()

        assertTrue(resolved.player(TestGame.BOT).isSuspended)
        val human = resolved.player(TestGame.HUMAN)
        assertEquals(listOf(burner, CardId("c02_lobbyist")), human.policyIds)
        assertTrue(human.mandates >= 7, "mandates were ${human.mandates}")
        assertEquals(10, human.followers)
    }
}
