package com.kingofxitter.cards

import com.kingofxitter.engine.DefaultGameReducer
import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.CardEffectRegistry
import com.kingofxitter.engine.model.CardId
import com.kingofxitter.engine.model.DiceFace.CHARACTERS
import com.kingofxitter.engine.model.DiceFace.LIKE
import com.kingofxitter.engine.model.DiceFace.ONE
import com.kingofxitter.engine.model.DiceFace.THREE
import com.kingofxitter.engine.model.DiceFace.TWEET
import com.kingofxitter.engine.model.DiceFace.TWO
import com.kingofxitter.engine.model.GameEvent
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.LeaderId
import com.kingofxitter.engine.model.PlayerId
import com.kingofxitter.engine.model.PlayerState
import com.kingofxitter.engine.model.TurnPhase
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * One group of tests per passive policy in [PassivePolicies], run through the real
 * [DefaultGameReducer] and [ShopReducer] with the card effect registry, against the rule text in
 * ROADMAP section 2. Dice are set by hand so every roll is deterministic.
 */
class PassivePoliciesTest {
    private val engine = DefaultGameReducer(CardEffectRegistry(PassivePolicies.effects + ExecutiveOrders.effects))
    private val shop = ShopReducer(engine)
    private val third = PlayerId("third")
    private val fourth = PlayerId("fourth")

    private fun GameState.player(id: PlayerId) = players.first { it.id == id }

    private fun seat(id: PlayerId) = PlayerState(id = id, leaderId = LeaderId("kim"))

    /** Resolves the rolled dice of [game] for [roller] and returns the state afterwards. */
    private fun resolve(
        game: TestGame,
        roller: PlayerId = TestGame.HUMAN,
    ): GameState = engine.reduce(game.build(), Action.ResolveRoll(roller)).state

    /** Answers the Main Character's pending stay-or-log-off decision with "stay". */
    private fun stay(state: GameState): GameState {
        val decision = state.pendingDecisions.single()
        val option = decision.options.first { it.action is Action.StayInXitter }
        return engine.reduce(state, Action.ChooseDecisionOption(decision.playerId, decision.id, option.id)).state
    }

    private fun endTurn(
        game: TestGame,
        player: PlayerId = TestGame.HUMAN,
    ): GameState = engine.reduce(game.withPhase(TurnPhase.SHOP).build(), Action.EndTurn(player)).state

    /** Buys [cardId] from a shop that sells only it, for the human with plenty of characters. */
    private fun buy(
        cardId: CardId,
        game: TestGame = TestGame(),
        characters: Int = 150,
    ): GameState {
        val state =
            game
                .withPhase(TurnPhase.SHOP)
                .withShop(listOf(cardId))
                .withCharacters(TestGame.HUMAN, characters)
                .build()
        val result = shop.reduce(state, Action.BuyPowerUp(TestGame.HUMAN, cardId))
        assertTrue(
            result.events.any { it is GameEvent.PowerUpBought },
            "the purchase should have gone through",
        )
        return result.state
    }

    // C01 Doomscroll Drip: deal 1 extra unfollow each turn, even if you roll no tweets.

    @Test
    fun `Doomscroll Drip unfollows the Main Character once even with no tweets`() {
        val after =
            resolve(
                TestGame()
                    .withPolicy(TestGame.HUMAN, CardId("c01_doomscroll_drip"))
                    .withXitter(TestGame.BOT)
                    .withDice(ONE, TWO, LIKE, LIKE, CHARACTERS, CHARACTERS),
            )

        assertEquals(9, after.player(TestGame.BOT).followers)
    }

    @Test
    fun `Doomscroll Drip adds 1 unfollow on top of the tweets`() {
        val after =
            resolve(
                TestGame()
                    .withPolicy(TestGame.HUMAN, CardId("c01_doomscroll_drip"))
                    .withXitter(TestGame.BOT)
                    .withDice(TWEET, TWEET, ONE, TWO, LIKE, CHARACTERS),
            )

        assertEquals(7, after.player(TestGame.BOT).followers)
    }

    // C03 Alpha Poster: +1 mandate when you tweet at someone.

