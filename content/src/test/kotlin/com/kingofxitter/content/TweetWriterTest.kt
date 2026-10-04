package com.kingofxitter.content

import com.kingofxitter.engine.SplitMix64
import kotlin.test.Test
import kotlin.test.assertEquals

class TweetWriterTest {
    @Test
    fun `a writer sees the moment, the leader and the target`() {
        val writer = TweetWriter { event, leaderId, _ -> Tweet(leaderId, event.moment.name, listOfNotNull(event.target)) }
        val tweet = writer.write(TweetEvent(TweetMoment.ATTACKED, LeaderRoster.kim.id), LeaderRoster.trumpet.id, SplitMix64(1))
        assertEquals(LeaderRoster.trumpet.id, tweet.leaderId)
        assertEquals("ATTACKED", tweet.textKey)
        assertEquals(listOf(LeaderRoster.kim.id), tweet.mentions)
    }

    @Test
    fun `every moment the roadmap lists has a value`() {
        assertEquals(
            setOf(
                "ROLLED",
                "ATTACKED",
                "GOT_HIT",
                "ENTERED_XITTER",
                "LEFT_XITTER",
                "BOUGHT_POWER_UP",
                "SUSPENDED",
                "WON",
                "LOST",
                "BURNER",
            ),
            TweetMoment.entries.map { it.name }.toSet(),
        )
    }
}
