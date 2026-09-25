package com.kingofxitter.engine

import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.ActionRejectionReason
import com.kingofxitter.engine.model.DiceFace
import com.kingofxitter.engine.model.Die
import com.kingofxitter.engine.model.DieId
import com.kingofxitter.engine.model.GameEvent
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.MatchResult
import com.kingofxitter.engine.model.MatchStats
import com.kingofxitter.engine.model.PendingDecision
import com.kingofxitter.engine.model.PlayerId
import com.kingofxitter.engine.model.PlayerResource
import com.kingofxitter.engine.model.ResourceChangeReason
import com.kingofxitter.engine.model.TurnPhase
import com.kingofxitter.engine.model.VictoryReason

/**
 * Pure rules seam: identical state and action inputs produce the same transition.
 * Randomness is derived from and written back to [GameState.randomState].
 * Implementations do not read clocks or perform I/O. An illegal action leaves
 * the state unchanged and returns an [com.kingofxitter.engine.model.GameEvent.ActionRejected] event.
 */
fun interface GameReducer {
    /** Reduces [action] against [state] and returns the next state and ordered events. */
    fun reduce(
        state: GameState,
        action: Action,
    ): GameTransition
}

/** State and occurrence-ordered domain events produced by one reducer invocation. */
data class GameTransition(
    val state: GameState,
    val events: List<GameEvent>,
)

/** Reducer implementation for seeded dice rolls and rerolls. */
class DefaultGameReducer : GameReducer {
    override fun reduce(
        state: GameState,
        action: Action,
    ): GameTransition =
        when (action) {
            is Action.RollDice -> rollDice(state, action)
            is Action.ResolveRoll -> resolveRoll(state, action)
            is Action.ChooseDecisionOption -> chooseDecisionOption(state, action)
            is Action.StayInXitter -> stayInXitter(state, action)
            is Action.LogOffXitter -> logOffXitter(state, action)
            is Action.EndTurn -> endTurn(state, action)
            else -> reject(state, action.playerId, ActionRejectionReason.WRONG_PHASE)
        }

    private fun rollDice(
        state: GameState,
        action: Action.RollDice,
    ): GameTransition {
        if (state.result != null || state.phase == TurnPhase.FINISHED) {
            return reject(state, action.playerId, ActionRejectionReason.MATCH_FINISHED)
        }
        if (state.currentPlayerId != action.playerId) {
            return reject(state, action.playerId, ActionRejectionReason.WRONG_PLAYER)
        }
        if (state.phase != TurnPhase.ROLLING && state.phase != TurnPhase.TURN_START) {
            return reject(state, action.playerId, ActionRejectionReason.WRONG_PHASE)
        }

        val player =
            state.players.firstOrNull { it.id == action.playerId }
                ?: return reject(state, action.playerId, ActionRejectionReason.INVALID_SELECTION)
        if (player.isSuspended || player.extraDice < 0 || player.extraRerolls < 0) {
            return reject(state, action.playerId, ActionRejectionReason.INVALID_SELECTION)
        }

        val isFirstRoll = state.dice.isEmpty() && state.rollNumber == 0
        if ((!isFirstRoll && state.dice.isEmpty()) || (isFirstRoll && action.keptDice.isNotEmpty())) {
            return reject(state, action.playerId, ActionRejectionReason.INVALID_SELECTION)
        }
        if (!isFirstRoll && action.keptDice.any { id -> state.dice.none { it.id == id } }) {
            return reject(state, action.playerId, ActionRejectionReason.INVALID_SELECTION)
        }
        if (state.rollNumber >= MAX_ROLLS + player.extraRerolls) {
            return reject(state, action.playerId, ActionRejectionReason.INVALID_SELECTION)
        }

        val diceCount = BASE_DICE + player.extraDice
        if (!isFirstRoll && state.dice.size != diceCount) {
            return reject(state, action.playerId, ActionRejectionReason.INVALID_SELECTION)
        }
        if (isFirstRoll && diceCount <= 0) {
            return reject(state, action.playerId, ActionRejectionReason.INVALID_SELECTION)
        }

        val dice =
            if (isFirstRoll) {
                (0 until diceCount).map { index ->
                    Die(DieId(index), DiceFace.ONE)
                }
            } else {
                state.dice
            }
        var workingState = state
        val startEvents = mutableListOf<GameEvent>()
        if (state.phase == TurnPhase.TURN_START) {
            if (state.xitterPlayerId == action.playerId) {
                val changed =
                    changeResource(
                        workingState,
                        action.playerId,
                        PlayerResource.MANDATES,
                        2,
                        ResourceChangeReason.XITTER_TURN_START,
                    )
                workingState = changed.first
                startEvents += changed.second
                val previousStats = workingState.matchStats[action.playerId] ?: MatchStats()
                workingState =
                    workingState.copy(
                        matchStats =
                            workingState.matchStats + (
                                action.playerId to
                                    previousStats.copy(
                                        turnsAsMainCharacter = previousStats.turnsAsMainCharacter + 1,
                                    )
                            ),
                    )
            }
        }
        val random = SplitMix64(state.randomState)
        val updatedDice =
            dice.map { die ->
                val kept = die.id in action.keptDice
                if (kept) {
                    die.copy(isKept = true)
                } else {
                    die.copy(face = DiceFace.entries[random.nextInt(DiceFace.entries.size)], isKept = false)
                }
            }
        val rollNumber = state.rollNumber + 1
        val nextState =
            workingState.copy(
                phase = TurnPhase.ROLLING,
                dice = updatedDice,
                rollNumber = rollNumber,
                randomState = random.state,
            )
        return GameTransition(
            state = nextState,
            events = startEvents + GameEvent.DiceRolled(action.playerId, rollNumber, updatedDice),
        )
    }

