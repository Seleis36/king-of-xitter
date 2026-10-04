package com.kingofxitter.bots

import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.HookRandom
import com.kingofxitter.engine.model.PendingDecision

/**
 * B1: the easiest opponent. It picks uniformly among the legal moves [BotMoves] offers, so it
 * exercises every branch of the rules without any strategy at all.
 *
 * It needs nothing but the decision API, which makes it the first brain the engine's controller
 * and the simulator can run. Every choice comes from [random], so a seed replays a whole match.
 *
 * [market] prices the shop; with the default [CardMarket.Unknown] the intern never shops, which
 * is correct for a match on the base reducer (it would reject a purchase).
 */
class InternBot(
    private val market: CardMarket = CardMarket.Unknown,
) : BotBrain {
    override val difficulty = BotDifficulty.INTERN

    override fun decide(
        state: GameState,
        decision: PendingDecision?,
        random: HookRandom,
    ): Action? {
        val actions = BotMoves.legalActions(state, decision, market)
        if (actions.isEmpty()) return null
        return actions[random.nextInt(actions.size)]
    }
}
