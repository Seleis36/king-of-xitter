package com.kingofxitter.app.ui.endgame

import com.kingofxitter.app.content.VoiceEntry
import com.kingofxitter.content.TweetMoment
import com.kingofxitter.engine.GameFixtures
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.MatchResult
import com.kingofxitter.engine.model.PlayerId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EndTweetsTest {
    private fun tweet(
        state: GameState,
        by: PlayerId,
        moment: TweetMoment,
        text: String,
        target: PlayerId? = null,
    ) = VoiceEntry(by, state.players.first { it.id == by }.leaderId, moment, target, text, state.turnNumber)

    @Test
    fun `victory quotes the best-placed loser`() {
        val state = GameFixtures.victory
        val winner = (state.result as MatchResult.Victory).playerId
        val placed = standings(state).map { it.player.id }.filter { it != winner }
        val entries =
            listOf(
                tweet(state, placed.last(), TweetMoment.LOST, "last place sulks"),
                tweet(state, placed.first(), TweetMoment.LOST, "runner-up sulks"),
                tweet(state, winner, TweetMoment.WON, "winner gloats"),
            )

        assertEquals("runner-up sulks", EndTweets.reaction(entries, state)?.text)
        assertNull(EndTweets.reaction(entries, GameFixtures.yourRoll))
    }

    @Test
    fun `a suspended player sees the last tweet that hit them`() {
        val state = GameFixtures.defeat
        val you = state.players.first().id
        val kim = (state.result as MatchResult.Victory).playerId
        val other = state.players.first { it.id != you && it.id != kim }.id
        val entries =
            listOf(
                tweet(state, other, TweetMoment.ATTACKED, "earlier hit", you),
                tweet(state, kim, TweetMoment.ATTACKED, "the final blow", you),
                tweet(state, kim, TweetMoment.ATTACKED, "hit someone else", other),
                tweet(state, kim, TweetMoment.WON, "gloat"),
            )

        assertEquals("the final blow", EndTweets.finalTweet(entries, state, you)?.text)
    }

    @Test
    fun `a player who lost on score sees the winner's victory tweet`() {
        val state = GameFixtures.scoreDefeat
        val you = state.players.first().id
        val winner = (state.result as MatchResult.Victory).playerId
        val entries = listOf(tweet(state, winner, TweetMoment.WON, "gloat"))

        assertEquals("gloat", EndTweets.finalTweet(entries, state, you)?.text)
        assertNull(EndTweets.finalTweet(emptyList(), state, you))
    }
}
