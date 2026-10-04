package com.kingofxitter.app.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.kingofxitter.content.TweetEvent
import com.kingofxitter.content.TweetMoment
import com.kingofxitter.content.TweetMoments
import com.kingofxitter.engine.GameController
import com.kingofxitter.engine.SplitMix64
import com.kingofxitter.engine.model.CardId
import com.kingofxitter.engine.model.GameEvent
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.HookRandom
import com.kingofxitter.engine.model.LeaderId
import com.kingofxitter.engine.model.PlayerId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One tweet that was posted during the match, newest last. */
data class VoiceEntry(
    val playerId: PlayerId,
    val leaderId: LeaderId,
    val moment: TweetMoment,
    /** The leader the line is aimed at, when it has one. */
    val targetId: PlayerId?,
    val text: String,
    val turnNumber: Int,
    /** Followers the tweet cost, for the timeline's outcome tag. Zero when the moment has none. */
    val unfollows: Int = 0,
    /** The power-up that was bought, for [TweetMoment.BOUGHT_POWER_UP]. */
    val cardId: CardId? = null,
)

/**
 * The match as its leaders tell it. It turns the engine's events into V1 lines once, keeps them
 * in order for the timeline, and remembers each leader's latest line for the board's stage.
 *
 * A line is generated from a stream seeded by the position that caused it, so re-reading the log
 * never changes it, and the same match always reads the same way.
 *
 * [LIMIT] caps the log: a long match can run thousands of turns, and the timeline only ever
 * shows the recent end of it.
 */
class VoiceLog(
    private val write: (TweetEvent, LeaderId, HookRandom) -> String,
) {
    private val mutableEntries = MutableStateFlow<List<VoiceEntry>>(emptyList())
    private var sequence = 0L

    /** Every tweet so far, oldest first. */
    val entries: StateFlow<List<VoiceEntry>> = mutableEntries.asStateFlow()

    /** The newest line by [playerId], or null when that leader hasn't posted yet. */
    fun latest(playerId: PlayerId?): VoiceEntry? = playerId?.let { id -> mutableEntries.value.lastOrNull { it.playerId == id } }

    /** Records whatever [event] makes the leaders say, against the state it produced. */
    fun record(
        event: GameEvent,
        state: GameState,
    ) {
        val triggers = TweetMoments.of(event, state)
        if (triggers.isEmpty()) return
        val unfollows = (event as? GameEvent.ResourceChanged)?.takeIf { it.amount < 0 }?.let { -it.amount } ?: 0
        val cardId = (event as? GameEvent.PowerUpBought)?.cardId
        val added =
            triggers.mapNotNull { trigger ->
                val player = state.players.firstOrNull { it.id == trigger.playerId } ?: return@mapNotNull null
                entry(player.id, player.leaderId, trigger.event, state, unfollows, cardId)
            }
        if (added.isEmpty()) return
        mutableEntries.value = (mutableEntries.value + added).takeLast(LIMIT)
    }

    private fun entry(
        playerId: PlayerId,
        leaderId: LeaderId,
        event: TweetEvent,
        state: GameState,
        unfollows: Int,
        cardId: CardId?,
    ): VoiceEntry? {
        // The seed mixes the position and a counter, so two lines in the same batch still differ.
        val random = SplitMix64(state.randomState + state.turnNumber * TURN_MIX + sequence++)
        val text = write(event, leaderId, random)
        if (text.isEmpty()) return null
        return VoiceEntry(
            playerId = playerId,
            leaderId = leaderId,
            moment = event.moment,
            targetId = event.target?.let { target -> state.players.firstOrNull { it.leaderId == target }?.id },
            text = text,
            turnNumber = state.turnNumber,
            unfollows = unfollows,
            cardId = cardId,
        )
    }

    private companion object {
        const val LIMIT = 200
        const val TURN_MIX = 0x632BE59BD9B4E019L
    }
}

/**
 * A [VoiceLog] fed by [controller]'s events for as long as the caller is composed. Hoist it above
 * navigation, next to the effect queue, so no screen misses a tweet.
 */
@Composable
fun rememberVoiceLog(controller: GameController): VoiceLog {
    val texts = rememberTweetTexts()
    val log = remember(controller, texts) { VoiceLog(texts::write) }
    LaunchedEffect(controller, log) {
        controller.events.collect { event -> log.record(event, controller.state.value) }
    }
    return log
}
