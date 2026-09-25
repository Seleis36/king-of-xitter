package com.kingofxitter.engine.model

/** A player intent submitted to the pure game reducer. */
sealed interface Action {
    /** Player whose turn or decision this action represents. */
    val playerId: PlayerId

    /** Roll all dice on the first roll, or reroll every die not listed here. */
    data class RollDice(
        override val playerId: PlayerId,
        val keptDice: Set<DieId> = emptySet(),
    ) : Action

    /** Resolve the current roll after the player has finished rerolling. */
    data class ResolveRoll(
        override val playerId: PlayerId,
    ) : Action

    /** Choose to remain Main Character after an incoming tweet. */
    data class StayInXitter(
        override val playerId: PlayerId,
    ) : Action

    /** Log off after an incoming tweet, yielding Xitter to the current attacker. */
    data class LogOffXitter(
        override val playerId: PlayerId,
    ) : Action

    /** Select one option offered by a pending decision. */
    data class ChooseDecisionOption(
        override val playerId: PlayerId,
        val decisionId: DecisionId,
        val optionId: DecisionOptionId,
    ) : Action

    /** Buy a power-up currently available to the player. */
    data class BuyPowerUp(
        override val playerId: PlayerId,
        val cardId: CardId,
    ) : Action

    /** Discard the visible shop row for the rules-defined sweep cost. */
    data class SweepShop(
        override val playerId: PlayerId,
    ) : Action

    /** End the player's turn after all required decisions and effects resolve. */
    data class EndTurn(
        override val playerId: PlayerId,
    ) : Action

    /** Activate an owned power-up ability. */
    data class ActivateAbility(
        override val playerId: PlayerId,
        val abilityId: String,
        val targetPlayerId: PlayerId? = null,
        val dieIds: Set<DieId> = emptySet(),
    ) : Action
}
