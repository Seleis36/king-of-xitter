package com.kingofxitter.bots

import com.kingofxitter.engine.DefaultGameReducer
import com.kingofxitter.engine.GameFixtures
import com.kingofxitter.engine.MatchSetup
import com.kingofxitter.engine.SplitMix64
import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.DiceFace
import com.kingofxitter.engine.model.Die
import com.kingofxitter.engine.model.DieId
import com.kingofxitter.engine.model.GameEvent
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.LeaderId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class LeaderPersonalitiesTest {
    private val stream = SplitMix64(3)

    @Test
    fun `the six leaders the simulator plays all have a personality`() {
        assertEquals(DEFAULT_LEADERS.toSet(), LeaderPersonalities.leaders.toSet())
        assertEquals(6, LeaderPersonalities.leaders.size)
    }

    @Test
    fun `no two leaders play the same way`() {
        val sets = LeaderPersonalities.leaders.map(LeaderPersonalities::of)
        assertEquals(sets.size, sets.toSet().size)
    }

    @Test
    fun `a leader with no personality on record plays the plain Reply Guy`() {
        assertEquals(ReplyGuyWeights(), LeaderPersonalities.of(LeaderId("nobody")))
    }

    @Test
    fun `the voices come through in the weights`() {
        val trumpet = LeaderPersonalities.of(LeaderId("trumpet"))
        val kim = LeaderPersonalities.of(LeaderId("kim"))
        val mileon = LeaderPersonalities.of(LeaderId("mi-leon"))
        val xi = LeaderPersonalities.of(LeaderId("xi"))
        // Kim hoards mandates; Mi-León attacks everything and heals nothing.
        assertTrue(kim.mandateHunger > trumpet.mandateHunger)
        assertTrue(mileon.aggression > kim.aggression)
        assertTrue(mileon.selfPreservation < kim.selfPreservation)
        // Mi-León clings to Xitter; Kim retreats soonest, to keep the broadcast clean.
        assertTrue(mileon.logOffFollowers < kim.logOffFollowers)
        assertTrue(kim.logOffFollowers > trumpet.logOffFollowers)
        // Xi plays the long game: it saves characters for the plan.
        assertTrue(xi.shopAppetite > trumpet.shopAppetite)
        assertTrue(xi.charactersInReserve > 0)
    }

    @Test
    fun `an eager attacker keeps a lone unfollow where a reluctant one rerolls it`() {
        val base = GameFixtures.yourRoll
        val state =
            base.copy(
                dice =
                    listOf(DiceFace.TWEET, DiceFace.ONE, DiceFace.TWO, DiceFace.THREE, DiceFace.LIKE, DiceFace.CHARACTERS)
                        .mapIndexed { index, face -> Die(DieId(index), face) },
                xitterPlayerId = base.players[1].id,
                turnRollLimit = null,
                rollNumber = 1,
            )
        val player = state.players.first { it.id == state.currentPlayerId }

        fun keepsTweet(weights: ReplyGuyWeights): Boolean {
            val kept = ReplyGuyBot(weights = weights).keepSet(state, player)
            return state.dice.any { it.face == DiceFace.TWEET && it.id in kept }
        }
        assertTrue(keepsTweet(LeaderPersonalities.of(LeaderId("mi-leon"))))
        assertFalse(keepsTweet(ReplyGuyWeights(aggression = 0.5f)))
    }

    @Test
    fun `each seat plays its own leader's brain, built once`() {
        val bots = LeaderBots(BotDifficulty.REPLY_GUY)
        val kim = bots.brain(LeaderId("kim"))
        assertSame(kim, bots.brain(LeaderId("kim")))
        assertNotEquals(kim, bots.brain(LeaderId("mi-leon")))
        assertEquals(BotDifficulty.REPLY_GUY, bots.difficulty)
    }

    @Test
    fun `the intern ignores personalities, because it has no preferences to tune`() {
        val bots = LeaderBots(BotDifficulty.INTERN)
        assertEquals(BotDifficulty.INTERN, bots.brain(LeaderId("kim")).difficulty)
        assertEquals(BotDifficulty.INTERN, bots.brain(LeaderId("mi-leon")).difficulty)
    }

    @Test
    fun `personalities change how a position is played`() {
        val state = GameFixtures.incomingTweet
        val decision = state.pendingDecisions.first()
        val hurt = state.copy(players = state.players.map { if (it.id == decision.playerId) it.copy(followers = 3) else it })
        val bots = LeaderBots(BotDifficulty.REPLY_GUY)
        // Mi-León never logs off; Kim gives up Xitter well before it gets dangerous.
        val mileon = bots.brain(LeaderId("mi-leon")).decide(hurt, decision, stream) as Action.ChooseDecisionOption
        val kim = bots.brain(LeaderId("kim")).decide(hurt, decision, stream) as Action.ChooseDecisionOption
        assertNotEquals(mileon.optionId, kim.optionId)
    }

    @Test
    fun `a table of personalities still plays whole matches legally`() {
        val reducer = DefaultGameReducer()
        val bots = LeaderBots(BotDifficulty.REPLY_GUY)
        repeat(20) { seed ->
            var state: GameState = MatchSetup.create(DEFAULT_LEADERS[seed % 6], DEFAULT_LEADERS, seed.toLong()).state
            var steps = 0
            while (state.result == null) {
                val action = bots.decide(state, state.pendingDecisions.firstOrNull(), botRandom(state))
                assertTrue(action != null, "seed $seed stalled in ${state.phase}")
                val transition = reducer.reduce(state, action)
                assertTrue(transition.events.none { it is GameEvent.ActionRejected }, "seed $seed: $action rejected")
                assertTrue(transition.state != state, "seed $seed: $action changed nothing")
                state = transition.state
                assertTrue(++steps < 20_000, "seed $seed did not finish")
            }
        }
    }
}
