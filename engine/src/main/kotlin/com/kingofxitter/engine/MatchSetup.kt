package com.kingofxitter.engine

import com.kingofxitter.engine.model.DiceFace
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.LeaderId
import com.kingofxitter.engine.model.PlayerId
import com.kingofxitter.engine.model.PlayerState
import com.kingofxitter.engine.model.TurnPhase

/** One visible roll-off round. Tied leaders alone participate in subsequent rounds. */
data class RollOffRound(
    val rolls: Map<PlayerId, List<DiceFace>>,
)

/** Match ready to play, plus the dice history needed by the Versus screen. */
data class MatchStart(
    val state: GameState,
    val humanPlayerId: PlayerId,
    val rollOff: List<RollOffRound>,
)

/** Creates four distinct leaders and breaks first-player ties using the seeded engine stream. */
object MatchSetup {
    /** The content track supplies its six leader IDs; no portraits or bot code are needed here. */
    fun create(
        humanLeader: LeaderId,
        roster: List<LeaderId>,
        seed: Long,
    ): MatchStart {
        require(roster.size == 6 && roster.distinct().size == 6) { "Supply the six distinct leaders" }
        require(humanLeader in roster)
        val random = SplitMix64(seed)
        val others = roster.filterNot { it == humanLeader }.toMutableList()
        for (index in others.lastIndex downTo 1) {
            val other = random.nextInt(index + 1)
            val temporary = others[index]
            others[index] = others[other]
            others[other] = temporary
        }
        val players = (listOf(humanLeader) + others.take(3)).map { leader -> PlayerState(PlayerId(leader.value), leader) }
        var candidates = players.map { it.id }
        val history = mutableListOf<RollOffRound>()
        while (candidates.size > 1) {
            val rolls = candidates.associateWith { List(6) { DiceFace.entries[random.nextInt(DiceFace.entries.size)] } }
            history += RollOffRound(rolls)
            val highest = rolls.values.maxOf { dice -> dice.count { it == DiceFace.TWEET } }
            candidates = candidates.filter { id -> rolls.getValue(id).count { it == DiceFace.TWEET } == highest }
        }
        val state = GameState(players, candidates.single(), TurnPhase.TURN_START, turnNumber = 1, randomState = random.state)
        return MatchStart(state, players.first().id, history)
    }
}
