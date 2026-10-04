package com.kingofxitter.content

import com.kingofxitter.engine.model.LeaderId

/**
 * The trending hashtags from `design/screens/title-screen.html`: one per leader, plus two that
 * belong to nobody. The title screen's marquee scrolls them; the timeline can tag a match with
 * the Main Character's.
 */
object TrendingTags {
    /** Tags that belong to no leader. */
    val extras: List<String> = listOf("trending_tag_extra_0", "trending_tag_extra_1")

    /** [leaderId]'s own tag, or null for a leader outside the roster. */
    fun of(leaderId: LeaderId): String? = LeaderRoster.byId(leaderId)?.let { "trending_tag_${suffix(it)}" }

    /** Every tag, leaders first, in the order the design's marquee lists them. */
    val all: List<String> = LeaderRoster.all.mapNotNull { of(it.id) } + extras

    private fun suffix(leader: Leader) = leader.shortNameKey.removePrefix("leader_short_")
}
