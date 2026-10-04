package com.kingofxitter.bots

import com.kingofxitter.engine.DefaultGameReducer
import com.kingofxitter.engine.GameReducer
import com.kingofxitter.engine.SplitMix64
import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.GameEvent
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.HookRandom
import com.kingofxitter.engine.model.MatchResult
import com.kingofxitter.engine.model.PendingDecision
import com.kingofxitter.engine.model.PlayerId

/** How hard the Troll Farm thinks. Lower numbers are faster and weaker. */
data class TrollFarmSearch(
    /** Random playouts per candidate move. */
    val playouts: Int = 6,
    /** Actions a playout plays before it stops and scores the position. */
    val depth: Int = 48,
    /** Candidate moves considered per decision; more than this are sampled. */
    val candidates: Int = 10,
) {
    init {
        require(playouts > 0 && depth > 0 && candidates > 0)
    }
}

/**
 * B3: the hardest opponent. It uses the engine itself as the simulator, which is possible because
 * the reducer is pure: cloning a position is just keeping the `GameState` it was handed.
 *
 * For each candidate move it plays [TrollFarmSearch.playouts] random continuations
 * ([TrollFarmSearch.depth] actions deep, or to the end of the match) and keeps the move with the
 * best average position. Continuations are played by the intern, which is the standard cheap
 * rollout policy: it is unbiased and costs almost nothing per step.
 *
 * Everything is driven by the stream it is given, so a seed replays a whole match, including the
 * search. When the search can't separate two moves it falls back to [ReplyGuyBot]'s heuristic,
 * so it is never worse than the difficulty below it.
 */
class TrollFarmBot(
    private val market: CardMarket = CardMarket.Unknown,
    private val weights: ReplyGuyWeights = ReplyGuyWeights(),
    private val search: TrollFarmSearch = TrollFarmSearch(),
    private val reducer: GameReducer = DefaultGameReducer(),
) : BotBrain {
    override val difficulty = BotDifficulty.TROLL_FARM

    private val heuristic = ReplyGuyBot(market, weights)
    private val rollout = InternBot(market)

    override fun decide(
        state: GameState,
        decision: PendingDecision?,
        random: HookRandom,
    ): Action? {
        val actor = BotMoves.actingPlayerId(state) ?: return null
        val moves = candidates(state, decision, random)
        if (moves.size <= 1) return moves.firstOrNull() ?: heuristic.decide(state, decision, random)
        val fallback = heuristic.decide(state, decision, random)
        var best: Action? = null
        var bestScore = Double.NEGATIVE_INFINITY
        moves.forEachIndexed { index, move ->
            val score = evaluate(state, move, actor, random.nextInt(SEED_SPACE).toLong() + index)
            // A tie keeps the heuristic's own move, so search never makes it play worse.
            if (score > bestScore || (score == bestScore && move == fallback)) {
                bestScore = score
                best = move
            }
        }
        return best ?: fallback
    }

    /** The moves worth searching: all of them, or a sample plus the heuristic's own choice. */
    private fun candidates(
        state: GameState,
        decision: PendingDecision?,
        random: HookRandom,
    ): List<Action> {
        val legal = BotMoves.legalActions(state, decision, market)
        if (legal.size <= search.candidates) return legal
        val preferred = heuristic.decide(state, decision, random)
        val sampled = LinkedHashSet<Action>()
        preferred?.takeIf { it in legal }?.let(sampled::add)
        // Sampling without replacement keeps the candidate list varied across turns.
        while (sampled.size < search.candidates) sampled.add(legal[random.nextInt(legal.size)])
        return sampled.toList()
    }

    /** Average score of [move] for [actor] over several random continuations. */
    private fun evaluate(
        state: GameState,
        move: Action,
        actor: PlayerId,
        seed: Long,
    ): Double {
        val opening = apply(state, move) ?: return Double.NEGATIVE_INFINITY
        var total = 0.0
        repeat(search.playouts) { playout ->
            total += playOut(opening, actor, SplitMix64(seed * PLAYOUT_MIX + playout))
        }
        return total / search.playouts
    }

    private fun playOut(
        opening: GameState,
        actor: PlayerId,
        random: HookRandom,
    ): Double {
        var current = opening
        var steps = 0
        while (current.result == null && steps++ < search.depth) {
            val action = rollout.decide(current, current.pendingDecisions.firstOrNull(), random) ?: break
            current = apply(current, action) ?: break
        }
        return score(current, actor)
    }

    private fun apply(
        state: GameState,
        action: Action,
    ): GameState? {
        val transition =
            try {
                reducer.reduce(state, action)
            } catch (error: Exception) {
                return null
            }
        if (transition.events.any { it is GameEvent.ActionRejected } || transition.state == state) return null
        return transition.state
    }

    /**
     * How good [state] is for [actor]. Mandates are the win condition, so they dominate;
     * followers keep it alive and characters buy power-ups later. Rivals' mandates count against
     * it, because the match ends when any of them reaches twenty.
     */
    internal fun score(
        state: GameState,
        actor: PlayerId,
    ): Double {
        val me = state.players.firstOrNull { it.id == actor } ?: return LOSS
        when (val result = state.result) {
            is MatchResult.Victory -> return if (result.playerId == actor) WIN else LOSS
            MatchResult.NoWinner -> return LOSS
            null -> Unit
        }
        if (me.isSuspended) return LOSS
        val rivals = state.players.filter { it.id != actor && !it.isSuspended }
        val leadingRival = rivals.maxOfOrNull { it.mandates } ?: 0
        val onXitter = if (state.xitterPlayerId == actor) XITTER_BONUS else 0.0
        return me.mandates * MANDATE_WEIGHT * weights.mandateHunger +
            me.followers * FOLLOWER_WEIGHT * weights.selfPreservation +
            me.characters * CHARACTER_WEIGHT * weights.shopAppetite +
            me.policyIds.size * POLICY_WEIGHT +
            onXitter -
            leadingRival * RIVAL_WEIGHT * weights.aggression -
            rivals.size * SURVIVOR_WEIGHT
    }

    private companion object {
        const val WIN = 1_000.0
        const val LOSS = -1_000.0
        const val MANDATE_WEIGHT = 10.0
        const val FOLLOWER_WEIGHT = 4.0
        const val CHARACTER_WEIGHT = 0.3
        const val POLICY_WEIGHT = 3.0
        const val RIVAL_WEIGHT = 6.0
        const val SURVIVOR_WEIGHT = 2.0
        const val XITTER_BONUS = 6.0
        const val PLAYOUT_MIX = 0x9E3779B1L
        const val SEED_SPACE = 1 shl 20
    }
}
