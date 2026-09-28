package com.kingofxitter.app.ui.board

import com.kingofxitter.engine.model.LeaderId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class PlaceholderZingersTest {
    private val source =
        PlaceholderZingers(listOf("a %1\$s", "b %1\$s", "c %1\$s", "d %1\$s")) { it?.let { id -> "@${id.value}" } ?: "everyone" }
    private val me = LeaderId("macaron")

    @Test
    fun `fills in the target's handle`() {
        assertEquals(listOf("a @trumpet", "b @trumpet", "c @trumpet"), source.zingers(me, LeaderId("trumpet"), page = 0, count = 3))
        assertEquals("a everyone", source.zingers(me, null, page = 0, count = 1).single())
    }

    @Test
    fun `new takes page through the lines and wrap around`() {
        val first = source.zingers(me, null, page = 0, count = 3)
        val second = source.zingers(me, null, page = 1, count = 3)
        assertNotEquals(first, second)
        assertEquals(listOf("d everyone", "a everyone", "b everyone"), second)
        assertEquals(first, source.zingers(me, null, page = 0, count = 3))
    }

    @Test
    fun `never returns more lines than it has`() {
        assertEquals(4, source.zingers(me, null, page = 0, count = 10).size)
        assertEquals(emptyList(), PlaceholderZingers(emptyList()) { "" }.zingers(me, null, 0, 3))
    }
}
