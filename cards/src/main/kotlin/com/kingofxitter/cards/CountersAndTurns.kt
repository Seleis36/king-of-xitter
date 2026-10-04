package com.kingofxitter.cards

import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.AbilityOffer
import com.kingofxitter.engine.model.CardEffect
import com.kingofxitter.engine.model.CardEffectIntent
import com.kingofxitter.engine.model.CardHookContext
import com.kingofxitter.engine.model.CardHookWindow
import com.kingofxitter.engine.model.CardId
import com.kingofxitter.engine.model.CounterId
import com.kingofxitter.engine.model.DecisionOption
import com.kingofxitter.engine.model.DecisionOptionId
import com.kingofxitter.engine.model.DiceFace
import com.kingofxitter.engine.model.PlayerResource

object CountersAndTurns {
    val effects: List<CardEffect> = listOf(
        // c16_pinned_tweet (Freeze Time)
        object : CardEffect {
            override val cardId = CardId("c16_pinned_tweet")

            override fun onResolve(context: CardHookContext): List<CardEffectIntent> {
                val ones = context.state.dice.count { it.face == DiceFace.ONE }
                if (ones >= 3) {
                    return listOf(
                        CardEffectIntent.ChangeExtraTurns(context.ownerId, 1),
                        CardEffectIntent.ChangeDiceCount(context.ownerId, -1)
                    )
                }
                return emptyList()
            }
        },

        // c28_super_pac (Monster Batteries)
        object : CardEffect {
            override val cardId = CardId("c28_super_pac")

            override fun onBuy(context: CardHookContext): List<CardEffectIntent> {
                return listOf(
                    CardEffectIntent.ChangeCounter(context.ownerId, CounterId("super_pac"), 60)
                )
            }

            override fun onTurnStart(context: CardHookContext): List<CardEffectIntent> {
                val current = context.state.players.first { it.id == context.ownerId }.counters[CounterId("super_pac")] ?: 0
                if (current <= 0) return emptyList()
                val take = minOf(20, current)
                val intents = mutableListOf<CardEffectIntent>()
                intents += CardEffectIntent.ChangeCounter(context.ownerId, CounterId("super_pac"), -take)
                intents += CardEffectIntent.ChangeResource(context.ownerId, PlayerResource.CHARACTERS, take)
                if (current - take <= 0) {
                    intents += CardEffectIntent.DiscardPolicy(context.ownerId, cardId)
                }
                return intents
            }
        },

        // c35_cancel_campaign (Poison Spit)
        object : CardEffect {
            override val cardId = CardId("c35_cancel_campaign")

            override fun onDamageDealt(context: CardHookContext): List<CardEffectIntent> {
                return context.targetPlayerIds.map { targetId ->
                    CardEffectIntent.ChangeCounter(targetId, CounterId("cancelled"), 1)
                }
            }
        },

        // c40_shadowban (Shrink Ray)
        object : CardEffect {
            override val cardId = CardId("c40_shadowban")

            override fun onDamageDealt(context: CardHookContext): List<CardEffectIntent> {
                return context.targetPlayerIds.map { targetId ->
                    CardEffectIntent.ChangeCounter(targetId, CounterId("shadowban"), 1)
                }
            }
        },

        // c41_smoke_and_mirrors (Smoke Cloud)
        object : CardEffect {
            override val cardId = CardId("c41_smoke_and_mirrors")

            override fun onBuy(context: CardHookContext) = listOf(
                CardEffectIntent.ChangeCounter(context.ownerId, CounterId("smoke_cloud"), 3)
            )

            override fun activatedAbilities(context: CardHookContext): List<AbilityOffer> {
                if (context.window != CardHookWindow.AFTER_DICE_ROLL) return emptyList()
                val current = context.state.players.first { it.id == context.ownerId }.counters[CounterId("smoke_cloud")] ?: 0
                if (current > 0) {
                    return listOf(
                        AbilityOffer(
                            abilityId = "spend_smoke_cloud",
                            promptKey = "prompt_spend_smoke_cloud",
                            options = listOf(
                                DecisionOption(
                                    DecisionOptionId("spend"),
                                    Action.ActivateAbility(context.ownerId, cardId, "spend_smoke_cloud")
                                )
                            )
                        )
                    )
                }
                return emptyList()
            }

            override fun activate(context: CardHookContext, action: Action.ActivateAbility): List<CardEffectIntent> {
                val current = context.state.players.first { it.id == context.ownerId }.counters[CounterId("smoke_cloud")] ?: 0
                if (current > 0 && action.abilityId == "spend_smoke_cloud") {
                    val intents = mutableListOf<CardEffectIntent>()
                    intents += CardEffectIntent.ChangeCounter(context.ownerId, CounterId("smoke_cloud"), -1)
                    intents += CardEffectIntent.ChangeCounter(context.ownerId, CounterId("smoke_cloud_spent_this_turn"), 1)
                    intents += CardEffectIntent.ChangeRerollCount(context.ownerId, 1)
                    if (current - 1 <= 0) {
                        intents += CardEffectIntent.DiscardPolicy(context.ownerId, cardId)
                    }
                    return intents
                }
                return emptyList()
            }

            override fun onTurnEnd(context: CardHookContext): List<CardEffectIntent> {
                val spent = context.state.players.first { it.id == context.ownerId }.counters[CounterId("smoke_cloud_spent_this_turn")] ?: 0
                if (spent > 0) {
                    return listOf(
                        CardEffectIntent.ChangeCounter(context.ownerId, CounterId("smoke_cloud_spent_this_turn"), -spent),
                        CardEffectIntent.ChangeRerollCount(context.ownerId, -spent)
                    )
                }
                return emptyList()
            }
        }
    )
}
