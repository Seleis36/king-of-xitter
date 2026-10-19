package com.kingofxitter.engine

import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.ActionRejectionReason
import com.kingofxitter.engine.model.CardEffect
import com.kingofxitter.engine.model.CardEffectIntent
import com.kingofxitter.engine.model.CardEffectRegistry
import com.kingofxitter.engine.model.CardHookContext
import com.kingofxitter.engine.model.CardHookWindow
import com.kingofxitter.engine.model.CardId
import com.kingofxitter.engine.model.CounterId
import com.kingofxitter.engine.model.DecisionId
import com.kingofxitter.engine.model.DecisionKind
import com.kingofxitter.engine.model.DecisionOption
import com.kingofxitter.engine.model.DecisionOptionId
import com.kingofxitter.engine.model.DiceFace
import com.kingofxitter.engine.model.Die
import com.kingofxitter.engine.model.DieId
import com.kingofxitter.engine.model.EXTRA_TURN_DICE_PENALTY
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
import com.kingofxitter.engine.model.effectivePolicyIds

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

    /** Dispatch purchase hooks after the shop has updated the immutable state. */
    fun dispatchOnBuy(
        state: GameState,
        buyerId: PlayerId,
        purchasedCardId: com.kingofxitter.engine.model.CardId,
    ): GameTransition = GameTransition(state, emptyList())
}

/** State and occurrence-ordered domain events produced by one reducer invocation. */
data class GameTransition(
    val state: GameState,
    val events: List<GameEvent>,
)

