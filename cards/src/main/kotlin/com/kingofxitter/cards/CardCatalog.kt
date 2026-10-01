package com.kingofxitter.cards

import com.kingofxitter.engine.model.CardId

/**
 * Registry of 64 unique definitions representing all 66 base-game power-up cards.
 *
 * Card IDs are stable snake_case strings that match the KoT card number so they
 * remain sortable and won't conflict with future expansions.
 *
 * String resource keys follow the pattern `card_cNN_name`, `card_cNN_rules`,
 * `card_cNN_flavour` — all declared in `app/src/main/res/values/strings.xml`.
 *
 * Costs are in characters (×10 vs. original energy). The two duplicate cards
 * (Extra Head ×2 and Evacuation Orders ×2) have [CardDef.count] = 2.
 */
object CardCatalog {
    // -------------------------------------------------------------------------
    // Policies (kept cards, #1–48)
    // -------------------------------------------------------------------------

    private val doomscrollDrip =
        CardDef(
            id = CardId("c01_doomscroll_drip"),
            nameKey = "card_c01_name",
            cost = 60,
            type = CardType.POLICY,
            rulesKey = "card_c01_rules",
            flavourKey = "card_c01_flavour",
        )

    private val lobbyist =
        CardDef(
            id = CardId("c02_lobbyist"),
            nameKey = "card_c02_name",
            cost = 30,
            type = CardType.POLICY,
            rulesKey = "card_c02_rules",
            flavourKey = "card_c02_flavour",
        )

    private val alphaPoster =
        CardDef(
            id = CardId("c03_alpha_poster"),
            nameKey = "card_c03_name",
            cost = 50,
            type = CardType.POLICY,
            rulesKey = "card_c03_rules",
            flavourKey = "card_c03_flavour",
        )

    private val thickSkinPr =
        CardDef(
            id = CardId("c04_thick_skin_pr"),
            nameKey = "card_c04_name",
            cost = 40,
            type = CardType.POLICY,
            rulesKey = "card_c04_rules",
            flavourKey = "card_c04_flavour",
        )

    private val lurker =
        CardDef(
            id = CardId("c05_lurker"),
            nameKey = "card_c05_name",
            cost = 40,
            type = CardType.POLICY,
            rulesKey = "card_c05_rules",
            flavourKey = "card_c05_flavour",
        )

    private val homeTurf =
        CardDef(
            id = CardId("c06_home_turf"),
            nameKey = "card_c06_name",
            cost = 50,
            type = CardType.POLICY,
            rulesKey = "card_c06_rules",
            flavourKey = "card_c06_flavour",
        )

    private val parodyAccount =
        CardDef(
            id = CardId("c07_parody_account"),
            nameKey = "card_c07_name",
            cost = 30,
            type = CardType.POLICY,
            rulesKey = "card_c07_rules",
            flavourKey = "card_c07_flavour",
        )

    private val fullBingo =
        CardDef(
            id = CardId("c08_full_bingo"),
            nameKey = "card_c08_name",
            cost = 30,
            type = CardType.POLICY,
            rulesKey = "card_c08_rules",
            flavourKey = "card_c08_flavour",
        )

    private val pressOffice =
        CardDef(
            id = CardId("c09_press_office"),
            nameKey = "card_c09_name",
            cost = 30,
            type = CardType.POLICY,
            rulesKey = "card_c09_rules",
            flavourKey = "card_c09_flavour",
        )

    private val gravedancer =
        CardDef(
            id = CardId("c10_gravedancer"),
            nameKey = "card_c10_name",
            cost = 40,
            type = CardType.POLICY,
            rulesKey = "card_c10_rules",
            flavourKey = "card_c10_flavour",
        )

    private val draftsFolder =
        CardDef(
            id = CardId("c11_drafts_folder"),
            nameKey = "card_c11_name",
            cost = 30,
            type = CardType.POLICY,
            rulesKey = "card_c11_rules",
            flavourKey = "card_c11_flavour",
        )

    private val botFarm =
        CardDef(
            id = CardId("c12_bot_farm"),
            nameKey = "card_c12_name",
            cost = 40,
            type = CardType.POLICY,
            rulesKey = "card_c12_rules",
            flavourKey = "card_c12_flavour",
        )

