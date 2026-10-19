package com.kingofxitter.cards

import com.kingofxitter.engine.model.AbilityOffer
import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.CardEffect
import com.kingofxitter.engine.model.CardEffectIntent
import com.kingofxitter.engine.model.CardHookContext
import com.kingofxitter.engine.model.CardHookWindow
import com.kingofxitter.engine.model.CardId
import com.kingofxitter.engine.model.DamageAdjustment
import com.kingofxitter.engine.model.DecisionOption
import com.kingofxitter.engine.model.DecisionOptionId
import com.kingofxitter.engine.model.DiceFace
import com.kingofxitter.engine.model.PlayerId
import com.kingofxitter.engine.model.PlayerResource

object ComplexPolicies {
    val effects: List<CardEffect> =
        listOf(
            // c07_parody_account: When hit, roll 1 die per hit; each like cancels one
            object : CardEffect {
                override val cardId = CardId("c07_parody_account")

                override fun modifyIncomingDamage(context: CardHookContext): DamageAdjustment {
                    if (context.damage <= 0) return DamageAdjustment()
                    var cancelled = 0
                    for (i in 0 until context.damage) {
                        val face = DiceFace.entries[context.random.nextInt(DiceFace.entries.size)]
                        if (face == DiceFace.LIKE) cancelled++
                    }
                    return DamageAdjustment(damageReduction = cancelled)
                }
            },
            // c20_retweet_for_retweet: Your likes can heal others; they pay you 20 chars per 1M
            object : CardEffect {
                override val cardId = CardId("c20_retweet_for_retweet")

                override fun activatedAbilities(context: CardHookContext): List<AbilityOffer> {
                    if (context.window != CardHookWindow.AFTER_DICE_ROLL) return emptyList()
                    if (context.ownerId != context.state.currentPlayerId) return emptyList()
                    val availableLikes = context.state.dice.filter { it.face == DiceFace.LIKE }
                    if (availableLikes.isEmpty()) return emptyList()

                    val targets =
                        context.state.players.filter {
                            it.id != context.ownerId && !it.isSuspended && it.followers < it.maxFollowers && it.characters >= 20
                        }
                    if (targets.isEmpty()) return emptyList()

                    return listOf(
                        AbilityOffer(
                            abilityId = "sell_like",
                            promptKey = "ability_sell_like_prompt",
                            options =
                                targets.map { target ->
                                    DecisionOption(
                                        DecisionOptionId("sell_like_${target.id.value}"),
                                        Action.ActivateAbility(
                                            playerId = context.ownerId,
                                            cardId = cardId,
                                            abilityId = "sell_like",
                                            targetPlayerId = target.id,
                                            dieIds = setOf(availableLikes.first().id),
                                        ),
                                    )
                                },
                        ),
                    )
                }

                override fun activate(
                    context: CardHookContext,
                    action: Action.ActivateAbility,
                ): List<CardEffectIntent> {
                    if (action.abilityId == "sell_like") {
                        val targetId = action.targetPlayerId ?: return emptyList()
                        val dieId = action.dieIds.firstOrNull() ?: return emptyList()
                        return listOf(
                            CardEffectIntent.ConsumeDie(context.ownerId, dieId),
                            CardEffectIntent.ChangeResource(targetId, PlayerResource.FOLLOWERS, 1),
                            CardEffectIntent.ChangeResource(targetId, PlayerResource.CHARACTERS, -20),
                            CardEffectIntent.ChangeResource(context.ownerId, PlayerResource.CHARACTERS, 20),
                        )
                    }
                    return emptyList()
                }
            },
            // c23_burner_account: When suspended: discard all cards, lose all mandates, return with 10M followers
            object : CardEffect {
                override val cardId = CardId("c23_burner_account")

                override fun onEliminated(context: CardHookContext): List<CardEffectIntent> {
                    // The engine runs this hook for every policy holder when anyone is suspended.
                    if (context.eliminatedPlayerId != context.ownerId) return emptyList()
                    val owner = context.state.players.firstOrNull { it.id == context.ownerId } ?: return emptyList()
                    return buildList {
                        // Discard all policies
                        for (policy in owner.policyIds) {
                            add(CardEffectIntent.DiscardPolicy(context.ownerId, policy))
                        }
                        // Lose all mandates
                        if (owner.mandates > 0) {
                            add(CardEffectIntent.ChangeResource(context.ownerId, PlayerResource.MANDATES, -owner.mandates))
                        }
                        // Return with 10M followers
                        add(CardEffectIntent.ChangeResource(context.ownerId, PlayerResource.FOLLOWERS, 10))
                    }
                }
            },
            // c25_leaked_memo: Peek at and buy the top card of the deck
            object : CardEffect {
                override val cardId = CardId("c25_leaked_memo")

                override fun activatedAbilities(context: CardHookContext): List<AbilityOffer> {
                    if (context.window != CardHookWindow.AFTER_RESOLVE) return emptyList()
                    if (context.ownerId != context.state.currentPlayerId) return emptyList()
                    val topCardId = context.state.drawPileCardIds.firstOrNull() ?: return emptyList()
                    val topCardDef = CardCatalog.byId[topCardId] ?: return emptyList()

                    val owner = context.state.players.firstOrNull { it.id == context.ownerId } ?: return emptyList()
                    if (owner.characters < topCardDef.cost) return emptyList()

                    return listOf(
                        AbilityOffer(
                            abilityId = "buy_top_card",
                            promptKey = "ability_buy_top_card_prompt",
                            options =
                                listOf(
                                    DecisionOption(
                                        DecisionOptionId("buy"),
                                        Action.ActivateAbility(
                                            playerId = context.ownerId,
                                            cardId = cardId,
                                            abilityId = "buy_top_card",
                                        ),
                                    ),
                                ),
                        ),
                    )
                }

                override fun activate(
                    context: CardHookContext,
                    action: Action.ActivateAbility,
                ): List<CardEffectIntent> {
                    val topCardId = context.state.drawPileCardIds.firstOrNull() ?: return emptyList()
                    val topCardDef = CardCatalog.byId[topCardId] ?: return emptyList()
                    val isEo = topCardDef.type == CardType.EXECUTIVE_ORDER
                    return listOf(CardEffectIntent.BuyTopCard(context.ownerId, topCardDef.cost, isEo))
                }
            },
            // c26_pivot: Once per turn, discard a policy for its full cost in chars
            object : CardEffect {
                override val cardId = CardId("c26_pivot")

                override fun activatedAbilities(context: CardHookContext): List<AbilityOffer> {
                    if (context.window != CardHookWindow.AFTER_RESOLVE) return emptyList()
                    if (context.ownerId != context.state.currentPlayerId) return emptyList()
                    val owner = context.state.players.firstOrNull { it.id == context.ownerId } ?: return emptyList()
                    val policiesToDiscard = owner.policyIds.filter { it != cardId }
                    if (policiesToDiscard.isEmpty()) return emptyList()

                    return listOf(
                        AbilityOffer(
                            abilityId = "discard_policy",
                            promptKey = "ability_discard_policy_prompt",
                            options =
                                policiesToDiscard.map { policyId ->
                                    DecisionOption(
                                        DecisionOptionId("discard_${policyId.value}"),
                                        Action.ActivateAbility(
                                            playerId = context.ownerId,
                                            cardId = cardId,
                                            abilityId = "discard_policy:${policyId.value}",
                                        ),
                                    )
                                },
                        ),
                    )
                }

                override fun activate(
                    context: CardHookContext,
                    action: Action.ActivateAbility,
                ): List<CardEffectIntent> {
                    val policyToDiscard = CardId(action.abilityId.substringAfter(":"))
                    val owner = context.state.players.firstOrNull { it.id == context.ownerId } ?: return emptyList()
                    if (policyToDiscard !in owner.policyIds || policyToDiscard == cardId) return emptyList()
                    val def = CardCatalog.byId[policyToDiscard] ?: return emptyList()
                    return listOf(
                        CardEffectIntent.DiscardPolicy(context.ownerId, policyToDiscard),
                        CardEffectIntent.ChangeResource(context.ownerId, PlayerResource.CHARACTERS, def.cost),
                    )
                }
            },
            // c32_hostile_takeover: Buy other leaders' policies by paying them
            object : CardEffect {
                override val cardId = CardId("c32_hostile_takeover")

                override fun activatedAbilities(context: CardHookContext): List<AbilityOffer> {
                    if (context.window != CardHookWindow.AFTER_RESOLVE) return emptyList()
                    if (context.ownerId != context.state.currentPlayerId) return emptyList()

                    val owner = context.state.players.firstOrNull { it.id == context.ownerId } ?: return emptyList()
                    val options = mutableListOf<DecisionOption>()

                    for (other in context.state.players) {
                        if (other.id == context.ownerId || other.isSuspended) continue
                        for (policy in other.policyIds) {
                            val def = CardCatalog.byId[policy] ?: continue
                            if (owner.characters >= def.cost) {
                                options.add(
                                    DecisionOption(
                                        DecisionOptionId("buy_${other.id.value}_${policy.value}"),
                                        Action.ActivateAbility(
                                            playerId = context.ownerId,
                                            cardId = cardId,
                                            abilityId = "buy_policy:${other.id.value}:${policy.value}",
                                        ),
                                    ),
                                )
                            }
                        }
                    }

                    if (options.isEmpty()) return emptyList()

                    return listOf(
                        AbilityOffer(
                            abilityId = "buy_policy",
                            promptKey = "ability_hostile_takeover_prompt",
                            options = options,
                        ),
                    )
                }

                override fun activate(
                    context: CardHookContext,
                    action: Action.ActivateAbility,
                ): List<CardEffectIntent> {
                    val parts = action.abilityId.split(":")
                    if (parts.size != 3) return emptyList()
                    val targetId = PlayerId(parts[1])
                    val policyId = CardId(parts[2])
                    val target = context.state.players.firstOrNull { it.id == targetId } ?: return emptyList()
                    if (policyId !in target.policyIds) return emptyList()
                    val def = CardCatalog.byId[policyId] ?: return emptyList()
                    return listOf(
                        CardEffectIntent.TransferPolicy(targetId, context.ownerId, policyId),
                        CardEffectIntent.ChangeResource(context.ownerId, PlayerResource.CHARACTERS, -def.cost),
                        CardEffectIntent.ChangeResource(targetId, PlayerResource.CHARACTERS, def.cost),
                    )
                }
            },
            // c27_copycat_policy: Copy another leader's policy; pay 10 chars to switch
            object : CardEffect {
                override val cardId = CardId("c27_copycat_policy")

                override fun activatedAbilities(context: CardHookContext): List<AbilityOffer> {
                    if (context.window != CardHookWindow.AFTER_RESOLVE) return emptyList()
                    if (context.ownerId != context.state.currentPlayerId) return emptyList()
                    val owner = context.state.players.firstOrNull { it.id == context.ownerId } ?: return emptyList()
                    val switching = owner.copiedPolicyId != null
                    if (switching && owner.characters < SWITCH_COST) return emptyList()

                    val options =
                        context.state.players
                            .filter { it.id != context.ownerId && !it.isSuspended }
                            .flatMap { it.policyIds }
                            .filter { it != cardId && it != owner.copiedPolicyId && it !in owner.policyIds }
                            .distinct()
                            .map { policyId ->
                                DecisionOption(
                                    DecisionOptionId("copy_${policyId.value}"),
                                    Action.ActivateAbility(
                                        playerId = context.ownerId,
                                        cardId = cardId,
                                        abilityId = "copy_policy:${policyId.value}",
                                    ),
                                )
                            }
                    if (options.isEmpty()) return emptyList()
                    return listOf(AbilityOffer("copy_policy", "ability_copycat_prompt", options))
                }

                override fun activate(
                    context: CardHookContext,
                    action: Action.ActivateAbility,
                ): List<CardEffectIntent> {
                    val policyId = CardId(action.abilityId.substringAfter(":"))
                    val owner = context.state.players.firstOrNull { it.id == context.ownerId } ?: return emptyList()
                    val held = context.state.players.any { it.id != context.ownerId && policyId in it.policyIds }
                    if (!held) return emptyList()
                    val intents = mutableListOf<CardEffectIntent>(CardEffectIntent.SetCopiedPolicy(context.ownerId, policyId))
                    if (owner.copiedPolicyId != null) {
                        intents += CardEffectIntent.ChangeResource(context.ownerId, PlayerResource.CHARACTERS, -SWITCH_COST)
                    }
                    return intents
                }

                private val SWITCH_COST = 10
            },
            // c31_breaking_news_alerts: buying newly revealed cards out of turn is handled by ShopReducer.
            object : CardEffect {
                override val cardId = CardId("c31_breaking_news_alerts")
            },
        )
}
