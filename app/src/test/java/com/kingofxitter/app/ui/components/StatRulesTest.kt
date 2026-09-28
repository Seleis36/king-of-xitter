package com.kingofxitter.app.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class StatRulesTest {
    @Test
    fun `certification follows the roadmap thresholds`() {
        assertEquals(Certification.Unverified, Certification.of(0))
        assertEquals(Certification.Unverified, Certification.of(5))
        assertEquals(Certification.Blue, Certification.of(6))
        assertEquals(Certification.Blue, Certification.of(14))
        assertEquals(Certification.Gold, Certification.of(15))
        assertEquals(Certification.Gold, Certification.of(25))
    }

    @Test
    fun `mandate goal points at the next badge, then the win`() {
        assertEquals(MandateGoal(MandateGoal.Kind.BlueCheck, 6), MandateGoal.after(0))
        assertEquals(MandateGoal(MandateGoal.Kind.GoldCheck, 7), MandateGoal.after(8))
        assertEquals(MandateGoal(MandateGoal.Kind.Win, 4), MandateGoal.after(16))
        assertEquals(MandateGoal(MandateGoal.Kind.Win, 0), MandateGoal.after(22))
    }

    @Test
    fun `bars clamp to their range`() {
        assertEquals(0.4f, mandateFraction(8))
        assertEquals(1f, mandateFraction(30))
        assertEquals(0f, mandateFraction(-1))
        assertEquals(60f / 280f, characterRingFraction(60))
        assertEquals(1f, characterRingFraction(400))
    }

    @Test
    fun `follower pips show current and lost followers up to the maximum`() {
        assertEquals(List(7) { FollowerPip.Full } + List(3) { FollowerPip.Lost }, followerPips(7, 10))
        assertEquals(12, followerPips(12, 12).size)
        assertEquals(List(10) { FollowerPip.Lost }, followerPips(-2, 10))
        assertEquals(emptyList(), followerPips(3, 0))
    }
}
