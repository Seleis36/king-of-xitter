package com.kingofxitter.bots

import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.CardId
import com.kingofxitter.engine.model.DecisionKind
import com.kingofxitter.engine.model.DecisionOption
import com.kingofxitter.engine.model.DiceFace
import com.kingofxitter.engine.model.DieId
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.HookRandom
import com.kingofxitter.engine.model.PendingDecision
import com.kingofxitter.engine.model.PlayerId
import com.kingofxitter.engine.model.PlayerState
import com.kingofxitter.engine.model.TurnPhase

/**
 * What makes one Reply Guy different from another. V6 gives each leader its own set; the defaults
 * are the design's description of the difficulty: buys power-ups, rarely logs off, tweets at
 * whoever is weakest.
 *
 * The four appetites weigh the dice the bot keeps. [logOffFollowers] is the follower count at or
 * below which it finally gives up Xitter, and [charactersInReserve] is what it refuses to spend
 * so a sweep stays possible.
 */
data class ReplyGuyWeights(
    /** How much a scoring set of numbers is worth. */
    val mandateHunger: Float = 1f,
    /** How much an unfollow is worth. */
    val aggression: Float = 1f,
    /** How much a like is worth when followers are missing. */
    val selfPreservation: Float = 1f,
    /** How much a character face is worth while something in the shop is out of reach. */
    val shopAppetite: Float = 1f,
    /** Followers at or below which the bot logs off rather than hold Xitter. */
    val logOffFollowers: Int = 3,
    /** Characters the bot keeps back instead of spending on the shop. */
    val charactersInReserve: Int = 0,
) {
    init {
        require(mandateHunger >= 0 && aggression >= 0 && selfPreservation >= 0 && shopAppetite >= 0)
        require(logOffFollowers >= 0 && charactersInReserve >= 0)
    }
}

/**
 * B2: the middle difficulty. It plays the base rules on heuristics rather than search, which
 * makes it fast enough to run four at a time on a phone and predictable enough to balance
 * against.
 *
 * Its three policies are the ones the design names:
 * - **Dice.** It keeps the faces that are worth something in the current position (a pair that
 *   could become a scoring set, unfollows when there is somebody to hit, likes when followers are
 *   missing, characters while the shop holds something it can't afford) and rerolls the rest. It
 *   resolves early once there is nothing left worth rerolling.
 * - **Xitter.** It stays Main Character until its followers reach [ReplyGuyWeights.logOffFollowers],
 *   because the spot is worth +2 mandates a turn.
 * - **Shop.** It buys the dearest card it can afford, since cost tracks strength in the base game,
 *   and sweeps a row it can't use when it can spare the characters.
 *
 * Choices that need a card's meaning (which die to change, which ability to use) are declined:
 * `:bots` can't depend on `:cards`, so the bot never pretends to understand an effect it can't
 * read. [market] is the only thing it knows about shop cards, and with
 * [CardMarket.Unknown] it simply doesn't shop.
 */
