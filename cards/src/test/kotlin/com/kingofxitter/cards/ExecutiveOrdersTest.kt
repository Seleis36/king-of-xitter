package com.kingofxitter.cards

import com.kingofxitter.engine.DefaultGameReducer
import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.CardEffectRegistry
import com.kingofxitter.engine.model.CardId
import com.kingofxitter.engine.model.PlayerState
import com.kingofxitter.engine.model.TurnPhase
import kotlin.test.Test
import kotlin.test.assertEquals

class ExecutiveOrdersTest {
    private val reducer = ShopReducer(DefaultGameReducer(CardEffectRegistry(ExecutiveOrders.effects)))

    private fun testBuy(cardIdStr: String, setup: TestGame.() -> Unit = {}): List<PlayerState> {
        val cardId = CardId(cardIdStr)
        val state = TestGame()
            .withPhase(TurnPhase.SHOP)
            .withShop(listOf(cardId))
            .withCharacters(TestGame.HUMAN, 150) // enough for any card
            .apply(setup)
            .build()
        val result = reducer.reduce(state, Action.BuyPowerUp(TestGame.HUMAN, cardId))
        return result.state.players
    }

    @Test
    fun `Op-Ed adds 3 mandates`() {
        val players = testBuy("c49_op_ed")
        assertEquals(3, players.first { it.id == TestGame.HUMAN }.mandates)
    }

    @Test
    fun `Photo Op adds 2 mandates`() {
        val players = testBuy("c50_photo_op")
        assertEquals(2, players.first { it.id == TestGame.HUMAN }.mandates)
    }

    @Test
    fun `Baby Kissing adds 1 mandate`() {
        val players = testBuy("c51_baby_kissing")
        assertEquals(1, players.first { it.id == TestGame.HUMAN }.mandates)
    }

    @Test
    fun `Hijack the Trend adds 2 mandates and takes Xitter`() {
        val cardId = CardId("c52_hijack_the_trend")
        val state = TestGame()
            .withPhase(TurnPhase.SHOP)
            .withShop(listOf(cardId))
            .withCharacters(TestGame.HUMAN, 100)
            .build()
        val result = reducer.reduce(state, Action.BuyPowerUp(TestGame.HUMAN, cardId))
        val human = result.state.players.first { it.id == TestGame.HUMAN }
        assertEquals(2, human.mandates)
        assertEquals(TestGame.HUMAN, result.state.xitterPlayerId)
    }

    @Test
    fun `Content Binge adds 90 characters`() {
        val players = testBuy("c53_content_binge") {
            withCharacters(TestGame.HUMAN, 100)
        }
        val cost = CardCatalog.byId[CardId("c53_content_binge")]!!.cost
        assertEquals(100 - cost + 90, players.first { it.id == TestGame.HUMAN }.characters)
    }

    @Test
    fun `Leaked Group Chat reduces other leaders mandates by 5`() {
        val players = testBuy("c54_leaked_group_chat") {
            withMandates(TestGame.BOT, 10)
        }
        assertEquals(5, players.first { it.id == TestGame.BOT }.mandates)
    }

    @Test
    fun `Subtweet Storm deals 2 unfollows to other leaders`() {
        val players = testBuy("c56_subtweet_storm") {
            withFollowers(TestGame.BOT, 10)
        }
        assertEquals(8, players.first { it.id == TestGame.BOT }.followers)
    }

    @Test
    fun `State Media Blitz gives 2 mandates and deals 3 unfollows to others`() {
        val players = testBuy("c58_state_media_blitz") {
            withMandates(TestGame.HUMAN, 0)
            withFollowers(TestGame.BOT, 10)
        }
        assertEquals(2, players.first { it.id == TestGame.HUMAN }.mandates)
        assertEquals(7, players.first { it.id == TestGame.BOT }.followers)
    }

    @Test
    fun `Buy Followers adds 2M followers`() {
        val players = testBuy("c59_buy_followers") {
            withFollowers(TestGame.HUMAN, 5)
        }
        assertEquals(7, players.first { it.id == TestGame.HUMAN }.followers)
    }

    @Test
    fun `Reply-All Disaster deals 3 unfollows to everyone`() {
        val players = testBuy("c60_reply_all_disaster") {
            withFollowers(TestGame.HUMAN, 10)
            withFollowers(TestGame.BOT, 10)
        }
        assertEquals(7, players.first { it.id == TestGame.HUMAN }.followers)
        assertEquals(7, players.first { it.id == TestGame.BOT }.followers)
    }

    @Test
    fun `Military Parade adds 5 mandates and self deals 4 unfollows`() {
        val players = testBuy("c61_military_parade") {
            withFollowers(TestGame.HUMAN, 10)
            withMandates(TestGame.HUMAN, 0)
        }
        assertEquals(5, players.first { it.id == TestGame.HUMAN }.mandates)
        assertEquals(6, players.first { it.id == TestGame.HUMAN }.followers)
    }

    @Test
    fun `Press Conference adds 2 mandates and self deals 2 unfollows`() {
        val players = testBuy("c62_press_conference") {
            withFollowers(TestGame.HUMAN, 10)
        }
        assertEquals(2, players.first { it.id == TestGame.HUMAN }.mandates)
        assertEquals(8, players.first { it.id == TestGame.HUMAN }.followers)
    }

    @Test
    fun `Rally adds 2 mandates and heals 3M followers`() {
        val players = testBuy("c63_rally") {
            withFollowers(TestGame.HUMAN, 5)
        }
        assertEquals(2, players.first { it.id == TestGame.HUMAN }.mandates)
        assertEquals(8, players.first { it.id == TestGame.HUMAN }.followers)
    }

    @Test
    fun `Golden Tower adds 4 mandates`() {
        val players = testBuy("c64_golden_tower")
        assertEquals(4, players.first { it.id == TestGame.HUMAN }.mandates)
    }

    @Test
    fun `Hot Mic adds 4 mandates and self deals 3 unfollows`() {
        val players = testBuy("c65_hot_mic") {
            withFollowers(TestGame.HUMAN, 10)
        }
        assertEquals(4, players.first { it.id == TestGame.HUMAN }.mandates)
        assertEquals(7, players.first { it.id == TestGame.HUMAN }.followers)
    }

    @Test
    fun `Market Crash adds 2 mandates and removes 10 chars per 20 from others`() {
        val players = testBuy("c66_market_crash") {
            withCharacters(TestGame.BOT, 50)
        }
        assertEquals(2, players.first { it.id == TestGame.HUMAN }.mandates)
        assertEquals(30, players.first { it.id == TestGame.BOT }.characters)
    }
}
