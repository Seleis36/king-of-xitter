package com.kingofxitter.cards

import com.kingofxitter.engine.model.CardId
import com.kingofxitter.engine.model.DiceFace
import com.kingofxitter.engine.model.Die
import com.kingofxitter.engine.model.DieId
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.LeaderId
import com.kingofxitter.engine.model.PlayerId
import com.kingofxitter.engine.model.PlayerState
import com.kingofxitter.engine.model.TurnPhase

/**
 * Fluent builder for [GameState] snapshots used in unit tests and Compose previews.
 *
 * Defaults to a two-player match (human vs. one bot) in [TurnPhase.ROLLING]
 * with a seeded random state, so tests only need to override what matters.
 *
 * ```kotlin
 * val state = TestGame()
 *     .withPolicy(human, CardCatalog[CardId("c02_lobbyist")].id)
 *     .withCharacters(human, 60)
 *     .withDice(DiceFace.TWEET, DiceFace.TWEET, DiceFace.TWEET)
 *     .build()
 * ```
 */
class TestGame {
    companion object {
        /** Stable player IDs for the default two-player layout. */
        val HUMAN = PlayerId("human")
        val BOT = PlayerId("bot")

        /** Default random seed, kept constant so tests are reproducible. */
        const val DEFAULT_SEED: Long = 42L
    }

    private var human: PlayerState =
        PlayerState(
            id = HUMAN,
            leaderId = LeaderId("trumpet"),
            followers = 10,
            maxFollowers = 10,
            mandates = 0,
            characters = 0,
        )

    private var bot: PlayerState =
        PlayerState(
            id = BOT,
            leaderId = LeaderId("poutine"),
            followers = 10,
            maxFollowers = 10,
            mandates = 0,
            characters = 0,
        )

    private var extraPlayers: List<PlayerState> = emptyList()
    private var currentPlayer: PlayerId = HUMAN
    private var phase: TurnPhase = TurnPhase.ROLLING
    private var xitterPlayerId: PlayerId? = null
    private var dice: List<Die> = emptyList()
    private var rollNumber: Int = 1
    private var drawPile: List<CardId> = emptyList()
    private var shop: List<CardId> = emptyList()
    private var discardPile: List<CardId> = emptyList()
    private var randomState: Long = DEFAULT_SEED

    // -------------------------------------------------------------------------
    // Player builders
    // -------------------------------------------------------------------------

    /** Overrides the human player's full state. */
    fun withHuman(state: PlayerState): TestGame = apply { human = state.copy(id = HUMAN) }

    /** Overrides the bot player's full state. */
    fun withBot(state: PlayerState): TestGame = apply { bot = state.copy(id = BOT) }

    /** Adds [policy] to the target player's policy list. */
    fun withPolicy(
        playerId: PlayerId,
        policy: CardId,
    ): TestGame = withPolicies(playerId, listOf(policy))

    /** Adds [policies] to the target player's policy list. */
    fun withPolicies(
        playerId: PlayerId,
        policies: List<CardId>,
    ): TestGame =
        apply {
            when (playerId) {
                HUMAN -> human = human.copy(policyIds = human.policyIds + policies)
                BOT -> bot = bot.copy(policyIds = bot.policyIds + policies)
                else -> {
                    val idx = extraPlayers.indexOfFirst { it.id == playerId }
                    if (idx >= 0) {
                        val updated = extraPlayers[idx].copy(policyIds = extraPlayers[idx].policyIds + policies)
                        extraPlayers = extraPlayers.toMutableList().also { it[idx] = updated }
                    }
                }
            }
        }

    /** Sets the character count for [playerId]. */
    fun withCharacters(
        playerId: PlayerId,
        amount: Int,
    ): TestGame =
        apply {
            when (playerId) {
                HUMAN -> human = human.copy(characters = amount)
                BOT -> bot = bot.copy(characters = amount)
                else -> updateExtra(playerId) { copy(characters = amount) }
            }
        }

    /** Sets the follower count for [playerId]. */
    fun withFollowers(
        playerId: PlayerId,
        amount: Int,
    ): TestGame =
        apply {
            when (playerId) {
                HUMAN -> human = human.copy(followers = amount)
                BOT -> bot = bot.copy(followers = amount)
                else -> updateExtra(playerId) { copy(followers = amount) }
            }
        }

    /** Sets the mandate count for [playerId]. */
    fun withMandates(
        playerId: PlayerId,
        amount: Int,
    ): TestGame =
        apply {
            when (playerId) {
                HUMAN -> human = human.copy(mandates = amount)
                BOT -> bot = bot.copy(mandates = amount)
                else -> updateExtra(playerId) { copy(mandates = amount) }
            }
        }

    // -------------------------------------------------------------------------
    // Match state builders
    // -------------------------------------------------------------------------

    /** Sets the active player for this turn. */
    fun withCurrentPlayer(playerId: PlayerId): TestGame = apply { currentPlayer = playerId }

    /** Sets the current game phase. */
    fun withPhase(phase: TurnPhase): TestGame = apply { this.phase = phase }

    /** Places a player on Xitter (as Main Character). */
    fun withXitter(playerId: PlayerId?): TestGame = apply { xitterPlayerId = playerId }

    /** Sets the visible dice for the current roll. */
    fun withDice(vararg faces: DiceFace): TestGame =
        apply {
            dice = faces.mapIndexed { i, face -> Die(DieId(i), face) }
            if (rollNumber < 1) rollNumber = 1
        }

    /** Sets the roll counter. */
    fun withRollNumber(n: Int): TestGame = apply { rollNumber = n }

    /** Sets the draw pile card IDs (top of pile first). */
    fun withDrawPile(cardIds: List<CardId>): TestGame = apply { drawPile = cardIds }

    /** Sets the visible shop row. */
    fun withShop(cardIds: List<CardId>): TestGame = apply { shop = cardIds }

    /** Sets the discard pile. */
    fun withDiscardPile(cardIds: List<CardId>): TestGame = apply { discardPile = cardIds }

    /** Overrides the deterministic random seed. */
    fun withSeed(seed: Long): TestGame = apply { randomState = seed }

    // -------------------------------------------------------------------------
    // Build
    // -------------------------------------------------------------------------

    /** Produces an immutable [GameState] from the current builder configuration. */
    fun build(): GameState =
        GameState(
            players = listOf(human, bot) + extraPlayers,
            currentPlayerId = currentPlayer,
            phase = phase,
            xitterPlayerId = xitterPlayerId,
            dice = dice,
            rollNumber = rollNumber,
            drawPileCardIds = drawPile,
            shopCardIds = shop,
            discardPileCardIds = discardPile,
            randomState = randomState,
        )

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun updateExtra(
        playerId: PlayerId,
        transform: PlayerState.() -> PlayerState,
    ) {
        val idx = extraPlayers.indexOfFirst { it.id == playerId }
        if (idx >= 0) {
            extraPlayers = extraPlayers.toMutableList().also { it[idx] = it[idx].transform() }
        }
    }
}
