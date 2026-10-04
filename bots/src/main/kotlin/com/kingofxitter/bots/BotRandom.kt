package com.kingofxitter.bots

import com.kingofxitter.engine.SplitMix64
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.HookRandom

/**
 * A bot's own deterministic stream for [state]. It is seeded from the match's random state, turn
 * and roll, so the same position always produces the same bot choice, and a bot never advances
 * the reducer's stream (which would change the dice).
 */
fun botRandom(state: GameState): HookRandom =
    SplitMix64(
        state.randomState * MIX + state.turnNumber * TURN + state.rollNumber * ROLL + state.pendingDecisions.size,
    )

/** Fixed odd multipliers; any odd constants keep the seeds distinct. */
private const val MIX = 0x2545F4914F6CDD1DL
private const val TURN = -0x61c8864680b583ebL
private const val ROLL = -0x40a7b892e31b1a47L
