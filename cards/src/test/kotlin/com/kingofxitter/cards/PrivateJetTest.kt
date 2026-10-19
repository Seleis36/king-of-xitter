package com.kingofxitter.cards

import com.kingofxitter.engine.DefaultGameReducer
import com.kingofxitter.engine.GameTransition
import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.CardEffectRegistry
import com.kingofxitter.engine.model.CardId
import com.kingofxitter.engine.model.DecisionKind
import com.kingofxitter.engine.model.DecisionOptionId
import com.kingofxitter.engine.model.DiceFace
import com.kingofxitter.engine.model.Die
import com.kingofxitter.engine.model.DieId
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.PlayerId
import com.kingofxitter.engine.model.TurnPhase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PrivateJetTest {
    private val reducer = ShopReducer(DefaultGameReducer(CardEffectRegistry(CardEffects.all)))
    private val jet = CardId("c24_private_jet")

    private fun GameState.player(id: PlayerId) = players.first { it.id == id }

    /** The bot rolls [tweets] tweets against the human, who is on Xitter. */
    private fun botTweets(
        tweets: Int,
        humanPolicies: List<CardId>,
    ): GameTransition {
        val state =
            TestGame()
                .withCurrentPlayer(TestGame.BOT)
                .withPhase(TurnPhase.ROLLING)
                .withPolicies(TestGame.HUMAN, humanPolicies)
                .withXitter(TestGame.HUMAN)
                .withDice(*Array(tweets) { DiceFace.TWEET } + DiceFace.LIKE)
                .build()
        return reducer.reduce(state, Action.ResolveRoll(TestGame.BOT))
    }

    private fun answer(
        transition: GameTransition,
        optionId: String,
    ): GameTransition {
        val decision = transition.state.pendingDecisions.first()
        assertEquals(DecisionKind.STAY_OR_LOG_OFF, decision.kind)
        return reducer.reduce(transition.state, Action.ChooseDecisionOption(decision.playerId, decision.id, DecisionOptionId(optionId)))
    }

    @Test
    fun `Private Jet restores the followers lost to the attack when the holder logs off`() {
        val hit = botTweets(2, listOf(jet))
        assertEquals(8, hit.state.player(TestGame.HUMAN).followers)

        val loggedOff = answer(hit, "log-off").state

        assertEquals(10, loggedOff.player(TestGame.HUMAN).followers)
        assertEquals(TestGame.BOT, loggedOff.xitterPlayerId)
    }

    @Test
    fun `Private Jet changes nothing when the holder stays`() {
        val stayed = answer(botTweets(2, listOf(jet)), "stay").state

        assertEquals(8, stayed.player(TestGame.HUMAN).followers)
        assertEquals(TestGame.HUMAN, stayed.xitterPlayerId)
    }

    @Test
    fun `Without Private Jet the holder keeps the loss after logging off`() {
        val loggedOff = answer(botTweets(2, emptyList()), "log-off").state

        assertEquals(8, loggedOff.player(TestGame.HUMAN).followers)
    }

    @Test
    fun `Private Jet only restores the latest attack, not an earlier one the holder stayed through`() {
        val stayed = answer(botTweets(2, listOf(jet)), "stay").state
        assertEquals(8, stayed.player(TestGame.HUMAN).followers)

        val nextAttack =
            stayed.copy(
                currentPlayerId = TestGame.BOT,
                phase = TurnPhase.ROLLING,
                pendingDecisions = emptyList(),
                phaseAfterDecisions = null,
                resolveHooksAppliedForRoll = false,
                dice = listOf(Die(DieId(0), DiceFace.TWEET), Die(DieId(1), DiceFace.LIKE)),
            )
        val hit = reducer.reduce(nextAttack, Action.ResolveRoll(TestGame.BOT))
        assertEquals(7, hit.state.player(TestGame.HUMAN).followers)

        val loggedOff = answer(hit, "log-off").state

        assertEquals(8, loggedOff.player(TestGame.HUMAN).followers)
    }

    @Test
    fun `Private Jet does not push followers above the maximum`() {
        val loggedOff = answer(botTweets(1, listOf(jet)), "log-off").state.player(TestGame.HUMAN)

        assertTrue(loggedOff.followers <= loggedOff.maxFollowers)
        assertEquals(10, loggedOff.followers)
    }
}