/** Reducer implementation for seeded dice rolls and rerolls. */
class DefaultGameReducer(
    private val cardEffects: CardEffectRegistry = CardEffectRegistry(),
) : GameReducer {
    override fun reduce(
        state: GameState,
        action: Action,
    ): GameTransition =
        when (action) {
            is Action.RollDice -> rollDice(state, action)
            is Action.ResolveRoll -> resolveRoll(state, action)
            is Action.ChooseDecisionOption -> chooseDecisionOption(state, action)
            is Action.StayInXitter -> chooseOfferedAction(state, action)
            is Action.LogOffXitter -> chooseOfferedAction(state, action)
            is Action.EndTurn -> endTurn(state, action)
            is Action.ActivateAbility -> activateAbility(state, action)
            else -> reject(state, action.playerId, ActionRejectionReason.WRONG_PHASE)
        }

    override fun dispatchOnBuy(
        state: GameState,
        buyerId: PlayerId,
        purchasedCardId: com.kingofxitter.engine.model.CardId,
    ): GameTransition {
        val buyer =
            state.players.firstOrNull { it.id == buyerId }
                ?: return reject(state, buyerId, ActionRejectionReason.INVALID_SELECTION)
        val effects =
            (buyer.policyIds + purchasedCardId)
                .distinct()
                .mapNotNull(cardEffects::get)
                .sortedBy { it.cardId.value }
        val random = SplitMix64(state.randomState)
        val intents =
            effects.flatMap { effect ->
                effect.onBuy(hookContext(state, buyerId, effect, CardHookWindow.ON_BUY, random, purchasedCardId = purchasedCardId))
            }
        val applied = applyCardIntents(state, intents, random, buyerId)
        val finished = finishIfNeeded(applied.first.copy(randomState = random.state))
        return GameTransition(awaitDecisions(finished.first), applied.second + finished.second)
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

        var workingState = state
        val random = SplitMix64(state.randomState)
        val startEvents = mutableListOf<GameEvent>()
        if (state.phase == TurnPhase.TURN_START && !state.turnStartHooksApplied) {
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
                val stats = workingState.matchStats[action.playerId] ?: MatchStats()
                workingState =
                    workingState.copy(
                        matchStats =
                            workingState.matchStats + (
                                action.playerId to stats.copy(turnsAsMainCharacter = stats.turnsAsMainCharacter + 1)
                            ),
                    )
            }
            if (workingState.players.any { it.id == action.playerId && it.mandates >= MANDATES_TO_WIN }) {
                val finished = finishIfNeeded(workingState)
                return GameTransition(finished.first, startEvents + finished.second)
            }
            val onStart =
                invokeOwnerHooks(
                    workingState,
                    action.playerId,
                    CardHookWindow.TURN_START,
                    random,
                ) { effect, hookContext -> effect.onTurnStart(hookContext) }
            val appliedStart = applyCardIntents(workingState, onStart, random, action.playerId)
            workingState = appliedStart.first
            startEvents += appliedStart.second
            val finished = finishIfNeeded(workingState)
            workingState = finished.first
            startEvents += finished.second
            if (workingState.result != null) {
                return GameTransition(workingState.copy(randomState = random.state), startEvents)
            }
            if (workingState.players.any { it.id == action.playerId && it.isSuspended }) {
                val next = completeEndTurn(workingState.copy(randomState = random.state), action.playerId)
                return GameTransition(next.state, startEvents + next.events)
            }
            workingState = workingState.copy(turnStartHooksApplied = true)
            if (workingState.pendingDecisions.isNotEmpty()) {
                val waiting =
                    workingState.copy(
                        phase = TurnPhase.DECISION,
                        phaseAfterDecisions = TurnPhase.TURN_START,
                        randomState = random.state,
                    )
                return GameTransition(waiting, startEvents)
            }
        }

        val player =
            workingState.players.firstOrNull { it.id == action.playerId }
                ?: return reject(state, action.playerId, ActionRejectionReason.INVALID_SELECTION)
        if (player.isSuspended || player.extraDice < 0 || player.extraRerolls < 0 || player.extraTurns < 0) {
            return reject(state, action.playerId, ActionRejectionReason.INVALID_SELECTION)
        }

        val isFirstRoll = workingState.dice.isEmpty() && workingState.rollNumber == 0
        if ((!isFirstRoll && workingState.dice.isEmpty()) || (isFirstRoll && action.keptDice.isNotEmpty())) {
            return reject(state, action.playerId, ActionRejectionReason.INVALID_SELECTION)
        }
        if (!isFirstRoll && action.keptDice.any { id -> workingState.dice.none { it.id == id } }) {
            return reject(state, action.playerId, ActionRejectionReason.INVALID_SELECTION)
        }

        val diceBonus =
            if (isFirstRoll) {
                invokeNumericHooks(workingState, action.playerId, CardHookWindow.DICE_COUNT, random) { effect, hookContext ->
                    effect.diceCount(hookContext)
                }
            } else {
                0
            }
        val rerollBonus =
            if (isFirstRoll) {
                invokeNumericHooks(workingState, action.playerId, CardHookWindow.REROLL_COUNT, random) { effect, hookContext ->
                    effect.rerollCount(hookContext)
                }
            } else {
                0
            }
        val shadowbanCounters = player.counters[CounterId("shadowban")] ?: 0
        val extraTurnPenalty = player.counters[CounterId(EXTRA_TURN_DICE_PENALTY)] ?: 0
        val diceCount =
            workingState.turnDiceCount
                ?: (BASE_DICE + player.extraDice + diceBonus - shadowbanCounters - extraTurnPenalty).coerceAtLeast(1)
        val rollLimit = workingState.turnRollLimit ?: (MAX_ROLLS + player.extraRerolls + rerollBonus)
        if (workingState.rollNumber >= rollLimit) {
            return reject(state, action.playerId, ActionRejectionReason.INVALID_SELECTION)
        }

        if (!isFirstRoll && workingState.dice.size > diceCount) {
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
                workingState.dice
            }
        val updatedDice =
            dice.map { die ->
                val kept = die.id in action.keptDice
                if (kept) {
                    die.copy(isKept = true)
                } else {
                    die.copy(face = DiceFace.entries[random.nextInt(DiceFace.entries.size)], isKept = false)
                }
            }
        val rollNumber = workingState.rollNumber + 1
        var nextState =
            workingState.copy(
                phase = TurnPhase.ROLLING,
                dice = updatedDice,
                rollNumber = rollNumber,
                randomState = random.state,
                turnDiceCount = diceCount,
                turnRollLimit = rollLimit,
                resolveHooksAppliedForRoll = false,
            )
        val abilityDecisions =
            nextState.players
                .filter { !it.isSuspended }
                .flatMap { player ->
                    collectAbilityDecisions(
                        nextState,
                        player.id,
                        CardHookWindow.AFTER_DICE_ROLL,
                        random,
                    )
                }
        if (abilityDecisions.isNotEmpty()) {
            nextState =
                nextState.copy(
                    phase = TurnPhase.DECISION,
                    phaseAfterDecisions = TurnPhase.ROLLING,
                    pendingDecisions = nextState.pendingDecisions + abilityDecisions,
                    randomState = random.state,
                )
        }
        return GameTransition(
            state = nextState,
            events =
                startEvents + GameEvent.DiceRolled(action.playerId, rollNumber, updatedDice) +
                    abilityDecisions.map(GameEvent::DecisionQueued),
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

        val random = SplitMix64(state.randomState)
        var hookedState = state
        val hookEvents = mutableListOf<GameEvent>()
        if (!state.resolveHooksAppliedForRoll) {
            val intents =
                invokeOwnerHooks(
                    state,
                    action.playerId,
                    CardHookWindow.ON_RESOLVE,
                    random,
                ) { effect, hookContext -> effect.onResolve(hookContext) }
            val applied = applyCardIntents(state, intents, random, action.playerId)
            hookedState = applied.first.copy(resolveHooksAppliedForRoll = true)
            hookEvents += applied.second
        }
        val finished = finishIfNeeded(hookedState)
        if (finished.first.result != null) {
            return GameTransition(finished.first.copy(randomState = random.state), hookEvents + finished.second)
        }
        if (hookedState.players.any { it.id == action.playerId && it.isSuspended }) {
            val next = completeEndTurn(hookedState.copy(randomState = random.state), action.playerId)
            return GameTransition(next.state, hookEvents + next.events)
        }
        if (hookedState.pendingDecisions.isNotEmpty()) {
            return GameTransition(
                hookedState.copy(
                    phase = TurnPhase.DECISION,
                    phaseAfterDecisions = TurnPhase.RESOLVING,
                    randomState = random.state,
                ),
                hookEvents,
            )
        }
        val completed = completeRollResolution(hookedState, action.playerId, random)
        return GameTransition(completed.state, hookEvents + completed.events)
    }

    private fun completeRollResolution(
        state: GameState,
        playerId: PlayerId,
        random: SplitMix64,
    ): GameTransition {
        var nextState = state.copy(phase = TurnPhase.RESOLVING, randomState = random.state)
        val events = mutableListOf<GameEvent>()
        val actor =
            nextState.players.firstOrNull { it.id == playerId }
                ?: return reject(state, playerId, ActionRejectionReason.INVALID_SELECTION)
        if (actor.isSuspended) return completeEndTurn(nextState, playerId)
        for (face in listOf(DiceFace.ONE, DiceFace.TWO, DiceFace.THREE)) {
            val count = nextState.dice.count { it.face == face }
            if (count >= 3) {
                val score = face.ordinal + 1 + count - 3
                val changed =
                    changeResource(
                        nextState,
                        playerId,
                        PlayerResource.MANDATES,
                        score,
                        ResourceChangeReason.OTHER,
                    )
                nextState = changed.first
                events += changed.second
            }
        }

        if (nextState.players.any { it.id == playerId && it.mandates >= MANDATES_TO_WIN }) {
            val finished = finishIfNeeded(nextState)
            return GameTransition(finished.first, events + finished.second)
        }

        var likes = nextState.dice.count { it.face == DiceFace.LIKE }
        if (likes > 0) {
            val shadowbanCounters = actor.counters[CounterId("shadowban")] ?: 0
            val removeShadowban = minOf(likes, shadowbanCounters)
            if (removeShadowban > 0) {
                val intent = CardEffectIntent.ChangeCounter(playerId, CounterId("shadowban"), -removeShadowban)
                val applied = applyIntents(nextState, listOf(intent))
                nextState = applied.first
                events += applied.second
                likes -= removeShadowban
            }

            val cancelledCounters = nextState.players.first { it.id == playerId }.counters[CounterId("cancelled")] ?: 0
            val removeCancelled = minOf(likes, cancelledCounters)
            if (removeCancelled > 0) {
                val intent = CardEffectIntent.ChangeCounter(playerId, CounterId("cancelled"), -removeCancelled)
                val applied = applyIntents(nextState, listOf(intent))
                nextState = applied.first
                events += applied.second
                likes -= removeCancelled
            }
        }

        if (nextState.xitterPlayerId != playerId && likes > 0) {
            val currentActor = nextState.players.first { it.id == playerId }
            val actual = minOf(likes, currentActor.maxFollowers - currentActor.followers)
            if (actual > 0) {
                val healingBonus =
                    invokeNumericHooks(nextState, playerId, CardHookWindow.HEALING, random) { effect, ctx -> effect.modifyHealing(ctx) }
                val changed =
                    changeResource(
                        nextState,
                        playerId,
                        PlayerResource.FOLLOWERS,
                        actual + healingBonus,
                        ResourceChangeReason.LIKE,
                    )
                nextState = changed.first
                events += changed.second
            }
        }

        val characterFaces = nextState.dice.count { it.face == DiceFace.CHARACTERS }
        if (characterFaces > 0) {
            val charBonus =
                invokeNumericHooks(
                    nextState,
                    playerId,
                    CardHookWindow.GAIN_CHARACTERS,
                    random,
                ) { effect, ctx -> effect.modifyGainCharacters(ctx) }
            val changed =
                changeResource(
                    nextState,
                    playerId,
                    PlayerResource.CHARACTERS,
                    characterFaces * CHARACTERS_PER_FACE + charBonus,
                    ResourceChangeReason.CHARACTERS_FACE,
                )
            nextState = changed.first
            events += changed.second
        }

        val tweetCount = nextState.dice.count { it.face == DiceFace.TWEET }
        val tweetTransition = resolveTweets(nextState, playerId, tweetCount, random)
        nextState = tweetTransition.state
        events += tweetTransition.events

        if (nextState.players.any { it.id == playerId && it.isSuspended }) {
            val completed = completeEndTurn(nextState, playerId)
            return GameTransition(completed.state, events + completed.events)
        }

        if (nextState.xitterPlayerId == null && nextState.players.any { it.id == playerId && !it.isSuspended }) {
            val changed =
                changeResource(
                    nextState,
                    playerId,
                    PlayerResource.MANDATES,
                    1,
                    ResourceChangeReason.XITTER_ENTRY,
                )
            nextState = changed.first.copy(xitterPlayerId = playerId)
            events += changed.second
            events += GameEvent.MainCharacterChanged(null, playerId)
        }

        nextState =
            nextState.copy(
                phase = if (nextState.pendingDecisions.isEmpty()) TurnPhase.SHOP else TurnPhase.DECISION,
                phaseAfterDecisions = if (nextState.pendingDecisions.isEmpty()) null else TurnPhase.SHOP,
            )
        val ended = finishIfNeeded(nextState)
        nextState = ended.first
        events += ended.second
        if (nextState.result == null) {
            val offers =
                nextState.players
                    .filter { !it.isSuspended }
                    .flatMap { player ->
                        collectAbilityDecisions(nextState, player.id, CardHookWindow.AFTER_RESOLVE, random)
                    }
            if (offers.isNotEmpty()) {
                nextState =
                    nextState.copy(
                        phase = TurnPhase.DECISION,
                        phaseAfterDecisions = TurnPhase.SHOP,
                        pendingDecisions = nextState.pendingDecisions + offers,
                    )
                events += offers.map(GameEvent::DecisionQueued)
            }
        }
        return GameTransition(nextState.copy(randomState = random.state), events)
    }

    private fun resolveTweets(
        state: GameState,
        attackerId: PlayerId,
        tweetCount: Int,
        random: SplitMix64,
    ): GameTransition {
        if (state.result != null) return GameTransition(state, emptyList())
        val mainCharacter = state.xitterPlayerId
        val baseTargets =
            when {
                mainCharacter == null -> emptyList()
                mainCharacter == attackerId -> state.players.filter { it.id != attackerId && !it.isSuspended }.map { it.id }
                else -> listOf(mainCharacter)
            }
        val outgoing =
            invokeOwnerDamageHooks(
                state,
                attackerId,
                CardHookWindow.OUTGOING_DAMAGE,
                random,
                targetIds = baseTargets.toSet(),
                damage = tweetCount,
                outgoing = true,
            )
        val rawDamage = (tweetCount + outgoing.sumOf { it.bonusDamage }).coerceAtLeast(0)
        val targets =
            if (outgoing.any { it.hitsAllOpponents }) {
                state.players.filter { it.id != attackerId && !it.isSuspended }.map { it.id }
            } else {
                baseTargets
            }
        val targetDamage =
            targets.associateWith { targetId ->
                val perTarget = outgoing.sumOf { it.perTargetBonus[targetId] ?: 0 }
                val baseDamage = rawDamage + perTarget
                if (baseDamage <= 0) {
                    0
                } else {
                    val incoming =
                        invokeOwnerDamageHooks(
                            state,
                            targetId,
                            CardHookWindow.INCOMING_DAMAGE,
                            random,
                            targetIds = setOf(targetId),
                            damage = baseDamage,
                            outgoing = false,
                        )
                    if (incoming.any { it.preventDamage }) {
                        0
                    } else {
                        (baseDamage - incoming.sumOf { it.damageReduction }).coerceAtLeast(0)
                    }
                }
            }

        var nextState = state.copy(randomState = random.state)
        val events = mutableListOf<GameEvent>()
        var totalUnfollows = 0
        val damagedTargets = mutableListOf<PlayerId>()
        for ((targetId, damageToDeal) in targetDamage) {
            val target = nextState.players.firstOrNull { it.id == targetId } ?: continue
            val damage = minOf(damageToDeal, target.followers)
            if (damage > 0) {
                totalUnfollows += damage
                val intent = CardEffectIntent.ChangeResource(targetId, PlayerResource.FOLLOWERS, -damage, ResourceChangeReason.TWEET)
                val damageApplied = applyCardIntents(nextState, listOf(intent), random, attackerId, transferMainCharacter = false)
                nextState = damageApplied.first
                events += damageApplied.second

                val reactIntents =
                    invokeOwnerHooks(nextState, targetId, CardHookWindow.ON_DAMAGE_RECEIVED, random, damage = damage) { effect, ctx ->
                        effect.onDamageReceived(ctx)
                    }
                val reactApplied = applyCardIntents(nextState, reactIntents, random, attackerId, transferMainCharacter = false)
                nextState = reactApplied.first
                events += reactApplied.second
                damagedTargets += targetId
            }
        }

        if (damagedTargets.isNotEmpty()) {
            val intents =
                invokeOwnerHooks(
                    nextState,
                    attackerId,
                    CardHookWindow.DAMAGE_DEALT,
                    random,
                    targetPlayerIds = damagedTargets.toSet(),
                ) { effect, hookContext -> effect.onDamageDealt(hookContext) }
            val applied =
                applyCardIntents(
                    nextState,
                    intents,
                    random,
                    attackerId,
                    transferMainCharacter = false,
                )
            nextState = applied.first
            events += applied.second
        }

        val currentStats = nextState.matchStats[attackerId] ?: MatchStats()
        nextState =
            nextState.copy(
                turnUnfollowsDealt = nextState.turnUnfollowsDealt + totalUnfollows,
                matchStats =
                    nextState.matchStats + (
                        attackerId to
                            currentStats.copy(
                                tweetsSent = currentStats.tweetsSent + tweetCount,
                                unfollowsDealt = currentStats.unfollowsDealt + totalUnfollows,
                            )
                    ),
            )

        if (mainCharacter != null && targets.contains(mainCharacter) && (targetDamage[mainCharacter] ?: 0) > 0) {
            val mainCharacterState = nextState.players.first { it.id == mainCharacter }
            val attackerAlive = nextState.players.any { it.id == attackerId && !it.isSuspended }
            if (mainCharacterState.isSuspended) {
                val newOwner = if (attackerAlive) attackerId else null
                nextState = nextState.copy(xitterPlayerId = newOwner)
                events += GameEvent.MainCharacterChanged(mainCharacter, newOwner)
                // Taking control of Xitter scores its mandate however the spot came free.
                if (newOwner != null) {
                    val entry = changeResource(nextState, newOwner, PlayerResource.MANDATES, 1, ResourceChangeReason.XITTER_ENTRY)
                    nextState = entry.first
                    events += entry.second
                }
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
        val suspendedOwner = nextState.xitterPlayerId?.takeIf { id -> nextState.players.any { it.id == id && it.isSuspended } }
        if (suspendedOwner != null) {
            nextState = nextState.copy(xitterPlayerId = null)
            events += GameEvent.MainCharacterChanged(suspendedOwner, null)
        }
        return GameTransition(nextState.copy(randomState = random.state), events)
    }

    private fun invokeOwnerHooks(
        state: GameState,
        ownerId: PlayerId,
        window: CardHookWindow,
        random: SplitMix64,
        purchasedCardId: CardId? = null,
        actingPlayerId: PlayerId? = null,
        targetPlayerId: PlayerId? = null,
        targetPlayerIds: Set<PlayerId> = emptySet(),
        damage: Int = 0,
        eliminatedPlayerId: PlayerId? = null,
        invoke: (CardEffect, CardHookContext) -> List<CardEffectIntent>,
    ): List<CardEffectIntent> {
        val owner = state.players.firstOrNull { it.id == ownerId } ?: return emptyList()
        return cardEffects.ordered(state.effectivePolicyIds(owner)).flatMap { effect ->
            invoke(
                effect,
                hookContext(
                    state,
                    ownerId,
                    effect,
                    window,
                    random,
                    purchasedCardId = purchasedCardId,
                    actingPlayerId = actingPlayerId,
                    targetPlayerId = targetPlayerId,
                    targetPlayerIds = targetPlayerIds,
                    damage = damage,
                    eliminatedPlayerId = eliminatedPlayerId,
                ),
            )
        }
    }

    private fun invokeOwnerSetHooks(
        state: GameState,
        ownerIds: Collection<PlayerId>,
        window: CardHookWindow,
        random: SplitMix64,
        invoke: (CardEffect, CardHookContext) -> List<CardEffectIntent>,
    ): List<CardEffectIntent> {
        val intents = mutableListOf<CardEffectIntent>()
        for (eliminatedId in ownerIds.distinct()) {
            for (observer in state.players) {
                intents +=
                    invokeOwnerHooks(
                        state,
                        observer.id,
                        window,
                        random,
                        eliminatedPlayerId = eliminatedId,
                        invoke = invoke,
                    )
            }
        }
        return intents
    }

    private fun invokeNumericHooks(
        state: GameState,
        ownerId: PlayerId,
        window: CardHookWindow,
        random: SplitMix64,
        invoke: (CardEffect, CardHookContext) -> Int,
    ): Int {
        val owner = state.players.firstOrNull { it.id == ownerId } ?: return 0
        return cardEffects.ordered(state.effectivePolicyIds(owner)).sumOf { effect ->
            invoke(effect, hookContext(state, ownerId, effect, window, random)).coerceAtLeast(0)
        }
    }

    private fun invokeOwnerDamageHooks(
        state: GameState,
        ownerId: PlayerId,
        window: CardHookWindow,
        random: SplitMix64,
        targetIds: Set<PlayerId>,
        damage: Int,
        outgoing: Boolean,
    ): List<com.kingofxitter.engine.model.DamageAdjustment> {
        val owner = state.players.firstOrNull { it.id == ownerId } ?: return emptyList()
        return cardEffects.ordered(state.effectivePolicyIds(owner)).map { effect ->
            val context =
                hookContext(
                    state,
                    ownerId,
                    effect,
                    window,
                    random,
                    targetPlayerIds = targetIds,
                    damage = damage,
                    actingPlayerId = if (outgoing) ownerId else state.currentPlayerId,
                )
            if (outgoing) effect.modifyOutgoingDamage(context) else effect.modifyIncomingDamage(context)
        }
    }

    private fun hookContext(
        state: GameState,
        ownerId: PlayerId,
        effect: CardEffect,
        window: CardHookWindow,
        random: SplitMix64,
        actingPlayerId: PlayerId? = null,
        targetPlayerId: PlayerId? = null,
        targetPlayerIds: Set<PlayerId> = emptySet(),
        damage: Int = 0,
        purchasedCardId: CardId? = null,
        eliminatedPlayerId: PlayerId? = null,
    ) = CardHookContext(
        state = state,
        ownerId = ownerId,
        sourceCardId = effect.cardId,
        window = window,
        actingPlayerId = actingPlayerId,
        targetPlayerId = targetPlayerId,
        targetPlayerIds = targetPlayerIds,
        damage = damage,
        purchasedCardId = purchasedCardId,
        eliminatedPlayerId = eliminatedPlayerId,
        random = random,
    )

    private fun collectAbilityDecisions(
        state: GameState,
        ownerId: PlayerId,
        window: CardHookWindow,
        random: SplitMix64,
    ): List<PendingDecision> {
        val owner = state.players.firstOrNull { it.id == ownerId } ?: return emptyList()
        val decisions = mutableListOf<PendingDecision>()
        for (effect in cardEffects.ordered(state.effectivePolicyIds(owner))) {
            val offers = effect.activatedAbilities(hookContext(state, ownerId, effect, window, random))
            for (offer in offers) {
                val validOptions =
                    offer.options.filter { option ->
                        val action = option.action as? Action.ActivateAbility
                        action != null &&
                            action.playerId == ownerId &&
                            action.cardId == effect.cardId &&
                            (action.abilityId == offer.abilityId || action.abilityId.startsWith("${offer.abilityId}:"))
                    }
                if (validOptions.isEmpty() || validOptions.size != offer.options.size) continue
                val decision =
                    PendingDecision(
                        id =
                            DecisionId(
                                "ability-${state.turnNumber}-${state.rollNumber}-$window-${effect.cardId.value}-${offer.abilityId}",
                            ),
                        playerId = ownerId,
                        kind = DecisionKind.ACTIVATE_ABILITY,
                        options = validOptions + DecisionOption(DecisionOptionId("decline"), Action.DeclineAbility(ownerId)),
                        promptKey = offer.promptKey,
                    )
                if (decisions.none { it.id == decision.id }) decisions += decision
            }
        }
        return decisions
    }

    private fun applyCardIntents(
        initialState: GameState,
        intents: List<CardEffectIntent>,
        random: SplitMix64,
        actingPlayerId: PlayerId,
        previouslyEliminated: Set<PlayerId> = emptySet(),
        transferMainCharacter: Boolean = true,
    ): Pair<GameState, List<GameEvent>> {
        val applied = applyIntents(initialState, intents, random)
        var state = applied.first
        val events = applied.second.toMutableList()
        val handled = previouslyEliminated.toMutableSet()

        fun unhandledEliminations(newEvents: List<GameEvent>): Set<PlayerId> =
            newEvents
                .filterIsInstance<GameEvent.ResourceChanged>()
                .filter {
                    it.resource == PlayerResource.FOLLOWERS && it.amount < 0 && it.totalAfter == 0
                }.map { it.playerId }
                .filterNot(handled::contains)
                .toSet()

        var eliminated = unhandledEliminations(applied.second)
        val newlyHandled = mutableSetOf<PlayerId>()
        while (eliminated.isNotEmpty()) {
            handled += eliminated
            newlyHandled += eliminated
            val eliminationIntents =
                invokeOwnerSetHooks(
                    state,
                    eliminated,
                    CardHookWindow.ELIMINATED,
                    random,
                ) { effect, hookContext -> effect.onEliminated(hookContext) }
            val eliminationChanges = applyIntents(state, eliminationIntents)
            state = eliminationChanges.first
            events += eliminationChanges.second
            eliminated = unhandledEliminations(eliminationChanges.second)
        }

        val stillSuspended =
            handled.filterTo(mutableSetOf()) { playerId ->
                state.players.any { it.id == playerId && it.isSuspended }
            }
        stillSuspended
            .filter { it in newlyHandled }
            .forEach { events += GameEvent.PlayerSuspended(it) }
        if (transferMainCharacter && state.xitterPlayerId in stillSuspended) {
            val newOwner = actingPlayerId.takeIf { id -> state.players.any { it.id == id && !it.isSuspended } }
            val previousOwner = state.xitterPlayerId
            state = state.copy(xitterPlayerId = newOwner)
            events += GameEvent.MainCharacterChanged(previousOwner, newOwner)
        }
        state =
            state.copy(
                pendingDecisions =
                    state.pendingDecisions.filter { decision ->
                        state.players.any {
                            it.id == decision.playerId &&
                                !it.isSuspended
                        }
                    },
            )
        return state to events
    }

    private fun combineIntents(intents: List<CardEffectIntent>): List<CardEffectIntent> {
        fun sum(values: List<Int>): Int = values.sumOf { it.toLong() }.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()
        val resources =
            intents
                .filterIsInstance<CardEffectIntent.ChangeResource>()
                .groupBy { it.playerId to it.resource }
                .values
                .map { group -> group.first().copy(amount = sum(group.map { it.amount })) }
                .sortedBy { it.resource.ordinal }
        val limits =
            intents
                .filterIsInstance<CardEffectIntent.ChangeFollowerLimit>()
                .groupBy { it.playerId }
                .values
                .map { group -> group.first().copy(amount = sum(group.map { it.amount })) }
        val counters =
            intents
                .filterIsInstance<CardEffectIntent.ChangeCounter>()
                .groupBy { it.playerId to it.counterId }
                .values
                .map { group -> group.first().copy(amount = sum(group.map { it.amount })) }
        val dice =
            intents
                .filterIsInstance<CardEffectIntent.ChangeDiceCount>()
                .groupBy { it.playerId }
                .values
                .map { group -> group.first().copy(amount = sum(group.map { it.amount })) }
        val rerolls =
            intents
                .filterIsInstance<CardEffectIntent.ChangeRerollCount>()
                .groupBy { it.playerId to it.thisTurnOnly }
                .values
                .map { group -> group.first().copy(amount = sum(group.map { it.amount })) }
        val turns =
            intents
                .filterIsInstance<CardEffectIntent.ChangeExtraTurns>()
                .groupBy { it.playerId }
                .values
                .map { group -> group.first().copy(amount = sum(group.map { it.amount })) }
        return limits + resources + counters + dice + rerolls + turns +
            intents.filter {
                it is CardEffectIntent.ChangeDie ||
                    it is CardEffectIntent.ChangeMainCharacter ||
                    it is CardEffectIntent.QueueDecision ||
                    it is CardEffectIntent.DiscardPolicy ||
                    it is CardEffectIntent.SetCopiedPolicy ||
                    it is CardEffectIntent.TransferPolicy ||
                    it is CardEffectIntent.BuyTopCard ||
                    it is CardEffectIntent.ConsumeDie
            }
    }

    private fun applyIntents(
        initialState: GameState,
        intents: List<CardEffectIntent>,
        random: SplitMix64? = null,
    ): Pair<GameState, List<GameEvent>> {
        var state = initialState
        val events = mutableListOf<GameEvent>()
        for (intent in combineIntents(intents)) {
            when (intent) {
                is CardEffectIntent.ChangeResource -> {
                    if (state.players.any { it.id == intent.playerId }) {
                        var amount = intent.amount
                        if (intent.resource == PlayerResource.FOLLOWERS && amount > 0 && random != null) {
                            amount +=
                                invokeNumericHooks(
                                    state,
                                    intent.playerId,
                                    CardHookWindow.HEALING,
                                    random,
                                ) { effect, ctx -> effect.modifyHealing(ctx) }
                        }
                        if (intent.resource == PlayerResource.CHARACTERS && amount > 0 && random != null) {
                            amount +=
                                invokeNumericHooks(
                                    state,
                                    intent.playerId,
                                    CardHookWindow.GAIN_CHARACTERS,
                                    random,
                                ) { effect, ctx -> effect.modifyGainCharacters(ctx) }
                        }
                        val changed = changeResource(state, intent.playerId, intent.resource, amount, intent.reason)
                        state = changed.first
                        events += changed.second
                    }
                }

                is CardEffectIntent.ChangeFollowerLimit -> {
                    val index = state.players.indexOfFirst { it.id == intent.playerId }
                    if (index >= 0) {
                        val previous = state.players[index]
                        val newLimit =
                            (previous.maxFollowers.toLong() + intent.amount)
                                .coerceAtLeast(
                                    1L,
                                ).coerceAtMost(Int.MAX_VALUE.toLong())
                                .toInt()
                        val updated = previous.copy(maxFollowers = newLimit, followers = minOf(previous.followers, newLimit))
                        state = state.copy(players = state.players.toMutableList().also { it[index] = updated })
                        val delta = updated.followers - previous.followers
                        if (delta != 0) {
                            events +=
                                GameEvent.ResourceChanged(
                                    intent.playerId,
                                    PlayerResource.FOLLOWERS,
                                    delta,
                                    updated.followers,
                                    ResourceChangeReason.POWER_UP,
                                )
                        }
                    }
                }

                is CardEffectIntent.ChangeDie -> {
                    if (intent.playerId == state.currentPlayerId && state.dice.any { it.id == intent.dieId }) {
                        state =
                            state.copy(
                                dice =
                                    state.dice.map { die ->
                                        if (die.id == intent.dieId) die.copy(face = intent.face) else die
                                    },
                            )
                        events += GameEvent.DiceChanged(intent.playerId, state.dice)
                    }
                }

                is CardEffectIntent.ConsumeDie -> {
                    if (intent.playerId == state.currentPlayerId && state.dice.any { it.id == intent.dieId }) {
                        state =
                            state.copy(
                                dice = state.dice.filter { it.id != intent.dieId },
                            )
                        events += GameEvent.DiceChanged(intent.playerId, state.dice)
                    }
                }

                is CardEffectIntent.ChangeCounter -> {
                    val index = state.players.indexOfFirst { it.id == intent.playerId }
                    if (index >= 0) {
                        val player = state.players[index]
                        val previous = player.counters[intent.counterId] ?: 0
                        val updatedCount =
                            (previous.toLong() + intent.amount)
                                .coerceAtLeast(
                                    0L,
                                ).coerceAtMost(Int.MAX_VALUE.toLong())
                                .toInt()
                        val counters = player.counters.toMutableMap()
                        if (updatedCount == 0) counters.remove(intent.counterId) else counters[intent.counterId] = updatedCount
                        val updatedPlayer = player.copy(counters = counters)
                        state = state.copy(players = state.players.toMutableList().also { it[index] = updatedPlayer })
                        events += GameEvent.CounterChanged(intent.playerId, intent.counterId, updatedCount - previous, updatedCount)
                    }
                }

                is CardEffectIntent.ChangeDiceCount -> {
                    val index = state.players.indexOfFirst { it.id == intent.playerId }
                    if (index >= 0) {
                        val player = state.players[index]
                        val value =
                            (player.extraDice.toLong() + intent.amount)
                                .coerceAtLeast(
                                    0L,
                                ).coerceAtMost(Int.MAX_VALUE.toLong())
                                .toInt()
                        state = state.copy(players = state.players.toMutableList().also { it[index] = player.copy(extraDice = value) })
                    }
                }

                is CardEffectIntent.ChangeRerollCount -> {
                    val index = state.players.indexOfFirst { it.id == intent.playerId }
                    if (index >= 0 && intent.thisTurnOnly) {
                        if (intent.playerId == state.currentPlayerId) {
                            state =
                                state.copy(
                                    turnRollLimit =
                                        state.turnRollLimit?.let { (it + intent.amount).coerceAtLeast(state.rollNumber) },
                                )
                        }
                    } else if (index >= 0) {
                        val player = state.players[index]
                        val value =
                            (player.extraRerolls.toLong() + intent.amount)
                                .coerceAtLeast(
                                    0L,
                                ).coerceAtMost(Int.MAX_VALUE.toLong())
                                .toInt()
                        state =
                            state.copy(
                                players = state.players.toMutableList().also { it[index] = player.copy(extraRerolls = value) },
                                turnRollLimit =
                                    if (intent.playerId == state.currentPlayerId) {
                                        state.turnRollLimit?.let { (it + value - player.extraRerolls).coerceAtLeast(state.rollNumber) }
                                    } else {
                                        state.turnRollLimit
                                    },
                            )
                    }
                }

                is CardEffectIntent.ChangeExtraTurns -> {
                    val index = state.players.indexOfFirst { it.id == intent.playerId }
                    if (index >= 0) {
                        val player = state.players[index]
                        val value =
                            (player.extraTurns.toLong() + intent.amount)
                                .coerceAtLeast(
                                    0L,
                                ).coerceAtMost(Int.MAX_VALUE.toLong())
                                .toInt()
                        state = state.copy(players = state.players.toMutableList().also { it[index] = player.copy(extraTurns = value) })
                        events += GameEvent.ExtraTurnsChanged(intent.playerId, value - player.extraTurns, value)
                    }
                }

                is CardEffectIntent.ChangeMainCharacter -> {
                    val target = intent.playerId?.let { id -> state.players.firstOrNull { it.id == id && !it.isSuspended } }
                    if (intent.playerId == null || target != null) {
                        if (state.xitterPlayerId != intent.playerId) {
                            val previous = state.xitterPlayerId
                            state = state.copy(xitterPlayerId = intent.playerId)
                            events += GameEvent.MainCharacterChanged(previous, intent.playerId)
                        }
                    }
                }

                is CardEffectIntent.QueueDecision -> {
                    if (state.players.any { it.id == intent.decision.playerId } &&
                        intent.decision.options.isNotEmpty() &&
                        state.pendingDecisions.none { it.id == intent.decision.id }
                    ) {
                        state = state.copy(pendingDecisions = state.pendingDecisions + intent.decision)
                        events += GameEvent.DecisionQueued(intent.decision)
                    }
                }

                is CardEffectIntent.DiscardPolicy -> {
                    val index = state.players.indexOfFirst { it.id == intent.playerId }
                    if (index >= 0) {
                        val player = state.players[index]
                        if (intent.cardId in player.policyIds) {
                            state =
                                state.copy(
                                    players =
                                        state.players.toMutableList().also {
                                            it[index] = player.copy(policyIds = player.policyIds - intent.cardId)
                                        },
                                    discardPileCardIds = state.discardPileCardIds + intent.cardId,
                                )
                        }
                    }
                }

                is CardEffectIntent.SetCopiedPolicy -> {
                    val index = state.players.indexOfFirst { it.id == intent.playerId }
                    if (index >= 0) {
                        state =
                            state.copy(
                                players =
                                    state.players.toMutableList().also {
                                        it[index] =
                                            state.players[index].copy(copiedPolicyId = intent.policyId)
                                    },
                            )
                    }
                }

                is CardEffectIntent.TransferPolicy -> {
                    val fromIndex = state.players.indexOfFirst { it.id == intent.fromPlayerId }
                    val toIndex = state.players.indexOfFirst { it.id == intent.toPlayerId }
                    if (fromIndex >= 0 && toIndex >= 0) {
                        val fromPlayer = state.players[fromIndex]
                        val toPlayer = state.players[toIndex]
                        if (intent.policyId in fromPlayer.policyIds) {
                            val updatedFrom = fromPlayer.copy(policyIds = fromPlayer.policyIds - intent.policyId)
                            val updatedTo = toPlayer.copy(policyIds = toPlayer.policyIds + intent.policyId)
                            state =
                                state.copy(
                                    players =
                                        state.players.toMutableList().also {
                                            it[fromIndex] = updatedFrom
                                            it[toIndex] = updatedTo
                                        },
                                )
                        }
                    }
                }

                is CardEffectIntent.BuyTopCard -> {
                    val index = state.players.indexOfFirst { it.id == intent.playerId }
                    if (index >= 0 && state.drawPileCardIds.isNotEmpty()) {
                        val player = state.players[index]
                        if (player.characters >= intent.cost) {
                            val cardId = state.drawPileCardIds.first()
                            val drawPile = state.drawPileCardIds.drop(1)
                            val paidPlayer = player.copy(characters = player.characters - intent.cost)
                            val updatedPolicyIds = if (!intent.isExecutiveOrder) paidPlayer.policyIds + cardId else paidPlayer.policyIds
                            val fullyUpdatedPlayer = paidPlayer.copy(policyIds = updatedPolicyIds)
                            state =
                                state.copy(
                                    players = state.players.toMutableList().also { it[index] = fullyUpdatedPlayer },
                                    drawPileCardIds = drawPile,
                                    discardPileCardIds =
                                        if (intent.isExecutiveOrder) state.discardPileCardIds + cardId else state.discardPileCardIds,
                                )
                            events +=
                                GameEvent.ResourceChanged(
                                    playerId = intent.playerId,
                                    resource = PlayerResource.CHARACTERS,
                                    amount = -intent.cost,
                                    totalAfter = paidPlayer.characters,
                                    reason = ResourceChangeReason.POWER_UP,
                                )
                            events += GameEvent.PowerUpBought(intent.playerId, cardId)

                            val purchased = this.dispatchOnBuy(state, intent.playerId, cardId)
                            state = purchased.state
                            events += purchased.events
                        }
                    }
                }
            }
        }
        return state to events
    }

    private fun activateAbility(
        state: GameState,
        action: Action.ActivateAbility,
        offeredByDecision: Boolean = false,
    ): GameTransition {
        if (!offeredByDecision || state.result != null) {
            return reject(state, action.playerId, ActionRejectionReason.OPTION_NOT_AVAILABLE)
        }
        val player =
            state.players.firstOrNull { it.id == action.playerId }
                ?: return reject(state, action.playerId, ActionRejectionReason.INVALID_SELECTION)
        if (action.cardId !in state.effectivePolicyIds(player)) {
            return reject(state, action.playerId, ActionRejectionReason.INVALID_SELECTION)
        }
        val effect =
            cardEffects[action.cardId]
                ?: return reject(state, action.playerId, ActionRejectionReason.INVALID_SELECTION)
        val random = SplitMix64(state.randomState)
        val context =
            hookContext(
                state,
                action.playerId,
                effect,
                CardHookWindow.ACTIVATED_ABILITY,
                random,
                actingPlayerId = state.currentPlayerId,
                targetPlayerId = action.targetPlayerId,
            )
        val intents = effect.activate(context, action)
        val unaffordable =
            intents
                .filterIsInstance<CardEffectIntent.ChangeResource>()
                .filter { it.resource == PlayerResource.CHARACTERS }
                .groupBy { it.playerId }
                .any { (id, changes) ->
                    val balance = state.players.firstOrNull { it.id == id }?.characters ?: 0
                    balance.toLong() + changes.sumOf { it.amount.toLong() } < 0
                }
        if (unaffordable) return reject(state, action.playerId, ActionRejectionReason.OPTION_NOT_AVAILABLE)
        val applied = applyCardIntents(state, intents, random, action.playerId)
        return GameTransition(applied.first.copy(randomState = random.state), applied.second)
    }

    private fun chooseOfferedAction(
        state: GameState,
        action: Action,
    ): GameTransition {
        val decision =
            state.pendingDecisions.firstOrNull()
                ?: return reject(state, action.playerId, ActionRejectionReason.OPTION_NOT_AVAILABLE)
        val option =
            decision.options.firstOrNull { it.action == action }
                ?: return reject(state, action.playerId, ActionRejectionReason.OPTION_NOT_AVAILABLE)
        return chooseDecisionOption(state, Action.ChooseDecisionOption(action.playerId, decision.id, option.id))
    }

    private fun awaitDecisions(state: GameState): GameState =
        if (state.result == null && state.pendingDecisions.isNotEmpty() && state.phase != TurnPhase.DECISION) {
            state.copy(phase = TurnPhase.DECISION, phaseAfterDecisions = state.phase)
        } else {
            state
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
        val previousOwner = state.xitterPlayerId
        // The attacker takes control of Xitter, which scores its mandate as entering an empty one does.
        val entry =
            changeResource(
                state.copy(xitterPlayerId = attacker.id),
                attacker.id,
                PlayerResource.MANDATES,
                1,
                ResourceChangeReason.XITTER_ENTRY,
            )
        var nextState = entry.first
        val events = mutableListOf<GameEvent>(GameEvent.MainCharacterChanged(previousOwner, attacker.id))
        events += entry.second

        val random = SplitMix64(state.randomState)
        val intents =
            invokeOwnerHooks(
                nextState,
                action.playerId,
                CardHookWindow.ON_LOG_OFF,
                random,
                targetPlayerId = attacker.id,
                actingPlayerId = action.playerId,
            ) { effect, ctx -> effect.onLogOff(ctx) }
        val applied = applyCardIntents(nextState, intents, random, action.playerId)
        return GameTransition(applied.first.copy(randomState = random.state), events + applied.second)
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
        val applied =
            when (val selected = option.action) {
                is Action.ActivateAbility -> activateAbility(preparedState, selected, offeredByDecision = true)
                is Action.DeclineAbility -> GameTransition(preparedState, emptyList())
                is Action.EndTurn ->
                    if (resumePhase ==
                        TurnPhase.END_TURN
                    ) {
                        GameTransition(preparedState, emptyList())
                    } else {
                        endTurn(preparedState, selected)
                    }
                is Action.StayInXitter -> stayInXitter(preparedState, selected)
                is Action.LogOffXitter -> logOffXitter(preparedState, selected)
                else -> reduce(preparedState, selected)
            }
        if (applied.events.any { it is GameEvent.ActionRejected }) {
            return GameTransition(
                state = state,
                events = applied.events,
            )
        }
        val finished = finishIfNeeded(applied.state)
        val events = listOf(GameEvent.DecisionResolved(action.decisionId, action.optionId)) + applied.events + finished.second
        if (finished.first.pendingDecisions.isNotEmpty() || finished.first.result != null) {
            return GameTransition(awaitDecisions(finished.first), events)
        }
        // A decision can suspend the player whose turn it is (logging off onto Home Turf, for
        // one). Nobody could act for them, so their turn ends here as it does after a roll.
        if (finished.first.players.any { it.id == state.currentPlayerId && it.isSuspended }) {
            val completed = completeEndTurn(finished.first.copy(phaseAfterDecisions = null), state.currentPlayerId)
            return GameTransition(completed.state, events + completed.events)
        }
        if (finished.first.phase != resumePhase) return GameTransition(finished.first, events)
        val continuation = if (resumePhase == TurnPhase.DECISION) state.phaseAfterDecisions ?: TurnPhase.END_TURN else resumePhase
        val resumed = finished.first.copy(phase = continuation, phaseAfterDecisions = null)
        return when (continuation) {
            TurnPhase.RESOLVING -> {
                val random = SplitMix64(finished.first.randomState)
                val completed = completeRollResolution(resumed, state.currentPlayerId, random)
                GameTransition(completed.state, events + completed.events)
            }
            TurnPhase.END_TURN -> {
                val completed = completeEndTurn(resumed, state.currentPlayerId)
                GameTransition(completed.state, events + completed.events)
            }
            else -> GameTransition(resumed, events)
        }
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
        if (state.phase !in setOf(TurnPhase.SHOP, TurnPhase.END_TURN) || state.pendingDecisions.isNotEmpty()) {
            return reject(state, action.playerId, ActionRejectionReason.WRONG_PHASE)
        }
        if (state.players.none { it.id == action.playerId }) {
            return reject(state, action.playerId, ActionRejectionReason.INVALID_SELECTION)
        }
        val random = SplitMix64(state.randomState)
        val endingPlayer = state.players.first { it.id == action.playerId }
        val cancelledCounters = endingPlayer.counters[CounterId("cancelled")] ?: 0
        val counterIntents =
            if (cancelledCounters > 0) {
                listOf(
                    CardEffectIntent.ChangeResource(
                        action.playerId,
                        PlayerResource.FOLLOWERS,
                        -cancelledCounters,
                        ResourceChangeReason.OTHER,
                    ),
                )
            } else {
                emptyList()
            }

        val intents =
            invokeOwnerHooks(
                state.copy(phase = TurnPhase.END_TURN),
                action.playerId,
                CardHookWindow.TURN_END,
                random,
            ) { effect, hookContext -> effect.onTurnEnd(hookContext) }
        val applied = applyCardIntents(state, counterIntents + intents, random, action.playerId)
        val endState = applied.first.copy(randomState = random.state)
        val events = applied.second
        val completed = completeEndTurn(endState, action.playerId)
        return GameTransition(completed.state, events + completed.events)
    }

    private fun completeEndTurn(
        state: GameState,
        endingPlayerId: PlayerId,
    ): GameTransition {
        val turnEnded = GameEvent.TurnEnded(endingPlayerId, state.turnNumber)
        val finished = finishIfNeeded(state)
        if (finished.first.result != null) {
            return GameTransition(finished.first, listOf(turnEnded) + finished.second)
        }

        val livingDecisions =
            state.pendingDecisions.filter { decision ->
                state.players.any { it.id == decision.playerId && !it.isSuspended }
            }
        if (livingDecisions.isNotEmpty()) {
            return GameTransition(
                state.copy(phase = TurnPhase.DECISION, phaseAfterDecisions = TurnPhase.END_TURN, pendingDecisions = livingDecisions),
                emptyList(),
            )
        }
        val endingPlayer =
            state.players.firstOrNull { it.id == endingPlayerId }
                ?: return reject(state, endingPlayerId, ActionRejectionReason.INVALID_SELECTION)
        var startEvents = emptyList<GameEvent>()
        val takesExtraTurn = !endingPlayer.isSuspended && endingPlayer.extraTurns > 0
        var afterExtra = state
        if (takesExtraTurn) {
            val changed = applyIntents(state, listOf(CardEffectIntent.ChangeExtraTurns(endingPlayerId, -1)))
            afterExtra = changed.first
            startEvents = changed.second
        }
        val livingPlayers = afterExtra.players.filterNot { it.isSuspended }
        if (livingPlayers.isEmpty()) {
            val noWinner = finishIfNeeded(afterExtra)
            return GameTransition(noWinner.first, listOf(turnEnded) + startEvents + noWinner.second)
        }
        val chainEnded =
            if (takesExtraTurn) {
                afterExtra
            } else {
                clearCounter(afterExtra, endingPlayerId, CounterId(EXTRA_TURN_DICE_PENALTY))
            }
        afterExtra = chainEnded
        val nextPlayer =
            if (takesExtraTurn) {
                afterExtra.players.first { it.id == endingPlayerId }
            } else {
                val all = afterExtra.players
                val oldIndex = all.indexOfFirst { it.id == endingPlayerId }
                (1..all.size)
                    .asSequence()
                    .map { offset -> all[(oldIndex + offset) % all.size] }
                    .firstOrNull { !it.isSuspended }
                    ?: livingPlayers.first()
            }
        val nextTurnNumber = afterExtra.turnNumber + 1
        val nextState =
            afterExtra.copy(
                currentPlayerId = nextPlayer.id,
                phase = TurnPhase.TURN_START,
                turnNumber = nextTurnNumber,
                dice = emptyList(),
                rollNumber = 0,
                phaseAfterDecisions = null,
                pendingDecisions = emptyList(),
                turnStartHooksApplied = false,
                resolveHooksAppliedForRoll = false,
                turnDiceCount = null,
                turnRollLimit = null,
                turnUnfollowsDealt = 0,
            )
        return GameTransition(
            nextState,
            listOf(turnEnded) + startEvents + GameEvent.TurnStarted(nextPlayer.id, nextTurnNumber),
        )
    }

    private fun clearCounter(
        state: GameState,
        playerId: PlayerId,
        counterId: CounterId,
    ): GameState =
        state.copy(
            players =
                state.players.map {
                    if (it.id == playerId && counterId in it.counters) it.copy(counters = it.counters - counterId) else it
                },
        )

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
        val maximum =
            when (resource) {
                PlayerResource.FOLLOWERS -> player.maxFollowers
                PlayerResource.CHARACTERS -> state.characterCap ?: Int.MAX_VALUE
                PlayerResource.MANDATES -> Int.MAX_VALUE
            }
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
        return state.copy(phase = TurnPhase.FINISHED, result = result, pendingDecisions = emptyList(), phaseAfterDecisions = null) to
            listOf(GameEvent.MatchEnded(result))
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
