package com.kingofxitter.bots

import com.kingofxitter.engine.DefaultGameReducer
import com.kingofxitter.engine.GameReducer
import com.kingofxitter.engine.SplitMix64
import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.CardId
import com.kingofxitter.engine.model.GameEvent
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.HookRandom
import com.kingofxitter.engine.model.MatchResult
import com.kingofxitter.engine.model.PendingDecision
import com.kingofxitter.engine.model.PlayerId
import com.kingofxitter.engine.model.PlayerState
import kotlin.math.exp
import kotlin.math.ln1p
import kotlin.math.sqrt

/** How hard the Troll Farm thinks. Lower numbers are faster and weaker. */
data class TrollFarmSearch(
    /** Playouts per candidate move. */
    val playouts: Int = 12,
    /** Actions a playout plays before it stops and scores the position. */
    val depth: Int = 12,
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
 * For each candidate move it plays [TrollFarmSearch.playouts] continuations
 * ([TrollFarmSearch.depth] actions deep, or to the end of the match) and keeps the move with the
 * best average position. Every seat in a continuation plays [ReplyGuyBot]'s heuristic, which is a
 * far better model of the table than random moves and costs about as little per step.
 *
 * The search never sees the real dice. The reducer rolls from `GameState.randomState`, so each
 * playout replaces it with dice of its own. Those dice come from the playout's seed plus the turn
 * and roll being played, and every candidate is measured against the same seeds, so two moves
 * are compared on the same luck rather than on whichever got the better rolls.
 *
 * Everything is driven by the stream it is given, so a seed replays a whole match, including the
 * search. The heuristic's own move gets a head start of [HEURISTIC_PRIOR], so the search only
 * overrules Reply Guy when the playouts clearly disagree with it, rather than on noise.
 *
 * [reducer] must be the match's own: the search only finds moves the reducer accepts, so with a
 * reducer that has no shop it never buys a power-up.
 */
class TrollFarmBot(
    private val market: CardMarket = CardMarket.Unknown,
    private val weights: ReplyGuyWeights = ReplyGuyWeights(),
    private val search: TrollFarmSearch = TrollFarmSearch(),
    private val reducer: GameReducer = DefaultGameReducer(),
) : BotBrain {
    override val difficulty = BotDifficulty.TROLL_FARM

    private val heuristic = ReplyGuyBot(market, weights)

    /** Plays every seat in a playout. Plain weights, because it models the whole table. */
    private val rollout = ReplyGuyBot(market)

    /** The head start, in percentage points of win chance, shrinking as playouts cut the noise. */
    private val prior = HEURISTIC_PRIOR / sqrt(search.playouts.toDouble())

    override fun decide(
        state: GameState,
        decision: PendingDecision?,
        random: HookRandom,
    ): Action? {
        val actor = BotMoves.actingPlayerId(state) ?: return null
        val moves = candidates(state, decision, random)
        if (moves.size <= 1) return moves.firstOrNull() ?: heuristic.decide(state, decision, random)
        val fallback = heuristic.decide(state, decision, random)
        // One seed for every candidate: common random numbers, so the moves face the same dice.
        val seed = random.nextInt(SEED_SPACE).toLong()
        var best: Action? = null
        var bestScore = Double.NEGATIVE_INFINITY
        for (move in moves) {
            val score = evaluate(state, move, actor, seed) + if (move == fallback) prior else 0.0
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

    /** Average score of [move] for [actor] over several continuations. */
    private fun evaluate(
        state: GameState,
        move: Action,
        actor: PlayerId,
        seed: Long,
    ): Double {
        var total = 0.0
        repeat(search.playouts) { playout ->
            val luck = SplitMix64(seed * PLAYOUT_MIX + playout).let { it.nextInt(Int.MAX_VALUE).toLong() * PLAYOUT_MIX + playout }
            val opening = apply(sampleDice(state, luck), move) ?: return Double.NEGATIVE_INFINITY
            total += playOut(opening, actor, luck)
        }
        return total / search.playouts
    }

    private fun playOut(
        opening: GameState,
        actor: PlayerId,
        luck: Long,
    ): Double {
        val random = SplitMix64(luck)
        var current = opening
        var steps = 0
        while (current.result == null && steps++ < search.depth) {
            val action = rollout.decide(current, current.pendingDecisions.firstOrNull(), random) ?: break
            val next = if (action is Action.RollDice) sampleDice(current, luck) else current
            current = apply(next, action) ?: break
        }
        return score(current, actor)
    }

    /**
     * [state] with dice drawn from [luck] and the turn and roll it is at, never from the real
     * `randomState`. Keying on the turn and roll gives every candidate the same dice for the same
     * roll, however many draws the moves before it used.
     */
    private fun sampleDice(
        state: GameState,
        luck: Long,
    ): GameState {
        val dice = SplitMix64(luck + state.turnNumber * TURN_MIX + state.rollNumber * ROLL_MIX)
        dice.nextInt(Int.MAX_VALUE)
        return state.copy(randomState = dice.state)
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
     * How good [state] is for [actor], as a percentage chance of winning: [WIN] for a won match,
     * [LOSS] for a lost one or a suspended leader, and otherwise [actor]'s share of the table.
     *
     * Each living leader gets a strength and the shares are a softmax over them, so a rival's
     * gain is the bot's loss. The weights were fitted to 6,000 Reply Guy matches: mandates count
     * more the closer they get to twenty, followers count most when they are running out, and the
     * next leader to roll is slightly ahead. Characters and policies are priced as before, with a
     * policy above the characters it cost because it keeps paying out after the playout ends.
     * The bot's personality scales its own appetites, and [ReplyGuyWeights.aggression] how much
     * it fears a rival's mandates.
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
        val roller = state.players.indexOfFirst { it.id == state.currentPlayerId }.coerceAtLeast(0)
        var mine = 0.0
        var table = 0.0
        state.players.forEachIndexed { seat, player ->
            if (player.isSuspended) return@forEachIndexed
            val seatsUntilTurn = Math.floorMod(seat - roller, state.players.size)
            val share = exp(strength(state, player, seatsUntilTurn, isMe = player.id == actor))
            table += share
            if (player.id == actor) mine = share
        }
        return WIN * mine / table
    }

    private fun strength(
        state: GameState,
        player: PlayerState,
        seatsUntilTurn: Int,
        isMe: Boolean,
    ): Double {
        val mandates = player.mandates * MANDATE_WEIGHT + player.mandates * player.mandates * MANDATE_CURVE
        val followers = ln1p(player.followers.toDouble()) * FOLLOWER_WEIGHT
        val characters = player.characters * CHARACTER_WEIGHT
        val policies = player.policyIds.sumOf(::policyWorth)
        val onXitter = if (state.xitterPlayerId == player.id) XITTER_WEIGHT else 0.0
        val common = policies + onXitter - seatsUntilTurn * SEAT_WEIGHT
        return if (isMe) {
            common + mandates * weights.mandateHunger + followers * weights.selfPreservation + characters * weights.shopAppetite
        } else {
            common + mandates * weights.aggression + followers + characters
        }
    }

    /** A policy at [POLICY_PREMIUM] times its price, or a flat [POLICY_WEIGHT] when the market can't price it. */
    private fun policyWorth(cardId: CardId): Double =
        market.appraise(cardId)?.let { it.cost * CHARACTER_WEIGHT * POLICY_PREMIUM } ?: POLICY_WEIGHT

    private companion object {
        const val WIN = 100.0
        const val LOSS = 0.0

        // Strength weights, fitted as a conditional logit on 6,000 four-seat Reply Guy matches.
        const val MANDATE_WEIGHT = 0.1215

        /** 0.1509 per mandate squared over twenty: the last mandates before twenty count most. */
        const val MANDATE_CURVE = 0.1509 / 20

        /** Per ln(1 + followers): the first follower lost hurts least, the last one most. */
        const val FOLLOWER_WEIGHT = 1.4538

        /** Per seat between a leader and the next roll. */
        const val SEAT_WEIGHT = 0.1975

        /**
         * The fit puts Xitter slightly negative for Reply Guy, who stays on it too long; the +2
         * mandates it pays already show up in the playouts, so this only breaks ties in its favour.
         */
        const val XITTER_WEIGHT = 0.05

        /** Characters are only worth what they buy, so a small weight; no shop in the fit. */
        const val CHARACTER_WEIGHT = 0.005

        /** An unpriced policy: about a third of a mandate, as before the refit. */
        const val POLICY_WEIGHT = 0.08

        /** Above the hungriest leader's character appetite (1.3), so holding cash never beats a policy. */
        const val POLICY_PREMIUM = 1.5

        /**
         * The heuristic's head start at one playout, in percentage points of win chance; divided
         * by the square root of the playouts, like the noise it guards against. Measured: 7 points
         * at two playouts and 3 at twelve won the most matches.
         */
        const val HEURISTIC_PRIOR = 10.0
        const val PLAYOUT_MIX = 0x9E3779B1L
        const val TURN_MIX = -0x61c8864680b583ebL
        const val ROLL_MIX = -0x40a7b892e31b1a47L
        const val SEED_SPACE = 1 shl 20
    }
}
