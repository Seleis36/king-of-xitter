package com.kingofxitter.cards

import com.kingofxitter.engine.model.CardId
import com.kingofxitter.engine.model.DiceFace
import com.kingofxitter.engine.model.TurnPhase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for [CardCatalog] (C1) and [TestGame] (S3).
 *
 * One test per rule stated in ROADMAP §2.
 */
class CardCatalogTest {
    // -------------------------------------------------------------------------
    // Catalogue structure
    // -------------------------------------------------------------------------

    @Test
    fun `catalogue contains exactly 66 unique definitions`() {
        assertEquals(66, CardCatalog.all.size)
        val ids = CardCatalog.all.map { it.id }
        assertEquals(ids.distinct().size, ids.size, "Card IDs must be unique")
    }

    @Test
    fun `standard deck contains exactly 68 cards`() {
        // 66 base + 1 extra Second Phone + 1 extra Leaked Group Chat = 68
        assertEquals(68, CardCatalog.standardDeck.size)
    }

    @Test
    fun `second phone appears twice in the standard deck`() {
        val id = CardId("c13_second_phone")
        assertEquals(2, CardCatalog.standardDeck.count { it == id })
    }

    @Test
    fun `leaked group chat appears twice in the standard deck`() {
        val id = CardId("c54_leaked_group_chat")
        assertEquals(2, CardCatalog.standardDeck.count { it == id })
    }

    @Test
    fun `all other cards appear exactly once in the standard deck`() {
        val duplicateIds = setOf(CardId("c13_second_phone"), CardId("c54_leaked_group_chat"))
        CardCatalog.all
            .filter { it.id !in duplicateIds }
            .forEach { def ->
                assertEquals(1, CardCatalog.standardDeck.count { it == def.id }, "Expected 1 copy of ${def.id}")
            }
    }

    @Test
    fun `every definition is reachable by id`() {
        CardCatalog.all.forEach { def ->
            assertEquals(def, CardCatalog[def.id])
        }
    }

    @Test
    fun `every card has non-blank string keys`() {
        CardCatalog.all.forEach { def ->
            assertTrue(def.nameKey.isNotBlank(), "${def.id} nameKey is blank")
            assertTrue(def.rulesKey.isNotBlank(), "${def.id} rulesKey is blank")
            assertTrue(def.flavourKey.isNotBlank(), "${def.id} flavourKey is blank")
        }
    }

    @Test
    fun `every string key follows the card_cNN_field pattern`() {
        val namePattern = Regex("^card_c\\d+_name$")
        val rulesPattern = Regex("^card_c\\d+_rules$")
        val flavourPattern = Regex("^card_c\\d+_flavour$")
        CardCatalog.all.forEach { def ->
            assertTrue(namePattern.matches(def.nameKey), "${def.id} nameKey '${def.nameKey}' does not match pattern")
            assertTrue(rulesPattern.matches(def.rulesKey), "${def.id} rulesKey '${def.rulesKey}' does not match pattern")
            assertTrue(flavourPattern.matches(def.flavourKey), "${def.id} flavourKey '${def.flavourKey}' does not match pattern")
        }
    }

    // -------------------------------------------------------------------------
    // Costs (ROADMAP §2)
    // -------------------------------------------------------------------------

    @Test
    fun `all costs are positive multiples of 10`() {
        CardCatalog.all.forEach { def ->
            assertTrue(def.cost > 0, "${def.id} cost must be positive")
            assertEquals(0, def.cost % 10, "${def.id} cost must be a multiple of 10")
        }
    }

    @Test
    fun `spot-check known card costs`() {
        // Policies
        assertEquals(60, CardCatalog[CardId("c01_doomscroll_drip")].cost) // Acid Attack 6
        assertEquals(30, CardCatalog[CardId("c02_lobbyist")].cost) // Alien Metabolism 3
        assertEquals(50, CardCatalog[CardId("c03_alpha_poster")].cost) // Alpha Monster 5
        assertEquals(40, CardCatalog[CardId("c04_thick_skin_pr")].cost) // Armor Plating 4
        assertEquals(70, CardCatalog[CardId("c13_second_phone")].cost) // Extra Head 7
        assertEquals(80, CardCatalog[CardId("c27_copycat_policy")].cost) // Mimic 8
        assertEquals(20, CardCatalog[CardId("c42_ghostwriter")].cost) // Solar Powered 2
        assertEquals(60, CardCatalog[CardId("c48_touch_grass")].cost) // Wings 6

        // Executive Orders
        assertEquals(50, CardCatalog[CardId("c49_op_ed")].cost) // Apartment Building 5
        assertEquals(80, CardCatalog[CardId("c53_content_binge")].cost) // Energize 8
        assertEquals(70, CardCatalog[CardId("c54_leaked_group_chat")].cost) // Evacuation Orders 7
        assertEquals(60, CardCatalog[CardId("c66_market_crash")].cost) // Vast Storm 6
    }

    // -------------------------------------------------------------------------
    // Types (ROADMAP §2)
    // -------------------------------------------------------------------------

