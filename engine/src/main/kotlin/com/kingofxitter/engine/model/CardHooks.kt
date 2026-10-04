package com.kingofxitter.engine.model

/** Card implementation boundary called by the engine at rule-defined windows. */
interface CardEffect {
    /** Stable card identity used to bind the effect to a definition. */
    val cardId: CardId

    /** Additional dice contributed at the start of a turn. */
    fun diceCount(context: CardHookContext): Int = 0

    /** Additional rerolls contributed at the start of a turn. */
    fun rerollCount(context: CardHookContext): Int = 0

    /** Effects triggered after a power-up purchase. */
    fun onBuy(context: CardHookContext): List<CardEffectIntent> = emptyList()

    /** Effects triggered once after dice are rolled and before base resolution. */
    fun onResolve(context: CardHookContext): List<CardEffectIntent> = emptyList()

    /** Modifies damage dealt by this card owner's attack. */
    fun modifyOutgoingDamage(context: CardHookContext): DamageAdjustment = DamageAdjustment()

    /** Modifies damage received by this card owner. */
    fun modifyIncomingDamage(context: CardHookContext): DamageAdjustment = DamageAdjustment()

    /** Effects triggered when this card owner successfully deals damage. Context targetPlayerIds contains the players who took damage. */
    fun onDamageDealt(context: CardHookContext): List<CardEffectIntent> = emptyList()

    /** Effects triggered after start-of-turn Xitter scoring. */
    fun onTurnStart(context: CardHookContext): List<CardEffectIntent> = emptyList()

    /** Effects triggered after the shop and decisions resolve. */
    fun onTurnEnd(context: CardHookContext): List<CardEffectIntent> = emptyList()

    /** Effects triggered when this card owner reaches zero followers. */
    fun onEliminated(context: CardHookContext): List<CardEffectIntent> = emptyList()

    /** Optional abilities offered at a rule-defined decision window. */
    fun activatedAbilities(context: CardHookContext): List<AbilityOffer> = emptyList()

    /** Applies a previously offered ability action. */
    fun activate(
        context: CardHookContext,
        action: Action.ActivateAbility,
    ): List<CardEffectIntent> = emptyList()
}

/** Immutable rule snapshot and trigger details passed to one card hook. */
data class CardHookContext(
    val state: GameState,
    val ownerId: PlayerId,
    val sourceCardId: CardId,
    val window: CardHookWindow,
    val actingPlayerId: PlayerId? = null,
    val targetPlayerId: PlayerId? = null,
    val targetPlayerIds: Set<PlayerId> = emptySet(),
    val damage: Int = 0,
    val purchasedCardId: CardId? = null,
    val eliminatedPlayerId: PlayerId? = null,
    val random: HookRandom,
)

/** Rule window that caused a hook call. */
enum class CardHookWindow {
    TURN_START,
    DICE_COUNT,
    REROLL_COUNT,
    AFTER_DICE_ROLL,
    ON_RESOLVE,
    AFTER_RESOLVE,
    OUTGOING_DAMAGE,
    INCOMING_DAMAGE,
    DAMAGE_DEALT,
    ON_BUY,
    TURN_END,
    ELIMINATED,
    ACTIVATED_ABILITY,
}

/** Deterministic random stream owned by one reducer invocation. */
interface HookRandom {
    /** Returns a value in `0 until bound` and advances the reducer's random state. */
    fun nextInt(bound: Int): Int
}

/** Additive damage and target/prevention changes proposed by a card hook. */
data class DamageAdjustment(
    val bonusDamage: Int = 0,
    val damageReduction: Int = 0,
    val preventDamage: Boolean = false,
    val hitsAllOpponents: Boolean = false,
)

/** Optional player-facing ability offered by an active card. */
data class AbilityOffer(
    val abilityId: String,
    val promptKey: String,
    val options: List<DecisionOption>,
)

/** Declarative state changes that the reducer can apply and emit as events. */
sealed interface CardEffectIntent {
    /** Changes one player resource by a signed amount. */
    data class ChangeResource(
        val playerId: PlayerId,
        val resource: PlayerResource,
        val amount: Int,
        val reason: ResourceChangeReason = ResourceChangeReason.POWER_UP,
    ) : CardEffectIntent

    /** Changes the maximum follower count. */
    data class ChangeFollowerLimit(
        val playerId: PlayerId,
        val amount: Int,
    ) : CardEffectIntent

    /** Changes one current die face. */
    data class ChangeDie(
        val playerId: PlayerId,
        val dieId: DieId,
        val face: DiceFace,
    ) : CardEffectIntent

    /** Changes a player-owned counter. */
    data class ChangeCounter(
        val playerId: PlayerId,
        val counterId: CounterId,
        val amount: Int,
    ) : CardEffectIntent

    /** Changes the owner's extra-dice modifier. */
    data class ChangeDiceCount(
        val playerId: PlayerId,
        val amount: Int,
    ) : CardEffectIntent

    /** Changes the owner's extra-reroll modifier. */
    data class ChangeRerollCount(
        val playerId: PlayerId,
        val amount: Int,
    ) : CardEffectIntent

    /** Grants or removes an extra turn. */
    data class ChangeExtraTurns(
        val playerId: PlayerId,
        val amount: Int,
    ) : CardEffectIntent

    /** Changes Main Character ownership. */
    data class ChangeMainCharacter(
        val playerId: PlayerId?,
    ) : CardEffectIntent

    /** Adds a decision to the ordered queue. */
    data class QueueDecision(
        val decision: PendingDecision,
    ) : CardEffectIntent

    /** Discards a policy card from a player's active policies. */
    data class DiscardPolicy(
        val playerId: PlayerId,
        val cardId: CardId,
    ) : CardEffectIntent
}

/** Registry of card effects injected by the app's card catalogue. */
class CardEffectRegistry(
    effects: Collection<CardEffect> = emptyList(),
) {
    private val byId =
        effects.associateBy(CardEffect::cardId).also { indexed ->
            require(indexed.size == effects.size) { "Card effect IDs must be unique" }
        }

    /** Returns the registered implementation, if present. */
    operator fun get(cardId: CardId): CardEffect? = byId[cardId]

    /** Returns registered effects ordered by stable ID, independent of policy acquisition order. */
    fun ordered(cardIds: Collection<CardId>): List<CardEffect> = cardIds.distinct().mapNotNull(byId::get).sortedBy { it.cardId.value }
}
