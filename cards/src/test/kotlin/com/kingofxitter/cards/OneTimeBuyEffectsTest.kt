package com.kingofxitter.cards

import com.kingofxitter.engine.DefaultGameReducer
import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.CardEffectRegistry
import com.kingofxitter.engine.model.CardId
import com.kingofxitter.engine.model.CounterId
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.PlayerId
import com.kingofxitter.engine.model.TurnPhase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The engine runs `onBuy` for every held policy on each purchase, so one-time effects must check the bought card. */
class OneTimeBuyEffectsTest {
    private val reducer = ShopReducer(DefaultGameReducer(CardEffectRegistry(CardEffects.all)))
    private val other = CardId("c02_lobbyist")

    private fun GameState.player(id: PlayerId) = players.first { it.id == id }

    /** The human holds [held] and then buys an unrelated policy. */
    private fun buysSomethingElse(held: String): GameState {
        val state =
            TestGame()
                .withPhase(TurnPhase.SHOP)
                .withPolicy(TestGame.HUMAN, CardId(held))
                .withShop(listOf(other))
                .withCharacters(TestGame.HUMAN, 100)
                .withFollowers(TestGame.HUMAN, 5)
                .build()
        return reducer.reduce(state, Action.BuyPowerUp(TestGame.HUMAN, other)).state
    }

    @Test
    fun `Bot Farm does not grant followers again when another policy is bought`() {
        val human = buysSomethingElse("c12_bot_farm").player(TestGame.HUMAN)

        assertEquals(10, human.maxFollowers)
        assertEquals(5, human.followers)
    }

    @Test
    fun `Super PAC does not reload 60 chars when another policy is bought`() {
        val human = buysSomethingElse("c28_super_pac").player(TestGame.HUMAN)

        assertNull(human.counters[CounterId("super_pac")])
    }

    @Test
    fun `Smoke and Mirrors does not add counters when another policy is bought`() {
        val human = buysSomethingElse("c41_smoke_and_mirrors").player(TestGame.HUMAN)

        assertNull(human.counters[CounterId("smoke_cloud")])
    }

    @Test
    fun `Bot Farm still grants followers when it is bought`() {
        val state =
            TestGame()
                .withPhase(TurnPhase.SHOP)
                .withShop(listOf(CardId("c12_bot_farm")))
                .withCharacters(TestGame.HUMAN, 100)
                .withFollowers(TestGame.HUMAN, 5)
                .build()

        val human = reducer.reduce(state, Action.BuyPowerUp(TestGame.HUMAN, CardId("c12_bot_farm"))).state.player(TestGame.HUMAN)

        assertEquals(12, human.maxFollowers)
        assertEquals(7, human.followers)
    }
}
