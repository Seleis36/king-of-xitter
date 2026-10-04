package com.kingofxitter.content

import com.kingofxitter.engine.model.HookRandom
import com.kingofxitter.engine.model.LeaderId

/** The match moments every leader has a line for (ROADMAP §5, V1). */
enum class TweetMoment {
    /** Dice landed, nothing resolved yet. */
    ROLLED,

    /** The leader tweeted at someone. */
    ATTACKED,

    /** The leader lost followers to someone else's tweet. */
    GOT_HIT,

    /** The leader became Main Character. */
    ENTERED_XITTER,

    /** The leader gave up Xitter. */
    LEFT_XITTER,

    /** The leader bought a power-up. */
    BOUGHT_POWER_UP,

    /** The leader's account was suspended. */
    SUSPENDED,

    /** The leader won the match. */
    WON,

    /** The leader lost the match while still standing. */
    LOST,

    /** The leader came back through Burner Account (card #23). */
    BURNER,
}

/**
 * What the writer is reacting to. [target] is the other leader involved, when the moment has one
 * (who was tweeted at, who took Xitter); the voice may mention them.
 */
data class TweetEvent(
    val moment: TweetMoment,
    val target: LeaderId? = null,
)

/**
 * One generated tweet. [textKey] is a `strings.xml` key; [mentions] fill the template's
 * positional arguments in order, as the handles of those leaders. The module is pure Kotlin, so
 * it never carries display text.
 */
data class Tweet(
    val leaderId: LeaderId,
    val textKey: String,
    val mentions: List<LeaderId> = emptyList(),
)

/**
 * Writes a leader's tweet for a match moment (ROADMAP §3). Implementations are pure: the same
 * [event], [leaderId] and random draw always give the same [Tweet], so the timeline, the board
 * and tests agree.
 */
fun interface TweetWriter {
    /** Picks a line for [leaderId] reacting to [event], drawing from [random] to vary it. */
    fun write(
        event: TweetEvent,
        leaderId: LeaderId,
        random: HookRandom,
    ): Tweet
}
