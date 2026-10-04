package com.kingofxitter.bots

import com.kingofxitter.engine.DefaultGameReducer
import com.kingofxitter.engine.GameReducer
import com.kingofxitter.engine.MatchSetup
import com.kingofxitter.engine.model.GameEvent
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.LeaderId
import com.kingofxitter.engine.model.MatchResult
import com.kingofxitter.engine.model.PlayerId

/**
 * The six leader IDs `:content`'s roster publishes. `:bots` must not depend on `:content`
 * (ROADMAP §3), so the simulator names them instead; `--leaders` overrides the list.
 */
val DEFAULT_LEADERS: List<LeaderId> = listOf("trumpet", "poutine", "kim", "macaron", "xi", "mi-leon").map(::LeaderId)

/** What one simulation run should play. */
data class SimulationConfig(
    val games: Int = 1_000,
    val seed: Long = 1L,
    /** One brain per seat. Four seats play, so the list is cycled if it is shorter. */
    val difficulties: List<BotDifficulty> = BotDifficulty.entries,
    val leaders: List<LeaderId> = DEFAULT_LEADERS,
    val market: CardMarket = CardMarket.Unknown,
    /** A match that passes this many actions is reported as stuck rather than run forever. */
    val actionLimit: Int = 20_000,
) {
    init {
        require(games > 0 && actionLimit > 0)
        require(difficulties.isNotEmpty())
        require(leaders.size == 6 && leaders.distinct().size == 6) { "Supply the six distinct leaders" }
    }
}

/** A match that could not be completed, with the seed that reproduces it. */
data class SimulationFailure(
    val seed: Long,
    val reason: String,
)

/** What a run found. Rates are counts, so the caller decides how to present them. */
data class SimulationReport(
    val games: Int,
    val winsByDifficulty: Map<BotDifficulty, Int>,
    val seatsByDifficulty: Map<BotDifficulty, Int>,
    val winsByLeader: Map<LeaderId, Int>,
    val seatsByLeader: Map<LeaderId, Int>,
    val noWinner: Int,
    val totalTurns: Long,
    val failures: List<SimulationFailure>,
) {
    /** Share of the matches a difficulty was seated in that it won. */
    fun winRate(difficulty: BotDifficulty): Double {
        val seats = seatsByDifficulty[difficulty] ?: 0
        return if (seats == 0) 0.0 else (winsByDifficulty[difficulty] ?: 0).toDouble() / seats
    }

    /** Share of the matches a leader was drawn into that it won. */
    fun winRate(leader: LeaderId): Double {
        val seats = seatsByLeader[leader] ?: 0
        return if (seats == 0) 0.0 else (winsByLeader[leader] ?: 0).toDouble() / seats
    }

    /** Average turns per completed match. */
    val averageTurns: Double
        get() = if (games == 0) 0.0 else totalTurns.toDouble() / games
}

/**
 * B4: plays whole matches head-to-head with no UI, which is how Track B balances cards and how
 * engine bugs that only appear after thousands of turns get found.
 *
 * Every match is driven by a seed, so a failure is reproducible from its report line alone. A
 * brain that throws, returns an illegal action, or stops making progress is recorded as a
 * failure instead of ending the run, so one bad seed can't hide the other 9,999.
 */
