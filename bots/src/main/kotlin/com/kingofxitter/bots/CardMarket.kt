package com.kingofxitter.bots

import com.kingofxitter.engine.model.CardId

/** What a bot can learn about one shop card without `:bots` depending on `:cards`. */
data class CardAppraisal(
    /** Character cost in the shop. */
    val cost: Int,
    /** True for a Policy (kept), false for an Executive Order (resolved once). */
    val isPolicy: Boolean,
)

/**
 * Prices the opaque [CardId]s in `GameState.shopCardIds`. `:app` supplies one backed by the
 * cards catalogue; pure-engine callers use [Unknown].
 *
 * A market that appraises nothing also switches shop moves off, which is what a match without
 * `ShopReducer` needs: the base reducer rejects `BuyPowerUp`, so a bot must not offer one.
 */
fun interface CardMarket {
    /** The card's cost and type, or null when this market doesn't know the card. */
    fun appraise(cardId: CardId): CardAppraisal?

    companion object {
        /** Knows no cards, so bots never shop. */
        val Unknown = CardMarket { null }
    }
}