    /** Second Phone appears twice in the standard deck. */
    private val secondPhone =
        CardDef(
            id = CardId("c13_second_phone"),
            nameKey = "card_c13_name",
            cost = 70,
            type = CardType.POLICY,
            rulesKey = "card_c13_rules",
            flavourKey = "card_c13_flavour",
            count = 2,
        )

    private val quoteTweet =
        CardDef(
            id = CardId("c15_quote_tweet"),
            nameKey = "card_c15_name",
            cost = 40,
            type = CardType.POLICY,
            rulesKey = "card_c15_rules",
            flavourKey = "card_c15_flavour",
        )

    private val pinnedTweet =
        CardDef(
            id = CardId("c16_pinned_tweet"),
            nameKey = "card_c16_name",
            cost = 50,
            type = CardType.POLICY,
            rulesKey = "card_c16_rules",
            flavourKey = "card_c16_flavour",
        )

    private val influencerDeal =
        CardDef(
            id = CardId("c17_influencer_deal"),
            nameKey = "card_c17_name",
            cost = 30,
            type = CardType.POLICY,
            rulesKey = "card_c17_rules",
            flavourKey = "card_c17_flavour",
        )

    private val speechwriter =
        CardDef(
            id = CardId("c18_speechwriter"),
            nameKey = "card_c18_name",
            cost = 50,
            type = CardType.POLICY,
            rulesKey = "card_c18_rules",
            flavourKey = "card_c18_flavour",
        )

    private val oldGuard =
        CardDef(
            id = CardId("c19_old_guard"),
            nameKey = "card_c19_name",
            cost = 40,
            type = CardType.POLICY,
            rulesKey = "card_c19_rules",
            flavourKey = "card_c19_flavour",
        )

    private val retweetForRetweet =
        CardDef(
            id = CardId("c20_retweet_for_retweet"),
            nameKey = "card_c20_name",
            cost = 40,
            type = CardType.POLICY,
            rulesKey = "card_c20_rules",
            flavourKey = "card_c20_flavour",
        )

    private val peaceSummit =
        CardDef(
            id = CardId("c21_peace_summit"),
            nameKey = "card_c21_name",
            cost = 50,
            type = CardType.POLICY,
            rulesKey = "card_c21_rules",
            flavourKey = "card_c21_flavour",
        )

    private val pollRigger =
        CardDef(
            id = CardId("c22_poll_rigger"),
            nameKey = "card_c22_name",
            cost = 30,
            type = CardType.POLICY,
            rulesKey = "card_c22_rules",
            flavourKey = "card_c22_flavour",
        )

    private val burnerAccount =
        CardDef(
            id = CardId("c23_burner_account"),
            nameKey = "card_c23_name",
            cost = 70,
            type = CardType.POLICY,
            rulesKey = "card_c23_rules",
            flavourKey = "card_c23_flavour",
        )

    private val privateJet =
        CardDef(
            id = CardId("c24_private_jet"),
            nameKey = "card_c24_name",
            cost = 50,
            type = CardType.POLICY,
            rulesKey = "card_c24_rules",
            flavourKey = "card_c24_flavour",
        )

    private val leakedMemo =
        CardDef(
            id = CardId("c25_leaked_memo"),
            nameKey = "card_c25_name",
            cost = 20,
            type = CardType.POLICY,
            rulesKey = "card_c25_rules",
            flavourKey = "card_c25_flavour",
        )

    private val pivot =
        CardDef(
            id = CardId("c26_pivot"),
            nameKey = "card_c26_name",
            cost = 30,
            type = CardType.POLICY,
            rulesKey = "card_c26_rules",
            flavourKey = "card_c26_flavour",
        )

    private val copycatPolicy =
        CardDef(
            id = CardId("c27_copycat_policy"),
            nameKey = "card_c27_name",
            cost = 80,
            type = CardType.POLICY,
            rulesKey = "card_c27_rules",
            flavourKey = "card_c27_flavour",
        )

    private val superPac =
        CardDef(
            id = CardId("c28_super_pac"),
            nameKey = "card_c28_name",
            cost = 20,
            type = CardType.POLICY,
            rulesKey = "card_c28_rules",
            flavourKey = "card_c28_flavour",
        )

    private val goingViral =
        CardDef(
            id = CardId("c29_going_viral"),
            nameKey = "card_c29_name",
            cost = 70,
            type = CardType.POLICY,
            rulesKey = "card_c29_rules",
            flavourKey = "card_c29_flavour",
        )

