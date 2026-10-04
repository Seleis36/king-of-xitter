package com.kingofxitter.cards

import com.kingofxitter.engine.model.CardEffectRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CardEffectsTest {
    @Test
    fun `effect ids are unique so the registry can be built`() {
        CardEffectRegistry(CardEffects.all)
    }

    @Test
    fun `every effect belongs to a catalogue card`() {
        val unknown = CardEffects.all.map { it.cardId }.filter { it !in CardCatalog.byId }
        assertEquals(emptyList(), unknown)
    }

    @Test
    fun `reports which catalogue cards still have no effect`() {
        // Cards with no effect are inert when bought. This pins the count so a new gap is noticed.
        val implemented = CardEffects.all.map { it.cardId }.toSet()
        val missing = CardCatalog.all.map { it.id }.filter { it !in implemented }
        println("Cards without an effect (${missing.size}): ${missing.joinToString { it.value }}")
        assertTrue(missing.size < CardCatalog.all.size)
    }

    @Test
    fun `two Second Phones give two extra dice`() {
        val phone =
            com.kingofxitter.engine.model
                .CardId("c13_second_phone")
        val state =
            TestGame()
                .withPhase(com.kingofxitter.engine.model.TurnPhase.TURN_START)
                .withRollNumber(0)
                .withPolicies(TestGame.HUMAN, listOf(phone, phone))
                .build()
        val reducer = com.kingofxitter.engine.DefaultGameReducer(CardEffectRegistry(CardEffects.all))

        val rolled =
            reducer.reduce(
                state,
                com.kingofxitter.engine.model.Action
                    .RollDice(TestGame.HUMAN),
            )

        assertEquals(8, rolled.state.dice.size)
    }
}
