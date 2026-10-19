package com.kingofxitter.app.ui.endgame

import com.kingofxitter.app.content.VoiceEntry
import com.kingofxitter.content.TweetMoment
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.MatchResult
import com.kingofxitter.engine.model.PlayerId

/** Which of the match's V1 tweets the end screens quote. */
object EndTweets {
    /** Victory's sore loser: the best-placed loser's "I did NOT lose" line. */
    fun reaction(
        entries: List<VoiceEntry>,
        state: GameState,
    ): VoiceEntry? {
        val winner = (state.result as? MatchResult.Victory)?.playerId ?: return null
        val order = standings(state).map { it.player.id }
        return entries
            .filter { it.moment == TweetMoment.LOST && it.playerId != winner && it.playerId in order }
            .minByOrNull { order.indexOf(it.playerId) }
    }

    /**
     * Defeat's quoted tweet: the last one that hit you when you were suspended, otherwise the
     * winner's victory line. Null when there is no such tweet (a power-up suspended you, say).
     */
    fun finalTweet(
        entries: List<VoiceEntry>,
        state: GameState,
        humanId: PlayerId,
    ): VoiceEntry? {
        val you = state.players.firstOrNull { it.id == humanId } ?: return null
        if (you.isSuspended) return entries.lastOrNull { it.moment == TweetMoment.ATTACKED && it.targetId == humanId }
        val winner = (state.result as? MatchResult.Victory)?.playerId ?: return null
        return entries.lastOrNull { it.moment == TweetMoment.WON && it.playerId == winner }
    }
}
