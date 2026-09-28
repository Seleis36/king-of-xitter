package com.kingofxitter.app.ui.components

import com.kingofxitter.engine.model.LeaderId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class LeaderVisualTest {
    private val leaders = listOf("trumpet", "poutine", "kim", "macaron", "xi", "mi-leon").map(::LeaderId)
    private val unknown = leaderVisual(LeaderId("nobody"))

    @Test
    fun `all six leaders have their own colour and names`() {
        val visuals = leaders.map(::leaderVisual)
        visuals.forEach { assertNotEquals(unknown, it) }
        assertEquals(6, visuals.map { it.color }.toSet().size)
        assertEquals(6, visuals.map { it.shortName }.toSet().size)
    }

    @Test
    fun `unknown leaders fall back instead of crashing`() {
        assertEquals(unknown, leaderVisual(LeaderId("")))
    }
}
