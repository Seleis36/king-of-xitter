package com.kingofxitter.cards

import com.kingofxitter.engine.model.CardEffect
import com.kingofxitter.engine.model.CardEffectIntent
import com.kingofxitter.engine.model.CardHookContext
import com.kingofxitter.engine.model.CardId
import com.kingofxitter.engine.model.CounterId
import com.kingofxitter.engine.model.DamageAdjustment
import com.kingofxitter.engine.model.DiceFace
import com.kingofxitter.engine.model.PlayerResource

object PassivePolicies {
    val effects: List<CardEffect> =
        listOf(
            // C01 Acid Attack / Doomscroll Drip
            object : CardEffect {
                override val cardId = CardId("c01_doomscroll_drip")

                override fun modifyOutgoingDamage(context: CardHookContext) = DamageAdjustment(bonusDamage = 1)
            },
            // C02 Alien Metabolism / Lobbyist (Handled in ShopReducer)
            object : CardEffect {
                override val cardId = CardId("c02_lobbyist")
            },
            // C03 Alpha Monster / Alpha Poster
            object : CardEffect {
                override val cardId = CardId("c03_alpha_poster")

                override fun onResolve(context: CardHookContext): List<CardEffectIntent> {
                    val tweetCount = context.state.dice.count { it.face == DiceFace.TWEET }
                    val isXitter = context.state.xitterPlayerId == context.ownerId
                    val hasTarget = isXitter || context.state.xitterPlayerId != null
                    if (tweetCount > 0 && hasTarget) {
                        return listOf(CardEffectIntent.ChangeResource(context.ownerId, PlayerResource.MANDATES, 1))
                    }
                    return emptyList()
                }
            },
            // C04 Armor Plating / Thick Skin PR
            object : CardEffect {
                override val cardId = CardId("c04_thick_skin_pr")

                override fun modifyIncomingDamage(context: CardHookContext) =
                    if (context.damage == 1) {
                        DamageAdjustment(preventDamage = true)
                    } else {
                        DamageAdjustment()
                    }
            },
            // C06 Burrowing / Home Turf
            object : CardEffect {
                override val cardId = CardId("c06_home_turf")

                // Burrowing adds to a tweet from Xitter; it is not a hit on its own.
                override fun modifyOutgoingDamage(context: CardHookContext) =
                    if (context.state.xitterPlayerId == context.ownerId && context.damage > 0) {
                        DamageAdjustment(bonusDamage = 1)
                    } else {
                        DamageAdjustment()
                    }

                override fun onLogOff(context: CardHookContext): List<CardEffectIntent> =
                    context.targetPlayerId?.let { newOwnerId ->
                        listOf(CardEffectIntent.ChangeResource(newOwnerId, PlayerResource.FOLLOWERS, -1))
                    } ?: emptyList()
            },
            // C08 Complete Destruction / Full Bingo
            object : CardEffect {
                override val cardId = CardId("c08_full_bingo")

                override fun onResolve(context: CardHookContext): List<CardEffectIntent> {
                    val hasAll = DiceFace.entries.all { face -> context.state.dice.any { it.face == face } }
                    if (hasAll) {
                        return listOf(CardEffectIntent.ChangeResource(context.ownerId, PlayerResource.MANDATES, 9))
                    }
                    return emptyList()
                }
            },
            // C09 Dedicated News Team / Press Office
            object : CardEffect {
                override val cardId = CardId("c09_press_office")

                override fun onBuy(context: CardHookContext): List<CardEffectIntent> =
                    listOf(CardEffectIntent.ChangeResource(context.ownerId, PlayerResource.MANDATES, 1))
            },
            // C10 Eater of the Dead / Gravedancer
            object : CardEffect {
                override val cardId = CardId("c10_gravedancer")

                override fun onEliminated(context: CardHookContext): List<CardEffectIntent> =
                    if (context.eliminatedPlayerId != context.ownerId) {
                        listOf(CardEffectIntent.ChangeResource(context.ownerId, PlayerResource.MANDATES, 3))
                    } else {
                        emptyList()
                    }
            },
            // C11 Energy Hoarder / Drafts Folder
            object : CardEffect {
                override val cardId = CardId("c11_drafts_folder")

                override fun onTurnEnd(context: CardHookContext): List<CardEffectIntent> {
                    val chars =
                        context.state.players
                            .first { it.id == context.ownerId }
                            .characters
                    val mandates = chars / 60
                    if (mandates > 0) {
                        return listOf(CardEffectIntent.ChangeResource(context.ownerId, PlayerResource.MANDATES, mandates))
                    }
                    return emptyList()
                }
            },
            // C12 Even Bigger / Bot Farm
            object : CardEffect {
                override val cardId = CardId("c12_bot_farm")

                // The engine runs onBuy for every held policy on each purchase; this is one-time.
                override fun onBuy(context: CardHookContext): List<CardEffectIntent> =
                    if (context.purchasedCardId == cardId) {
                        listOf(
                            CardEffectIntent.ChangeFollowerLimit(context.ownerId, 2),
                            CardEffectIntent.ChangeResource(context.ownerId, PlayerResource.FOLLOWERS, 2),
                        )
                    } else {
                        emptyList()
                    }
            },
            // C13 Extra Head / Second Phone
            object : CardEffect {
                override val cardId = CardId("c13_second_phone")

                // The deck holds two copies, and the registry runs this effect once per card id.
                override fun diceCount(context: CardHookContext): Int =
                    context.state.players
                        .first { it.id == context.ownerId }
                        .policyIds
                        .count { it == cardId }
            },
            // C15 Fire Breathing / Quote Tweet
            object : CardEffect {
                override val cardId = CardId("c15_quote_tweet")

                override fun modifyOutgoingDamage(context: CardHookContext): DamageAdjustment {
                    if (context.damage > 0) {
                        val living = context.state.players.filterNot { it.isSuspended }
                        val ownerIndex = living.indexOfFirst { it.id == context.ownerId }
                        if (ownerIndex == -1) return DamageAdjustment()
                        val left = living[(ownerIndex - 1 + living.size) % living.size].id
                        val right = living[(ownerIndex + 1) % living.size].id
                        return DamageAdjustment(perTargetBonus = mapOf(left to 1, right to 1))
                    }
                    return DamageAdjustment()
                }
            },
            // C17 Friend of Children / Influencer Deal
            object : CardEffect {
                override val cardId = CardId("c17_influencer_deal")

                override fun modifyGainCharacters(context: CardHookContext): Int = 10
            },
            // C18 Giant Brain / Speechwriter
            object : CardEffect {
                override val cardId = CardId("c18_speechwriter")

                override fun rerollCount(context: CardHookContext): Int = 1
            },
            // C19 Gourmet / Old Guard
            object : CardEffect {
                override val cardId = CardId("c19_old_guard")

                override fun onResolve(context: CardHookContext): List<CardEffectIntent> {
                    val ones = context.state.dice.count { it.face == DiceFace.ONE }
                    if (ones >= 3) {
                        return listOf(CardEffectIntent.ChangeResource(context.ownerId, PlayerResource.MANDATES, 2))
                    }
                    return emptyList()
                }
            },
            // C21 Herbivore / Peace Summit
            object : CardEffect {
                override val cardId = CardId("c21_peace_summit")

                override fun onTurnEnd(context: CardHookContext): List<CardEffectIntent> {
                    if (context.state.turnUnfollowsDealt == 0) {
                        return listOf(CardEffectIntent.ChangeResource(context.ownerId, PlayerResource.MANDATES, 1))
                    }
                    return emptyList()
                }
            },
            // C24 Jets / Private Jet
            object : CardEffect {
                override val cardId = CardId("c24_private_jet")

                private val lastHit = CounterId("private_jet_last_hit")

                private fun recorded(context: CardHookContext): Int =
                    context.state.players
                        .first { it.id == context.ownerId }
                        .counters[lastHit] ?: 0

                // The engine applies an attack's damage before it offers the log-off choice and the
                // log-off hook does not carry that damage. Record the latest hit taken on Xitter as
                // the counter's exact value, so a log-off can undo it and an earlier hit never adds up.
                override fun onDamageReceived(context: CardHookContext): List<CardEffectIntent> =
                    if (context.state.xitterPlayerId == context.ownerId && context.damage != recorded(context)) {
                        listOf(CardEffectIntent.ChangeCounter(context.ownerId, lastHit, context.damage - recorded(context)))
                    } else {
                        emptyList()
                    }

                override fun onLogOff(context: CardHookContext): List<CardEffectIntent> {
                    val lost = recorded(context)
                    return if (lost > 0) {
                        listOf(
                            CardEffectIntent.ChangeResource(context.ownerId, PlayerResource.FOLLOWERS, lost),
                            CardEffectIntent.ChangeCounter(context.ownerId, lastHit, -lost),
                        )
                    } else {
                        emptyList()
                    }
                }
            },
            // C29 Nova Breath / Going Viral
            object : CardEffect {
                override val cardId = CardId("c29_going_viral")

                override fun modifyOutgoingDamage(context: CardHookContext) = DamageAdjustment(hitsAllOpponents = true)
            },
            // C30 Omnivore / Big Tent
            object : CardEffect {
                override val cardId = CardId("c30_big_tent")

                override fun onResolve(context: CardHookContext): List<CardEffectIntent> {
                    val has1 = context.state.dice.any { it.face == DiceFace.ONE }
                    val has2 = context.state.dice.any { it.face == DiceFace.TWO }
                    val has3 = context.state.dice.any { it.face == DiceFace.THREE }
                    if (has1 && has2 && has3) {
                        return listOf(CardEffectIntent.ChangeResource(context.ownerId, PlayerResource.MANDATES, 2))
                    }
                    return emptyList()
                }
            },
            // C34 Poison Quills / Fact Check
            object : CardEffect {
                override val cardId = CardId("c34_fact_check")

                override fun modifyOutgoingDamage(context: CardHookContext): DamageAdjustment =
                    if (context.state.dice.count { it.face == DiceFace.TWO } >= 3) {
                        DamageAdjustment(bonusDamage = 2)
                    } else {
                        DamageAdjustment()
                    }
            },
            // C38 Regeneration / Comeback Tour
            object : CardEffect {
                override val cardId = CardId("c38_comeback_tour")

                override fun modifyHealing(context: CardHookContext): Int = 1
            },
            // C39 Rooting for the Underdog / Underdog Narrative
            object : CardEffect {
                override val cardId = CardId("c39_underdog_narrative")

                override fun onTurnEnd(context: CardHookContext): List<CardEffectIntent> {
                    val myMandates =
                        context.state.players
                            .first { it.id == context.ownerId }
                            .mandates
                    val others =
                        context.state.players
                            .filter { it.id != context.ownerId && !it.isSuspended }
                    if (others.isNotEmpty() && others.all { it.mandates > myMandates }) {
                        return listOf(CardEffectIntent.ChangeResource(context.ownerId, PlayerResource.MANDATES, 1))
                    }
                    return emptyList()
                }
            },
            // C42 Solar Powered / Ghostwriter
            object : CardEffect {
                override val cardId = CardId("c42_ghostwriter")

                override fun onTurnEnd(context: CardHookContext): List<CardEffectIntent> {
                    val chars =
                        context.state.players
                            .first { it.id == context.ownerId }
                            .characters
                    if (chars == 0) {
                        return listOf(CardEffectIntent.ChangeResource(context.ownerId, PlayerResource.CHARACTERS, 10))
                    }
                    return emptyList()
                }
            },
            // C43 Spiked Tail / CAPS LOCK STUCK
            object : CardEffect {
                override val cardId = CardId("c43_caps_lock_stuck")

                override fun modifyOutgoingDamage(context: CardHookContext): DamageAdjustment =
                    if (context.damage > 0) {
                        DamageAdjustment(bonusDamage = 1)
                    } else {
                        DamageAdjustment()
                    }
            },
            // C46 Urbavore / Main Character Energy
            object : CardEffect {
                override val cardId = CardId("c46_main_character_energy")

                override fun onTurnStart(context: CardHookContext): List<CardEffectIntent> =
                    if (context.state.xitterPlayerId == context.ownerId) {
                        listOf(CardEffectIntent.ChangeResource(context.ownerId, PlayerResource.MANDATES, 1))
                    } else {
                        emptyList()
                    }

                // Urbavore adds to a tweet from Xitter; it is not a hit on its own.
                override fun modifyOutgoingDamage(context: CardHookContext): DamageAdjustment =
                    if (context.state.xitterPlayerId == context.ownerId && context.damage > 0) {
                        DamageAdjustment(bonusDamage = 1)
                    } else {
                        DamageAdjustment()
                    }
            },
            // C47 We're Only Making It Stronger / Martyr Complex
            object : CardEffect {
                override val cardId = CardId("c47_martyr_complex")

                override fun onDamageReceived(context: CardHookContext): List<CardEffectIntent> =
                    if (context.damage >= 2) {
                        listOf(CardEffectIntent.ChangeResource(context.ownerId, PlayerResource.CHARACTERS, 10))
                    } else {
                        emptyList()
                    }
            },
        )
}