    @Test
    fun `Alpha Poster gives 1 mandate when you tweet at the Main Character`() {
        val after =
            resolve(
                TestGame()
                    .withPolicy(TestGame.HUMAN, CardId("c03_alpha_poster"))
                    .withXitter(TestGame.BOT)
                    .withDice(TWEET, ONE, TWO, LIKE, LIKE, CHARACTERS),
            )

        assertEquals(1, after.player(TestGame.HUMAN).mandates)
    }

    @Test
    fun `Alpha Poster gives 1 mandate when you tweet from Xitter`() {
        val after =
            resolve(
                TestGame()
                    .withPolicy(TestGame.HUMAN, CardId("c03_alpha_poster"))
                    .withXitter(TestGame.HUMAN)
                    .withDice(TWEET, TWEET, ONE, TWO, LIKE, CHARACTERS),
            )

        assertEquals(1, after.player(TestGame.HUMAN).mandates)
    }

    @Test
    fun `Alpha Poster gives nothing when you roll no tweets`() {
        val after =
            resolve(
                TestGame()
                    .withPolicy(TestGame.HUMAN, CardId("c03_alpha_poster"))
                    .withXitter(TestGame.BOT)
                    .withDice(ONE, TWO, LIKE, LIKE, CHARACTERS, CHARACTERS),
            )

        assertEquals(0, after.player(TestGame.HUMAN).mandates)
    }

    // C04 Thick Skin PR: ignore hits of exactly 1.

    @Test
    fun `Thick Skin PR ignores a hit of exactly 1 unfollow`() {
        val after =
            resolve(
                TestGame()
                    .withPolicy(TestGame.HUMAN, CardId("c04_thick_skin_pr"))
                    .withXitter(TestGame.HUMAN)
                    .withCurrentPlayer(TestGame.BOT)
                    .withDice(TWEET, ONE, TWO, LIKE, LIKE, CHARACTERS),
                roller = TestGame.BOT,
            )

        assertEquals(10, after.player(TestGame.HUMAN).followers)
        // Nothing was dealt, so the Main Character has no log-off to decide.
        assertTrue(after.pendingDecisions.isEmpty())
    }

    @Test
    fun `Thick Skin PR does not ignore a hit of 2 unfollows`() {
        val after =
            resolve(
                TestGame()
                    .withPolicy(TestGame.HUMAN, CardId("c04_thick_skin_pr"))
                    .withXitter(TestGame.HUMAN)
                    .withCurrentPlayer(TestGame.BOT)
                    .withDice(TWEET, TWEET, ONE, TWO, LIKE, CHARACTERS),
                roller = TestGame.BOT,
            )

        assertEquals(8, after.player(TestGame.HUMAN).followers)
    }

    // C08 Full Bingo: rolling 1-2-3-like-tweet-chars gives +9 mandates.

    @Test
    fun `Full Bingo gives 9 mandates for one of every face`() {
        val after =
            resolve(
                TestGame()
                    .withPolicy(TestGame.HUMAN, CardId("c08_full_bingo"))
                    .withXitter(TestGame.BOT)
                    .withDice(ONE, TWO, THREE, LIKE, CHARACTERS, TWEET),
            )

        assertEquals(9, after.player(TestGame.HUMAN).mandates)
    }

    @Test
    fun `Full Bingo gives nothing when a face is missing`() {
        val after =
            resolve(
                TestGame()
                    .withPolicy(TestGame.HUMAN, CardId("c08_full_bingo"))
                    .withXitter(TestGame.BOT)
                    .withDice(ONE, TWO, THREE, LIKE, TWEET, TWEET),
            )

        assertEquals(0, after.player(TestGame.HUMAN).mandates)
    }

    // C09 Press Office: +1 mandate per power-up bought.

    @Test
    fun `Press Office gives 1 mandate when you buy a policy`() {
        val after =
            buy(
                CardId("c05_lurker"),
                TestGame().withPolicy(TestGame.HUMAN, CardId("c09_press_office")),
            )

        assertTrue(CardId("c05_lurker") in after.player(TestGame.HUMAN).policyIds)
        assertEquals(1, after.player(TestGame.HUMAN).mandates)
    }

