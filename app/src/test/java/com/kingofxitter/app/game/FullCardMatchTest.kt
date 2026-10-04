package com.kingofxitter.app.game

import com.kingofxitter.app.navigation.CatalogMarket
import com.kingofxitter.bots.SimulationConfig
import com.kingofxitter.bots.Simulator
import com.kingofxitter.cards.CardCatalog
import com.kingofxitter.cards.CardEffects
import com.kingofxitter.cards.ShopLogic
import com.kingofxitter.cards.ShopReducer
import com.kingofxitter.engine.DefaultGameReducer
import com.kingofxitter.engine.GameReducer
import com.kingofxitter.engine.GameTransition
import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.CardEffectRegistry
import com.kingofxitter.engine.model.CardId
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.PlayerId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Plays whole headless matches with the real deck, every card effect and shopping bots. */
class FullCardMatchTest {
    /** The simulator deals no deck, so deal one on the first action of each match. */
    private class DealingReducer(
        private val inner: GameReducer,
    ) : GameReducer {
        private var dealt = false

        override fun reduce(
            state: GameState,
            action: Action,
        ): GameTransition {
            val ready =
                if (!dealt) {
                    dealt = true
                    ShopLogic.setupDeck(state, CardCatalog.standardDeck, state.randomState)
                } else {
                    state
                }
            return inner.reduce(ready, action)
        }

        override fun dispatchOnBuy(
            state: GameState,
            buyerId: PlayerId,
            purchasedCardId: CardId,
        ): GameTransition = inner.dispatchOnBuy(state, buyerId, purchasedCardId)
    }

    @Test
    fun `matches with every card effect and shopping bots all finish`() {
        val report =
            Simulator.run(
                SimulationConfig(games = 300, seed = 100L, market = CatalogMarket),
                reducer = { DealingReducer(ShopReducer(DefaultGameReducer(CardEffectRegistry(CardEffects.all)))) },
            )

        assertEquals(emptyList(), report.failures)
        assertEquals(300, report.games)
        assertTrue(report.averageTurns > 1.0)
    }
}
