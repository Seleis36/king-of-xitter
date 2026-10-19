package com.kingofxitter.app.ui.board

import com.kingofxitter.app.navigation.matchReducer
import com.kingofxitter.cards.TestGame
import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.CardId
import com.kingofxitter.engine.model.DecisionId
import com.kingofxitter.engine.model.DecisionKind
import com.kingofxitter.engine.model.DecisionOption
import com.kingofxitter.engine.model.DecisionOptionId
import com.kingofxitter.engine.model.DiceFace
import com.kingofxitter.engine.model.PendingDecision
import com.kingofxitter.engine.model.TurnPhase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AbilityPromptTest {
    /** The decision the real card offers the human after their first roll. */
    private fun offeredBy(card: String): PendingDecision {
        val state =
            TestGame()
                .withPhase(TurnPhase.ROLLING)
                .withRollNumber(0)
                .withPolicy(TestGame.HUMAN, CardId(card))
                .withCharacters(TestGame.HUMAN, 50)
                .withFollowers(TestGame.HUMAN, 5)
                .build()
        return matchReducer()
            .reduce(state, Action.RollDice(TestGame.HUMAN, emptySet()))
            .state.pendingDecisions
            .single()
    }

    @Test
    fun `Poll Rigger is a die picked on the tray`() {
        val decision = offeredBy("c22_poll_rigger")
        val prompt = assertIs<AbilityPrompt.PickDie>(AbilityPrompt.of(decision))

        val offeredDice = decision.options.mapNotNull { (it.action as? Action.ActivateAbility)?.dieIds?.single() }
        assertEquals(offeredDice.toSet(), prompt.byDie.keys)
        assertIs<Action.DeclineAbility>(assertNotNull(prompt.decline).action)
    }

    @Test
    fun `Spin Doctor is a die then one of its other faces`() {
        val prompt = assertIs<AbilityPrompt.PickDieThenFace>(AbilityPrompt.of(offeredBy("c44_spin_doctor")))

        assertEquals(6, prompt.byDie.size)
        prompt.byDie.values.forEach { faces -> assertEquals(5, faces.size) }
    }

    @Test
    fun `Paid Promotion is a single use or skip`() {
        val prompt = assertIs<AbilityPrompt.Choose>(AbilityPrompt.of(offeredBy("c37_paid_promotion")))

        assertEquals(1, prompt.options.size)
        assertNotNull(prompt.decline)
    }

    @Test
    fun `a leader per option is picked from the portraits`() {
        val die =
            setOf(
                com.kingofxitter.engine.model
                    .DieId(0),
            )
        val options =
            listOf(TestGame.BOT, TestGame.HUMAN).map { target ->
                DecisionOption(
                    DecisionOptionId(target.value),
                    Action.ActivateAbility(TestGame.HUMAN, CardId("c20_retweet_for_retweet"), "sell_like", target, die),
                )
            }
        val prompt = assertIs<AbilityPrompt.PickLeader>(AbilityPrompt.of(decision(options)))

        assertEquals(setOf(TestGame.BOT, TestGame.HUMAN), prompt.byLeader.keys)
        assertNull(prompt.decline)
    }

    @Test
    fun `cards, buys and anything mixed fall back to one button per option`() {
        val pivot =
            listOf("c18_speechwriter", "c09_press_office").map {
                DecisionOption(DecisionOptionId(it), Action.ActivateAbility(TestGame.HUMAN, CardId("c26_pivot"), "discard_policy:$it"))
            }
        assertEquals(2, assertIs<AbilityPrompt.Choose>(AbilityPrompt.of(decision(pivot))).options.size)

        val buys = listOf(DecisionOption(DecisionOptionId("buy"), Action.BuyPowerUp(TestGame.HUMAN, CardId("c02_lobbyist"))))
        assertIs<AbilityPrompt.Choose>(AbilityPrompt.of(decision(buys, DecisionKind.BUY_POWER_UP)))

        // A die-only use next to a die-and-face use can't share the tray.
        val die =
            setOf(
                com.kingofxitter.engine.model
                    .DieId(1),
            )
        val mixed =
            listOf(
                DecisionOption(DecisionOptionId("a"), Action.ActivateAbility(TestGame.HUMAN, CardId("x"), "reroll", dieIds = die)),
                DecisionOption(DecisionOptionId("b"), Action.ActivateAbility(TestGame.HUMAN, CardId("x"), "set:LIKE", dieIds = die)),
            )
        assertIs<AbilityPrompt.Choose>(AbilityPrompt.of(decision(mixed)))
    }

    @Test
    fun `the face comes from the ability id`() {
        assertEquals(DiceFace.LIKE, AbilityPrompt.faceOf(Action.ActivateAbility(TestGame.HUMAN, CardId("x"), "use_spin_doctor:LIKE")))
        assertNull(AbilityPrompt.faceOf(Action.ActivateAbility(TestGame.HUMAN, CardId("x"), "discard_policy:c02_lobbyist")))
        assertNull(AbilityPrompt.faceOf(Action.ActivateAbility(TestGame.HUMAN, CardId("x"), "use_poll_rigger")))
    }

    private fun decision(
        options: List<DecisionOption>,
        kind: DecisionKind = DecisionKind.ACTIVATE_ABILITY,
    ) = PendingDecision(DecisionId("d"), TestGame.HUMAN, kind, options)
}