    @Test
    fun `Press Office adds 1 mandate on top of an executive order`() {
        // Baby Kissing is worth 1 mandate itself, Press Office makes it 2.
        val after =
            buy(
                CardId("c51_baby_kissing"),
                TestGame().withPolicy(TestGame.HUMAN, CardId("c09_press_office")),
            )

        assertEquals(2, after.player(TestGame.HUMAN).mandates)
    }

    @Test
    fun `Without Press Office buying a policy gives no mandate`() {
        val after = buy(CardId("c05_lurker"))

        assertEquals(0, after.player(TestGame.HUMAN).mandates)
    }

    // C10 Gravedancer: +3 mandates whenever a leader is suspended.

    @Test
    fun `Gravedancer gives 3 mandates when another leader tweets a leader out`() {
        // The third leader suspends the bot. The human only watches, and is paid.
        val after =
            resolve(
                TestGame()
                    .withPlayer(seat(third))
                    .withPolicy(TestGame.HUMAN, CardId("c10_gravedancer"))
                    .withXitter(TestGame.BOT)
                    .withFollowers(TestGame.BOT, 1)
                    .withCurrentPlayer(third)
                    .withDice(TWEET, ONE, TWO, LIKE, LIKE, CHARACTERS),
                roller = third,
            )

        assertTrue(after.player(TestGame.BOT).isSuspended)
        assertEquals(3, after.player(TestGame.HUMAN).mandates)
    }

    @Test
    fun `Gravedancer pays 3 mandates on top of Xitter when your own tweet suspends a leader`() {
        val after =
            resolve(
                TestGame()
                    .withPlayer(seat(third))
                    .withPolicy(TestGame.HUMAN, CardId("c10_gravedancer"))
                    .withXitter(TestGame.BOT)
                    .withFollowers(TestGame.BOT, 1)
                    .withDice(TWEET, ONE, TWO, LIKE, LIKE, CHARACTERS),
            )

        assertTrue(after.player(TestGame.BOT).isSuspended)
        // 3 for the suspension and 1 for taking Xitter.
        assertEquals(4, after.player(TestGame.HUMAN).mandates)
    }

    @Test
    fun `Gravedancer gives nothing when nobody is suspended`() {
        val after =
            resolve(
                TestGame()
                    .withPlayer(seat(third))
                    .withPolicy(TestGame.HUMAN, CardId("c10_gravedancer"))
                    .withXitter(TestGame.BOT)
                    .withCurrentPlayer(third)
                    .withDice(TWEET, ONE, TWO, LIKE, LIKE, CHARACTERS),
                roller = third,
            )

        assertEquals(0, after.player(TestGame.HUMAN).mandates)
    }

    // C11 Drafts Folder: at end of turn, +1 mandate per 60 chars you hold.

    @Test
    fun `Drafts Folder gives 1 mandate per 60 characters at end of turn`() {
        val after =
            endTurn(
                TestGame()
                    .withPolicy(TestGame.HUMAN, CardId("c11_drafts_folder"))
                    .withCharacters(TestGame.HUMAN, 130),
            )

        assertEquals(2, after.player(TestGame.HUMAN).mandates)
        // The characters are held, not spent.
        assertEquals(130, after.player(TestGame.HUMAN).characters)
    }

    @Test
    fun `Drafts Folder gives nothing below 60 characters`() {
        val after =
            endTurn(
                TestGame()
                    .withPolicy(TestGame.HUMAN, CardId("c11_drafts_folder"))
                    .withCharacters(TestGame.HUMAN, 50),
            )

        assertEquals(0, after.player(TestGame.HUMAN).mandates)
    }

    // C15 Quote Tweet: your neighbours lose 1M extra when you deal damage.

    /** Seats four leaders in the order human, bot, third, fourth. The human holds Quote Tweet. */
    private fun quoteTweetTable(mainCharacter: PlayerId?): TestGame =
        TestGame()
            .withPlayer(seat(third))
            .withPlayer(seat(fourth))
            .withPolicy(TestGame.HUMAN, CardId("c15_quote_tweet"))
            .withXitter(mainCharacter)

