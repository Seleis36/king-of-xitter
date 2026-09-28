package com.kingofxitter.app.feedback

import androidx.annotation.RawRes
import com.kingofxitter.app.R
import com.kingofxitter.engine.model.GameEvent
import com.kingofxitter.engine.model.MatchResult
import com.kingofxitter.engine.model.PlayerId
import com.kingofxitter.engine.model.PlayerResource
import com.kingofxitter.engine.model.ResourceChangeReason

/** A sound and haptic moment. [haptic] is how hard the phone buzzes, if at all. */
enum class Cue(
    @param:RawRes val sound: Int,
    val haptic: Haptic,
) {
    /** Any roll or reroll. */
    Dice(R.raw.sfx_dice, Haptic.Tick),

    /** Your tweets land on someone else. */
    Tweet(R.raw.sfx_tweet, Haptic.None),

    /** Someone's tweets land on you. */
    Hit(R.raw.sfx_hit, Haptic.Heavy),

    /** You won. */
    Win(R.raw.sfx_win, Haptic.Confirm),

    /** The match ended and you didn't win. */
    Lose(R.raw.sfx_lose, Haptic.Reject),
}

/** Haptic strengths, mapped to platform feedback constants by [FeedbackPlayer]. */
enum class Haptic { None, Tick, Heavy, Confirm, Reject }

/** Cues for one engine event from the human's point of view. */
fun cuesFor(
    event: GameEvent,
    humanId: PlayerId,
    actingPlayerId: PlayerId? = null,
): List<Cue> =
    when (event) {
        is GameEvent.DiceRolled -> listOf(Cue.Dice)
        is GameEvent.ResourceChanged ->
            if (event.resource == PlayerResource.FOLLOWERS && event.amount < 0 && event.reason == ResourceChangeReason.TWEET) {
                when {
                    event.playerId == humanId -> listOf(Cue.Hit)
                    actingPlayerId == humanId -> listOf(Cue.Tweet)
                    else -> emptyList()
                }
            } else {
                emptyList()
            }
        is GameEvent.MatchEnded -> listOf(if ((event.result as? MatchResult.Victory)?.playerId == humanId) Cue.Win else Cue.Lose)
        else -> emptyList()
    }

/**
 * Drops a cue repeated within [windowMillis], so one resolution hitting three leaders plays one
 * tweet sound, not three stacked ones.
 */
class CueThrottle(
    private val windowMillis: Long = 250,
) {
    private val lastPlayed = mutableMapOf<Cue, Long>()

    /** True when [cue] should play at [nowMillis]; records it if so. */
    fun allow(
        cue: Cue,
        nowMillis: Long,
    ): Boolean {
        val last = lastPlayed[cue]
        if (last != null && nowMillis - last < windowMillis) return false
        lastPlayed[cue] = nowMillis
        return true
    }
}

/** Queues sounds requested before SoundPool finishes loading them. */
internal class SoundLoadQueue {
    private val loaded = mutableSetOf<Int>()
    private val failed = mutableSetOf<Int>()
    private val pending = mutableListOf<Int>()

    /** Returns true if [sampleId] is ready to play; failed loads are dropped. */
    fun request(sampleId: Int): Boolean =
        when {
            sampleId in loaded -> true
            sampleId in failed -> false
            else -> false.also { pending += sampleId }
        }

    /** Marks a load complete and returns queued requests to play on success. */
    fun complete(
        sampleId: Int,
        success: Boolean,
    ): List<Int> {
        if (success) loaded += sampleId else failed += sampleId
        val queued = pending.filter { it == sampleId }
        pending.removeAll { it == sampleId }
        return if (success) queued else emptyList()
    }

    /** Discards requests when the player is disposed. */
    fun clear() {
        pending.clear()
        loaded.clear()
        failed.clear()
    }
}
