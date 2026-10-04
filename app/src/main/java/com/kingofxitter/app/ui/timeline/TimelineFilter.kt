package com.kingofxitter.app.ui.timeline

import androidx.annotation.StringRes
import com.kingofxitter.app.R
import com.kingofxitter.app.content.VoiceEntry
import com.kingofxitter.content.TweetMoment
import com.kingofxitter.engine.model.PlayerId

/** The design's three timeline tabs. */
enum class TimelineFilter(
    @param:StringRes val label: Int,
    @param:StringRes val empty: Int,
) {
    /** Everything that was said. */
    FOR_YOU(R.string.timeline_filter_for_you, R.string.timeline_empty),

    /** Only tweets that cost somebody followers, from either side. */
    BEEF(R.string.timeline_filter_beef, R.string.timeline_empty_beef),

    /** Only your own attacks. */
    MINE(R.string.timeline_filter_mine, R.string.timeline_empty_mine),
    ;

    /** Whether [entry] belongs on this tab, for the human playing as [humanId]. */
    fun accepts(
        entry: VoiceEntry,
        humanId: PlayerId,
    ): Boolean =
        when (this) {
            FOR_YOU -> true
            BEEF -> entry.moment == TweetMoment.ATTACKED || entry.moment == TweetMoment.GOT_HIT
            MINE -> entry.moment == TweetMoment.ATTACKED && entry.playerId == humanId
        }
}

/** [entries] on this tab, newest first, as the design's timeline reads. */
fun List<VoiceEntry>.onTimeline(
    filter: TimelineFilter,
    humanId: PlayerId,
): List<VoiceEntry> = filter { filter.accepts(it, humanId) }.asReversed()

/**
 * The fake 3 AM clock the design's timestamps use. Later turns read as later minutes, so the
 * timeline's order and its times agree.
 */
fun timelineClock(turnNumber: Int): Pair<Int, Int> = HOUR to (turnNumber * MINUTES_PER_TURN) % 60

private const val HOUR = 3
private const val MINUTES_PER_TURN = 7
