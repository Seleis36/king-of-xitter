package com.kingofxitter.app.ui.board

import androidx.compose.ui.unit.dp
import com.kingofxitter.app.ui.components.DieSize
import kotlin.test.Test
import kotlin.test.assertEquals

class DiceTrayTest {
    @Test
    fun `six dice keep the design's size on a regular phone`() {
        // 411dp Pixel 7 minus the board's 16dp gutters.
        assertEquals(DieSize.Default, dieSizeFor(6, 379.dp))
    }

    @Test
    fun `six dice shrink to compact on a 320dp phone with 48dp touch targets`() {
        assertEquals(DieSize.Compact, dieSizeFor(6, 288.dp))
    }

    @Test
    fun `extra dice shrink further`() {
        assertEquals(DieSize.Compact, dieSizeFor(7, 379.dp))
        assertEquals(DieSize.Small, dieSizeFor(7, 288.dp))
        assertEquals(DieSize.ExtraSmall, dieSizeFor(12, 288.dp))
    }
}
