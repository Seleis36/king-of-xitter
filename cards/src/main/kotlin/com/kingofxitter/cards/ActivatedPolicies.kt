package com.kingofxitter.cards

import com.kingofxitter.engine.model.AbilityOffer
import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.CardEffect
import com.kingofxitter.engine.model.CardEffectIntent
import com.kingofxitter.engine.model.CardHookContext
import com.kingofxitter.engine.model.CardHookWindow
import com.kingofxitter.engine.model.CardId
import com.kingofxitter.engine.model.CounterId
import com.kingofxitter.engine.model.DamageAdjustment
import com.kingofxitter.engine.model.DecisionOption
import com.kingofxitter.engine.model.DecisionOptionId
import com.kingofxitter.engine.model.DiceFace
import com.kingofxitter.engine.model.PlayerResource

object ActivatedPolicies {
    private val goingViral = CardId("c29_going_viral")

    /**
     * Whether the current roll's tweets could reach the owner: someone else is rolling, a tweet
     * is showing, and the owner is in the line of fire (one of them holds Xitter, or the roller's
     * tweets hit everyone).
     */
    private fun couldBeHit(context: CardHookContext): Boolean {
        val state = context.state
        val roller = state.players.firstOrNull { it.id == state.currentPlayerId } ?: return false
        if (roller.id == context.ownerId || state.dice.none { it.face == DiceFace.TWEET }) return false
        val xitter = state.xitterPlayerId
        return xitter == roller.id || xitter == context.ownerId || goingViral in roller.policyIds || goingViral == roller.copiedPolicyId
    }

