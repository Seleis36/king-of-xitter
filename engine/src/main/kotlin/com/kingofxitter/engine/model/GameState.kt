package com.kingofxitter.engine.model

/**
 * Complete immutable snapshot of a match at one point in the rules flow.
 * Replace collections when changing state so observers receive new snapshots.
 * [randomState] is opaque reducer-owned PRNG state, making rolls reproducible.
 * Decision order is queue order, with the first entry resolved next.
 * Card piles hold opaque [CardId] references so the engine does not depend on the cards module.
 */
data class GameState(
    val players: List<PlayerState>,
    val currentPlayerId: PlayerId,
    val phase: TurnPhase,
    val turnNumber: Int = 0,
    val xitterPlayerId: PlayerId? = null,
    val dice: List<Die> = emptyList(),
    val rollNumber: Int = 0,
    val pendingDecisions: List<PendingDecision> = emptyList(),
    val drawPileCardIds: List<CardId> = emptyList(),
    val shopCardIds: List<CardId> = emptyList(),
    val discardPileCardIds: List<CardId> = emptyList(),
    val result: MatchResult? = null,
    val randomState: Long = 0L,
    /** Phase to resume after the final queued decision is resolved. */
    val phaseAfterDecisions: TurnPhase? = null,
    /** Per-player counters displayed on end-game screens. */
    val matchStats: Map<PlayerId, MatchStats> = emptyMap(),
    /** Prevents start-of-turn hooks from running twice across a pending decision. */
    val turnStartHooksApplied: Boolean = false,
    /** Prevents resolve hooks from running twice across a pending decision. */
    val resolveHooksAppliedForRoll: Boolean = false,
    /** Dice total frozen for the current turn after its first-roll hooks. */
    val turnDiceCount: Int? = null,
    /** Roll limit frozen for the current turn after its first-roll hooks. */
    val turnRollLimit: Int? = null,
)

/** Current game-flow phase, shared by the reducer, bots, and screens. */
enum class TurnPhase {
    SETUP,
    TURN_START,
    ROLLING,
    RESOLVING,
    DECISION,
    SHOP,
    END_TURN,
    FINISHED,
}

/** Immutable state for a player-controlled leader. Resource values are never negative. */
data class PlayerState(
    val id: PlayerId,
    val leaderId: LeaderId,
    val followers: Int = 10,
    val maxFollowers: Int = 10,
    val mandates: Int = 0,
    val characters: Int = 0,
    /** Additional dice granted by active effects. */
    val extraDice: Int = 0,
    /** Additional rerolls granted by active effects. */
    val extraRerolls: Int = 0,
    /** Extra turns queued for this player. */
    val extraTurns: Int = 0,
    val policyIds: List<CardId> = emptyList(),
    val counters: Map<CounterId, Int> = emptyMap(),
) {
    /** Whether this player has been eliminated from the match. */
    val isSuspended: Boolean
        get() = followers <= 0
}

/** One die's current face and whether it is being kept for a later roll. */
data class Die(
    val id: DieId,
    val face: DiceFace,
    val isKept: Boolean = false,
)

/** Match totals used by Victory, Defeat, and Standings screens. */
data class MatchStats(
    val tweetsSent: Int = 0,
    val unfollowsDealt: Int = 0,
    val turnsAsMainCharacter: Int = 0,
)

/** The six faces used by the game's standard dice. */
enum class DiceFace {
    ONE,
    TWO,
    THREE,
    LIKE,
    CHARACTERS,
    TWEET,
}

/** Why a match ended. */
enum class VictoryReason {
    TWENTY_MANDATES,
    LAST_ACCOUNT_STANDING,
}

/** Final match outcome; [NoWinner] represents a simultaneous elimination. */
sealed interface MatchResult {
    /** A player won through score or survival. */
    data class Victory(
        val playerId: PlayerId,
        val reason: VictoryReason,
    ) : MatchResult

    /** Every remaining player was eliminated at the same time. */
    data object NoWinner : MatchResult
}
