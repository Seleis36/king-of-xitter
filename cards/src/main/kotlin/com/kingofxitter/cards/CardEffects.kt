package com.kingofxitter.cards

import com.kingofxitter.engine.model.CardEffect

/** Every implemented card effect, for the engine's `CardEffectRegistry`. */
object CardEffects {
    val all: List<CardEffect> =
        ExecutiveOrders.effects +
            PassivePolicies.effects +
            ActivatedPolicies.effects +
            CountersAndTurns.effects +
            ComplexPolicies.effects
}
