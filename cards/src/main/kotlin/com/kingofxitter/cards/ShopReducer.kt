package com.kingofxitter.cards

import com.kingofxitter.engine.GameReducer
import com.kingofxitter.engine.GameTransition
import com.kingofxitter.engine.SplitMix64
import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.ActionRejectionReason
import com.kingofxitter.engine.model.CardId
import com.kingofxitter.engine.model.DecisionId
import com.kingofxitter.engine.model.DecisionKind
import com.kingofxitter.engine.model.DecisionOption
import com.kingofxitter.engine.model.DecisionOptionId
import com.kingofxitter.engine.model.GameEvent
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.PendingDecision
import com.kingofxitter.engine.model.PlayerId
import com.kingofxitter.engine.model.PlayerResource
import com.kingofxitter.engine.model.PlayerState
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
            is Action.ChooseDecisionOption -> chooseDecision(state, action)
            else -> baseReducer.reduce(state, action)
        }

    /** Handles an out-of-turn purchase offered by Breaking News Alerts; everything else is the engine's. */
    private fun chooseDecision(
        state: GameState,
        action: Action.ChooseDecisionOption,
    ): GameTransition {
        val decision = state.pendingDecisions.firstOrNull()
        val buy =
            decision
                ?.takeIf { it.kind == DecisionKind.BUY_POWER_UP && it.id == action.decisionId && it.playerId == action.playerId }
                ?.options
                ?.firstOrNull { it.id == action.optionId }
                ?.action as? Action.BuyPowerUp
        if (decision == null || buy == null) return baseReducer.reduce(state, action)
        val remaining = state.pendingDecisions.drop(1)
        val prepared =
            state.copy(
                phase = if (remaining.isEmpty()) state.phaseAfterDecisions ?: TurnPhase.SHOP else TurnPhase.DECISION,
                pendingDecisions = remaining,
                phaseAfterDecisions = if (remaining.isEmpty()) null else state.phaseAfterDecisions,
            )
        val bought = purchase(prepared, buy)
        if (bought.events.any { it is GameEvent.ActionRejected }) return bought
        return GameTransition(bought.state, listOf(GameEvent.DecisionResolved(action.decisionId, action.optionId)) + bought.events)
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
        return purchase(state, action)
    }

    /** Buys [action]'s card for its player. The caller has already checked that the player may act. */
    private fun purchase(
        state: GameState,
        action: Action.BuyPowerUp,
    ): GameTransition {
        if (state.result != null ||
            state.phase == TurnPhase.FINISHED
        ) {
            return reject(state, action.playerId, ActionRejectionReason.MATCH_FINISHED)
        }
        if (state.players.none { it.id == action.playerId && !it.isSuspended }) {
            return reject(state, action.playerId, ActionRejectionReason.INVALID_SELECTION)
        }
        val definition =
            CardCatalog.byId[action.cardId]
                ?: return reject(state, action.playerId, ActionRejectionReason.INVALID_SELECTION)
        val player = state.players.first { it.id == action.playerId }
        val cost = costFor(player, definition)

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
        val refilled = offerRevealedCards(discarded, refillShop(discarded), action.playerId)
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
        val refilled = offerRevealedCards(swept, refillShop(swept), action.playerId)
        return GameTransition(
            refilled,
            listOf(payment(action.playerId, ShopLogic.SWEEP_COST, player.characters), GameEvent.ShopChanged(refilled.shopCardIds)),
        )
    }

    private fun costFor(
        player: PlayerState,
        definition: CardDef,
    ): Int = if (LOBBYIST in player.policyIds) maxOf(0, definition.cost - 10) else definition.cost

    /**
     * Queues an out-of-turn purchase for every other leader holding Breaking News Alerts who can
     * afford a card that [after] revealed and [before] did not show.
     */
    private fun offerRevealedCards(
        before: GameState,
        after: GameState,
        buyerId: PlayerId,
    ): GameState {
        val revealed = after.shopCardIds.filterNot { it in before.shopCardIds }
        if (revealed.isEmpty() || after.result != null) return after
        val offers =
            after.players
                .filter { it.id != buyerId && !it.isSuspended && BREAKING_NEWS in it.policyIds }
                .mapNotNull { owner ->
                    val affordable = revealed.filter { id -> CardCatalog.byId[id]?.let { costFor(owner, it) <= owner.characters } == true }
                    if (affordable.isEmpty()) {
                        null
                    } else {
                        PendingDecision(
                            id =
                                DecisionId(
                                    "breaking-news-${after.turnNumber}-${owner.id.value}-${revealed.joinToString("-") { it.value }}",
                                ),
                            playerId = owner.id,
                            kind = DecisionKind.BUY_POWER_UP,
                            options =
                                affordable.map { DecisionOption(DecisionOptionId("buy_${it.value}"), Action.BuyPowerUp(owner.id, it)) } +
                                    DecisionOption(DecisionOptionId("decline"), Action.DeclineAbility(owner.id)),
                            promptKey = "ability_breaking_news_prompt",
                        )
                    }
                }
        if (offers.isEmpty()) return after
        return after.copy(
            phase = TurnPhase.DECISION,
            phaseAfterDecisions = after.phaseAfterDecisions ?: after.phase,
            pendingDecisions = after.pendingDecisions + offers,
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
    ): ActionRejectionReason? {
        if (state.result != null || state.phase == TurnPhase.FINISHED) return ActionRejectionReason.MATCH_FINISHED
        if (state.currentPlayerId != playerId) return ActionRejectionReason.WRONG_PLAYER
        if (state.phase != TurnPhase.SHOP || state.pendingDecisions.isNotEmpty()) return ActionRejectionReason.WRONG_PHASE
        if (state.players.none { it.id == playerId && !it.isSuspended }) return ActionRejectionReason.INVALID_SELECTION
        return null
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

private val LOBBYIST = CardId("c02_lobbyist")
private val BREAKING_NEWS = CardId("c31_breaking_news_alerts")
