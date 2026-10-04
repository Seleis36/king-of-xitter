package com.kingofxitter.bots

import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.CardId
import com.kingofxitter.engine.model.DiceFace
import com.kingofxitter.engine.model.DieId
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.PendingDecision
import com.kingofxitter.engine.model.PlayerId
import com.kingofxitter.engine.model.PlayerState
import com.kingofxitter.engine.model.TurnPhase

/**
 * The legal-move surface every brain shares. It mirrors the reducer's own guards, so an action
 * from here is never rejected: brains only have to choose between them.
 */
object BotMoves {
    /** Base dice and rolls per turn from ROADMAP §1, used before the engine freezes the turn's totals. */
    const val BASE_DICE = 6
    const val BASE_ROLLS = 3

    /** Characters a shop sweep costs (ROADMAP §1). */
    const val SWEEP_COST = 20

    /** Whoever the state is waiting on: the owner of the head decision, otherwise the current player. */
    fun actingPlayerId(state: GameState): PlayerId? {
        if (state.result != null || state.phase == TurnPhase.FINISHED) return null
        return state.pendingDecisions.firstOrNull()?.playerId ?: state.currentPlayerId
    }

    /** Rolls allowed this turn, including power-up bonuses once the engine has frozen them. */
    fun rollLimit(state: GameState): Int =
        state.turnRollLimit ?: (BASE_ROLLS + (state.players.firstOrNull { it.id == state.currentPlayerId }?.extraRerolls ?: 0))

    /** Whether the current player may still reroll. */
    fun canReroll(state: GameState): Boolean = state.phase == TurnPhase.ROLLING && state.rollNumber < rollLimit(state)

    /**
     * Every action the reducer would accept next. With a [decision] the result is one
     * [Action.ChooseDecisionOption] per offered option, in the order the engine offered them.
     * Without one, it is the current player's turn moves, with shop moves included only for
     * cards [market] can appraise.
     */
    fun legalActions(
        state: GameState,
        decision: PendingDecision? = state.pendingDecisions.firstOrNull(),
        market: CardMarket = CardMarket.Unknown,
    ): List<Action> {
        if (state.result != null || state.phase == TurnPhase.FINISHED) return emptyList()
        if (decision != null) {
            return decision.options.map { option -> Action.ChooseDecisionOption(decision.playerId, decision.id, option.id) }
        }
        if (state.pendingDecisions.isNotEmpty()) return emptyList()
        val player = state.players.firstOrNull { it.id == state.currentPlayerId } ?: return emptyList()
        if (player.isSuspended) return emptyList()
        return when (state.phase) {
            TurnPhase.TURN_START -> listOf(Action.RollDice(player.id))
            TurnPhase.ROLLING ->
                buildList {
                    if (state.rollNumber > 0) add(Action.ResolveRoll(player.id))
                    if (canReroll(state)) addAll(keepCandidates(state).map { kept -> Action.RollDice(player.id, kept) })
                }
            TurnPhase.SHOP -> shopActions(state, player, market) + Action.EndTurn(player.id)
            TurnPhase.END_TURN -> listOf(Action.EndTurn(player.id))
            else -> emptyList()
        }
    }

    /**
     * Candidate keep sets for a reroll, one per subset of the faces on the table (keeping a face
     * keeps all of its dice). That is at most 64 sets instead of 2^dice, and it loses nothing:
     * two dice showing the same face are interchangeable.
     *
     * Keeping every die is dropped, because it would reroll nothing. The empty set comes first,
     * so a brain that just takes the first candidate rerolls everything.
     */
    fun keepCandidates(state: GameState): List<Set<DieId>> {
        val byFace = state.dice.groupBy { it.face }
        val faces = DiceFace.entries.filter { byFace.containsKey(it) }
        if (faces.isEmpty()) return listOf(emptySet())
        return (0 until (1 shl faces.size)).mapNotNull { mask ->
            val chosen = faces.filterIndexed { index, _ -> mask shr index and 1 == 1 }
            val kept = chosen.flatMap { face -> byFace.getValue(face).map { it.id } }.toSet()
            if (kept.size == state.dice.size) null else kept
        }
    }

    /** Buys the player can afford plus a sweep, for cards [market] knows. */
    fun shopActions(
        state: GameState,
        player: PlayerState,
        market: CardMarket,
    ): List<Action> {
        val priced = state.shopCardIds.mapNotNull { id -> market.appraise(id)?.let { id to it } }
        if (priced.isEmpty()) return emptyList()
        val buys = priced.filter { (_, card) -> card.cost <= player.characters }.map { (id, _) -> Action.BuyPowerUp(player.id, id) }
        val sweep = if (player.characters >= SWEEP_COST) listOf(Action.SweepShop(player.id)) else emptyList()
        return buys + sweep
    }

    /** Appraisals for the current shop row, keyed by card, for cards [market] knows. */
    fun pricedShop(
        state: GameState,
        market: CardMarket,
    ): Map<CardId, CardAppraisal> = state.shopCardIds.mapNotNull { id -> market.appraise(id)?.let { id to it } }.toMap()
}
