package com.kingofxitter.bots

import com.kingofxitter.engine.DefaultGameReducer
import com.kingofxitter.engine.GameFixtures
import com.kingofxitter.engine.MatchSetup
import com.kingofxitter.engine.SplitMix64
import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.CardId
import com.kingofxitter.engine.model.DecisionId
import com.kingofxitter.engine.model.DecisionKind
import com.kingofxitter.engine.model.DecisionOption
import com.kingofxitter.engine.model.DecisionOptionId
import com.kingofxitter.engine.model.DiceFace
import com.kingofxitter.engine.model.Die
import com.kingofxitter.engine.model.DieId
import com.kingofxitter.engine.model.GameEvent
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.LeaderId
import com.kingofxitter.engine.model.PendingDecision
import com.kingofxitter.engine.model.PlayerId
import com.kingofxitter.engine.model.TurnPhase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReplyGuyBotTest {
    private val roster = listOf("trumpet", "poutine", "kim", "macaron", "xi", "mi-leon").map(::LeaderId)
    private val market =
        CardMarket { id ->
            when (id.value) {
                "cheap-order" -> CardAppraisal(cost = 30, isPolicy = false)
                "cheap-policy" -> CardAppraisal(cost = 30, isPolicy = true)
                "dear-policy" -> CardAppraisal(cost = 50, isPolicy = true)
                else -> null
            }
        }
    private val bot = ReplyGuyBot()
    private val shopper = ReplyGuyBot(market)
    private val stream = SplitMix64(7)

    private fun roll(
        vararg faces: DiceFace,
        xitter: PlayerId? = GameFixtures.yourRoll.currentPlayerId,
        followers: Int = 7,
        characters: Int = 30,
        shop: List<String> = emptyList(),
        rollNumber: Int = 1,
    ): GameState {
        val base = GameFixtures.yourRoll
        val you = base.currentPlayerId
        return base.copy(
            dice = faces.mapIndexed { index, face -> Die(DieId(index), face) },
            xitterPlayerId = xitter,
            rollNumber = rollNumber,
            turnRollLimit = null,
            shopCardIds = shop.map(::CardId),
            players = base.players.map { if (it.id == you) it.copy(followers = followers, characters = characters) else it },
        )
    }

    private fun keptFaces(
        state: GameState,
        bot: ReplyGuyBot = this.bot,
    ): Set<DiceFace> {
        val player = state.players.first { it.id == state.currentPlayerId }
        val kept = bot.keepSet(state, player)
        return state.dice
            .filter { it.id in kept }
            .map { it.face }
            .toSet()
    }

    // ---- Dice ----

    @Test
    fun `a completed set is always kept`() {
        val state = roll(DiceFace.TWO, DiceFace.TWO, DiceFace.TWO, DiceFace.ONE, DiceFace.ONE, DiceFace.LIKE)
        assertTrue(DiceFace.TWO in keptFaces(state))
    }

    @Test
    fun `only the best pair of numbers is kept, so there are dice left to complete it`() {
        val state = roll(DiceFace.THREE, DiceFace.THREE, DiceFace.ONE, DiceFace.ONE, DiceFace.LIKE, DiceFace.LIKE)
        val kept = keptFaces(state)
        assertTrue(DiceFace.THREE in kept)
        assertFalse(DiceFace.ONE in kept)
    }

    @Test
    fun `a lone number is rerolled`() {
        val state = roll(DiceFace.THREE, DiceFace.TWO, DiceFace.ONE, DiceFace.LIKE, DiceFace.LIKE, DiceFace.LIKE)
        val kept = keptFaces(state)
        assertFalse(DiceFace.THREE in kept)
        assertFalse(DiceFace.TWO in kept)
    }

    @Test
    fun `likes are kept only when they can heal`() {
        val hurt =
            roll(DiceFace.LIKE, DiceFace.LIKE, DiceFace.ONE, DiceFace.TWO, DiceFace.THREE, DiceFace.TWEET, xitter = null, followers = 6)
        assertTrue(DiceFace.LIKE in keptFaces(hurt))
        // On Xitter the Main Character gets no likes (ROADMAP §1).
        val onXitter = hurt.copy(xitterPlayerId = hurt.currentPlayerId)
        assertFalse(DiceFace.LIKE in keptFaces(onXitter))
        val full = hurt.copy(players = hurt.players.map { if (it.id == hurt.currentPlayerId) it.copy(followers = 10) else it })
        assertFalse(DiceFace.LIKE in keptFaces(full))
    }

    @Test
    fun `unfollows are kept when there is somebody to hit and dropped when Xitter is empty`() {
        val attacking =
            roll(
                DiceFace.TWEET,
                DiceFace.ONE,
                DiceFace.TWO,
                DiceFace.THREE,
                DiceFace.LIKE,
                DiceFace.CHARACTERS,
                xitter = GameFixtures.yourRoll.players[1].id,
            )
        assertTrue(DiceFace.TWEET in keptFaces(attacking))
        val empty = attacking.copy(xitterPlayerId = null)
        assertFalse(DiceFace.TWEET in keptFaces(empty))
    }

    @Test
    fun `characters are only hoarded while the shop holds something out of reach`() {
        val dice = arrayOf(DiceFace.CHARACTERS, DiceFace.ONE, DiceFace.TWO, DiceFace.THREE, DiceFace.LIKE, DiceFace.TWEET)
        // An empty shop, or a market the bot can't read, gives characters nothing to buy.
        assertFalse(DiceFace.CHARACTERS in keptFaces(roll(*dice, shop = emptyList()), shopper))
        assertFalse(DiceFace.CHARACTERS in keptFaces(roll(*dice, shop = listOf("dear-policy"))))
        val reachable = roll(*dice, characters = 30, shop = listOf("dear-policy"))
        assertTrue(DiceFace.CHARACTERS in keptFaces(reachable, shopper))
        val affordable = roll(*dice, characters = 60, shop = listOf("cheap-policy"))
        assertFalse(DiceFace.CHARACTERS in keptFaces(affordable, shopper))
    }

    @Test
    fun `it resolves when there is nothing left worth rerolling and at the roll limit`() {
        val everything =
            roll(
                DiceFace.TWO,
                DiceFace.TWO,
                DiceFace.TWO,
                DiceFace.TWEET,
                DiceFace.TWEET,
                DiceFace.TWEET,
                xitter = GameFixtures.yourRoll.players[1].id,
            )
        assertTrue(bot.decide(everything, null, stream) is Action.ResolveRoll)
        val lastRoll =
            roll(
                DiceFace.ONE,
                DiceFace.TWO,
                DiceFace.THREE,
                DiceFace.LIKE,
                DiceFace.LIKE,
                DiceFace.LIKE,
                rollNumber = 3,
            ).copy(turnRollLimit = 3)
        assertTrue(bot.decide(lastRoll, null, stream) is Action.ResolveRoll)
    }

    @Test
    fun `it keeps rerolling a weak roll, and its keep set is one the engine offers`() {
        val weak = roll(DiceFace.ONE, DiceFace.TWO, DiceFace.THREE, DiceFace.ONE, DiceFace.LIKE, DiceFace.CHARACTERS, followers = 10)
        val action = bot.decide(weak, null, stream)
        assertTrue(action is Action.RollDice)
        assertTrue(action.keptDice in BotMoves.keepCandidates(weak), "${action.keptDice}")
    }

    // ---- Xitter ----

    @Test
    fun `it rarely logs off, but gives up Xitter when its followers run low`() {
        val healthy = GameFixtures.incomingTweet
        val decision = healthy.pendingDecisions.first()
        val stay = bot.decide(healthy, decision, stream) as Action.ChooseDecisionOption
        assertEquals(DecisionOptionId("stay"), stay.optionId)

        val dying = healthy.copy(players = healthy.players.map { if (it.id == decision.playerId) it.copy(followers = 2) else it })
        val logOff = bot.decide(dying, decision, stream) as Action.ChooseDecisionOption
        assertEquals(DecisionOptionId("log-off"), logOff.optionId)
    }

    @Test
    fun `weights move the point at which it logs off`() {
        val state = GameFixtures.incomingTweet
        val decision = state.pendingDecisions.first()
        val diehard = ReplyGuyBot(weights = ReplyGuyWeights(logOffFollowers = 0))
        val coward = ReplyGuyBot(weights = ReplyGuyWeights(logOffFollowers = 9))
        val hurt = state.copy(players = state.players.map { if (it.id == decision.playerId) it.copy(followers = 1) else it })
        assertEquals(DecisionOptionId("stay"), (diehard.decide(hurt, decision, stream) as Action.ChooseDecisionOption).optionId)
        assertEquals(DecisionOptionId("log-off"), (coward.decide(state, decision, stream) as Action.ChooseDecisionOption).optionId)
    }

    // ---- Decisions needing a card's meaning ----

    @Test
    fun `an ability it cannot read is declined`() {
        val state = GameFixtures.yourRoll
        val you = state.currentPlayerId
        val decision =
            PendingDecision(
                DecisionId("ability"),
                you,
                DecisionKind.ACTIVATE_ABILITY,
                listOf(
                    DecisionOption(DecisionOptionId("use"), Action.ActivateAbility(you, CardId("x"), "x")),
                    DecisionOption(DecisionOptionId("no"), Action.DeclineAbility(you)),
                ),
            )
        val action = bot.decide(state, decision, stream) as Action.ChooseDecisionOption
        assertEquals(DecisionOptionId("no"), action.optionId)
    }

    @Test
    fun `it targets the weakest leader`() {
        val state = GameFixtures.yourRoll
        val you = state.currentPlayerId
        val options =
            state.players.filter { it.id != you }.map { player ->
                DecisionOption(DecisionOptionId(player.id.value), Action.ActivateAbility(you, CardId("x"), "x", targetPlayerId = player.id))
            }
        val decision = PendingDecision(DecisionId("target"), you, DecisionKind.CHOOSE_TARGET, options)
        val action = bot.decide(state, decision, stream) as Action.ChooseDecisionOption
        val weakest = state.players.filter { it.id != you }.minBy { it.followers }
        assertEquals(DecisionOptionId(weakest.id.value), action.optionId)
    }

    @Test
    fun `an offered purchase picks the dearest policy it can afford`() {
        val state = GameFixtures.shop
        val you = state.currentPlayerId
        val rich = state.copy(players = state.players.map { if (it.id == you) it.copy(characters = 60) else it })
        val options =
            listOf("cheap-order", "cheap-policy", "dear-policy").map { id ->
                DecisionOption(DecisionOptionId(id), Action.BuyPowerUp(you, CardId(id)))
            } + DecisionOption(DecisionOptionId("no"), Action.DeclineAbility(you))
        val decision = PendingDecision(DecisionId("buy"), you, DecisionKind.BUY_POWER_UP, options)
        assertEquals(DecisionOptionId("dear-policy"), (shopper.decide(rich, decision, stream) as Action.ChooseDecisionOption).optionId)

        val poor = state.copy(players = state.players.map { if (it.id == you) it.copy(characters = 30) else it })
        assertEquals(DecisionOptionId("cheap-policy"), (shopper.decide(poor, decision, stream) as Action.ChooseDecisionOption).optionId)

        val broke = state.copy(players = state.players.map { if (it.id == you) it.copy(characters = 0) else it })
        assertEquals(DecisionOptionId("no"), (shopper.decide(broke, decision, stream) as Action.ChooseDecisionOption).optionId)
    }

    // ---- Shop ----

    @Test
    fun `it buys the dearest card it can afford`() {
        val state = GameFixtures.shop.copy(shopCardIds = listOf("cheap-policy", "dear-policy", "cheap-order").map(::CardId))
        val rich = state.copy(players = state.players.map { if (it.id == state.currentPlayerId) it.copy(characters = 60) else it })
        assertEquals(Action.BuyPowerUp(rich.currentPlayerId, CardId("dear-policy")), shopper.decide(rich, null, stream))
    }

    @Test
    fun `a same-priced policy beats an executive order`() {
        val state = GameFixtures.shop.copy(shopCardIds = listOf("cheap-order", "cheap-policy").map(::CardId))
        val buyer = state.copy(players = state.players.map { if (it.id == state.currentPlayerId) it.copy(characters = 30) else it })
        assertEquals(Action.BuyPowerUp(buyer.currentPlayerId, CardId("cheap-policy")), shopper.decide(buyer, null, stream))
    }

    @Test
    fun `it sweeps a row it cannot use only when a fresh one could still be bought`() {
        val state = GameFixtures.shop.copy(shopCardIds = listOf("dear-policy").map(::CardId))
        // 20 for the sweep plus 20 for the cheapest card in the catalogue.
        val canSweep = state.copy(players = state.players.map { if (it.id == state.currentPlayerId) it.copy(characters = 45) else it })
        assertEquals(Action.SweepShop(canSweep.currentPlayerId), shopper.decide(canSweep, null, stream))
        val tooPoor = state.copy(players = state.players.map { if (it.id == state.currentPlayerId) it.copy(characters = 25) else it })
        assertEquals(Action.EndTurn(tooPoor.currentPlayerId), shopper.decide(tooPoor, null, stream))
    }

    @Test
    fun `it never shops without a market`() {
        val state = GameFixtures.shop.copy(players = GameFixtures.shop.players.map { it.copy(characters = 100) })
        assertEquals(Action.EndTurn(state.currentPlayerId), bot.decide(state, null, stream))
    }

    @Test
    fun `reserved characters are not spent`() {
        val state = GameFixtures.shop.copy(shopCardIds = listOf(CardId("cheap-policy")))
        val saver = ReplyGuyBot(market, ReplyGuyWeights(charactersInReserve = 20))
        val buyer = state.copy(players = state.players.map { if (it.id == state.currentPlayerId) it.copy(characters = 40) else it })
        assertEquals(Action.EndTurn(buyer.currentPlayerId), saver.decide(buyer, null, stream))
    }

    // ---- Whole matches ----

    @Test
    fun `a finished or suspended position gives it nothing to do`() {
        assertNull(bot.decide(GameFixtures.victory, null, stream))
        val suspended =
            GameFixtures.yourRoll.copy(
                phase = TurnPhase.TURN_START,
                players =
                    GameFixtures.yourRoll.players.map {
                        if (it.id ==
                            GameFixtures.yourRoll.currentPlayerId
                        ) {
                            it.copy(followers = 0)
                        } else {
                            it
                        }
                    },
            )
        assertNull(bot.decide(suspended, null, stream))
    }

    @Test
    fun `four Reply Guys play whole matches without an illegal move`() {
        val reducer = DefaultGameReducer()
        repeat(30) { seed ->
            var state = MatchSetup.create(roster[seed % roster.size], roster, seed.toLong()).state
            var steps = 0
            while (state.result == null) {
                val action = assertNotNull(bot.decide(state, state.pendingDecisions.firstOrNull(), botRandom(state)), "seed $seed stalled")
                val transition = reducer.reduce(state, action)
                assertTrue(
                    transition.events.none { it is GameEvent.ActionRejected },
                    "seed $seed: $action rejected in ${state.phase} roll=${state.rollNumber}",
                )
                assertTrue(transition.state != state, "seed $seed: $action changed nothing in ${state.phase}")
                state = transition.state
                assertTrue(++steps < 20_000, "seed $seed did not finish")
            }
        }
    }

    @Test
    fun `it beats the intern more often than not`() {
        val reducer = DefaultGameReducer()
        var replyGuyWins = 0
        var decided = 0
        repeat(60) { seed ->
            var state = MatchSetup.create(roster[seed % roster.size], roster, seed.toLong()).state
            // The first two seats play Reply Guy, the last two the intern.
            val replyGuys =
                state.players
                    .take(2)
                    .map { it.id }
                    .toSet()
            val intern = InternBot()
            var steps = 0
            while (state.result == null && steps < 20_000) {
                val actor = BotMoves.actingPlayerId(state) ?: break
                val brain = if (actor in replyGuys) bot else intern
                val action = brain.decide(state, state.pendingDecisions.firstOrNull(), botRandom(state)) ?: break
                state = reducer.reduce(state, action).state
                steps++
            }
            val winner = (state.result as? com.kingofxitter.engine.model.MatchResult.Victory)?.playerId ?: return@repeat
            decided++
            if (winner in replyGuys) replyGuyWins++
        }
        assertTrue(decided > 40, "only $decided of 60 matches produced a winner")
        // Two of four seats, so parity is 50%. Heuristics should be clearly ahead of random play.
        assertTrue(replyGuyWins * 100 > decided * 60, "Reply Guy won $replyGuyWins of $decided")
    }
}
