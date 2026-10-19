package com.kingofxitter.app.ui.board

import com.kingofxitter.engine.model.DecisionKind
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.PendingDecision
import com.kingofxitter.engine.model.PlayerId
import com.kingofxitter.engine.model.TurnPhase

/** What the human can do on the board right now; drives the bottom controls. */
sealed interface BoardMode {
    /** Start of the human's turn: roll all dice. */
    data object ReadyToRoll : BoardMode

    /** The human has rolled: keep dice and reroll, or resolve. */
    data class Rolling(
        val rollNumber: Int,
        val rollLimit: Int,
    ) : BoardMode {
        val canReroll: Boolean get() = rollNumber < rollLimit
    }

    /** After resolving: shop, then end the turn. */
    data object Shopping : BoardMode

    /** The human was hit as Main Character and must stay or log off. */
    data class IncomingTweet(
        val decision: PendingDecision,
    ) : BoardMode

    /** Any other decision the human owns, such as an optional power-up ability. */
    data class Deciding(
        val decision: PendingDecision,
    ) : BoardMode

    /** Someone else is acting (their turn, or their decision on the human's turn). */
    data class Watching(
        val actorId: PlayerId,
    ) : BoardMode

    /** The match is over, or your account is suspended while the others play on. */
    data object Finished : BoardMode

    companion object {
        /** Base rolls per turn (one roll plus two rerolls) before power-ups add more. */
        const val BASE_ROLLS = 3

        /** Mode for [humanId] in [state]. The first queued decision always comes first. */
        fun of(
            state: GameState,
            humanId: PlayerId,
        ): BoardMode {
            if (state.result != null || state.phase == TurnPhase.FINISHED) return Finished
            // Suspended, you have nothing left to do but see how it ended for you.
            if (state.players.any { it.id == humanId && it.isSuspended }) return Finished
            val decision = state.pendingDecisions.firstOrNull()
            if (decision != null) {
                return when {
                    decision.playerId != humanId -> Watching(decision.playerId)
                    decision.kind == DecisionKind.STAY_OR_LOG_OFF -> IncomingTweet(decision)
                    else -> Deciding(decision)
                }
            }
            if (state.currentPlayerId != humanId) return Watching(state.currentPlayerId)
            return when (state.phase) {
                TurnPhase.TURN_START, TurnPhase.SETUP -> ReadyToRoll
                TurnPhase.ROLLING -> {
                    val player = state.players.first { it.id == humanId }
                    Rolling(state.rollNumber, state.turnRollLimit ?: (BASE_ROLLS + player.extraRerolls))
                }
                TurnPhase.RESOLVING, TurnPhase.DECISION -> Watching(humanId)
                TurnPhase.SHOP, TurnPhase.END_TURN -> Shopping
                TurnPhase.FINISHED -> Finished
            }
        }
    }
}