    private fun resolveRoll(
        state: GameState,
        action: Action.ResolveRoll,
    ): GameTransition {
        if (state.result != null || state.phase == TurnPhase.FINISHED) {
            return reject(state, action.playerId, ActionRejectionReason.MATCH_FINISHED)
        }
        if (state.currentPlayerId != action.playerId) {
            return reject(state, action.playerId, ActionRejectionReason.WRONG_PLAYER)
        }
        if (state.phase != TurnPhase.ROLLING || state.rollNumber == 0 || state.pendingDecisions.isNotEmpty()) {
            return reject(state, action.playerId, ActionRejectionReason.WRONG_PHASE)
        }

        var nextState = state.copy(phase = TurnPhase.RESOLVING)
        val events = mutableListOf<GameEvent>()
        val actor =
            nextState.players.firstOrNull { it.id == action.playerId }
                ?: return reject(state, action.playerId, ActionRejectionReason.INVALID_SELECTION)
        for (face in listOf(DiceFace.ONE, DiceFace.TWO, DiceFace.THREE)) {
            val count = nextState.dice.count { it.face == face }
            if (count >= 3) {
                val score = face.ordinal + 1 + count - 3
                val changed =
                    changeResource(
                        nextState,
                        action.playerId,
                        PlayerResource.MANDATES,
                        score,
                        ResourceChangeReason.OTHER,
                    )
                nextState = changed.first
                events += changed.second
            }
        }

        val likes = nextState.dice.count { it.face == DiceFace.LIKE }
        if (nextState.xitterPlayerId != action.playerId && likes > 0) {
            val actual = minOf(likes, actor.maxFollowers - actor.followers)
            if (actual > 0) {
                val changed =
                    changeResource(
                        nextState,
                        action.playerId,
                        PlayerResource.FOLLOWERS,
                        actual,
                        ResourceChangeReason.LIKE,
                    )
                nextState = changed.first
                events += changed.second
            }
        }

        val characterFaces = nextState.dice.count { it.face == DiceFace.CHARACTERS }
        if (characterFaces > 0) {
            val changed =
                changeResource(
                    nextState,
                    action.playerId,
                    PlayerResource.CHARACTERS,
                    characterFaces * CHARACTERS_PER_FACE,
                    ResourceChangeReason.CHARACTERS_FACE,
                )
            nextState = changed.first
            events += changed.second
        }

        val tweetCount = nextState.dice.count { it.face == DiceFace.TWEET }
        val tweetTransition = resolveTweets(nextState, action.playerId, tweetCount)
        nextState = tweetTransition.state
        events += tweetTransition.events

        if (nextState.xitterPlayerId == null && nextState.players.any { it.id == action.playerId && !it.isSuspended }) {
            val changed =
                changeResource(
                    nextState,
                    action.playerId,
                    PlayerResource.MANDATES,
                    1,
                    ResourceChangeReason.XITTER_ENTRY,
                )
            nextState = changed.first.copy(xitterPlayerId = action.playerId)
            events += changed.second
            events += GameEvent.MainCharacterChanged(null, action.playerId)
        }

        nextState =
            nextState.copy(
                phase = if (nextState.pendingDecisions.isEmpty()) TurnPhase.SHOP else TurnPhase.DECISION,
                phaseAfterDecisions = if (nextState.pendingDecisions.isEmpty()) null else TurnPhase.SHOP,
            )
        val ended = finishIfNeeded(nextState)
        return GameTransition(ended.first, events + ended.second)
    }

