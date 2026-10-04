package com.kingofxitter.cards

import com.kingofxitter.engine.DefaultGameReducer
import com.kingofxitter.engine.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ActivatedPoliciesTest {
    private val reducer = DefaultGameReducer(CardEffectRegistry(ActivatedPolicies.effects))

    @Test
    fun `Lurker allows rerolling a 3`() {
        val state =
            TestGame()
                .withPhase(TurnPhase.ROLLING)
                .withPolicy(TestGame.HUMAN, CardId("c05_lurker"))
                .withDice(Die(DieId(1), DiceFace.THREE))
                .build()
                
        val rollResult = reducer.reduce(state, Action.RollDice(TestGame.HUMAN, emptySet()))
        
        // After rolling, Lurker should prompt an ability activation
        assertEquals(TurnPhase.DECISION, rollResult.state.phase)
        val decision = rollResult.state.pendingDecisions.first()
        assertEquals(DecisionKind.ACTIVATE_ABILITY, decision.kind)
        
        // Use Lurker ability
        val useLurker = decision.options.first { it.id.value == "1" }.action
        val activatedResult = reducer.reduce(rollResult.state, Action.ChooseDecisionOption(TestGame.HUMAN, decision.id, DecisionOptionId("1")))
        
        // Face is changed
        val die = activatedResult.state.dice.first { it.id == DieId(1) }
        assertNotNull(die)
    }
}
