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
}
