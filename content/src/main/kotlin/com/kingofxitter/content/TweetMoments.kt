package com.kingofxitter.content

import com.kingofxitter.engine.model.GameEvent
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.MatchResult
import com.kingofxitter.engine.model.PlayerId
import com.kingofxitter.engine.model.PlayerResource
import com.kingofxitter.engine.model.ResourceChangeReason

/** One leader's reason to tweet: who posts, and what they are reacting to. */
data class TweetTrigger(
    val playerId: PlayerId,
    val event: TweetEvent,
)

/**
 * Turns the engine's events into the moments the voices react to, so the board, the timeline and
 * the end screens all read the same match the same way.
 *
 * Only the first roll of a turn becomes a [TweetMoment.ROLLED], because rerolls are the same
 * leader talking about the same turn. A tweet that lands produces two triggers, the attacker's
 * and the victim's, which is how the design's timeline shows a beef.
 */
object TweetMoments {
    /** The triggers [event] causes, read against the state it produced. Empty for quiet events. */
    fun of(
        event: GameEvent,
        state: GameState,
    ): List<TweetTrigger> =
        when (event) {
            is GameEvent.DiceRolled ->
                if (event.rollNumber == 1) listOf(TweetTrigger(event.playerId, TweetEvent(TweetMoment.ROLLED))) else emptyList()

            is GameEvent.ResourceChanged ->
                if (event.resource == PlayerResource.FOLLOWERS &&
                    event.reason == ResourceChangeReason.TWEET &&
                    event.amount < 0 &&
                    event.playerId != state.currentPlayerId
                ) {
                    listOf(
                        TweetTrigger(state.currentPlayerId, TweetEvent(TweetMoment.ATTACKED, leaderOf(state, event.playerId))),
                        TweetTrigger(event.playerId, TweetEvent(TweetMoment.GOT_HIT, leaderOf(state, state.currentPlayerId))),
                    )
                } else {
                    emptyList()
                }

            is GameEvent.MainCharacterChanged ->
                buildList {
                    event.newPlayerId?.let {
                        add(TweetTrigger(it, TweetEvent(TweetMoment.ENTERED_XITTER, leaderOf(state, event.previousPlayerId))))
                    }
                    event.previousPlayerId?.let {
                        add(TweetTrigger(it, TweetEvent(TweetMoment.LEFT_XITTER, leaderOf(state, event.newPlayerId))))
                    }
                }

            is GameEvent.PowerUpBought -> listOf(TweetTrigger(event.playerId, TweetEvent(TweetMoment.BOUGHT_POWER_UP)))

            is GameEvent.PlayerSuspended -> listOf(TweetTrigger(event.playerId, TweetEvent(TweetMoment.SUSPENDED)))

            is GameEvent.MatchEnded -> endOfMatch(event.result, state)

            else -> emptyList()
        }

    private fun endOfMatch(
        result: MatchResult,
        state: GameState,
    ): List<TweetTrigger> {
        val winner = (result as? MatchResult.Victory)?.playerId
        return state.players.mapNotNull { player ->
            when {
                player.id == winner -> TweetTrigger(player.id, TweetEvent(TweetMoment.WON))
                // A suspended leader has already said its piece with TweetMoment.SUSPENDED.
                player.isSuspended -> null
                else -> TweetTrigger(player.id, TweetEvent(TweetMoment.LOST, leaderOf(state, winner)))
            }
        }
    }

    private fun leaderOf(
        state: GameState,
        playerId: PlayerId?,
    ) = playerId?.let { id -> state.players.firstOrNull { it.id == id }?.leaderId }
}
