package com.kingofxitter.bots

import com.kingofxitter.engine.DefaultGameReducer
import com.kingofxitter.engine.GameFixtures
import com.kingofxitter.engine.MatchSetup
import com.kingofxitter.engine.SplitMix64
import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.GameEvent
import com.kingofxitter.engine.model.MatchResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TrollFarmBotTest {
    private val roster = DEFAULT_LEADERS
    private val quick = TrollFarmSearch(playouts = 2, depth = 12, candidates = 4)
    private val bot = TrollFarmBot(search = quick)

    @Test
    fun `it only ever picks a legal move`() {
        val state = GameFixtures.yourRoll
        val legal = BotMoves.legalActions(state).toSet()
        repeat(20) { seed -> assertTrue(bot.decide(state, null, SplitMix64(seed.toLong())) in legal) }
    }

    @Test
    fun `the same position and stream always give the same move`() {
        val state = GameFixtures.yourRoll
        assertEquals(
            List(10) { bot.decide(state, null, SplitMix64(31)) },
            List(10) { bot.decide(state, null, SplitMix64(31)) },
        )
    }

    @Test
    fun `a finished match gives it nothing to do`() {
        assertNull(bot.decide(GameFixtures.victory, null, SplitMix64(1)))
    }

    @Test
    fun `a single legal move is taken without searching`() {
        val lastRoll = GameFixtures.yourRoll.copy(rollNumber = 3, turnRollLimit = 3)
        assertEquals(Action.ResolveRoll(lastRoll.currentPlayerId), bot.decide(lastRoll, null, SplitMix64(1)))
    }

    @Test
    fun `it answers a decision with one of its options`() {
        val state = GameFixtures.incomingTweet
        val decision = state.pendingDecisions.first()
        val action = bot.decide(state, decision, SplitMix64(5)) as Action.ChooseDecisionOption
        assertTrue(action.optionId in decision.options.map { it.id })
    }

    @Test
    fun `winning beats every live position and losing is worse than any of them`() {
        val you = GameFixtures.yourRoll.currentPlayerId
        val live = bot.score(GameFixtures.yourRoll, you)
        assertTrue(bot.score(GameFixtures.victory, you) > live)
        assertTrue(bot.score(GameFixtures.defeat, you) < live)
        assertTrue(bot.score(GameFixtures.noWinner, you) < live)
        // A suspended leader has lost whatever the scoreboard says.
        val suspended =
            GameFixtures.yourRoll.copy(
                players =
                    GameFixtures.yourRoll.players.map {
                        if (it.id ==
                            you
                        ) {
                            it.copy(followers = 0, mandates = 19)
                        } else {
                            it
                        }
                    },
            )
        assertTrue(bot.score(suspended, you) < live)
    }

    @Test
    fun `more mandates, more followers and Xitter all score better`() {
        val state = GameFixtures.yourRoll
        val you = state.currentPlayerId
        val base = state.copy(xitterPlayerId = null)

        fun withYou(change: (com.kingofxitter.engine.model.PlayerState) -> com.kingofxitter.engine.model.PlayerState) =
            base.copy(players = base.players.map { if (it.id == you) change(it) else it })
        val reference = bot.score(base, you)
        assertTrue(bot.score(withYou { it.copy(mandates = it.mandates + 3) }, you) > reference)
        assertTrue(bot.score(withYou { it.copy(followers = it.followers + 2) }, you) > reference)
        assertTrue(bot.score(base.copy(xitterPlayerId = you), you) > reference)
        // A rival closing on twenty mandates is bad news.
        val rival = base.players.first { it.id != you }.id
        assertTrue(bot.score(base.copy(players = base.players.map { if (it.id == rival) it.copy(mandates = 19) else it }), you) < reference)
    }

    @Test
    fun `four Troll Farms play whole matches without an illegal move`() {
        val reducer = DefaultGameReducer()
        repeat(4) { seed ->
            var state = MatchSetup.create(roster[seed], roster, seed.toLong()).state
            var steps = 0
            while (state.result == null) {
                val action = assertNotNull(bot.decide(state, state.pendingDecisions.firstOrNull(), botRandom(state)), "seed $seed stalled")
                val transition = reducer.reduce(state, action)
                assertTrue(transition.events.none { it is GameEvent.ActionRejected }, "seed $seed: $action rejected in ${state.phase}")
                assertTrue(transition.state != state, "seed $seed: $action changed nothing")
                state = transition.state
                assertTrue(++steps < 20_000, "seed $seed did not finish")
            }
        }
    }

    @Test
    fun `search makes it stronger than the heuristic it falls back on`() {
        val reducer = DefaultGameReducer()
        val replyGuy = ReplyGuyBot()
        var trollWins = 0
        var decided = 0
        repeat(24) { seed ->
            var state = MatchSetup.create(roster[seed % roster.size], roster, seed.toLong()).state
            val trolls =
                state.players
                    .take(2)
                    .map { it.id }
                    .toSet()
            var steps = 0
            while (state.result == null && steps < 20_000) {
                val actor = BotMoves.actingPlayerId(state) ?: break
                val brain = if (actor in trolls) bot else replyGuy
                val action = brain.decide(state, state.pendingDecisions.firstOrNull(), botRandom(state)) ?: break
                state = reducer.reduce(state, action).state
                steps++
            }
            val winner = (state.result as? MatchResult.Victory)?.playerId ?: return@repeat
            decided++
            if (winner in trolls) trollWins++
        }
        assertTrue(decided > 16, "only $decided of 24 matches produced a winner")
        // Two of four seats, so parity is 50%; the search should be ahead even at this depth.
        assertTrue(trollWins * 2 > decided, "Troll Farm won $trollWins of $decided")
    }
}
