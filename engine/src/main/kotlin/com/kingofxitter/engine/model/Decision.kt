package com.kingofxitter.engine.model

import kotlinx.serialization.Serializable

/**
 * A choice the active player or bot must make before the rules can continue.
 * Option IDs must be unique within the decision, and every option's action
 * must belong to [playerId].
 */
@Serializable
data class PendingDecision(
    val id: DecisionId,
    val playerId: PlayerId,
    val kind: DecisionKind,
    val options: List<DecisionOption>,
    /** Localization key for an ability prompt, when applicable. */
    val promptKey: String? = null,
)

/** Kind of decision being presented to the player or bot. */
@Serializable
enum class DecisionKind {
    KEEP_DICE,
    STAY_OR_LOG_OFF,
    BUY_POWER_UP,
    ACTIVATE_ABILITY,
    CHOOSE_TARGET,
    CHOOSE_DIE,
}

/** A legal action offered as one option for a pending decision. */
@Serializable
data class DecisionOption(
    val id: DecisionOptionId,
    val action: Action,
    /** Localization key for the option label, when applicable. */
    val labelKey: String? = null,
)
