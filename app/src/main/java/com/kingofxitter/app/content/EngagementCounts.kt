package com.kingofxitter.app.content

import com.kingofxitter.content.Engagement
import com.kingofxitter.content.EngagementNumbers
import com.kingofxitter.engine.model.LeaderId
import java.util.Locale

/** V2's counts for a tweet by [leaderId] identified by [seed], as compact display strings. */
fun engagementCounts(
    leaderId: LeaderId,
    seed: Int,
): List<String> = EngagementNumbers.of(leaderId, seed).let(::compactCounts)

/** Replies, retweets, likes and views in the order the design shows them under a tweet. */
fun compactCounts(engagement: Engagement): List<String> =
    listOf(engagement.replies, engagement.retweets, engagement.likes, engagement.views).map(::compactCount)

/** 12400 → "12.4K", 9900000 → "9.9M". Locale-independent, like the design's own numbers. */
fun compactCount(value: Long): String =
    when {
        value >= 1_000_000 -> "%.1fM".format(Locale.ROOT, value / 1_000_000.0)
        value >= 1_000 -> "%.1fK".format(Locale.ROOT, value / 1_000.0)
        else -> value.toString()
    }
