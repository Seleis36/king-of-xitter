package com.kingofxitter.engine

import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.ActionRejectionReason
import com.kingofxitter.engine.model.CardEffect
import com.kingofxitter.engine.model.CardEffectIntent
import com.kingofxitter.engine.model.CardEffectRegistry
import com.kingofxitter.engine.model.CardHookContext
import com.kingofxitter.engine.model.CardHookWindow
import com.kingofxitter.engine.model.CardId
import com.kingofxitter.engine.model.DecisionId
import com.kingofxitter.engine.model.DecisionKind
import com.kingofxitter.engine.model.DecisionOption
import com.kingofxitter.engine.model.DecisionOptionId
import com.kingofxitter.engine.model.DiceFace
import com.kingofxitter.engine.model.Die
import com.kingofxitter.engine.model.DieId
import com.kingofxitter.engine.model.GameEvent
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.HookRandom
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
            is Action.StayInXitter -> stayInXitter(state, action)
            is Action.LogOffXitter -> logOffXitter(state, action)
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
        val applied = applyIntents(state, intents)
        return GameTransition(applied.first.copy(randomState = random.state), applied.second)
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
            val onStart =
                invokeOwnerHooks(
                    workingState,
                    action.playerId,
                    CardHookWindow.TURN_START,
                    random,
                ) { effect, hookContext -> effect.onTurnStart(hookContext) }
            val appliedStart = applyIntents(workingState, onStart)
            workingState = appliedStart.first
            startEvents += appliedStart.second
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
        val diceCount = workingState.turnDiceCount ?: (BASE_DICE + player.extraDice + diceBonus)
        val rollLimit = workingState.turnRollLimit ?: (MAX_ROLLS + player.extraRerolls + rerollBonus)
        if (workingState.rollNumber >= rollLimit) {
            return reject(state, action.playerId, ActionRejectionReason.INVALID_SELECTION)
        }

        if (!isFirstRoll && workingState.dice.size != diceCount) {
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
            collectAbilityDecisions(
                nextState,
                action.playerId,
                CardHookWindow.AFTER_DICE_ROLL,
                random,
            )
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
            val applied = applyIntents(state, intents)
            hookedState = applied.first.copy(resolveHooksAppliedForRoll = true)
            hookEvents += applied.second
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

        val likes = nextState.dice.count { it.face == DiceFace.LIKE }
        if (nextState.xitterPlayerId != playerId && likes > 0) {
            val actual = minOf(likes, actor.maxFollowers - actor.followers)
            if (actual > 0) {
                val changed =
                    changeResource(
                        nextState,
                        playerId,
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
                    playerId,
                    PlayerResource.CHARACTERS,
                    characterFaces * CHARACTERS_PER_FACE,
                    ResourceChangeReason.CHARACTERS_FACE,
                )
            nextState = changed.first
            events += changed.second
        }

        val tweetCount = nextState.dice.count { it.face == DiceFace.TWEET }
        val tweetTransition = resolveTweets(nextState, playerId, tweetCount, random)
        nextState = tweetTransition.state
        events += tweetTransition.events

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
            val offers = collectAbilityDecisions(nextState, playerId, CardHookWindow.AFTER_RESOLVE, random)
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
        if (tweetCount <= 0 || state.result != null) return GameTransition(state, emptyList())
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
                val incoming =
                    invokeOwnerDamageHooks(
                        state,
                        targetId,
                        CardHookWindow.INCOMING_DAMAGE,
                        random,
                        targetIds = setOf(targetId),
                        damage = rawDamage,
                        outgoing = false,
                    )
                if (incoming.any { it.preventDamage }) {
                    0
                } else {
                    (rawDamage - incoming.sumOf { it.damageReduction }).coerceAtLeast(0)
                }
            }

        var nextState = state.copy(randomState = random.state)
        val events = mutableListOf<GameEvent>()
        var totalUnfollows = 0
        val newlySuspended = mutableListOf<PlayerId>()
        for ((targetId, damageToDeal) in targetDamage) {
            val target = nextState.players.firstOrNull { it.id == targetId } ?: continue
            val damage = minOf(damageToDeal, target.followers)
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
                    newlySuspended += targetId
                }
            }
        }

        if (newlySuspended.isNotEmpty()) {
            val intents =
                invokeOwnerSetHooks(
                    nextState,
                    newlySuspended,
                    CardHookWindow.ELIMINATED,
                    random,
                ) { effect, hookContext -> effect.onEliminated(hookContext) }
            val applied = applyIntents(nextState, intents)
            nextState = applied.first
            events += applied.second
            newlySuspended
                .filter { id -> nextState.players.any { it.id == id && it.isSuspended } }
                .forEach { events += GameEvent.PlayerSuspended(it) }
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

        if (mainCharacter != null && targets.contains(mainCharacter) && (targetDamage[mainCharacter] ?: 0) > 0) {
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
        return GameTransition(nextState.copy(randomState = random.state), events)
    }

    private fun invokeOwnerHooks(
        state: GameState,
        ownerId: PlayerId,
        window: CardHookWindow,
        random: SplitMix64,
        purchasedCardId: CardId? = null,
        actingPlayerId: PlayerId? = null,
        invoke: (CardEffect, CardHookContext) -> List<CardEffectIntent>,
    ): List<CardEffectIntent> {
        val owner = state.players.firstOrNull { it.id == ownerId } ?: return emptyList()
        return cardEffects.ordered(owner.policyIds).flatMap { effect ->
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
        val ordered =
            ownerIds
                .distinct()
                .flatMap { ownerId ->
                    val owner = state.players.firstOrNull { it.id == ownerId } ?: return@flatMap emptyList()
                    cardEffects.ordered(owner.policyIds).map { effect -> ownerId to effect }
                }.sortedWith(compareBy({ it.second.cardId.value }, { it.first.value }))
        return ordered.flatMap { (ownerId, effect) ->
            invoke(effect, hookContext(state, ownerId, effect, window, random, eliminatedPlayerId = ownerId))
        }
    }

    private fun invokeNumericHooks(
        state: GameState,
        ownerId: PlayerId,
        window: CardHookWindow,
        random: SplitMix64,
        invoke: (CardEffect, CardHookContext) -> Int,
    ): Int {
        val owner = state.players.firstOrNull { it.id == ownerId } ?: return 0
        return cardEffects.ordered(owner.policyIds).sumOf { effect ->
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
        return cardEffects.ordered(owner.policyIds).map { effect ->
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
        for (effect in cardEffects.ordered(owner.policyIds)) {
            val offers = effect.activatedAbilities(hookContext(state, ownerId, effect, window, random))
            for (offer in offers) {
                val validOptions =
                    offer.options.filter { option ->
                        val action = option.action as? Action.ActivateAbility
                        action != null &&
                            action.playerId == ownerId &&
                            action.cardId == effect.cardId &&
                            action.abilityId == offer.abilityId
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
                        options = validOptions,
                        promptKey = offer.promptKey,
                    )
                if (decisions.none { it.id == decision.id }) decisions += decision
            }
        }
        return decisions
    }

    private fun applyIntents(
        initialState: GameState,
        intents: List<CardEffectIntent>,
    ): Pair<GameState, List<GameEvent>> {
        var state = initialState
        val events = mutableListOf<GameEvent>()
        for (intent in intents) {
            when (intent) {
                is CardEffectIntent.ChangeResource -> {
                    if (state.players.any { it.id == intent.playerId }) {
                        val changed = changeResource(state, intent.playerId, intent.resource, intent.amount, intent.reason)
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
                    if (index >= 0) {
                        val player = state.players[index]
                        val value =
                            (player.extraRerolls.toLong() + intent.amount)
                                .coerceAtLeast(
                                    0L,
                                ).coerceAtMost(Int.MAX_VALUE.toLong())
                                .toInt()
                        state = state.copy(players = state.players.toMutableList().also { it[index] = player.copy(extraRerolls = value) })
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
                    if (state.players.any { it.id == intent.decision.playerId } && intent.decision.options.isNotEmpty()) {
                        state = state.copy(pendingDecisions = state.pendingDecisions + intent.decision)
                        events += GameEvent.DecisionQueued(intent.decision)
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
        if (action.cardId !in player.policyIds) {
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
        val applied = applyIntents(state, intents)
        return GameTransition(applied.first.copy(randomState = random.state), applied.second)
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
        val applied =
            if (option.action is Action.ActivateAbility) {
                activateAbility(preparedState, option.action, offeredByDecision = true)
            } else {
                reduce(preparedState, option.action)
            }
        if (applied.events.any { it is GameEvent.ActionRejected }) {
            return GameTransition(
                state = state,
                events = applied.events,
            )
        }
        val events = listOf(GameEvent.DecisionResolved(action.decisionId, action.optionId)) + applied.events
        if (remainingDecisions.isNotEmpty() || applied.state.result != null) {
            return GameTransition(applied.state, events)
        }
        return when (resumePhase) {
            TurnPhase.RESOLVING -> {
                val random = SplitMix64(applied.state.randomState)
                val completed = completeRollResolution(applied.state, state.currentPlayerId, random)
                GameTransition(completed.state, events + completed.events)
            }
            TurnPhase.END_TURN -> {
                val completed = completeEndTurn(applied.state, state.currentPlayerId)
                GameTransition(completed.state, events + completed.events)
            }
            else -> GameTransition(applied.state, events)
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
        if (state.phase != TurnPhase.END_TURN || state.pendingDecisions.isNotEmpty()) {
            return reject(state, action.playerId, ActionRejectionReason.WRONG_PHASE)
        }
        if (state.players.none { it.id == action.playerId }) {
            return reject(state, action.playerId, ActionRejectionReason.INVALID_SELECTION)
        }
        val random = SplitMix64(state.randomState)
        val intents =
            invokeOwnerHooks(
                state,
                action.playerId,
                CardHookWindow.TURN_END,
                random,
            ) { effect, hookContext -> effect.onTurnEnd(hookContext) }
        val applied = applyIntents(state, intents)
        val endState = applied.first.copy(randomState = random.state)
        val events = applied.second
        if (endState.pendingDecisions.isNotEmpty()) {
            return GameTransition(
                endState.copy(phase = TurnPhase.DECISION, phaseAfterDecisions = TurnPhase.END_TURN),
                events,
            )
        }
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
                turnStartHooksApplied = false,
                resolveHooksAppliedForRoll = false,
                turnDiceCount = null,
                turnRollLimit = null,
            )
        return GameTransition(
            nextState,
            listOf(turnEnded) + startEvents + GameEvent.TurnStarted(nextPlayer.id, nextTurnNumber),
        )
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
) : HookRandom {
    override fun nextInt(bound: Int): Int {
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
