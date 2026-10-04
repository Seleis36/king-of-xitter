package com.kingofxitter.app.navigation

import com.kingofxitter.bots.CardAppraisal
import com.kingofxitter.bots.CardMarket
import com.kingofxitter.cards.CardCatalog
import com.kingofxitter.cards.CardType
import com.kingofxitter.engine.model.CardId

/** Lets the bots price shop cards from the cards catalogue. */
object CatalogMarket : CardMarket {
    override fun appraise(cardId: CardId): CardAppraisal? =
        CardCatalog.byId[cardId]?.let { CardAppraisal(cost = it.cost, isPolicy = it.type == CardType.POLICY) }
}