    private fun resolveTweets(
        state: GameState,
        attackerId: PlayerId,
        tweetCount: Int,
    ): GameTransition {
        if (tweetCount <= 0 || state.result != null) return GameTransition(state, emptyList())
        val mainCharacter = state.xitterPlayerId
        val targets =
            when {
                mainCharacter == null -> emptyList()
                mainCharacter == attackerId -> state.players.filter { it.id != attackerId && !it.isSuspended }.map { it.id }
                else -> listOf(mainCharacter)
            }
        var nextState = state
        val events = mutableListOf<GameEvent>()
        var totalUnfollows = 0
        for (targetId in targets) {
            val target = nextState.players.first { it.id == targetId }
            val damage = minOf(tweetCount, target.followers)
            if (damage > 0) {
                val changed =
                    changeResource(
                        nextState,
                        targetId,
                        PlayerResource.FOLLOWERS,
                        -damage,
                        ResourceChangeReason.TWEET,
                    )
                nextState = changed.first
                events += changed.second
                totalUnfollows += damage
                if (target.followers > 0 && target.followers - damage == 0) {
                    events += GameEvent.PlayerSuspended(targetId)
                }
            }
        }

        val currentStats = nextState.matchStats[attackerId] ?: MatchStats()
        nextState =
            nextState.copy(
                matchStats =
                    nextState.matchStats + (
                        attackerId to
                            currentStats.copy(
                                tweetsSent = currentStats.tweetsSent + tweetCount,
                                unfollowsDealt = currentStats.unfollowsDealt + totalUnfollows,
                            )
                    ),
            )

        if (mainCharacter != null && targets.contains(mainCharacter)) {
            val mainCharacterState = nextState.players.first { it.id == mainCharacter }
            val attackerAlive = nextState.players.any { it.id == attackerId && !it.isSuspended }
            if (mainCharacterState.isSuspended) {
                val newOwner = if (attackerAlive) attackerId else null
                nextState = nextState.copy(xitterPlayerId = newOwner)
                events += GameEvent.MainCharacterChanged(mainCharacter, newOwner)
            } else {
                val decision =
                    PendingDecision(
                        id =
                            com.kingofxitter.engine.model
                                .DecisionId("stay-or-log-off-$attackerId-${state.rollNumber}"),
                        playerId = mainCharacter,
                        kind = com.kingofxitter.engine.model.DecisionKind.STAY_OR_LOG_OFF,
                        options =
                            listOf(
                                com.kingofxitter.engine.model.DecisionOption(
                                    com.kingofxitter.engine.model
                                        .DecisionOptionId("stay"),
                                    Action.StayInXitter(mainCharacter),
                                ),
                                com.kingofxitter.engine.model.DecisionOption(
                                    com.kingofxitter.engine.model
                                        .DecisionOptionId("log-off"),
                                    Action.LogOffXitter(mainCharacter),
                                ),
                            ),
                    )
                nextState = nextState.copy(pendingDecisions = nextState.pendingDecisions + decision)
                events += GameEvent.DecisionQueued(decision)
            }
        }
        return GameTransition(nextState, events)
    }

