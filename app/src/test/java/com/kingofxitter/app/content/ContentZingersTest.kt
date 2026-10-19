package com.kingofxitter.app.content

import com.kingofxitter.content.LeaderRoster
import com.kingofxitter.content.TweetMoment
import com.kingofxitter.content.TweetVoices
import com.kingofxitter.content.ZingerBook
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContentZingersTest {
    private val xi = LeaderRoster.xi.id
    private val trumpet = LeaderRoster.trumpet.id

    @Test
    fun `the first page is the pair's own lines`() {
        assertEquals(ZingerBook.keys(xi, trumpet), ContentZingers.keys(xi, trumpet, page = 0, count = 3))
    }

    @Test
    fun `new takes are new lines, not the same three again`() {
        val first = ContentZingers.keys(xi, trumpet, page = 0, count = 3)
        val second = ContentZingers.keys(xi, trumpet, page = 1, count = 3)

        assertTrue(first.intersect(second.toSet()).isEmpty(), "$first then $second")
        assertTrue(second.all { it in TweetVoices.keys(xi, TweetMoment.ATTACKED) })
    }

    @Test
    fun `every line in the pool comes round, and pages wrap`() {
        val pool = ZingerBook.keys(xi, trumpet) + TweetVoices.keys(xi, TweetMoment.ATTACKED)
        val seen = (0 until pool.size).flatMap { ContentZingers.keys(xi, trumpet, it, 3) }.toSet()

        assertEquals(pool.toSet(), seen)
        assertEquals(ContentZingers.keys(xi, trumpet, 0, 3), ContentZingers.keys(xi, trumpet, pool.size, 3))
    }

    @Test
    fun `tweeting at everyone uses the whole-timeline lines first`() {
        assertEquals(ZingerBook.keys(xi, null), ContentZingers.keys(xi, null, page = 0, count = 3))
    }
}