    private val bigTent =
        CardDef(
            id = CardId("c30_big_tent"),
            nameKey = "card_c30_name",
            cost = 40,
            type = CardType.POLICY,
            rulesKey = "card_c30_rules",
            flavourKey = "card_c30_flavour",
        )

    private val breakingNewsAlerts =
        CardDef(
            id = CardId("c31_breaking_news_alerts"),
            nameKey = "card_c31_name",
            cost = 30,
            type = CardType.POLICY,
            rulesKey = "card_c31_rules",
            flavourKey = "card_c31_flavour",
        )

    private val hostileTakeover =
        CardDef(
            id = CardId("c32_hostile_takeover"),
            nameKey = "card_c32_name",
            cost = 40,
            type = CardType.POLICY,
            rulesKey = "card_c32_rules",
            flavourKey = "card_c32_flavour",
        )

    private val octoberSurprise =
        CardDef(
            id = CardId("c33_october_surprise"),
            nameKey = "card_c33_name",
            cost = 30,
            type = CardType.POLICY,
            rulesKey = "card_c33_rules",
            flavourKey = "card_c33_flavour",
        )

    private val factCheck =
        CardDef(
            id = CardId("c34_fact_check"),
            nameKey = "card_c34_name",
            cost = 30,
            type = CardType.POLICY,
            rulesKey = "card_c34_rules",
            flavourKey = "card_c34_flavour",
        )

    private val cancelCampaign =
        CardDef(
            id = CardId("c35_cancel_campaign"),
            nameKey = "card_c35_name",
            cost = 40,
            type = CardType.POLICY,
            rulesKey = "card_c35_rules",
            flavourKey = "card_c35_flavour",
        )

    private val phoneTap =
        CardDef(
            id = CardId("c36_phone_tap"),
            nameKey = "card_c36_name",
            cost = 30,
            type = CardType.POLICY,
            rulesKey = "card_c36_rules",
            flavourKey = "card_c36_flavour",
        )

    private val paidPromotion =
        CardDef(
            id = CardId("c37_paid_promotion"),
            nameKey = "card_c37_name",
            cost = 30,
            type = CardType.POLICY,
            rulesKey = "card_c37_rules",
            flavourKey = "card_c37_flavour",
        )

    private val comebackTour =
        CardDef(
            id = CardId("c38_comeback_tour"),
            nameKey = "card_c38_name",
            cost = 40,
            type = CardType.POLICY,
            rulesKey = "card_c38_rules",
            flavourKey = "card_c38_flavour",
        )

    private val underdogNarrative =
        CardDef(
            id = CardId("c39_underdog_narrative"),
            nameKey = "card_c39_name",
            cost = 30,
            type = CardType.POLICY,
            rulesKey = "card_c39_rules",
            flavourKey = "card_c39_flavour",
        )

    private val shadowban =
        CardDef(
            id = CardId("c40_shadowban"),
            nameKey = "card_c40_name",
            cost = 60,
            type = CardType.POLICY,
            rulesKey = "card_c40_rules",
            flavourKey = "card_c40_flavour",
        )

    private val smokeAndMirrors =
        CardDef(
            id = CardId("c41_smoke_and_mirrors"),
            nameKey = "card_c41_name",
            cost = 40,
            type = CardType.POLICY,
            rulesKey = "card_c41_rules",
            flavourKey = "card_c41_flavour",
        )

    private val ghostwriter =
        CardDef(
            id = CardId("c42_ghostwriter"),
            nameKey = "card_c42_name",
            cost = 20,
            type = CardType.POLICY,
            rulesKey = "card_c42_rules",
            flavourKey = "card_c42_flavour",
        )

    private val capsLockStuck =
        CardDef(
            id = CardId("c43_caps_lock_stuck"),
            nameKey = "card_c43_name",
            cost = 50,
            type = CardType.POLICY,
            rulesKey = "card_c43_rules",
            flavourKey = "card_c43_flavour",
        )

    private val spinDoctor =
        CardDef(
            id = CardId("c44_spin_doctor"),
            nameKey = "card_c44_name",
            cost = 30,
            type = CardType.POLICY,
            rulesKey = "card_c44_rules",
            flavourKey = "card_c44_flavour",
        )

