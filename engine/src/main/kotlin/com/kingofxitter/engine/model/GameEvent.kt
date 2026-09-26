package com.kingofxitter.engine.model

/** A domain event produced while reducing an action. Event order is significant. */
sealed interface GameEvent {
    /** A player's turn began. */
    data class TurnStarted(
        val playerId: PlayerId,
        val turnNumber: Int,
    ) : GameEvent

    /** A player's turn ended. */
    data class TurnEnded(
        val playerId: PlayerId,
        val turnNumber: Int,
    ) : GameEvent

    /** Dice were rolled for the current player; [dice] is the full resulting roll. */
    data class DiceRolled(
        val playerId: PlayerId,
        val rollNumber: Int,
        val dice: List<Die>,
    ) : GameEvent

    /** Existing dice changed after a power-up effect. */
    data class DiceChanged(
        val playerId: PlayerId,
        val dice: List<Die>,
    ) : GameEvent

    /** One of a player's tracked resources changed; [amount] is a signed delta. */
    data class ResourceChanged(
        val playerId: PlayerId,
        val resource: PlayerResource,
        val amount: Int,
        val totalAfter: Int,
        val reason: ResourceChangeReason,
    ) : GameEvent

    /** Xitter ownership changed after entry, attack, or logging off. */
    data class MainCharacterChanged(
        val previousPlayerId: PlayerId?,
        val newPlayerId: PlayerId?,
    ) : GameEvent

    /** A player reached zero Followers and was eliminated. */
    data class PlayerSuspended(
        val playerId: PlayerId,
    ) : GameEvent

    /** A new decision was added to the decision queue. */
    data class DecisionQueued(
        val decision: PendingDecision,
    ) : GameEvent

    /** A pending decision was answered. */
    data class DecisionResolved(
        val decisionId: DecisionId,
        val optionId: DecisionOptionId,
    ) : GameEvent

    /** A power-up was added to a player's policies or resolved as an order. */
    data class PowerUpBought(
        val playerId: PlayerId,
        val cardId: CardId,
    ) : GameEvent

    /** A power-up counter changed. */
    data class CounterChanged(
        val playerId: PlayerId,
        val counterId: CounterId,
        val amount: Int,
        val totalAfter: Int,
    ) : GameEvent

    /** A player gained one or more extra turns. */
    data class ExtraTurnsChanged(
        val playerId: PlayerId,
        val amount: Int,
        val totalAfter: Int,
    ) : GameEvent

    /** The visible shop row changed. */
    data class ShopChanged(
        val cardIds: List<CardId>,
    ) : GameEvent

    /** The match reached a final outcome. */
    data class MatchEnded(
        val result: MatchResult,
    ) : GameEvent

    /** The reducer declined an action that was not legal in the current state. */
    data class ActionRejected(
        val playerId: PlayerId,
        val reason: ActionRejectionReason,
    ) : GameEvent
}

/** A resource tracked on [PlayerState]. */
enum class PlayerResource {
    FOLLOWERS,
    MANDATES,
    CHARACTERS,
}

/** Rule source for a resource change, useful to UI animation and event history. */
enum class ResourceChangeReason {
    LIKE,
    CHARACTERS_FACE,
    TWEET,
    XITTER_ENTRY,
    XITTER_TURN_START,
    POWER_UP,
    OTHER,
}

/** Stable reason code for an action rejected by the reducer. */
enum class ActionRejectionReason {
    WRONG_PLAYER,
    WRONG_PHASE,
    UNKNOWN_DECISION,
    OPTION_NOT_AVAILABLE,
    INVALID_SELECTION,
    MATCH_FINISHED,
}
