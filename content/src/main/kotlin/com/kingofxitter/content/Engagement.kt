package com.kingofxitter.content

import com.kingofxitter.engine.SplitMix64
import com.kingofxitter.engine.model.LeaderId

/** The four counts under a tweet. They are flavour: nothing in the rules reads them. */
data class Engagement(
    val replies: Long,
    val retweets: Long,
    val likes: Long,
    val views: Long,
)

/**
 * V2's fake engagement. The numbers are derived from the tweet's own seed, so a tweet keeps its
 * counts when a screen is recomposed or reopened, and they follow the leader's voice: Kim's
 * numbers are always exactly 25 million and nobody ever replies to him
 * (`design/screens/the-cast-and-their-tweet-voices.html`), while Mi-León's replies run hot.
 */
object EngagementNumbers {
    /** Kim's single number, from the cast page. */
    const val SUPREME_NUMBER = 25_000_000L

    /** Counts for a tweet by [leaderId] identified by [seed]. */
    fun of(
        leaderId: LeaderId,
        seed: Int,
    ): Engagement {
        if (leaderId == LeaderRoster.kim.id) {
            return Engagement(replies = 0, retweets = SUPREME_NUMBER, likes = SUPREME_NUMBER, views = SUPREME_NUMBER)
        }
        val random = SplitMix64(seed.toLong() * SEED_MIX + leaderId.value.hashCode())
        val reach = BASE_REACH + random.nextInt(REACH_SPREAD)
        val replyRate = if (leaderId == LeaderRoster.mileon.id) HOT_REPLY_RATE else REPLY_RATE
        return Engagement(
            replies = reach.toLong() * replyRate / RATE_SCALE,
            retweets = reach.toLong() * RETWEET_RATE / RATE_SCALE,
            likes = reach.toLong() * LIKE_RATE / RATE_SCALE,
            views = reach.toLong() * VIEW_RATE,
        )
    }

    // Tuned so a typical tweet lands near the design's 12.4K replies and 9.9M views.
    private const val BASE_REACH = 100
    private const val REACH_SPREAD = 900
    private const val RATE_SCALE = 10
    private const val REPLY_RATE = 1_240
    private const val HOT_REPLY_RATE = 4_800
    private const val RETWEET_RATE = 4_810
    private const val LIKE_RATE = 21_100
    private const val VIEW_RATE = 99_000
    private const val SEED_MIX = 7_919L
}
