package com.kingofxitter.app.ui.components

import com.kingofxitter.engine.model.DiceFace
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiceTest {
    @Test
    fun `every face shows either a number or an icon`() {
        DiceFace.entries.forEach { face -> assertTrue((face.number() == null) != (face.icon() == null), "$face") }
        assertEquals(listOf(1, 2, 3), DiceFace.entries.mapNotNull { it.number() })
        assertEquals(
            6,
            DiceFace.entries
                .map { it.label() }
                .toSet()
                .size,
        )
    }

    @Test
    fun `tumble starts and ends at rest`() {
        listOf(0f, 1f, 1.2f).forEach { t ->
            assertEquals(0f, tumbleRotation(t))
            assertEquals(1f, tumbleScale(t))
            assertEquals(0f, tumbleHop(t))
        }
    }

    @Test
    fun `tumble follows the kit keyframes without jumps`() {
        assertEquals(160f, tumbleRotation(0.25f), 0.01f)
        assertEquals(1.18f, tumbleScale(0.25f), 0.001f)
        assertEquals(1f, tumbleHop(0.25f), 0.001f)
        assertEquals(330f, tumbleRotation(0.55f), 0.01f)
        assertEquals(0.94f, tumbleScale(0.55f), 0.001f)
        var previous = tumbleScale(0f)
        var t = 0.001f
        while (t < 1f) {
            val scale = tumbleScale(t)
            assertTrue(abs(scale - previous) < 0.02f, "scale jumps at $t")
            previous = scale
            t += 0.001f
        }
    }
}
