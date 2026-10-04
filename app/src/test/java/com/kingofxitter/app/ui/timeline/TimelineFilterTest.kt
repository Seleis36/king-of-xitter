package com.kingofxitter.app.ui.timeline

import com.kingofxitter.app.content.VoiceEntry
import com.kingofxitter.content.TweetMoment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TimelineFilterTest {
    private val humanId = TimelineSamples.humanId
    private val entries = TimelineSamples.entries

    @Test
    fun `for you keeps everything, newest first`() {
        val shown = entries.onTimeline(TimelineFilter.FOR_YOU, humanId)
        assertEquals(entries.size, shown.size)
        assertEquals(entries.reversed(), shown)
        // The design reads newest first, so turn numbers never increase down the list.
        assertTrue(shown.zipWithNext().all { (newer, older) -> newer.turnNumber >= older.turnNumber })
    }

    @Test
    fun `beef keeps only tweets that cost followers, from either side`() {
        val shown = entries.onTimeline(TimelineFilter.BEEF, humanId)
        assertTrue(shown.isNotEmpty())
        assertTrue(shown.all { it.moment == TweetMoment.ATTACKED || it.moment == TweetMoment.GOT_HIT })
        assertTrue(shown.any { it.playerId != humanId }, "beef includes hits the human took")
    }

    @Test
    fun `my ratios keeps only your own attacks`() {
        val shown = entries.onTimeline(TimelineFilter.MINE, humanId)
        assertTrue(shown.isNotEmpty())
        assertTrue(shown.all { it.playerId == humanId && it.moment == TweetMoment.ATTACKED })
    }

    @Test
    fun `every filter has a label and something to say when it is empty`() {
        assertEquals(
            TimelineFilter.entries.size,
            TimelineFilter.entries
                .map { it.label }
                .toSet()
                .size,
        )
        assertEquals(
            TimelineFilter.entries.size,
            TimelineFilter.entries
                .map { it.empty }
                .toSet()
                .size,
        )
        TimelineFilter.entries.forEach { filter ->
            assertEquals(emptyList(), emptyList<VoiceEntry>().onTimeline(filter, humanId))
        }
    }

    @Test
    fun `the clock runs later as the match goes on and stays a valid time`() {
        val times = (1..8).map(::timelineClock)
        assertTrue(times.all { (hour, minute) -> hour in 0..23 && minute in 0..59 }, "$times")
        assertTrue(timelineClock(2).second > timelineClock(1).second)
        assertEquals(timelineClock(4), timelineClock(4))
    }

    @Test
    fun `the samples cover every outcome tag the screen can draw`() {
        val moments = entries.map { it.moment }.toSet()
        assertTrue(TweetMoment.ATTACKED in moments)
        assertTrue(TweetMoment.GOT_HIT in moments)
        assertTrue(TweetMoment.ENTERED_XITTER in moments)
        assertTrue(TweetMoment.LEFT_XITTER in moments)
        assertTrue(TweetMoment.BOUGHT_POWER_UP in moments)
        assertTrue(TweetMoment.ROLLED in moments)
        assertTrue(entries.all { entry -> TimelineSamples.state.players.any { it.id == entry.playerId } })
    }
}