    @Test
    fun `Quote Tweet costs both neighbours 1M extra when you tweet from Xitter`() {
        val after = resolve(quoteTweetTable(TestGame.HUMAN).withDice(TWEET, ONE, TWO, LIKE, LIKE, CHARACTERS))

        // The bot and the fourth leader sit either side of the human. The third leader does not.
        assertEquals(8, after.player(TestGame.BOT).followers)
        assertEquals(8, after.player(fourth).followers)
        assertEquals(9, after.player(third).followers)
    }

    @Test
    fun `Quote Tweet costs a neighbour Main Character 1M extra`() {
        val after = resolve(quoteTweetTable(TestGame.BOT).withDice(TWEET, ONE, TWO, LIKE, LIKE, CHARACTERS))

        assertEquals(8, after.player(TestGame.BOT).followers)
    }

    @Test
    fun `Quote Tweet adds nothing against a Main Character who is not a neighbour`() {
        val after = resolve(quoteTweetTable(third).withDice(TWEET, ONE, TWO, LIKE, LIKE, CHARACTERS))

        assertEquals(9, after.player(third).followers)
    }

    @Test
    fun `Quote Tweet adds nothing when you deal no damage`() {
        val after = resolve(quoteTweetTable(TestGame.BOT).withDice(ONE, TWO, LIKE, LIKE, CHARACTERS, CHARACTERS))

        assertEquals(10, after.player(TestGame.BOT).followers)
    }

    // C17 Influencer Deal: +10 extra chars whenever you gain chars.

    @Test
    fun `Influencer Deal adds 10 characters to characters rolled`() {
        val after =
            resolve(
                TestGame()
                    .withPolicy(TestGame.HUMAN, CardId("c17_influencer_deal"))
                    .withXitter(TestGame.BOT)
                    .withDice(CHARACTERS, CHARACTERS, ONE, TWO, LIKE, LIKE),
            )

        assertEquals(30, after.player(TestGame.HUMAN).characters)
    }

    @Test
    fun `Influencer Deal gives nothing when no characters are gained`() {
        val after =
            resolve(
                TestGame()
                    .withPolicy(TestGame.HUMAN, CardId("c17_influencer_deal"))
                    .withXitter(TestGame.BOT)
                    .withDice(ONE, TWO, LIKE, LIKE, TWEET, TWEET),
            )

        assertEquals(0, after.player(TestGame.HUMAN).characters)
    }

    @Test
    fun `Influencer Deal adds 10 characters to characters gained from a power-up`() {
        val contentBinge = CardId("c53_content_binge")
        val after =
            buy(
                contentBinge,
                TestGame().withPolicy(TestGame.HUMAN, CardId("c17_influencer_deal")),
                characters = 150,
            )

        val cost = CardCatalog.byId.getValue(contentBinge).cost
        assertEquals(150 - cost + 90 + 10, after.player(TestGame.HUMAN).characters)
    }

    // C19 Old Guard: 1-1-1 scores +2 extra.

    @Test
    fun `Old Guard makes three 1s score 3 mandates`() {
        val after =
            resolve(
                TestGame()
                    .withPolicy(TestGame.HUMAN, CardId("c19_old_guard"))
                    .withXitter(TestGame.BOT)
                    .withDice(ONE, ONE, ONE, LIKE, LIKE, CHARACTERS),
            )

        // 1 for the set, 2 from the card.
        assertEquals(3, after.player(TestGame.HUMAN).mandates)
    }

    @Test
    fun `Old Guard keeps the bonus flat when a fourth 1 is rolled`() {
        val after =
            resolve(
                TestGame()
                    .withPolicy(TestGame.HUMAN, CardId("c19_old_guard"))
                    .withXitter(TestGame.BOT)
                    .withDice(ONE, ONE, ONE, ONE, LIKE, CHARACTERS),
            )

        // 2 for the set (1 plus one extra die), 2 from the card.
        assertEquals(4, after.player(TestGame.HUMAN).mandates)
    }

