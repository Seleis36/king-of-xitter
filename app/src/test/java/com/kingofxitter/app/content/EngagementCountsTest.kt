package com.kingofxitter.app.content

import com.kingofxitter.content.Engagement
import com.kingofxitter.content.LeaderRoster
import kotlin.test.Test
import kotlin.test.assertEquals

class EngagementCountsTest {
    @Test
    fun `counts are compact like the design's`() {
        assertEquals("999", compactCount(999))
        assertEquals("12.4K", compactCount(12_400))
        assertEquals("9.9M", compactCount(9_900_000))
        assertEquals("25.0M", compactCount(25_000_000))
    }

    @Test
    fun `the four counts come out in the design's order`() {
        assertEquals(listOf("1", "2", "3", "4"), compactCounts(Engagement(1, 2, 3, 4)))
    }

    @Test
    fun `a tweet's counts are stable across recompositions`() {
        assertEquals(engagementCounts(LeaderRoster.poutine.id, 5), engagementCounts(LeaderRoster.poutine.id, 5))
        assertEquals(4, engagementCounts(LeaderRoster.poutine.id, 5).size)
    }

    @Test
    fun `Kim's tweet reports no replies and 25 million of everything else`() {
        assertEquals(listOf("0", "25.0M", "25.0M", "25.0M"), engagementCounts(LeaderRoster.kim.id, 9))
    }
}