    val effects: List<CardEffect> =
        listOf(
            // c05_lurker
            object : CardEffect {
                override val cardId = CardId("c05_lurker")

                override fun activatedAbilities(context: CardHookContext): List<AbilityOffer> {
                    if (context.window != CardHookWindow.AFTER_DICE_ROLL) return emptyList()
                    if (context.ownerId != context.state.currentPlayerId) return emptyList()

                    val threes = context.state.dice.filter { it.face == DiceFace.THREE && !it.isKept }
                    if (threes.isEmpty()) return emptyList()

                    return listOf(
                        AbilityOffer(
                            abilityId = "reroll_3s",
                            promptKey = "card_c05_prompt",
                            options =
                                threes.map { die ->
                                    DecisionOption(
                                        id = DecisionOptionId(die.id.value.toString()),
                                        action = Action.ActivateAbility(context.ownerId, cardId, "reroll_3s", dieIds = setOf(die.id)),
                                        labelKey = "card_c05_option",
                                    )
                                },
                        ),
                    )
                }

                override fun activate(
                    context: CardHookContext,
                    action: Action.ActivateAbility,
                ): List<CardEffectIntent> {
                    // The engine offers abilities once per roll, so one use rerolls every 3 the owner holds.
                    // The chosen die only selects the option; all unkept 3s are rerolled in dice order.
                    if (action.dieIds.isEmpty()) return emptyList()
                    return context.state.dice
                        .filter { it.face == DiceFace.THREE && !it.isKept }
                        .map { die -> CardEffectIntent.ChangeDie(context.ownerId, die.id, DiceFace.entries[context.random.nextInt(6)]) }
                }
            },
            // c22_poll_rigger
            object : CardEffect {
                override val cardId = CardId("c22_poll_rigger")

                override fun activatedAbilities(context: CardHookContext): List<AbilityOffer> {
                    if (context.window != CardHookWindow.AFTER_DICE_ROLL && context.window != CardHookWindow.ON_RESOLVE) return emptyList()
                    // Herd Culler changes one of your own dice, so it only comes up on your turn.
                    if (context.ownerId != context.state.currentPlayerId) return emptyList()

                    val owner = context.state.players.first { it.id == context.ownerId }
                    if (owner.counters[CounterId("poll_rigger_${context.state.turnNumber}")] == 1) return emptyList()
                    val changeable = context.state.dice.filter { it.face != DiceFace.ONE }
                    if (changeable.isEmpty()) return emptyList()

                    return listOf(
                        AbilityOffer(
                            abilityId = "use_poll_rigger",
                            promptKey = "card_c22_prompt",
                            options =
                                changeable.map { die ->
                                    DecisionOption(
                                        id = DecisionOptionId(die.id.value.toString()),
                                        action = Action.ActivateAbility(context.ownerId, cardId, "use_poll_rigger", dieIds = setOf(die.id)),
                                        labelKey = "card_c22_option",
                                    )
                                },
                        ),
                    )
                }

                override fun activate(
                    context: CardHookContext,
                    action: Action.ActivateAbility,
                ): List<CardEffectIntent> {
                    val dieId = action.dieIds.firstOrNull() ?: return emptyList()
                    return listOf(
                        CardEffectIntent.ChangeDie(context.ownerId, dieId, DiceFace.ONE),
                        CardEffectIntent.ChangeCounter(context.ownerId, CounterId("poll_rigger_${context.state.turnNumber}"), 1),
                    )
                }
            },
            // c33_october_surprise
            object : CardEffect {
                override val cardId = CardId("c33_october_surprise")

                override fun activatedAbilities(context: CardHookContext): List<AbilityOffer> {
                    if (context.window != CardHookWindow.AFTER_DICE_ROLL && context.window != CardHookWindow.ON_RESOLVE) return emptyList()
                    // Plot Twist changes one of your own dice, so it only comes up on your turn.
                    if (context.ownerId != context.state.currentPlayerId) return emptyList()

                    return listOf(
                        AbilityOffer(
                            abilityId = "use_october_surprise",
                            promptKey = "card_c33_prompt",
                            options =
                                context.state.dice.flatMap { die ->
                                    DiceFace.entries.filter { it != die.face }.map { face ->
                                        DecisionOption(
                                            id = DecisionOptionId("${die.id.value}_${face.name}"),
                                            action =
                                                Action.ActivateAbility(
                                                    context.ownerId,
                                                    cardId,
                                                    "use_october_surprise:${face.name}",
                                                    dieIds = setOf(die.id),
                                                ),
                                            labelKey = "card_c33_option",
                                        )
                                    }
                                },
                        ),
                    )
                }

                override fun activate(
                    context: CardHookContext,
                    action: Action.ActivateAbility,
                ): List<CardEffectIntent> {
                    val dieId = action.dieIds.firstOrNull() ?: return emptyList()
                    val face = DiceFace.valueOf(action.abilityId.substringAfter(":"))
                    return listOf(
                        CardEffectIntent.ChangeDie(context.ownerId, dieId, face),
                        CardEffectIntent.DiscardPolicy(context.ownerId, cardId),
                    )
                }
            },
            // c36_phone_tap
            object : CardEffect {
                override val cardId = CardId("c36_phone_tap")

                override fun activatedAbilities(context: CardHookContext): List<AbilityOffer> {
                    if (context.window != CardHookWindow.AFTER_DICE_ROLL) return emptyList()
                    if (context.ownerId == context.state.currentPlayerId) return emptyList()

                    val owner = context.state.players.first { it.id == context.ownerId }
                    if (owner.counters[CounterId("phone_tap_${context.state.turnNumber}")] == 1) return emptyList()

                    return listOf(
                        AbilityOffer(
                            abilityId = "use_phone_tap",
                            promptKey = "card_c36_prompt",
                            options =
                                context.state.dice.map { die ->
                                    DecisionOption(
                                        id = DecisionOptionId(die.id.value.toString()),
                                        action = Action.ActivateAbility(context.ownerId, cardId, "use_phone_tap", dieIds = setOf(die.id)),
                                        labelKey = "card_c36_option",
                                    )
                                },
                        ),
                    )
                }

                override fun activate(
                    context: CardHookContext,
                    action: Action.ActivateAbility,
                ): List<CardEffectIntent> {
                    val dieId = action.dieIds.firstOrNull() ?: return emptyList()
                    val newFace = DiceFace.entries[context.random.nextInt(6)]
                    val intents =
                        mutableListOf<CardEffectIntent>(
                            CardEffectIntent.ChangeCounter(context.ownerId, CounterId("phone_tap_${context.state.turnNumber}"), 1),
                            CardEffectIntent.ChangeDie(context.state.currentPlayerId, dieId, newFace),
                        )
                    if (newFace == DiceFace.LIKE) {
                        intents.add(CardEffectIntent.DiscardPolicy(context.ownerId, cardId))
                    }
                    return intents
                }
            },
            // c37_paid_promotion
            object : CardEffect {
                override val cardId = CardId("c37_paid_promotion")

                override fun activatedAbilities(context: CardHookContext): List<AbilityOffer> {
                    if (context.window != CardHookWindow.AFTER_DICE_ROLL && context.window != CardHookWindow.ON_RESOLVE) return emptyList()

                    val owner = context.state.players.first { it.id == context.ownerId }
                    if (owner.characters < 20) return emptyList()
                    // Usable at any time, but only worth asking about when a follower is missing.
                    if (owner.followers >= owner.maxFollowers) return emptyList()

                    return listOf(
                        AbilityOffer(
                            abilityId = "use_paid_promotion",
                            promptKey = "card_c37_prompt",
                            options =
                                listOf(
                                    DecisionOption(
                                        id = DecisionOptionId("buy_followers"),
                                        action = Action.ActivateAbility(context.ownerId, cardId, "use_paid_promotion"),
                                        labelKey = "card_c37_option",
                                    ),
                                ),
                        ),
                    )
                }

                override fun activate(
                    context: CardHookContext,
                    action: Action.ActivateAbility,
                ): List<CardEffectIntent> =
                    listOf(
                        CardEffectIntent.ChangeResource(context.ownerId, PlayerResource.CHARACTERS, -20),
                        CardEffectIntent.ChangeResource(context.ownerId, PlayerResource.FOLLOWERS, 1),
                    )
            },
            // c44_spin_doctor
            object : CardEffect {
                override val cardId = CardId("c44_spin_doctor")

                override fun activatedAbilities(context: CardHookContext): List<AbilityOffer> {
                    if (context.window != CardHookWindow.AFTER_DICE_ROLL && context.window != CardHookWindow.ON_RESOLVE) return emptyList()

                    // Stretchy changes one of your own dice before they resolve: your turn only.
                    if (context.ownerId != context.state.currentPlayerId) return emptyList()
                    val owner = context.state.players.first { it.id == context.ownerId }
                    if (owner.characters < 20) return emptyList()

                    return listOf(
                        AbilityOffer(
                            abilityId = "use_spin_doctor",
                            promptKey = "card_c44_prompt",
                            options =
                                context.state.dice.flatMap { die ->
                                    DiceFace.entries.filter { it != die.face }.map { face ->
                                        DecisionOption(
                                            id = DecisionOptionId("${die.id.value}_${face.name}"),
                                            action =
                                                Action.ActivateAbility(
                                                    context.ownerId,
                                                    cardId,
                                                    "use_spin_doctor:${face.name}",
                                                    dieIds = setOf(die.id),
                                                ),
                                            labelKey = "card_c44_option",
                                        )
                                    }
                                },
                        ),
                    )
                }

                override fun activate(
                    context: CardHookContext,
                    action: Action.ActivateAbility,
                ): List<CardEffectIntent> {
                    val dieId = action.dieIds.firstOrNull() ?: return emptyList()
                    val face = DiceFace.valueOf(action.abilityId.substringAfter(":"))
                    return listOf(
                        CardEffectIntent.ChangeResource(context.ownerId, PlayerResource.CHARACTERS, -20),
                        CardEffectIntent.ChangeDie(context.state.currentPlayerId, dieId, face),
                    )
                }
            },
            // c45_focus_group
            object : CardEffect {
                override val cardId = CardId("c45_focus_group")

                override fun activatedAbilities(context: CardHookContext): List<AbilityOffer> {
                    if (context.window != CardHookWindow.AFTER_DICE_ROLL) return emptyList()
                    if (context.ownerId != context.state.currentPlayerId) return emptyList()

                    val owner = context.state.players.first { it.id == context.ownerId }
                    if (owner.characters < 10) return emptyList()

                    return listOf(
                        AbilityOffer(
                            abilityId = "buy_reroll",
                            promptKey = "card_c45_prompt",
                            options =
                                listOf(
                                    DecisionOption(
                                        id = DecisionOptionId("buy_reroll_option"),
                                        action = Action.ActivateAbility(context.ownerId, cardId, "buy_reroll"),
                                        labelKey = "card_c45_option",
                                    ),
                                ),
                        ),
                    )
                }

                override fun activate(
                    context: CardHookContext,
                    action: Action.ActivateAbility,
                ): List<CardEffectIntent> =
                    listOf(
                        CardEffectIntent.ChangeResource(context.ownerId, PlayerResource.CHARACTERS, -10),
                        CardEffectIntent.ChangeRerollCount(context.ownerId, 1, thisTurnOnly = true),
                    )
            },
            // c48_touch_grass
            object : CardEffect {
                override val cardId = CardId("c48_touch_grass")

                override fun activatedAbilities(context: CardHookContext): List<AbilityOffer> {
                    if (context.window != CardHookWindow.AFTER_DICE_ROLL && context.window != CardHookWindow.ON_RESOLVE) return emptyList()

                    val owner = context.state.players.first { it.id == context.ownerId }
                    if (owner.characters < 20) return emptyList()
                    if (owner.counters[CounterId("touch_grass_${context.state.turnNumber}")] == 1) return emptyList()
                    // Wings negates hits, so it is only offered on a roll whose tweets could reach you.
                    if (!couldBeHit(context)) return emptyList()

                    return listOf(
                        AbilityOffer(
                            abilityId = "use_touch_grass",
                            promptKey = "card_c48_prompt",
                            options =
                                listOf(
                                    DecisionOption(
                                        id = DecisionOptionId("buy"),
                                        action = Action.ActivateAbility(context.ownerId, cardId, "use_touch_grass"),
                                        labelKey = "card_c48_option",
                                    ),
                                ),
                        ),
                    )
                }

                override fun activate(
                    context: CardHookContext,
                    action: Action.ActivateAbility,
                ): List<CardEffectIntent> =
                    listOf(
                        CardEffectIntent.ChangeResource(context.ownerId, PlayerResource.CHARACTERS, -20),
                        CardEffectIntent.ChangeCounter(context.ownerId, CounterId("touch_grass_${context.state.turnNumber}"), 1),
                    )

                override fun modifyIncomingDamage(context: CardHookContext): DamageAdjustment {
                    val owner = context.state.players.first { it.id == context.ownerId }
                    if (owner.counters[CounterId("touch_grass_${context.state.turnNumber}")] == 1) {
                        return DamageAdjustment(preventDamage = true)
                    }
                    return DamageAdjustment()
                }
            },
        )
}
