package com.kingofxitter.app.ui.components

import com.kingofxitter.content.LeaderRoster
import com.kingofxitter.engine.model.LeaderId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class LeaderVisualTest {
    private val unknown = leaderVisual(LeaderId("nobody"))

    @Test
    fun `every roster leader has its own colour, names and portrait`() {
        val visuals = LeaderRoster.all.map { leaderVisual(it.id) }
        visuals.forEach { assertNotEquals(unknown, it) }
        assertEquals(LeaderRoster.all.size, visuals.map { it.color }.toSet().size)
        assertEquals(LeaderRoster.all.size, visuals.map { it.shortName }.toSet().size)
        assertEquals(LeaderRoster.all.size, visuals.map { it.firstName }.toSet().size)
        assertEquals(LeaderRoster.all.size, visuals.mapNotNull { it.portrait }.toSet().size)
    }

    @Test
    fun `the roster's colour reaches the UI unchanged`() {
        LeaderRoster.all.forEach { leader ->
            assertEquals(leader.colorArgb.toULong(), leaderVisual(leader.id).color.value shr 32)
        }
    }

    @Test
    fun `unknown leaders fall back instead of crashing`() {
        assertEquals(unknown, leaderVisual(LeaderId("")))
        assertNull(unknown.portrait)
        assertNotNull(leaderVisual(LeaderRoster.kim.id).portrait)
    }
}
