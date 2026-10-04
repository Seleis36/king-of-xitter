package com.kingofxitter.content

import com.kingofxitter.engine.SplitMix64
import com.kingofxitter.engine.model.LeaderId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TweetVoicesTest {
    private val writer = TemplateTweetWriter()

    @Test
    fun `every leader has at least five lines for every moment`() {
        LeaderRoster.all.forEach { leader ->
            TweetMoment.entries.forEach { moment ->
                val keys = TweetVoices.keys(leader.id, moment)
                assertTrue(keys.size >= 5, "${leader.id.value}/$moment has ${keys.size}")
                assertEquals(keys.size, keys.toSet().size)
            }
        }
    }

    @Test
    fun `keys are unique across the whole bank`() {
        assertEquals(LeaderRoster.all.size * TweetMoment.entries.size * TweetVoices.LINES_PER_MOMENT, TweetVoices.all.size)
        assertEquals(TweetVoices.all.size, TweetVoices.all.toSet().size)
    }

    @Test
    fun `a key names its leader and its moment`() {
        assertEquals(
            listOf(0, 1, 2, 3, 4).map { "tweet_mileon_got_hit_$it" },
            TweetVoices.keys(LeaderRoster.mileon.id, TweetMoment.GOT_HIT),
        )
    }

    @Test
    fun `the writer only ever returns a line of the right leader and moment`() {
        val stream = SplitMix64(11)
        LeaderRoster.all.forEach { leader ->
            TweetMoment.entries.forEach { moment ->
                repeat(20) {
                    val tweet = writer.write(TweetEvent(moment), leader.id, stream)
                    assertEquals(leader.id, tweet.leaderId)
                    assertTrue(tweet.textKey in TweetVoices.keys(leader.id, moment), tweet.textKey)
                }
            }
        }
    }

    @Test
    fun `the writer uses every line it has`() {
        val stream = SplitMix64(4)
        val used = List(400) { writer.write(TweetEvent(TweetMoment.WON), LeaderRoster.kim.id, stream).textKey }.toSet()
        assertEquals(TweetVoices.keys(LeaderRoster.kim.id, TweetMoment.WON).toSet(), used)
    }

    @Test
    fun `the target is carried through as a mention`() {
        val tweet = writer.write(TweetEvent(TweetMoment.ATTACKED, LeaderRoster.xi.id), LeaderRoster.poutine.id, SplitMix64(2))
        assertEquals(listOf(LeaderRoster.xi.id), tweet.mentions)
        assertEquals(emptyList(), writer.write(TweetEvent(TweetMoment.ROLLED), LeaderRoster.poutine.id, SplitMix64(2)).mentions)
    }

    @Test
    fun `a leader outside the roster writes nothing instead of crashing`() {
        assertEquals(emptyList(), TweetVoices.keys(LeaderId("nobody"), TweetMoment.WON))
        assertEquals("", writer.write(TweetEvent(TweetMoment.WON), LeaderId("nobody"), SplitMix64(1)).textKey)
    }
}
