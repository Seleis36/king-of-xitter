package com.kingofxitter.content

import com.kingofxitter.engine.model.LeaderId

/**
 * V2's attack lines for the compose screen: three per attacker and target, written in the
 * attacker's voice about that particular target, plus a set for tweeting at the whole timeline
 * as Main Character.
 *
 * Keys follow `zinger_<attacker>_<target>_<index>`, with `everyone` standing in for the target
 * when the tweet hits everybody. As with [TweetVoices], the text lives in the app's resources.
 */
object ZingerBook {
    /** Lines written for each attacker and target pair. */
    const val PER_PAIR = 3

    /** The target segment used when a tweet hits the whole timeline. */
    const val EVERYONE = "everyone"

    /**
     * [attacker]'s lines against [target], or its lines for the whole timeline when [target] is
     * null. Empty when either leader is outside the roster, or when they are the same leader:
     * nobody tweets at themselves.
     */
    fun keys(
        attacker: LeaderId,
        target: LeaderId?,
    ): List<String> {
        val from = suffix(attacker) ?: return emptyList()
        val at = if (target == null) EVERYONE else suffix(target) ?: return emptyList()
        if (at == from) return emptyList()
        return (0 until PER_PAIR).map { index -> "zinger_${from}_${at}_$index" }
    }

    /** Every key the book defines, for the resource test that proves none is missing. */
    val all: List<String> =
        LeaderRoster.all.flatMap { attacker ->
            (LeaderRoster.all.map { it.id } + null).flatMap { target -> keys(attacker.id, target) }
        }

    private fun suffix(leaderId: LeaderId) = LeaderRoster.byId(leaderId)?.shortNameKey?.removePrefix("leader_short_")
}
