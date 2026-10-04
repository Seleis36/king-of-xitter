package com.kingofxitter.cards

import com.kingofxitter.engine.model.CardEffect
import com.kingofxitter.engine.model.CardEffectIntent
import com.kingofxitter.engine.model.CardHookContext
import com.kingofxitter.engine.model.CardId
import com.kingofxitter.engine.model.PlayerResource

object ExecutiveOrders {
    val effects: List<CardEffect> =
        listOf(
            object : CardEffect {
                override val cardId = CardId("c49_op_ed")

                override fun onBuy(context: CardHookContext) =
                    listOf(
                        CardEffectIntent.ChangeResource(context.ownerId, PlayerResource.MANDATES, 3),
                    )
            },
            object : CardEffect {
                override val cardId = CardId("c50_photo_op")

                override fun onBuy(context: CardHookContext) =
                    listOf(
                        CardEffectIntent.ChangeResource(context.ownerId, PlayerResource.MANDATES, 2),
                    )
            },
            object : CardEffect {
                override val cardId = CardId("c51_baby_kissing")

                override fun onBuy(context: CardHookContext) =
                    listOf(
                        CardEffectIntent.ChangeResource(context.ownerId, PlayerResource.MANDATES, 1),
                    )
            },
            object : CardEffect {
                override val cardId = CardId("c52_hijack_the_trend")

                override fun onBuy(context: CardHookContext) =
                    listOf(
                        CardEffectIntent.ChangeResource(context.ownerId, PlayerResource.MANDATES, 2),
                        CardEffectIntent.ChangeMainCharacter(context.ownerId),
                    )
            },
            object : CardEffect {
                override val cardId = CardId("c53_content_binge")

                override fun onBuy(context: CardHookContext) =
                    listOf(
                        CardEffectIntent.ChangeResource(context.ownerId, PlayerResource.CHARACTERS, 90),
                    )
            },
            object : CardEffect {
                override val cardId = CardId("c54_leaked_group_chat")

                override fun onBuy(context: CardHookContext) =
                    context.state.players
                        .filter { it.id != context.ownerId && !it.isSuspended }
                        .map { CardEffectIntent.ChangeResource(it.id, PlayerResource.MANDATES, -5) }
            },
            object : CardEffect {
                override val cardId = CardId("c56_subtweet_storm")

                override fun onBuy(context: CardHookContext) =
                    context.state.players
                        .filter { it.id != context.ownerId && !it.isSuspended }
                        .map { CardEffectIntent.ChangeResource(it.id, PlayerResource.FOLLOWERS, -2) }
            },
            object : CardEffect {
                override val cardId = CardId("c57_all_nighter")

                override fun onBuy(context: CardHookContext) =
                    listOf(
                        CardEffectIntent.ChangeExtraTurns(context.ownerId, 1),
                    )
            },
            object : CardEffect {
                override val cardId = CardId("c58_state_media_blitz")

                override fun onBuy(context: CardHookContext) =
                    buildList {
                        add(CardEffectIntent.ChangeResource(context.ownerId, PlayerResource.MANDATES, 2))
                        addAll(
                            context.state.players
                                .filter { it.id != context.ownerId && !it.isSuspended }
                                .map { CardEffectIntent.ChangeResource(it.id, PlayerResource.FOLLOWERS, -3) },
                        )
                    }
            },
            object : CardEffect {
                override val cardId = CardId("c59_buy_followers")

                override fun onBuy(context: CardHookContext) =
                    listOf(
                        CardEffectIntent.ChangeResource(context.ownerId, PlayerResource.FOLLOWERS, 2),
                    )
            },
            object : CardEffect {
                override val cardId = CardId("c60_reply_all_disaster")

                override fun onBuy(context: CardHookContext) =
                    context.state.players
                        .filter { !it.isSuspended }
                        .map { CardEffectIntent.ChangeResource(it.id, PlayerResource.FOLLOWERS, -3) }
            },
            object : CardEffect {
                override val cardId = CardId("c61_military_parade")

                override fun onBuy(context: CardHookContext) =
                    listOf(
                        CardEffectIntent.ChangeResource(context.ownerId, PlayerResource.MANDATES, 5),
                        CardEffectIntent.ChangeResource(context.ownerId, PlayerResource.FOLLOWERS, -4),
                    )
            },
            object : CardEffect {
                override val cardId = CardId("c62_press_conference")

                override fun onBuy(context: CardHookContext) =
                    listOf(
                        CardEffectIntent.ChangeResource(context.ownerId, PlayerResource.MANDATES, 2),
                        CardEffectIntent.ChangeResource(context.ownerId, PlayerResource.FOLLOWERS, -2),
                    )
            },
            object : CardEffect {
                override val cardId = CardId("c63_rally")

                override fun onBuy(context: CardHookContext) =
                    listOf(
                        CardEffectIntent.ChangeResource(context.ownerId, PlayerResource.MANDATES, 2),
                        CardEffectIntent.ChangeResource(context.ownerId, PlayerResource.FOLLOWERS, 3),
                    )
            },
            object : CardEffect {
                override val cardId = CardId("c64_golden_tower")

                override fun onBuy(context: CardHookContext) =
                    listOf(
                        CardEffectIntent.ChangeResource(context.ownerId, PlayerResource.MANDATES, 4),
                    )
            },
            object : CardEffect {
                override val cardId = CardId("c65_hot_mic")

                override fun onBuy(context: CardHookContext) =
                    listOf(
                        CardEffectIntent.ChangeResource(context.ownerId, PlayerResource.MANDATES, 4),
                        CardEffectIntent.ChangeResource(context.ownerId, PlayerResource.FOLLOWERS, -3),
                    )
            },
            object : CardEffect {
                override val cardId = CardId("c66_market_crash")

                override fun onBuy(context: CardHookContext) =
                    buildList {
                        add(CardEffectIntent.ChangeResource(context.ownerId, PlayerResource.MANDATES, 2))
                        addAll(
                            context.state.players
                                .filter { it.id != context.ownerId && !it.isSuspended }
                                .map {
                                    val loss = (it.characters / 20) * 10
                                    CardEffectIntent.ChangeResource(it.id, PlayerResource.CHARACTERS, -loss)
                                },
                        )
                    }
            },
        )
}