    @Test
    fun `cards 1 to 48 are policies`() {
        val policyIds =
            setOf(
                "c01_doomscroll_drip",
                "c02_lobbyist",
                "c03_alpha_poster",
                "c04_thick_skin_pr",
                "c05_lurker",
                "c06_home_turf",
                "c07_parody_account",
                "c08_full_bingo",
                "c09_press_office",
                "c10_gravedancer",
                "c11_drafts_folder",
                "c12_bot_farm",
                "c13_second_phone",
                "c15_quote_tweet",
                "c16_pinned_tweet",
                "c17_influencer_deal",
                "c18_speechwriter",
                "c19_old_guard",
                "c20_retweet_for_retweet",
                "c21_peace_summit",
                "c22_poll_rigger",
                "c23_burner_account",
                "c24_private_jet",
                "c25_leaked_memo",
                "c26_pivot",
                "c27_copycat_policy",
                "c28_super_pac",
                "c29_going_viral",
                "c30_big_tent",
                "c31_breaking_news_alerts",
                "c32_hostile_takeover",
                "c33_october_surprise",
                "c34_fact_check",
                "c35_cancel_campaign",
                "c36_phone_tap",
                "c37_paid_promotion",
                "c38_comeback_tour",
                "c39_underdog_narrative",
                "c40_shadowban",
                "c41_smoke_and_mirrors",
                "c42_ghostwriter",
                "c43_caps_lock_stuck",
                "c44_spin_doctor",
                "c45_focus_group",
                "c46_main_character_energy",
                "c47_martyr_complex",
                "c48_touch_grass",
            )
        policyIds.forEach { rawId ->
            val def = CardCatalog[CardId(rawId)]
            assertEquals(CardType.POLICY, def.type, "$rawId should be a POLICY")
        }
    }

    @Test
    fun `cards 49 to 66 are executive orders`() {
        val orderIds =
            setOf(
                "c49_op_ed",
                "c50_photo_op",
                "c51_baby_kissing",
                "c52_hijack_the_trend",
                "c53_content_binge",
                "c54_leaked_group_chat",
                "c56_subtweet_storm",
                "c57_all_nighter",
                "c58_state_media_blitz",
                "c59_buy_followers",
                "c60_reply_all_disaster",
                "c61_military_parade",
                "c62_press_conference",
                "c63_rally",
                "c64_golden_tower",
                "c65_hot_mic",
                "c66_market_crash",
            )
        orderIds.forEach { rawId ->
            val def = CardCatalog[CardId(rawId)]
            assertEquals(CardType.EXECUTIVE_ORDER, def.type, "$rawId should be an EXECUTIVE_ORDER")
        }
    }

    // -------------------------------------------------------------------------
    // TestGame builder (S3)
    // -------------------------------------------------------------------------

    @Test
    fun `TestGame builds default two-player rolling state`() {
        val state = TestGame().build()
        assertEquals(2, state.players.size)
        assertEquals(TurnPhase.ROLLING, state.phase)
        assertEquals(TestGame.HUMAN, state.currentPlayerId)
        assertTrue(state.players.all { it.followers == 10 })
        assertTrue(state.players.all { it.mandates == 0 })
    }

    @Test
    fun `TestGame withPolicy adds cards to the correct player`() {
        val cardId = CardId("c02_lobbyist")
        val state =
            TestGame()
                .withPolicy(TestGame.HUMAN, cardId)
                .build()
        assertNotNull(
            state.players
                .first { it.id == TestGame.HUMAN }
                .policyIds
                .singleOrNull { it == cardId },
        )
        assertTrue(
            state.players
                .first { it.id == TestGame.BOT }
                .policyIds
                .isEmpty(),
        )
    }

    @Test
    fun `TestGame withCharacters sets the correct resource`() {
        val state =
            TestGame()
                .withCharacters(TestGame.HUMAN, 60)
                .build()
        assertEquals(60, state.players.first { it.id == TestGame.HUMAN }.characters)
        assertEquals(0, state.players.first { it.id == TestGame.BOT }.characters)
    }

    @Test
    fun `TestGame withDice produces correct face sequence`() {
        val state =
            TestGame()
                .withDice(DiceFace.TWEET, DiceFace.TWEET, DiceFace.LIKE)
                .build()
        assertEquals(3, state.dice.size)
        assertEquals(DiceFace.TWEET, state.dice[0].face)
        assertEquals(DiceFace.TWEET, state.dice[1].face)
        assertEquals(DiceFace.LIKE, state.dice[2].face)
    }

    @Test
    fun `TestGame withPhase changes the game phase`() {
        val state = TestGame().withPhase(TurnPhase.SHOP).build()
        assertEquals(TurnPhase.SHOP, state.phase)
    }

    @Test
    fun `TestGame withXitter sets the main character`() {
        val state = TestGame().withXitter(TestGame.BOT).build()
        assertEquals(TestGame.BOT, state.xitterPlayerId)
    }

    @Test
    fun `TestGame withMandates sets mandates for a player`() {
        val state =
            TestGame()
                .withMandates(TestGame.HUMAN, 19)
                .build()
        assertEquals(19, state.players.first { it.id == TestGame.HUMAN }.mandates)
        assertEquals(0, state.players.first { it.id == TestGame.BOT }.mandates)
    }

    @Test
    fun `TestGame withShop populates the shop row`() {
        val id1 = CardId("c49_op_ed")
        val id2 = CardId("c50_photo_op")
        val state = TestGame().withShop(listOf(id1, id2)).build()
        assertEquals(listOf(id1, id2), state.shopCardIds)
    }
}
