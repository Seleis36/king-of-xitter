package com.kingofxitter.bots

import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.HookRandom
import com.kingofxitter.engine.model.PendingDecision

/** The three opponent strengths offered on Pick your leader. */
enum class BotDifficulty {
    /** Random legal moves (B1). */
    INTERN,

    /** Heuristics: keep-dice policy, health-based log-off, card valuation (B2). */
    REPLY_GUY,

    /** Monte Carlo lookahead over the engine (B3). */
    TROLL_FARM,
}

/**
 * One bot's decision function (ROADMAP §3). The brain is pure: the same [state], [decision] and
 * random stream always give the same [Action], so matches replay and the simulator reproduces
 * bugs from a seed.
 *
 * The engine never blocks, so a brain is asked for two kinds of move. When [decision] is set,
 * the answer must choose one of its options. When it is null, the brain drives the current
 * player's turn (roll, reroll, resolve, shop, end turn). Returning null means "nothing to do",
 * which happens only when the match is over or the acting player cannot move.
 */
interface BotBrain {
    /** Which opponent strength this brain implements. */
    val difficulty: BotDifficulty

    /** Picks the bot's next action, or null when the state offers none. */
    fun decide(
        state: GameState,
        decision: PendingDecision?,
        random: HookRandom,
    ): Action?
}

/**
 * Adapts a [BotBrain] to `RealGameController`'s `botAction` seam: it reads the decision at the
 * head of the queue and derives its own random stream from [GameState.randomState], so bot
 * choices stay reproducible without consuming the reducer's stream.
 */
fun BotBrain.asBotAction(): (GameState) -> Action? =
    { state ->
        decide(state, state.pendingDecisions.firstOrNull(), botRandom(state))
    }

/**
 * The brain behind a difficulty. The app's match host and the simulator pick opponents the same
 * way, so what the player faces is what the balance numbers measured.
 */
fun botBrainFor(
    difficulty: BotDifficulty,
    market: CardMarket = CardMarket.Unknown,
): BotBrain =
    when (difficulty) {
        BotDifficulty.INTERN -> InternBot(market)
        BotDifficulty.REPLY_GUY -> ReplyGuyBot(market)
        BotDifficulty.TROLL_FARM -> TrollFarmBot(market)
    }
