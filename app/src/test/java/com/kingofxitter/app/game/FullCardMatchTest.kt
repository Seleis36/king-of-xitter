package com.kingofxitter.app.game

import com.kingofxitter.app.navigation.CatalogMarket
import com.kingofxitter.app.navigation.matchReducer
import com.kingofxitter.bots.BotDifficulty
import com.kingofxitter.bots.SimulationConfig
import com.kingofxitter.bots.Simulator
import com.kingofxitter.cards.CardCatalog
import com.kingofxitter.cards.ShopLogic
import com.kingofxitter.engine.model.GameState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Plays whole headless matches with the real deck, every card effect and shopping bots. */
class FullCardMatchTest {
    /** The simulator deals no deck, so deal the standard one into each opening position. */
    private val dealDeck: (GameState) -> GameState = { ShopLogic.setupDeck(it, CardCatalog.standardDeck, it.randomState) }

    @Test
    fun `matches with every card effect and shopping bots all finish`() {
        // The two fast difficulties cover the card effects; the search adds time, not coverage.
        val fast = listOf(BotDifficulty.INTERN, BotDifficulty.REPLY_GUY)
        val report =
            Simulator.run(
                SimulationConfig(games = 300, seed = 100L, difficulties = fast, market = CatalogMarket),
                setup = dealDeck,
                reducer = ::matchReducer,
            )

        assertEquals(emptyList(), report.failures)
        assertEquals(300, report.games)
        assertTrue(report.averageTurns > 1.0)
    }

    @Test
    fun `Troll Farm seats finish full-deck matches too`() {
        val report =
            Simulator.run(SimulationConfig(games = 24, seed = 300L, market = CatalogMarket), setup = dealDeck, reducer = ::matchReducer)

        assertEquals(emptyList(), report.failures)
        assertEquals(24, report.games)
    }

    @Test
    fun `every difficulty buys power-ups when the match has a shop`() {
        BotDifficulty.entries.forEach { difficulty ->
            val report =
                Simulator.run(
                    SimulationConfig(games = 4, seed = 7L, difficulties = listOf(difficulty), market = CatalogMarket),
                    setup = dealDeck,
                    reducer = ::matchReducer,
                )
            assertEquals(emptyList(), report.failures, "$difficulty")
            assertTrue(report.averagePowerUpsBought >= 1.0, "$difficulty bought ${report.averagePowerUpsBought} per match")
        }
    }
}
