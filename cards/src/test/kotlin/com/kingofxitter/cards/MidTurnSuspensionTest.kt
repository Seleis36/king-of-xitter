package com.kingofxitter.cards

import com.kingofxitter.engine.DefaultGameReducer
import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.CardEffectRegistry
import com.kingofxitter.engine.model.CardId
import com.kingofxitter.engine.model.DiceFace
import com.kingofxitter.engine.model.GameEvent
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.LeaderId
import com.kingofxitter.engine.model.PlayerId
import com.kingofxitter.engine.model.PlayerState
import com.kingofxitter.engine.model.TurnPhase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** A card can suspend the player whose turn it is in the middle of that turn. */
class MidTurnSuspensionTest {
    private val reducer = DefaultGameReducer(CardEffectRegistry(PassivePolicies.effects))
    private val third = PlayerId("third")

    private fun GameState.player(id: PlayerId) = players.first { it.id == id }

    /** The human, at 1M followers, tweets at the bot, who holds Xitter and Home Turf. A third leader keeps the match going. */
    private fun attackOnHomeTurf(): GameState =
        TestGame()
            .withPlayer(PlayerState(id = third, leaderId = LeaderId("kim")))
            .withFollowers(TestGame.HUMAN, 1)
            .withPolicy(TestGame.BOT, CardId("c06_home_turf"))
            .withXitter(TestGame.BOT)
            .withDice(DiceFace.TWEET, DiceFace.ONE, DiceFace.TWO, DiceFace.TWO, DiceFace.THREE, DiceFace.THREE)
            .build()

    @Test
    fun `logging off onto Home Turf suspends the attacker and ends their turn`() {
        val attacked = reducer.reduce(attackOnHomeTurf(), Action.ResolveRoll(TestGame.HUMAN)).state
        val decision = attacked.pendingDecisions.single()
        assertEquals(TestGame.BOT, decision.playerId)
        val logOff = decision.options.first { it.action is Action.LogOffXitter }

        val transition = reducer.reduce(attacked, Action.ChooseDecisionOption(TestGame.BOT, decision.id, logOff.id))
        val after = transition.state

        assertTrue(after.player(TestGame.HUMAN).isSuspended)
        assertTrue(transition.events.any { it is GameEvent.TurnEnded && it.playerId == TestGame.HUMAN })
        // The match goes on with someone who can act, instead of waiting on a suspended player's shop.
        assertNull(after.result)
        assertEquals(TurnPhase.TURN_START, after.phase)
        assertTrue(after.currentPlayerId != TestGame.HUMAN)
        assertTrue(after.pendingDecisions.isEmpty())
    }

    @Test
    fun `staying on Xitter leaves the attacker's turn alone`() {
        val attacked = reducer.reduce(attackOnHomeTurf(), Action.ResolveRoll(TestGame.HUMAN)).state
        val decision = attacked.pendingDecisions.single()
        val stay = decision.options.first { it.action is Action.StayInXitter }

        val after = reducer.reduce(attacked, Action.ChooseDecisionOption(TestGame.BOT, decision.id, stay.id)).state

        assertEquals(TestGame.HUMAN, after.currentPlayerId)
        assertEquals(TurnPhase.SHOP, after.phase)
        assertEquals(1, after.player(TestGame.HUMAN).followers)
    }
}