class ReplyGuyBot(
    private val market: CardMarket = CardMarket.Unknown,
    private val weights: ReplyGuyWeights = ReplyGuyWeights(),
) : BotBrain {
    override val difficulty = BotDifficulty.REPLY_GUY

    override fun decide(
        state: GameState,
        decision: PendingDecision?,
        random: HookRandom,
    ): Action? {
        if (state.result != null || state.phase == TurnPhase.FINISHED) return null
        if (decision != null) return answer(state, decision, random)
        if (state.pendingDecisions.isNotEmpty()) return null
        val player = state.players.firstOrNull { it.id == state.currentPlayerId }?.takeIf { !it.isSuspended } ?: return null
        return when (state.phase) {
            TurnPhase.TURN_START -> Action.RollDice(player.id)
            TurnPhase.ROLLING -> rollOrResolve(state, player)
            TurnPhase.SHOP -> shop(state, player)
            TurnPhase.END_TURN -> Action.EndTurn(player.id)
            else -> null
        }
    }

    // ---- Decisions ---------------------------------------------------------------------------

    private fun answer(
        state: GameState,
        decision: PendingDecision,
        random: HookRandom,
    ): Action? {
        val chosen =
            when (decision.kind) {
                DecisionKind.STAY_OR_LOG_OFF -> stayOrLogOff(state, decision)
                DecisionKind.CHOOSE_TARGET -> weakestTarget(state, decision)
                DecisionKind.BUY_POWER_UP -> bestOffer(state, decision)
                DecisionKind.ACTIVATE_ABILITY, DecisionKind.CHOOSE_DIE, DecisionKind.KEEP_DICE -> decline(decision)
            } ?: decision.options.getOrNull(random.nextInt(decision.options.size.coerceAtLeast(1)))
        return chosen?.let { Action.ChooseDecisionOption(decision.playerId, decision.id, it.id) }
    }

    /** Xitter is worth +2 mandates a turn, so it holds on until its followers run low. */
    private fun stayOrLogOff(
        state: GameState,
        decision: PendingDecision,
    ): DecisionOption? {
        val me = state.players.firstOrNull { it.id == decision.playerId } ?: return null
        val wantToStay = me.followers > weights.logOffFollowers
        val action = if (wantToStay) Action.StayInXitter::class else Action.LogOffXitter::class
        return decision.options.firstOrNull { action.isInstance(it.action) }
            ?: decision.options.firstOrNull { it.action is Action.StayInXitter }
    }

    /** The design's "tweets at whoever is weakest": the fewest followers, then the most mandates. */
    private fun weakestTarget(
        state: GameState,
        decision: PendingDecision,
    ): DecisionOption? =
        decision.options
            .mapNotNull { option -> target(option)?.let { option to it } }
            .mapNotNull { (option, id) -> state.players.firstOrNull { it.id == id }?.let { option to it } }
            .filterNot { (_, player) -> player.id == decision.playerId || player.isSuspended }
            .minWithOrNull(compareBy({ it.second.followers }, { -it.second.mandates }, { it.second.id.value }))
            ?.first

    /** Among offered purchases, the dearest one it can afford; otherwise nothing. */
    private fun bestOffer(
        state: GameState,
        decision: PendingDecision,
    ): DecisionOption? {
        val me = state.players.firstOrNull { it.id == decision.playerId } ?: return null
        val buys =
            decision.options.mapNotNull { option ->
                val card = (option.action as? Action.BuyPowerUp)?.cardId ?: return@mapNotNull null
                appraise(card)?.let { option to it }
            }
        return buys
            .filter { (_, card) -> card.cost <= me.characters }
            .maxWithOrNull(compareBy({ worth(it.second) }, { it.second.cost }))
            ?.first ?: decline(decision)
    }

    /** Declining is always safe; the engine offers it for every optional ability. */
    private fun decline(decision: PendingDecision): DecisionOption? = decision.options.firstOrNull { it.action is Action.DeclineAbility }

    private fun target(option: DecisionOption): PlayerId? =
        when (val action = option.action) {
            is Action.ActivateAbility -> action.targetPlayerId
            else -> null
        }

    // ---- Dice --------------------------------------------------------------------------------

    private fun rollOrResolve(
        state: GameState,
        player: PlayerState,
    ): Action {
        if (state.rollNumber == 0) return Action.RollDice(player.id)
        val keep = keepSet(state, player)
        // Nothing left to gain from another roll, or no roll left: take what's on the table.
        if (!BotMoves.canReroll(state) || keep.size == state.dice.size) return Action.ResolveRoll(player.id)
        return Action.RollDice(player.id, keep)
    }

    /**
     * The faces worth keeping, which is also a legal keep set because keeping a face keeps all of
     * its dice (see [BotMoves.keepCandidates]).
     */
    internal fun keepSet(
        state: GameState,
        player: PlayerState,
    ): Set<DieId> {
        val byFace = state.dice.groupBy { it.face }
        val keptFaces = DiceFace.entries.filter { face -> keepFace(face, byFace[face]?.size ?: 0, state, player) }
        return keptFaces.flatMap { face -> byFace.getValue(face).map { it.id } }.toSet()
    }

    /**
     * A face is kept when what it is worth in this position, weighted by the matching appetite,
     * reaches [KEEP_THRESHOLD]. At the default weights that is exactly "keep what pays": a
     * completed set, the best pair, likes that can heal, characters for a card out of reach, and
     * unfollows with somebody to hit. A lower appetite makes the bot need more of a face before
     * it bothers, and a higher one makes it keep a single die.
     */
    private fun keepFace(
        face: DiceFace,
        count: Int,
        state: GameState,
        player: PlayerState,
    ): Boolean {
        if (count == 0) return false
        val value =
            when (face) {
                DiceFace.ONE, DiceFace.TWO, DiceFace.THREE -> numberValue(face, count, state) * weights.mandateHunger
                DiceFace.LIKE -> likeValue(count, state, player) * weights.selfPreservation
                DiceFace.CHARACTERS -> charactersValue(count, state, player) * weights.shopAppetite
                DiceFace.TWEET -> tweetValue(count, state, player) * weights.aggression
            }
        return value >= KEEP_THRESHOLD
    }

    /**
     * A completed set scores, so it is worth keeping twice over. A pair is only worth it when it
     * is the best pair on the table, because keeping two pairs leaves too few dice to complete
     * either.
     */
    private fun numberValue(
        face: DiceFace,
        count: Int,
        state: GameState,
    ): Float {
        if (count >= 3) return SET_VALUE
        if (count < 2) return 0f
        val numbers = listOf(DiceFace.ONE, DiceFace.TWO, DiceFace.THREE)
        val best =
            numbers
                .map { candidate -> candidate to state.dice.count { it.face == candidate } }
                .filter { it.second >= 2 }
                .maxWithOrNull(compareBy({ it.second }, { numbers.indexOf(it.first) }))
        return if (best?.first == face) KEEP_THRESHOLD else 0f
    }

    /** Likes are wasted on Xitter and above the follower maximum (ROADMAP §1). */
    private fun likeValue(
        count: Int,
        state: GameState,
        player: PlayerState,
    ): Float {
        if (state.xitterPlayerId == player.id) return 0f
        val missing = player.maxFollowers - player.followers
        if (missing <= 0) return 0f
        return KEEP_THRESHOLD + (minOf(count, missing) - 1) * EXTRA_DIE_VALUE
    }

    private fun charactersValue(
        count: Int,
        state: GameState,
        player: PlayerState,
    ): Float = if (wantsCharacters(state, player)) KEEP_THRESHOLD + (count - 1) * EXTRA_DIE_VALUE else 0f

    private fun tweetValue(
        count: Int,
        state: GameState,
        player: PlayerState,
    ): Float = if (hasSomeoneToHit(state, player)) KEEP_THRESHOLD + (count - 1) * EXTRA_DIE_VALUE else 0f

    /** Characters are only worth hoarding while the shop holds something out of reach. */
    private fun wantsCharacters(
        state: GameState,
        player: PlayerState,
    ): Boolean {
        val priced = BotMoves.pricedShop(state, market).values
        if (priced.isEmpty()) return false
        return priced.any { it.cost > player.characters } || priced.none { it.cost <= player.characters }
    }

    private fun hasSomeoneToHit(
        state: GameState,
        player: PlayerState,
    ): Boolean =
        when (state.xitterPlayerId) {
            // Entering an empty Xitter is worth a mandate, and tweets hit nobody on the way in.
            null -> false
            player.id -> state.players.any { it.id != player.id && !it.isSuspended }
            else -> true
        }

    // ---- Shop --------------------------------------------------------------------------------

    private fun shop(
        state: GameState,
        player: PlayerState,
    ): Action {
        val priced = BotMoves.pricedShop(state, market)
        val spendable = player.characters - weights.charactersInReserve
        val best =
            priced.entries
                .filter { (_, card) -> card.cost <= spendable }
                .maxWithOrNull(compareBy({ worth(it.value) }, { it.value.cost }, { it.key.value }))
        if (best != null) return Action.BuyPowerUp(player.id, best.key)
        // Nothing affordable: clear the row, but only with enough left for the cheapest card in
        // the catalogue, or the sweep just burns characters.
        val canSweep = priced.isNotEmpty() && spendable >= BotMoves.SWEEP_COST + BotMoves.CHEAPEST_CARD
        if (canSweep) return Action.SweepShop(player.id)
        return Action.EndTurn(player.id)
    }

    private fun appraise(cardId: CardId): CardAppraisal? = market.appraise(cardId)

    /**
     * How much a card is worth to the bot. Cost tracks strength in the base game, and a Policy
     * keeps paying every turn while an Executive Order fires once, so a Policy of the same price
     * is worth more.
     */
    private fun worth(card: CardAppraisal): Float = card.cost * (if (card.isPolicy) POLICY_WORTH else ORDER_WORTH)

    private companion object {
        const val KEEP_THRESHOLD = 1f
        const val SET_VALUE = 2f
        const val EXTRA_DIE_VALUE = 0.25f
        const val POLICY_WORTH = 1f
        const val ORDER_WORTH = 0.8f
    }
}
