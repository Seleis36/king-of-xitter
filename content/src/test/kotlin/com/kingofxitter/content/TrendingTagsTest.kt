package com.kingofxitter.content

import com.kingofxitter.engine.model.LeaderId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TrendingTagsTest {
    @Test
    fun `every leader has a tag and the extras come last`() {
        assertEquals(LeaderRoster.all.size + TrendingTags.extras.size, TrendingTags.all.size)
        assertEquals(TrendingTags.all.size, TrendingTags.all.toSet().size)
        assertEquals(TrendingTags.extras, TrendingTags.all.takeLast(TrendingTags.extras.size))
        assertEquals(LeaderRoster.all.mapNotNull { TrendingTags.of(it.id) }, TrendingTags.all.dropLast(TrendingTags.extras.size))
    }

    @Test
    fun `a tag names its leader`() {
        assertEquals("trending_tag_mileon", TrendingTags.of(LeaderRoster.mileon.id))
        assertNull(TrendingTags.of(LeaderId("nobody")))
    }
}