    private val focusGroup =
        CardDef(
            id = CardId("c45_focus_group"),
            nameKey = "card_c45_name",
            cost = 40,
            type = CardType.POLICY,
            rulesKey = "card_c45_rules",
            flavourKey = "card_c45_flavour",
        )

    private val mainCharacterEnergy =
        CardDef(
            id = CardId("c46_main_character_energy"),
            nameKey = "card_c46_name",
            cost = 40,
            type = CardType.POLICY,
            rulesKey = "card_c46_rules",
            flavourKey = "card_c46_flavour",
        )

    private val martyrComplex =
        CardDef(
            id = CardId("c47_martyr_complex"),
            nameKey = "card_c47_name",
            cost = 30,
            type = CardType.POLICY,
            rulesKey = "card_c47_rules",
            flavourKey = "card_c47_flavour",
        )

    private val touchGrass =
        CardDef(
            id = CardId("c48_touch_grass"),
            nameKey = "card_c48_name",
            cost = 60,
            type = CardType.POLICY,
            rulesKey = "card_c48_rules",
            flavourKey = "card_c48_flavour",
        )

    // -------------------------------------------------------------------------
    // Executive Orders (discarded on play, #49–66)
    // -------------------------------------------------------------------------

    private val opEd =
        CardDef(
            id = CardId("c49_op_ed"),
            nameKey = "card_c49_name",
            cost = 50,
            type = CardType.EXECUTIVE_ORDER,
            rulesKey = "card_c49_rules",
            flavourKey = "card_c49_flavour",
        )

    private val photoOp =
        CardDef(
            id = CardId("c50_photo_op"),
            nameKey = "card_c50_name",
            cost = 40,
            type = CardType.EXECUTIVE_ORDER,
            rulesKey = "card_c50_rules",
            flavourKey = "card_c50_flavour",
        )

    private val babyKissing =
        CardDef(
            id = CardId("c51_baby_kissing"),
            nameKey = "card_c51_name",
            cost = 30,
            type = CardType.EXECUTIVE_ORDER,
            rulesKey = "card_c51_rules",
            flavourKey = "card_c51_flavour",
        )

    private val hijackTheTrend =
        CardDef(
            id = CardId("c52_hijack_the_trend"),
            nameKey = "card_c52_name",
            cost = 50,
            type = CardType.EXECUTIVE_ORDER,
            rulesKey = "card_c52_rules",
            flavourKey = "card_c52_flavour",
        )

    private val contentBinge =
        CardDef(
            id = CardId("c53_content_binge"),
            nameKey = "card_c53_name",
            cost = 80,
            type = CardType.EXECUTIVE_ORDER,
            rulesKey = "card_c53_rules",
            flavourKey = "card_c53_flavour",
        )

    /** Leaked Group Chat appears twice in the standard deck. */
    private val leakedGroupChat =
        CardDef(
            id = CardId("c54_leaked_group_chat"),
            nameKey = "card_c54_name",
            cost = 70,
            type = CardType.EXECUTIVE_ORDER,
            rulesKey = "card_c54_rules",
            flavourKey = "card_c54_flavour",
            count = 2,
        )

    private val subtweetStorm =
        CardDef(
            id = CardId("c56_subtweet_storm"),
            nameKey = "card_c56_name",
            cost = 30,
            type = CardType.EXECUTIVE_ORDER,
            rulesKey = "card_c56_rules",
            flavourKey = "card_c56_flavour",
        )

    private val allNighter =
        CardDef(
            id = CardId("c57_all_nighter"),
            nameKey = "card_c57_name",
            cost = 70,
            type = CardType.EXECUTIVE_ORDER,
            rulesKey = "card_c57_rules",
            flavourKey = "card_c57_flavour",
        )

    private val stateMediaBlitz =
        CardDef(
            id = CardId("c58_state_media_blitz"),
            nameKey = "card_c58_name",
            cost = 60,
            type = CardType.EXECUTIVE_ORDER,
            rulesKey = "card_c58_rules",
            flavourKey = "card_c58_flavour",
        )

    private val buyFollowers =
        CardDef(
            id = CardId("c59_buy_followers"),
            nameKey = "card_c59_name",
            cost = 30,
            type = CardType.EXECUTIVE_ORDER,
            rulesKey = "card_c59_rules",
            flavourKey = "card_c59_flavour",
        )