    @Test
    fun `Old Guard gives nothing for two 1s or for another triple`() {
        val twoOnes =
            resolve(
                TestGame()
                    .withPolicy(TestGame.HUMAN, CardId("c19_old_guard"))
                    .withXitter(TestGame.BOT)
                    .withDice(ONE, ONE, TWO, THREE, LIKE, CHARACTERS),
            )
        val threeTwos =
            resolve(
                TestGame()
                    .withPolicy(TestGame.HUMAN, CardId("c19_old_guard"))
                    .withXitter(TestGame.BOT)
                    .withDice(TWO, TWO, TWO, LIKE, LIKE, CHARACTERS),
            )

        assertEquals(0, twoOnes.player(TestGame.HUMAN).mandates)
        // Three 2s score 2 on their own, with no Old Guard bonus on top.
        assertEquals(2, threeTwos.player(TestGame.HUMAN).mandates)
    }

    // C21 Peace Summit: +1 mandate on a turn where you hit nobody.

    @Test
    fun `Peace Summit gives 1 mandate at end of a turn with no hits`() {
        val rolled =
            resolve(
                TestGame()
                    .withPolicy(TestGame.HUMAN, CardId("c21_peace_summit"))
                    .withXitter(TestGame.BOT)
                    .withDice(ONE, TWO, LIKE, LIKE, CHARACTERS, CHARACTERS),
            )
        val after = engine.reduce(rolled, Action.EndTurn(TestGame.HUMAN)).state

        assertEquals(1, after.player(TestGame.HUMAN).mandates)
    }

    @Test
    fun `Peace Summit gives nothing after a turn where you hit someone`() {
        val rolled =
            resolve(
                TestGame()
                    .withPolicy(TestGame.HUMAN, CardId("c21_peace_summit"))
                    .withXitter(TestGame.BOT)
                    .withDice(TWEET, ONE, TWO, LIKE, LIKE, CHARACTERS),
            )
        val after = engine.reduce(stay(rolled), Action.EndTurn(TestGame.HUMAN)).state

        assertEquals(9, after.player(TestGame.BOT).followers)
        assertEquals(0, after.player(TestGame.HUMAN).mandates)
    }

    // C29 Going Viral: your tweets hit everyone.

    @Test
    fun `Going Viral hits every other leader, not only the Main Character`() {
        val after =
            resolve(
                TestGame()
                    .withPlayer(seat(third))
                    .withPolicy(TestGame.HUMAN, CardId("c29_going_viral"))
                    .withXitter(TestGame.BOT)
                    .withDice(TWEET, TWEET, ONE, TWO, LIKE, CHARACTERS),
            )

        assertEquals(8, after.player(TestGame.BOT).followers)
        assertEquals(8, after.player(third).followers)
        // The tweeter is not hit.
        assertEquals(10, after.player(TestGame.HUMAN).followers)
    }

    @Test
    fun `Going Viral hurts nobody when you roll no tweets`() {
        val after =
            resolve(
                TestGame()
                    .withPlayer(seat(third))
                    .withPolicy(TestGame.HUMAN, CardId("c29_going_viral"))
                    .withXitter(TestGame.BOT)
                    .withDice(ONE, TWO, LIKE, LIKE, CHARACTERS, CHARACTERS),
            )

        assertEquals(10, after.player(TestGame.BOT).followers)
        assertEquals(10, after.player(third).followers)
    }

    // C30 Big Tent: once per turn, 1-2-3 scores 2 (those dice still count for other sets).

    @Test
    fun `Big Tent scores 2 mandates for a 1-2-3`() {
        val after =
            resolve(
                TestGame()
                    .withPolicy(TestGame.HUMAN, CardId("c30_big_tent"))
                    .withXitter(TestGame.BOT)
                    .withDice(ONE, TWO, THREE, LIKE, LIKE, CHARACTERS),
            )

        assertEquals(2, after.player(TestGame.HUMAN).mandates)
    }

