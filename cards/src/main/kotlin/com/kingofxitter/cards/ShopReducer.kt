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

/**
 * Deck and Shop initialization logic.
 */
object ShopLogic {
    const val SWEEP_COST = 20
    const val SHOP_SIZE = 3

    /** Initializes the deck and shop in the given state snapshot. */
    fun setupDeck(
        state: GameState,
        allCardIds: List<CardId>,
        seed: Long,
    ): GameState {
        val random = SplitMix64(seed)
        val shuffled = allCardIds.toMutableList()
        for (i in shuffled.indices.reversed()) {
            val j = random.nextInt(i + 1)
            val temp = shuffled[i]
            shuffled[i] = shuffled[j]
            shuffled[j] = temp
        }
        val shop = shuffled.take(SHOP_SIZE)
        val drawPile = shuffled.drop(SHOP_SIZE)
        return state.copy(
            shopCardIds = shop,
            drawPileCardIds = drawPile,
            discardPileCardIds = emptyList(),
            randomState = random.state,
        )
    }
}

/**
 * Deck and Shop logic intercepting BuyPowerUp and SweepShop.
 * Because the engine does not depend on the cards module, this reducer intercepts
 * those actions, applies the cost and rules using [CardCatalog], and delegates
 * the rest to the base [GameReducer].
 */
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
        if (state.phase != TurnPhase.SHOP) {
            return reject(state, action.playerId, ActionRejectionReason.WRONG_PHASE)
        }
        if (state.currentPlayerId != action.playerId) {
            return reject(state, action.playerId, ActionRejectionReason.WRONG_PLAYER)
        }
        if (action.cardId !in state.shopCardIds) {
            return reject(state, action.playerId, ActionRejectionReason.INVALID_SELECTION)
        }

        val cardDef =
            CardCatalog.byId[action.cardId]
                ?: return reject(state, action.playerId, ActionRejectionReason.INVALID_SELECTION)

        val player =
            state.players.firstOrNull { it.id == action.playerId }
                ?: return reject(state, action.playerId, ActionRejectionReason.WRONG_PLAYER)

        val cost = cardDef.cost
        // The Lobbyist discount is handled by C4 (Passive policies), so we assume cost is final for now,
        // or we could check for the lobbyist policy directly here if it's not a hook.
        // Wait, since we are isolated, we just subtract the normal cost. The hook will be added if needed.
        if (player.characters < cost) {
            return reject(state, action.playerId, ActionRejectionReason.INVALID_SELECTION)
        }

        // Apply cost
        var workingState = state
        val events = mutableListOf<GameEvent>()

        val newCharacters = player.characters - cost
        val playerIdx = workingState.players.indexOfFirst { it.id == action.playerId }
        val updatedPlayer = player.copy(characters = newCharacters)

        events.add(
            GameEvent.ResourceChanged(
                playerId = action.playerId,
                resource = PlayerResource.CHARACTERS,
                amount = -cost,
                totalAfter = newCharacters,
                reason = ResourceChangeReason.POWER_UP,
            ),
        )

        // Process purchase
        val newShop = workingState.shopCardIds.toMutableList()
        newShop.remove(action.cardId)
        workingState = workingState.copy(shopCardIds = newShop)

        if (cardDef.type == CardType.POLICY) {
            val p = updatedPlayer.copy(policyIds = updatedPlayer.policyIds + action.cardId)
            val players = workingState.players.toMutableList().also { it[playerIdx] = p }
            workingState = workingState.copy(players = players)
        } else {
            val players = workingState.players.toMutableList().also { it[playerIdx] = updatedPlayer }
            workingState = workingState.copy(
                players = players,
                discardPileCardIds = workingState.discardPileCardIds + action.cardId,
            )
        }

        events.add(GameEvent.PowerUpBought(action.playerId, action.cardId))

        // Refill shop
        val refilled = refillShop(workingState)
        workingState = refilled.first
        events.addAll(refilled.second)

        return baseReducer.dispatchOnBuy(workingState, action.playerId, action.cardId).let {
            GameTransition(it.state, events + it.events)
        }
    }

    private fun sweepShop(
        state: GameState,
        action: Action.SweepShop,
    ): GameTransition {
        if (state.phase != TurnPhase.SHOP) {
            return reject(state, action.playerId, ActionRejectionReason.WRONG_PHASE)
        }
        if (state.currentPlayerId != action.playerId) {
            return reject(state, action.playerId, ActionRejectionReason.WRONG_PLAYER)
        }

        val player =
            state.players.firstOrNull { it.id == action.playerId }
                ?: return reject(state, action.playerId, ActionRejectionReason.WRONG_PLAYER)

        if (player.characters < ShopLogic.SWEEP_COST) {
            return reject(state, action.playerId, ActionRejectionReason.INVALID_SELECTION)
        }

        val newCharacters = player.characters - ShopLogic.SWEEP_COST
        val playerIdx = state.players.indexOfFirst { it.id == action.playerId }
        val updatedPlayer = player.copy(characters = newCharacters)
        val players = state.players.toMutableList().also { it[playerIdx] = updatedPlayer }

        val events = mutableListOf<GameEvent>()
        events.add(
            GameEvent.ResourceChanged(
                playerId = action.playerId,
                resource = PlayerResource.CHARACTERS,
                amount = -ShopLogic.SWEEP_COST,
                totalAfter = newCharacters,
                reason = ResourceChangeReason.POWER_UP,
            ),
        )

        var workingState = state.copy(
            players = players,
            discardPileCardIds = state.discardPileCardIds + state.shopCardIds,
            shopCardIds = emptyList(),
        )

        val refilled = refillShop(workingState)
        workingState = refilled.first
        events.addAll(refilled.second)

        return GameTransition(workingState, events)
    }

    private fun refillShop(state: GameState): Pair<GameState, List<GameEvent>> {
        var drawPile = state.drawPileCardIds
        var discardPile = state.discardPileCardIds
        val shop = state.shopCardIds.toMutableList()
        val random = SplitMix64(state.randomState)
        var changed = false

        while (shop.size < ShopLogic.SHOP_SIZE) {
            if (drawPile.isEmpty()) {
                if (discardPile.isEmpty()) {
                    break // Nothing to draw
                }
                // Shuffle discard pile into draw pile
                val shuffled = discardPile.toMutableList()
                // Fisher-Yates shuffle using SplitMix64
                for (i in shuffled.indices.reversed()) {
                    val j = random.nextInt(i + 1)
                    val temp = shuffled[i]
                    shuffled[i] = shuffled[j]
                    shuffled[j] = temp
                }
                drawPile = shuffled
                discardPile = emptyList()
            }
            shop.add(drawPile.first())
            drawPile = drawPile.drop(1)
            changed = true
        }

        val newState = state.copy(
            shopCardIds = shop,
            drawPileCardIds = drawPile,
            discardPileCardIds = discardPile,
            randomState = random.state,
        )

        val events = if (changed) listOf(GameEvent.ShopChanged(shop)) else emptyList()
        return Pair(newState, events)
    }

    private fun reject(
        state: GameState,
        playerId: PlayerId,
        reason: ActionRejectionReason,
    ): GameTransition = GameTransition(state, listOf(GameEvent.ActionRejected(playerId, reason)))
}
