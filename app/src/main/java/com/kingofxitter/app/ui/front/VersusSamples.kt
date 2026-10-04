package com.kingofxitter.app.ui.front

import com.kingofxitter.content.LeaderRoster
import com.kingofxitter.engine.MatchSetup
import com.kingofxitter.engine.RollOffRound
import com.kingofxitter.engine.model.DiceFace
import com.kingofxitter.engine.model.PlayerId

/**
 * Line-ups for the Versus previews. They come from the engine's own `MatchSetup`, so a preview
 * can never show a roll-off the rules wouldn't produce.
 */
object VersusSamples {
    /** The design's line-up: Macaron is you, and Kim wins the roll-off outright. */
    val design: VersusLineUp = lineUp(seed = 7)

    /** A seed whose roll-off needs more than one round, which is what the reveal is for. */
    val tieBreak: VersusLineUp = (0L until 400L).map(::lineUp).firstOrNull { it.rollOff.size > 1 } ?: design

    private fun lineUp(seed: Long): VersusLineUp {
        val start = MatchSetup.create(LeaderRoster.macaron.id, LeaderRoster.ids, seed)
        return VersusLineUp(
            humanPlayerId = start.humanPlayerId,
            firstPlayerId = start.state.currentPlayerId,
            players = start.state.players,
            rollOff = start.rollOff,
            matchNumber = Math.floorMod(seed, 10_000),
        )
    }

    /** Named so the roll-off's shape is obvious when reading a preview. */
    internal fun tweetsIn(round: RollOffRound): Map<PlayerId, Int> =
        round.rolls.mapValues { (_, dice) -> dice.count { it == DiceFace.TWEET } }
}