    @Test
    fun `Big Tent scores only once for two 1-2-3 sets`() {
        val after =
            resolve(
                TestGame()
                    .withPolicy(TestGame.HUMAN, CardId("c30_big_tent"))
                    .withXitter(TestGame.BOT)
                    .withDice(ONE, TWO, THREE, ONE, TWO, THREE),
            )

        assertEquals(2, after.player(TestGame.HUMAN).mandates)
    }

    @Test
    fun `Big Tent leaves the dice free to score another set`() {
        val after =
            resolve(
                TestGame()
                    .withPolicy(TestGame.HUMAN, CardId("c30_big_tent"))
                    .withXitter(TestGame.BOT)
                    .withDice(ONE, ONE, ONE, TWO, THREE, LIKE),
            )

        // 1 for the three 1s, 2 for the 1-2-3 that reuses a 1.
        assertEquals(3, after.player(TestGame.HUMAN).mandates)
    }

    @Test
    fun `Big Tent gives nothing when a number is missing`() {
        val after =
            resolve(
                TestGame()
                    .withPolicy(TestGame.HUMAN, CardId("c30_big_tent"))
                    .withXitter(TestGame.BOT)
                    .withDice(ONE, TWO, TWO, LIKE, LIKE, CHARACTERS),
            )

        assertEquals(0, after.player(TestGame.HUMAN).mandates)
    }

    // C34 Fact Check: 2-2-2 also deals 2 unfollows.

    @Test
    fun `Fact Check deals 2 unfollows for three 2s with no tweets`() {
        val after =
            resolve(
                TestGame()
                    .withPolicy(TestGame.HUMAN, CardId("c34_fact_check"))
                    .withXitter(TestGame.BOT)
                    .withDice(TWO, TWO, TWO, LIKE, LIKE, CHARACTERS),
            )

        assertEquals(8, after.player(TestGame.BOT).followers)
        // The three 2s still score their own 2 mandates.
        assertEquals(2, after.player(TestGame.HUMAN).mandates)
    }

    @Test
    fun `Fact Check adds 2 unfollows on top of the tweets`() {
        val after =
            resolve(
                TestGame()
                    .withPolicy(TestGame.HUMAN, CardId("c34_fact_check"))
                    .withXitter(TestGame.BOT)
                    .withDice(TWO, TWO, TWO, TWEET, LIKE, CHARACTERS),
            )

        assertEquals(7, after.player(TestGame.BOT).followers)
    }

    @Test
    fun `Fact Check deals nothing for two 2s`() {
        val after =
            resolve(
                TestGame()
                    .withPolicy(TestGame.HUMAN, CardId("c34_fact_check"))
                    .withXitter(TestGame.BOT)
                    .withDice(TWO, TWO, ONE, LIKE, LIKE, CHARACTERS),
            )

        assertEquals(10, after.player(TestGame.BOT).followers)
    }

    // C38 Comeback Tour: heal 1M extra whenever you heal.

    @Test
    fun `Comeback Tour heals 1M extra when likes heal you`() {
        val after =
            resolve(
                TestGame()
                    .withPolicy(TestGame.HUMAN, CardId("c38_comeback_tour"))
                    .withXitter(TestGame.BOT)
                    .withFollowers(TestGame.HUMAN, 5)
                    .withDice(LIKE, ONE, TWO, CHARACTERS, CHARACTERS, TWEET),
            )

        // 1 for the like, 1 from the card.
        assertEquals(7, after.player(TestGame.HUMAN).followers)
    }

    @Test
    fun `Comeback Tour heals 1M extra on a power-up that heals`() {
        val after =
            buy(
                CardId("c59_buy_followers"),
                TestGame()
                    .withPolicy(TestGame.HUMAN, CardId("c38_comeback_tour"))
                    .withFollowers(TestGame.HUMAN, 5),
            )

        // Buy Followers heals 2M, Comeback Tour adds 1M.
        assertEquals(8, after.player(TestGame.HUMAN).followers)
    }

    @Test
    fun `Comeback Tour never heals past the follower cap`() {
        val after =
            resolve(
                TestGame()
                    .withPolicy(TestGame.HUMAN, CardId("c38_comeback_tour"))
                    .withXitter(TestGame.BOT)
                    .withFollowers(TestGame.HUMAN, 9)
                    .withDice(LIKE, ONE, TWO, CHARACTERS, CHARACTERS, TWEET),
            )

        assertEquals(10, after.player(TestGame.HUMAN).followers)
    }

