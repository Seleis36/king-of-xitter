package com.kingofxitter.cards

import com.kingofxitter.engine.DefaultGameReducer
import com.kingofxitter.engine.GameTransition
import com.kingofxitter.engine.SplitMix64
import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.CardEffectRegistry
import com.kingofxitter.engine.model.CardId
import com.kingofxitter.engine.model.DiceFace
import com.kingofxitter.engine.model.TurnPhase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LurkerRerollsAllThreesTest {
    private val reducer = DefaultGameReducer(CardEffectRegistry(ActivatedPolicies.effects))

    /** What each 3 becomes when Lurker rerolls them in dice order from the roll's random state. */
    private fun expectedFaces(
        rolled: GameTransition,
        random: SplitMix64,
    ): List<DiceFace> =
        rolled.state.dice.map { die ->
            if (die.face == DiceFace.THREE) DiceFace.entries[random.nextInt(DiceFace.entries.size)] else die.face
        }

    /** Rolls with Lurker until a seed shows two or more 3s whose rerolls are not 3s again. */
    private fun rollWithSeveralThrees(): GameTransition {
        for (seed in 0L..2000L) {
            val state =
                TestGame()
                    .withPhase(TurnPhase.ROLLING)
                    .withPolicy(TestGame.HUMAN, CardId("c05_lurker"))
                    .withRollNumber(0)
                    .withSeed(seed)
                    .build()
            val rolled = reducer.reduce(state, Action.RollDice(TestGame.HUMAN, emptySet()))
            if (rolled.state.dice.count { it.face == DiceFace.THREE } < 2) continue
            val replay = SplitMix64(rolled.state.randomState)
            val threes = rolled.state.dice.filter { it.face == DiceFace.THREE }
            val rerolled = threes.map { DiceFace.entries[replay.nextInt(6)] }
            if (rerolled.none { it == DiceFace.THREE }) return rolled
        }
        error("no seed in 0..2000 rolled several 3s")
    }

    @Test
    fun `Lurker rerolls every 3 in one use`() {
        val rolled = rollWithSeveralThrees()
        val decision = rolled.state.pendingDecisions.first()
        val option = decision.options.first { it.id.value != "decline" }

        val activated = reducer.reduce(rolled.state, Action.ChooseDecisionOption(TestGame.HUMAN, decision.id, option.id))

        assertEquals(expectedFaces(rolled, SplitMix64(rolled.state.randomState)), activated.state.dice.map { it.face })
        assertTrue(activated.state.dice.none { it.face == DiceFace.THREE })
        assertEquals(TurnPhase.ROLLING, activated.state.phase)
    }
}
