package com.kingofxitter.app.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class XIconTest {
    @Test
    fun `every icon has its own drawable`() {
        assertEquals(
            XIcon.entries.size,
            XIcon.entries
                .map { it.drawable }
                .toSet()
                .size,
        )
    }
}