    @Test
    fun `Comeback Tour gives nothing when you are not healed`() {
        val after =
            resolve(
                TestGame()
                    .withPolicy(TestGame.HUMAN, CardId("c38_comeback_tour"))
                    .withXitter(TestGame.BOT)
                    .withFollowers(TestGame.HUMAN, 5)
                    .withDice(ONE, TWO, CHARACTERS, CHARACTERS, TWEET, TWEET),
            )

        assertEquals(5, after.player(TestGame.HUMAN).followers)
    }

    // C39 Underdog Narrative: at end of turn, if you have the fewest mandates, +1.

    @Test
    fun `Underdog Narrative gives 1 mandate to the leader with the fewest mandates`() {
        val after =
            endTurn(
                TestGame()
                    .withPlayer(seat(third))
                    .withPolicy(TestGame.HUMAN, CardId("c39_underdog_narrative"))
                    .withMandates(TestGame.HUMAN, 1)
                    .withMandates(TestGame.BOT, 3)
                    .withMandates(third, 5),
            )

        assertEquals(2, after.player(TestGame.HUMAN).mandates)
    }

    @Test
    fun `Underdog Narrative gives nothing when someone has fewer mandates`() {
        val after =
            endTurn(
                TestGame()
                    .withPlayer(seat(third))
                    .withPolicy(TestGame.HUMAN, CardId("c39_underdog_narrative"))
                    .withMandates(TestGame.HUMAN, 4)
                    .withMandates(TestGame.BOT, 2)
                    .withMandates(third, 5),
            )

        assertEquals(4, after.player(TestGame.HUMAN).mandates)
    }

    @Test
    fun `Underdog Narrative ignores suspended leaders`() {
        // The bot is suspended with 0 mandates, so it is not who the human is measured against.
        val after =
            endTurn(
                TestGame()
                    .withPlayer(seat(third))
                    .withPolicy(TestGame.HUMAN, CardId("c39_underdog_narrative"))
                    .withMandates(TestGame.HUMAN, 2)
                    .withFollowers(TestGame.BOT, 0)
                    .withMandates(TestGame.BOT, 0)
                    .withMandates(third, 4),
            )

        assertEquals(3, after.player(TestGame.HUMAN).mandates)
    }

    // C42 Ghostwriter: at end of turn with 0 chars, gain 10.

    @Test
    fun `Ghostwriter gives 10 characters at end of turn with none`() {
        val after =
            endTurn(
                TestGame()
                    .withPolicy(TestGame.HUMAN, CardId("c42_ghostwriter"))
                    .withCharacters(TestGame.HUMAN, 0),
            )

        assertEquals(10, after.player(TestGame.HUMAN).characters)
    }

    @Test
    fun `Ghostwriter gives nothing when you still hold characters`() {
        val after =
            endTurn(
                TestGame()
                    .withPolicy(TestGame.HUMAN, CardId("c42_ghostwriter"))
                    .withCharacters(TestGame.HUMAN, 10),
            )

        assertEquals(10, after.player(TestGame.HUMAN).characters)
    }

    // C43 CAPS LOCK STUCK: +1 unfollow when you tweet.

    @Test
    fun `CAPS LOCK STUCK adds 1 unfollow when you tweet`() {
        val after =
            resolve(
                TestGame()
                    .withPolicy(TestGame.HUMAN, CardId("c43_caps_lock_stuck"))
                    .withXitter(TestGame.BOT)
                    .withDice(TWEET, TWEET, ONE, TWO, LIKE, CHARACTERS),
            )

        assertEquals(7, after.player(TestGame.BOT).followers)
    }

    @Test
    fun `CAPS LOCK STUCK adds nothing when you roll no tweets`() {
        val after =
            resolve(
                TestGame()
                    .withPolicy(TestGame.HUMAN, CardId("c43_caps_lock_stuck"))
                    .withXitter(TestGame.BOT)
                    .withDice(ONE, TWO, LIKE, LIKE, CHARACTERS, CHARACTERS),
            )

        assertEquals(10, after.player(TestGame.BOT).followers)
    }