    private val replyAllDisaster =
        CardDef(
            id = CardId("c60_reply_all_disaster"),
            nameKey = "card_c60_name",
            cost = 40,
            type = CardType.EXECUTIVE_ORDER,
            rulesKey = "card_c60_rules",
            flavourKey = "card_c60_flavour",
        )

    private val militaryParade =
        CardDef(
            id = CardId("c61_military_parade"),
            nameKey = "card_c61_name",
            cost = 50,
            type = CardType.EXECUTIVE_ORDER,
            rulesKey = "card_c61_rules",
            flavourKey = "card_c61_flavour",
        )

    private val pressConference =
        CardDef(
            id = CardId("c62_press_conference"),
            nameKey = "card_c62_name",
            cost = 30,
            type = CardType.EXECUTIVE_ORDER,
            rulesKey = "card_c62_rules",
            flavourKey = "card_c62_flavour",
        )

    private val rally =
        CardDef(
            id = CardId("c63_rally"),
            nameKey = "card_c63_name",
            cost = 60,
            type = CardType.EXECUTIVE_ORDER,
            rulesKey = "card_c63_rules",
            flavourKey = "card_c63_flavour",
        )

    private val goldenTower =
        CardDef(
            id = CardId("c64_golden_tower"),
            nameKey = "card_c64_name",
            cost = 60,
            type = CardType.EXECUTIVE_ORDER,
            rulesKey = "card_c64_rules",
            flavourKey = "card_c64_flavour",
        )

    private val hotMic =
        CardDef(
            id = CardId("c65_hot_mic"),
            nameKey = "card_c65_name",
            cost = 40,
            type = CardType.EXECUTIVE_ORDER,
            rulesKey = "card_c65_rules",
            flavourKey = "card_c65_flavour",
        )

    private val marketCrash =
        CardDef(
            id = CardId("c66_market_crash"),
            nameKey = "card_c66_name",
            cost = 60,
            type = CardType.EXECUTIVE_ORDER,
            rulesKey = "card_c66_rules",
            flavourKey = "card_c66_flavour",
        )

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * All 64 unique card definitions in KoT catalogue order.
     * Duplicate entries (Second Phone and Leaked Group Chat) appear once here;
     * use [standardDeck] for the physical deck that contains two copies each.
     */
    val all: List<CardDef> =
        listOf(
            doomscrollDrip,
            lobbyist,
            alphaPoster,
            thickSkinPr,
            lurker,
            homeTurf,
            parodyAccount,
            fullBingo,
            pressOffice,
            gravedancer,
            draftsFolder,
            botFarm,
            secondPhone,
            quoteTweet,
            pinnedTweet,
            influencerDeal,
            speechwriter,
            oldGuard,
            retweetForRetweet,
            peaceSummit,
            pollRigger,
            burnerAccount,
            privateJet,
            leakedMemo,
            pivot,
            copycatPolicy,
            superPac,
            goingViral,
            bigTent,
            breakingNewsAlerts,
            hostileTakeover,
            octoberSurprise,
            factCheck,
            cancelCampaign,
            phoneTap,
            paidPromotion,
            comebackTour,
            underdogNarrative,
            shadowban,
            smokeAndMirrors,
            ghostwriter,
            capsLockStuck,
            spinDoctor,
            focusGroup,
            mainCharacterEnergy,
            martyrComplex,
            touchGrass,
            opEd,
            photoOp,
            babyKissing,
            hijackTheTrend,
            contentBinge,
            leakedGroupChat,
            subtweetStorm,
            allNighter,
            stateMediaBlitz,
            buyFollowers,
            replyAllDisaster,
            militaryParade,
            pressConference,
            rally,
            goldenTower,
            hotMic,
            marketCrash,
        )

    /**
     * Lookup map from [CardId] to its definition.
     * Unknown IDs return null when using this map; [get] rejects them.
     */
    val byId: Map<CardId, CardDef> = all.associateBy { it.id }

    /**
     * The physical draw deck: 66 cards, with Second Phone and Leaked Group Chat
     * each contributing two copies. The list preserves catalogue order so shuffling is the
     * caller's responsibility.
     */
    val standardDeck: List<CardId> =
        all.flatMap { def -> List(def.count) { def.id } }

    /**
     * Returns the definition for [id], or throws if [id] is not in the catalogue.
     */
    operator fun get(id: CardId): CardDef = byId[id] ?: error("Unknown CardId: $id")
}