    private fun stayInXitter(
        state: GameState,
        action: Action.StayInXitter,
    ): GameTransition {
        if (state.result != null || state.phase !in setOf(TurnPhase.SHOP, TurnPhase.DECISION) || state.xitterPlayerId != action.playerId) {
            return reject(state, action.playerId, ActionRejectionReason.WRONG_PHASE)
        }
        return GameTransition(state, emptyList())
    }

    private fun logOffXitter(
        state: GameState,
        action: Action.LogOffXitter,
    ): GameTransition {
        if (state.result != null || state.phase !in setOf(TurnPhase.SHOP, TurnPhase.DECISION) || state.xitterPlayerId != action.playerId) {
            return reject(state, action.playerId, ActionRejectionReason.WRONG_PHASE)
        }
        val attacker =
            state.players.firstOrNull { it.id == state.currentPlayerId && !it.isSuspended }
                ?: return reject(state, action.playerId, ActionRejectionReason.INVALID_SELECTION)
        return GameTransition(
            state.copy(xitterPlayerId = attacker.id),
            listOf(GameEvent.MainCharacterChanged(action.playerId, attacker.id)),
        )
    }

    private fun chooseDecisionOption(
        state: GameState,
        action: Action.ChooseDecisionOption,
    ): GameTransition {
        if (state.result != null || state.phase == TurnPhase.FINISHED) {
            return reject(state, action.playerId, ActionRejectionReason.MATCH_FINISHED)
        }
        if (state.phase != TurnPhase.DECISION) {
            return reject(state, action.playerId, ActionRejectionReason.WRONG_PHASE)
        }
        val decision = state.pendingDecisions.firstOrNull()
        if (decision == null || decision.id != action.decisionId || decision.playerId != action.playerId) {
            return reject(state, action.playerId, ActionRejectionReason.UNKNOWN_DECISION)
        }
        val option =
            decision.options.firstOrNull { it.id == action.optionId }
                ?: return reject(state, action.playerId, ActionRejectionReason.OPTION_NOT_AVAILABLE)
        if (option.action.playerId != action.playerId) {
            return reject(state, action.playerId, ActionRejectionReason.INVALID_SELECTION)
        }

        val remainingDecisions = state.pendingDecisions.drop(1)
        val resumePhase =
            if (remainingDecisions.isEmpty()) {
                state.phaseAfterDecisions ?: TurnPhase.END_TURN
            } else {
                TurnPhase.DECISION
            }
        val preparedState =
            state.copy(
                phase = resumePhase,
                pendingDecisions = remainingDecisions,
                phaseAfterDecisions = if (remainingDecisions.isEmpty()) null else state.phaseAfterDecisions,
            )
        val applied = reduce(preparedState, option.action)
        if (applied.events.any { it is GameEvent.ActionRejected }) {
            return GameTransition(
                state = state,
                events = applied.events,
            )
        }
        return GameTransition(
            state = applied.state,
            events = listOf(GameEvent.DecisionResolved(action.decisionId, action.optionId)) + applied.events,
        )
    }

    private fun endTurn(
        state: GameState,
        action: Action.EndTurn,
    ): GameTransition {
        if (state.result != null || state.phase == TurnPhase.FINISHED) {
            return reject(state, action.playerId, ActionRejectionReason.MATCH_FINISHED)
        }
        if (state.currentPlayerId != action.playerId) {
            return reject(state, action.playerId, ActionRejectionReason.WRONG_PLAYER)
        }
        if (state.phase != TurnPhase.END_TURN || state.pendingDecisions.isNotEmpty()) {
            return reject(state, action.playerId, ActionRejectionReason.WRONG_PHASE)
        }
        val livingPlayers = state.players.filterNot { it.isSuspended }
        if (livingPlayers.isEmpty() || livingPlayers.none { it.id == action.playerId }) {
            return reject(state, action.playerId, ActionRejectionReason.INVALID_SELECTION)
        }
        val currentIndex = livingPlayers.indexOfFirst { it.id == action.playerId }
        val nextPlayer = livingPlayers[(currentIndex + 1) % livingPlayers.size]
        val nextTurnNumber = state.turnNumber + 1
        val nextState =
            state.copy(
                currentPlayerId = nextPlayer.id,
                phase = TurnPhase.TURN_START,
                turnNumber = nextTurnNumber,
                dice = emptyList(),
                rollNumber = 0,
            )
        val events =
            listOf(
                GameEvent.TurnEnded(action.playerId, state.turnNumber),
                GameEvent.TurnStarted(nextPlayer.id, nextTurnNumber),
            )
        val finished = finishIfNeeded(nextState)
        return GameTransition(finished.first, events + finished.second)
    }