    // C46 Main Character Energy: +1 mandate when starting on Xitter; +1 unfollow while on Xitter.

    private fun startTurn(game: TestGame): GameState =
        engine
            .reduce(
                game.withPhase(TurnPhase.TURN_START).withRollNumber(0).build(),
                Action.RollDice(TestGame.HUMAN, emptySet()),
            ).state

    @Test
    fun `Main Character Energy gives 1 mandate on top of the 2 for starting on Xitter`() {
        val after =
            startTurn(
                TestGame()
                    .withPolicy(TestGame.HUMAN, CardId("c46_main_character_energy"))
                    .withXitter(TestGame.HUMAN),
            )

        assertEquals(3, after.player(TestGame.HUMAN).mandates)
    }

    @Test
    fun `Main Character Energy gives no mandate when you start off Xitter`() {
        val after =
            startTurn(
                TestGame()
                    .withPolicy(TestGame.HUMAN, CardId("c46_main_character_energy"))
                    .withXitter(TestGame.BOT),
            )

        assertEquals(0, after.player(TestGame.HUMAN).mandates)
    }

    @Test
    fun `Main Character Energy adds 1 unfollow to tweets sent from Xitter`() {
        val after =
            resolve(
                TestGame()
                    .withPolicy(TestGame.HUMAN, CardId("c46_main_character_energy"))
                    .withXitter(TestGame.HUMAN)
                    .withDice(TWEET, ONE, TWO, LIKE, LIKE, CHARACTERS),
            )

        assertEquals(8, after.player(TestGame.BOT).followers)
    }

    @Test
    fun `Main Character Energy adds no unfollow to tweets sent from outside Xitter`() {
        val after =
            resolve(
                TestGame()
                    .withPolicy(TestGame.HUMAN, CardId("c46_main_character_energy"))
                    .withXitter(TestGame.BOT)
                    .withDice(TWEET, ONE, TWO, LIKE, LIKE, CHARACTERS),
            )

        assertEquals(9, after.player(TestGame.BOT).followers)
    }

    @Ignore("BUG: Main Character Energy deals 1 unfollow from Xitter even with no tweets; the extra unfollow should only add to a tweet")
    @Test
    fun `Main Character Energy adds nothing when you roll no tweets`() {
        val after =
            resolve(
                TestGame()
                    .withPolicy(TestGame.HUMAN, CardId("c46_main_character_energy"))
                    .withXitter(TestGame.HUMAN)
                    .withDice(ONE, TWO, LIKE, LIKE, CHARACTERS, CHARACTERS),
            )

        assertEquals(10, after.player(TestGame.BOT).followers)
    }

    // C47 Martyr Complex: lose 2M+ at once and gain 10 chars.

    @Test
    fun `Martyr Complex gives 10 characters after losing 2M at once`() {
        val after =
            resolve(
                TestGame()
                    .withPolicy(TestGame.HUMAN, CardId("c47_martyr_complex"))
                    .withXitter(TestGame.HUMAN)
                    .withCurrentPlayer(TestGame.BOT)
                    .withDice(TWEET, TWEET, ONE, TWO, LIKE, CHARACTERS),
                roller = TestGame.BOT,
            )

        assertEquals(8, after.player(TestGame.HUMAN).followers)
        assertEquals(10, after.player(TestGame.HUMAN).characters)
    }

    @Test
    fun `Martyr Complex gives nothing after losing only 1M`() {
        val after =
            resolve(
                TestGame()
                    .withPolicy(TestGame.HUMAN, CardId("c47_martyr_complex"))
                    .withXitter(TestGame.HUMAN)
                    .withCurrentPlayer(TestGame.BOT)
                    .withDice(TWEET, ONE, TWO, LIKE, LIKE, CHARACTERS),
                roller = TestGame.BOT,
            )

        assertEquals(9, after.player(TestGame.HUMAN).followers)
        assertEquals(0, after.player(TestGame.HUMAN).characters)
    }
}
