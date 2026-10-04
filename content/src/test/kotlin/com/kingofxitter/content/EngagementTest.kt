package com.kingofxitter.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EngagementTest {
    @Test
    fun `the same tweet always has the same counts`() {
        LeaderRoster.all.forEach { leader ->
            assertEquals(EngagementNumbers.of(leader.id, 12), EngagementNumbers.of(leader.id, 12))
        }
    }

    @Test
    fun `different tweets and leaders get different counts`() {
        val sameLeader = (0 until 20).map { EngagementNumbers.of(LeaderRoster.macaron.id, it) }.toSet()
        assertTrue(sameLeader.size > 15, "${sameLeader.size} distinct of 20")
        val sameSeed =
            LeaderRoster.all
                .filter { it.id != LeaderRoster.kim.id }
                .map { EngagementNumbers.of(it.id, 3) }
                .toSet()
        assertEquals(5, sameSeed.size)
    }

    @Test
    fun `Kim always reports exactly 25 million and nobody ever replies`() {
        (0 until 10).forEach { seed ->
            val counts = EngagementNumbers.of(LeaderRoster.kim.id, seed)
            assertEquals(0, counts.replies)
            assertEquals(EngagementNumbers.SUPREME_NUMBER, counts.retweets)
            assertEquals(EngagementNumbers.SUPREME_NUMBER, counts.likes)
            assertEquals(EngagementNumbers.SUPREME_NUMBER, counts.views)
        }
    }

    @Test
    fun `views outrank likes, likes outrank retweets, and nothing is negative`() {
        LeaderRoster.all.forEach { leader ->
            (0 until 25).forEach { seed ->
                val counts = EngagementNumbers.of(leader.id, seed)
                assertTrue(counts.replies >= 0, "${leader.id.value}/$seed")
                assertTrue(counts.views >= counts.likes, "${leader.id.value}/$seed")
                assertTrue(counts.likes >= counts.retweets, "${leader.id.value}/$seed")
            }
        }
    }

    @Test
    fun `Mi-Leon gets more replies than the calmer leaders`() {
        val screaming = (0 until 30).sumOf { EngagementNumbers.of(LeaderRoster.mileon.id, it).replies }
        val serene = (0 until 30).sumOf { EngagementNumbers.of(LeaderRoster.xi.id, it).replies }
        assertTrue(screaming > serene, "$screaming vs $serene")
    }
}
