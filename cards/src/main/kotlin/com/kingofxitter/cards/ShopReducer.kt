package com.kingofxitter.cards

import com.kingofxitter.engine.GameReducer
import com.kingofxitter.engine.GameTransition
import com.kingofxitter.engine.SplitMix64
import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.ActionRejectionReason
import com.kingofxitter.engine.model.CardId
import com.kingofxitter.engine.model.GameEvent
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.PlayerId
import com.kingofxitter.engine.model.PlayerResource
import com.kingofxitter.engine.model.ResourceChangeReason
import com.kingofxitter.engine.model.TurnPhase

/** Deterministic initialization of the draw pile and three-card shop. */
object ShopLogic {
    const val SWEEP_COST = 20
    const val SHOP_SIZE = 3

    /** Shuffles the supplied physical deck and deals up to three cards to the shop. */
    fun setupDeck(
        state: GameState,
        allCardIds: List<CardId>,
        seed: Long,
    ): GameState {
        val random = SplitMix64(seed)
        val shuffled = shuffledDeck(allCardIds, random)
        return state.copy(
            shopCardIds = shuffled.take(SHOP_SIZE),
            drawPileCardIds = shuffled.drop(SHOP_SIZE),
            discardPileCardIds = emptyList(),
            randomState = random.state,
        )
    }
}

/** Handles shop actions using the catalogue and delegates other actions to the engine. */
class ShopReducer(
    private val baseReducer: GameReducer,
) : GameReducer {
    override fun reduce(
        state: GameState,
        action: Action,
    ): GameTransition =
        when (action) {
            is Action.BuyPowerUp -> buyPowerUp(state, action)
            is Action.SweepShop -> sweepShop(state, action)
            else -> baseReducer.reduce(state, action)
        }

    override fun dispatchOnBuy(
        state: GameState,
        buyerId: PlayerId,
        purchasedCardId: CardId,
    ): GameTransition = baseReducer.dispatchOnBuy(state, buyerId, purchasedCardId)

    private fun buyPowerUp(
        state: GameState,
        action: Action.BuyPowerUp,
    ): GameTransition {
        shopRejection(state, action.playerId)?.let { return reject(state, action.playerId, it) }
        val definition =
            CardCatalog.byId[action.cardId]
                ?: return reject(state, action.playerId, ActionRejectionReason.INVALID_SELECTION)
        val player = state.players.first { it.id == action.playerId }
        val cost = if (player.policyIds.contains(CardId("c02_lobbyist"))) {
            maxOf(0, definition.cost - 10)
        } else {
            definition.cost
        }
        
        if (action.cardId !in state.shopCardIds || player.characters < cost) {
            return reject(state, action.playerId, ActionRejectionReason.INVALID_SELECTION)
        }

        val shop = state.shopCardIds.toMutableList().apply { remove(action.cardId) }
        val paidState =
            state.copy(
                shopCardIds = shop,
                players =
                    state.players.map {
                        if (it.id == player.id) {
                            it.copy(
                                characters = it.characters - cost,
                                policyIds = if (definition.type == CardType.POLICY) it.policyIds + action.cardId else it.policyIds,
                            )
                        } else {
                            it
                        }
                    },
            )
        // Purchase effects see payment and ownership before discard or replacement cards.
        val purchased = baseReducer.dispatchOnBuy(paidState, action.playerId, action.cardId)
        val discarded =
            if (definition.type == CardType.EXECUTIVE_ORDER) {
                purchased.state.copy(discardPileCardIds = purchased.state.discardPileCardIds + action.cardId)
            } else {
                purchased.state
            }
        val refilled = refillShop(discarded)
        return GameTransition(
            refilled,
            listOf(payment(action.playerId, cost, player.characters), GameEvent.PowerUpBought(action.playerId, action.cardId)) +
                purchased.events + GameEvent.ShopChanged(refilled.shopCardIds),
        )
    }

    private fun sweepShop(
        state: GameState,
        action: Action.SweepShop,
    ): GameTransition {
        shopRejection(state, action.playerId)?.let { return reject(state, action.playerId, it) }
        val player = state.players.first { it.id == action.playerId }
        if (player.characters < ShopLogic.SWEEP_COST || state.shopCardIds.isEmpty()) {
            return reject(state, action.playerId, ActionRejectionReason.INVALID_SELECTION)
        }
        val swept =
            state.copy(
                players =
                    state.players.map {
                        if (it.id == player.id) it.copy(characters = it.characters - ShopLogic.SWEEP_COST) else it
                    },
                discardPileCardIds = state.discardPileCardIds + state.shopCardIds,
                shopCardIds = emptyList(),
            )
        val refilled = refillShop(swept)
        return GameTransition(
            refilled,
            listOf(payment(action.playerId, ShopLogic.SWEEP_COST, player.characters), GameEvent.ShopChanged(refilled.shopCardIds)),
        )
    }

    private fun refillShop(state: GameState): GameState {
        var drawPile = state.drawPileCardIds
        var discardPile = state.discardPileCardIds
        val shop = state.shopCardIds.toMutableList()
        val random = SplitMix64(state.randomState)
        while (shop.size < ShopLogic.SHOP_SIZE) {
            if (drawPile.isEmpty()) {
                if (discardPile.isEmpty()) break
                drawPile = shuffledDeck(discardPile, random)
                discardPile = emptyList()
            }
            shop.add(drawPile.first())
            drawPile = drawPile.drop(1)
        }
        return state.copy(
            shopCardIds = shop,
            drawPileCardIds = drawPile,
            discardPileCardIds = discardPile,
            randomState = random.state,
        )
    }

    private fun shopRejection(
        state: GameState,
        playerId: PlayerId,
    ): ActionRejectionReason? =
        when {
            state.result != null || state.phase == TurnPhase.FINISHED -> ActionRejectionReason.MATCH_FINISHED
            state.currentPlayerId != playerId -> ActionRejectionReason.WRONG_PLAYER
            state.phase != TurnPhase.SHOP || state.pendingDecisions.isNotEmpty() -> ActionRejectionReason.WRONG_PHASE
            state.players.none { it.id == playerId && !it.isSuspended } -> ActionRejectionReason.INVALID_SELECTION
            else -> null
        }

    private fun payment(
        playerId: PlayerId,
        cost: Int,
        charactersBefore: Int,
    ): GameEvent.ResourceChanged =
        GameEvent.ResourceChanged(
            playerId = playerId,
            resource = PlayerResource.CHARACTERS,
            amount = -cost,
            totalAfter = charactersBefore - cost,
            reason = ResourceChangeReason.POWER_UP,
        )

    private fun reject(
        state: GameState,
        playerId: PlayerId,
        reason: ActionRejectionReason,
    ): GameTransition = GameTransition(state, listOf(GameEvent.ActionRejected(playerId, reason)))
}

private fun shuffledDeck(
    cards: List<CardId>,
    random: SplitMix64,
): List<CardId> {
    val shuffled = cards.toMutableList()
    for (i in shuffled.indices.reversed()) {
        val j = random.nextInt(i + 1)
        val temp = shuffled[i]
        shuffled[i] = shuffled[j]
        shuffled[j] = temp
    }
    return shuffled
}
