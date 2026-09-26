package com.kingofxitter.app.ui.endgame

import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.MatchResult
import com.kingofxitter.engine.model.PlayerState

/** Display rank and participant; equal scores within the same outcome share a rank. */
data class Standing(
    val rank: Int,
    val player: PlayerState,
)

/** In progress: score order. Finished: winner first, then surviving players, then suspended players. */
fun standings(state: GameState): List<Standing> {
    val winner = (state.result as? MatchResult.Victory)?.playerId

    fun priority(player: PlayerState): Int =
        when {
            player.id == winner -> 0
            state.result != null && player.isSuspended -> 2
            else -> 1
        }
    val ordered = state.players.sortedWith(compareBy<PlayerState> { priority(it) }.thenByDescending { it.mandates })
    var previous: Pair<Int, Int>? = null
    var rank = 0
    return ordered.mapIndexed { index, player ->
        val key = priority(player) to player.mandates
        if (key != previous) rank = index + 1
        previous = key
        Standing(rank, player)
    }
}