    private fun changeResource(
        state: GameState,
        playerId: PlayerId,
        resource: PlayerResource,
        requestedAmount: Int,
        reason: ResourceChangeReason,
    ): Pair<GameState, GameEvent.ResourceChanged> {
        val playerIndex = state.players.indexOfFirst { it.id == playerId }
        require(playerIndex >= 0)
        val player = state.players[playerIndex]
        val previous =
            when (resource) {
                PlayerResource.FOLLOWERS -> player.followers
                PlayerResource.MANDATES -> player.mandates
                PlayerResource.CHARACTERS -> player.characters
            }
        val maximum = if (resource == PlayerResource.FOLLOWERS) player.maxFollowers else Int.MAX_VALUE
        val updated = (previous.toLong() + requestedAmount).coerceIn(0L, maximum.toLong()).toInt()
        val updatedPlayer =
            when (resource) {
                PlayerResource.FOLLOWERS -> player.copy(followers = updated)
                PlayerResource.MANDATES -> player.copy(mandates = updated)
                PlayerResource.CHARACTERS -> player.copy(characters = updated)
            }
        val updatedPlayers = state.players.toMutableList().also { it[playerIndex] = updatedPlayer }
        val event = GameEvent.ResourceChanged(playerId, resource, updated - previous, updated, reason)
        return state.copy(players = updatedPlayers) to event
    }

    private fun finishIfNeeded(state: GameState): Pair<GameState, List<GameEvent>> {
        if (state.result != null) return state to emptyList()
        val living = state.players.filterNot { it.isSuspended }
        val scoredWinner = living.firstOrNull { it.mandates >= MANDATES_TO_WIN }
        val result =
            when {
                living.isEmpty() -> MatchResult.NoWinner
                scoredWinner != null -> MatchResult.Victory(scoredWinner.id, VictoryReason.TWENTY_MANDATES)
                living.size == 1 -> MatchResult.Victory(living.single().id, VictoryReason.LAST_ACCOUNT_STANDING)
                else -> null
            } ?: return state to emptyList()
        return state.copy(phase = TurnPhase.FINISHED, result = result) to listOf(GameEvent.MatchEnded(result))
    }

    private fun reject(
        state: GameState,
        playerId: PlayerId,
        reason: ActionRejectionReason,
    ) = GameTransition(
        state = state,
        events = listOf(GameEvent.ActionRejected(playerId, reason)),
    )

    private companion object {
        const val BASE_DICE = 6
        const val CHARACTERS_PER_FACE = 10
        const val MANDATES_TO_WIN = 20
        const val MAX_ROLLS = 3
    }
}

/** Small deterministic generator whose complete state is stored in [GameState]. */
private class SplitMix64(
    var state: Long,
) {
    fun nextInt(bound: Int): Int {
        require(bound > 0)
        var bits: Long
        var value: Long
        do {
            state += GOLDEN_GAMMA
            var mixed = state
            mixed = (mixed xor (mixed ushr 30)) * MIX_1
            mixed = (mixed xor (mixed ushr 27)) * MIX_2
            bits = (mixed xor (mixed ushr 31)) ushr 1
            value = bits % bound
        } while (bits - value + (bound - 1) < 0L)
        return value.toInt()
    }

    private companion object {
        const val GOLDEN_GAMMA = -7046029254386353131L
        const val MIX_1 = -4658895280553007687L
        const val MIX_2 = -7723592293110705685L
    }
}
