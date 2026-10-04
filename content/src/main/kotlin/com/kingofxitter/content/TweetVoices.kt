package com.kingofxitter.content

import com.kingofxitter.engine.model.HookRandom
import com.kingofxitter.engine.model.LeaderId

/**
 * V1's line bank: the `strings.xml` keys of every leader's lines, one set per [TweetMoment].
 *
 * Keys follow `tweet_<leader>_<moment>_<index>`, where `<leader>` is the suffix the roster
 * already uses for its name keys, so nothing has to be listed twice. The text itself lives in
 * `app/src/main/res/values/strings_voices.xml`, which keeps the module free of display strings
 * and leaves the voices translatable (ROADMAP §7).
 */
object TweetVoices {
    /** Lines written for every leader and moment. */
    const val LINES_PER_MOMENT = 5

    /** The keys for [leaderId] reacting to [moment], or empty for a leader outside the roster. */
    fun keys(
        leaderId: LeaderId,
        moment: TweetMoment,
    ): List<String> {
        val leader = LeaderRoster.byId(leaderId) ?: return emptyList()
        val suffix = leader.shortNameKey.removePrefix(SHORT_NAME_PREFIX)
        val moments = moment.name.lowercase()
        return (0 until LINES_PER_MOMENT).map { index -> "tweet_${suffix}_${moments}_$index" }
    }

    /** Every key the bank defines, for the resource test that proves none is missing. */
    val all: List<String> =
        LeaderRoster.all.flatMap { leader -> TweetMoment.entries.flatMap { moment -> keys(leader.id, moment) } }

    private const val SHORT_NAME_PREFIX = "leader_short_"
}

/**
 * The default [TweetWriter]: it picks one of [TweetVoices]' lines for the moment and names the
 * leader involved, if any. It holds no state, so the board, the timeline and the tests all
 * generate the same tweet for the same position.
 *
 * A leader outside the roster writes nothing, which is what fixtures with made-up IDs need.
 */
class TemplateTweetWriter : TweetWriter {
    override fun write(
        event: TweetEvent,
        leaderId: LeaderId,
        random: HookRandom,
    ): Tweet {
        val keys = TweetVoices.keys(leaderId, event.moment)
        val key = if (keys.isEmpty()) "" else keys[random.nextInt(keys.size)]
        return Tweet(leaderId, key, listOfNotNull(event.target))
    }
}
