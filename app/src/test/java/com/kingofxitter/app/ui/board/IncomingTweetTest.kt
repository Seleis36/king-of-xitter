package com.kingofxitter.app.ui.board

import com.kingofxitter.engine.GameFixtures
import com.kingofxitter.engine.model.PlayerId
import kotlin.test.Test
import kotlin.test.assertEquals

class IncomingTweetTest {
    @Test
    fun `the estimate counts the attacker's tweet dice`() {
        assertEquals(IncomingHit(PlayerId("kim"), 1), IncomingHit.estimate(GameFixtures.incomingTweet))
        assertEquals(IncomingHit(PlayerId("trumpet"), 2), IncomingHit.estimate(IncomingSamples.design))
    }

    @Test
    fun `counts are compact like the design's`() {
        assertEquals("999", compactCount(999))
        assertEquals("12.4K", compactCount(12_400))
        assertEquals("9.9M", compactCount(9_900_000))
    }

    @Test
    fun `fake engagement is deterministic`() {
        assertEquals(fakeEngagement(8), fakeEngagement(8))
        assertEquals(4, fakeEngagement(8).size)
    }
}