object Simulator {
    /** Plays [config]'s matches and returns what happened. */
    fun run(
        config: SimulationConfig,
        reducer: () -> GameReducer = { DefaultGameReducer() },
    ): SimulationReport {
        val brains = config.difficulties.map { it to brainFor(it, config.market) }
        val winsByDifficulty = mutableMapOf<BotDifficulty, Int>()
        val seatsByDifficulty = mutableMapOf<BotDifficulty, Int>()
        val winsByLeader = mutableMapOf<LeaderId, Int>()
        val seatsByLeader = mutableMapOf<LeaderId, Int>()
        val failures = mutableListOf<SimulationFailure>()
        var noWinner = 0
        var totalTurns = 0L
        var completed = 0

        for (game in 0 until config.games) {
            val seed = config.seed + game
            val start = MatchSetup.create(config.leaders[game % config.leaders.size], config.leaders, seed)
            // Seat n always plays brain n, so a difficulty's win rate isn't skewed by turn order.
            val seats =
                start.state.players
                    .mapIndexed { index, player -> player.id to brains[index % brains.size] }
                    .toMap()
            start.state.players.forEachIndexed { index, player ->
                seatsByDifficulty.increment(brains[index % brains.size].first)
                seatsByLeader.increment(player.leaderId)
            }
            val outcome = play(start.state, seats, reducer(), config.actionLimit)
            when (outcome) {
                is Outcome.Failed -> failures += SimulationFailure(seed, outcome.reason)
                is Outcome.Finished -> {
                    completed++
                    totalTurns += outcome.turns
                    val winner = outcome.winner
                    if (winner == null) {
                        noWinner++
                    } else {
                        winsByDifficulty.increment(seats.getValue(winner).first)
                        winsByLeader.increment(outcome.leaders.getValue(winner))
                    }
                }
            }
        }
        return SimulationReport(
            games = completed,
            winsByDifficulty = winsByDifficulty,
            seatsByDifficulty = seatsByDifficulty,
            winsByLeader = winsByLeader,
            seatsByLeader = seatsByLeader,
            noWinner = noWinner,
            totalTurns = totalTurns,
            failures = failures,
        )
    }

    /** The brain behind a difficulty. The simulator and the app pick opponents the same way. */
    fun brainFor(
        difficulty: BotDifficulty,
        market: CardMarket = CardMarket.Unknown,
    ): BotBrain =
        when (difficulty) {
            BotDifficulty.INTERN -> InternBot(market)
            BotDifficulty.REPLY_GUY -> ReplyGuyBot(market)
            BotDifficulty.TROLL_FARM -> TrollFarmBot(market)
        }

    private fun play(
        initial: GameState,
        seats: Map<PlayerId, Pair<BotDifficulty, BotBrain>>,
        reducer: GameReducer,
        actionLimit: Int,
    ): Outcome {
        val leaders = initial.players.associate { it.id to it.leaderId }
        var state = initial
        var actions = 0
        while (state.result == null) {
            if (actions++ >= actionLimit) return Outcome.Failed("stuck after $actionLimit actions in ${state.phase}")
            val actor = BotMoves.actingPlayerId(state) ?: return Outcome.Failed("nobody to act in ${state.phase}")
            val brain = seats[actor]?.second ?: return Outcome.Failed("no brain for ${actor.value}")
            val transition =
                try {
                    val action =
                        brain.decide(state, state.pendingDecisions.firstOrNull(), botRandom(state))
                            ?: return Outcome.Failed("${brain.difficulty} had no move in ${state.phase}")
                    reducer.reduce(state, action)
                } catch (error: Exception) {
                    return Outcome.Failed("${error::class.simpleName}: ${error.message}")
                }
            val rejected = transition.events.filterIsInstance<GameEvent.ActionRejected>()
            if (rejected.isNotEmpty()) return Outcome.Failed("rejected: ${rejected.map { it.reason }}")
            if (transition.state == state) return Outcome.Failed("no progress in ${state.phase}")
            state = transition.state
        }
        return Outcome.Finished(
            winner = (state.result as? MatchResult.Victory)?.playerId,
            turns = state.turnNumber,
            leaders = leaders,
        )
    }

    private fun <K> MutableMap<K, Int>.increment(key: K) {
        this[key] = (this[key] ?: 0) + 1
    }

    private sealed interface Outcome {
        data class Finished(
            val winner: PlayerId?,
            val turns: Int,
            val leaders: Map<PlayerId, LeaderId>,
        ) : Outcome

        data class Failed(
            val reason: String,
        ) : Outcome
    }
}
