package com.kingofxitter.bots

import com.kingofxitter.engine.DefaultGameReducer
import com.kingofxitter.engine.GameReducer
import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.HookRandom
import com.kingofxitter.engine.model.LeaderId
import com.kingofxitter.engine.model.PendingDecision

/**
 * V6: each leader plays the same difficulty differently, by leaning on a different appetite in
 * [ReplyGuyWeights]. Nothing here changes the rules (ROADMAP §1 decision 3: leaders have no
 * gameplay abilities in v1); it only changes what a bot reaches for.
 *
 * The weights follow the voices in `design/screens/the-cast-and-their-tweet-voices.html`:
 * - **Trumpet** posts first and thinks later: he keeps unfollows on sight and never logs off.
 * - **Poutine** is patient: he banks characters, buys late, and does not give up the long table.
 * - **Kim** hoards mandates and would rather broadcast a record than take a hit.
 * - **Macaron** is the balanced centre, with a weakness for buying tools.
 * - **Xi** plays the five-year plan: it saves characters and keeps a reserve.
 * - **Mi-León** attacks everything and heals nothing.
 *
 * Keyed by [LeaderId] rather than `:content`'s roster, because `:bots` must not depend on it.
 *
 * The spread is deliberately narrow. B4 measured it: with no power-up deck in play, the only knob
 * that really bites is how long a bot clings to Xitter, because the Main Character takes everyone
 * else's tweets. Over 4,000 Reply Guy matches these weights put the six leaders between 18% and
 * 32% per seat; a wider spread made the brash leaders a trap. The shop appetites and the
 * character reserves do nothing until C2 wires the deck in, and the band should be measured again
 * then.
 */
object LeaderPersonalities {
    private val weights =
        mapOf(
            "trumpet" to
                ReplyGuyWeights(
                    mandateHunger = 1f,
                    aggression = 1.15f,
                    selfPreservation = 1f,
                    shopAppetite = 0.95f,
                    logOffFollowers = 2,
                ),
            "poutine" to
                ReplyGuyWeights(
                    mandateHunger = 1.05f,
                    aggression = 1.05f,
                    selfPreservation = 1.1f,
                    shopAppetite = 1.25f,
                    logOffFollowers = 3,
                    charactersInReserve = 20,
                ),
            "kim" to
                ReplyGuyWeights(
                    mandateHunger = 1.2f,
                    aggression = 1f,
                    selfPreservation = 1.1f,
                    shopAppetite = 0.95f,
                    logOffFollowers = 4,
                ),
            "macaron" to ReplyGuyWeights(shopAppetite = 1.15f, logOffFollowers = 3),
            "xi" to
                ReplyGuyWeights(
                    mandateHunger = 1.1f,
                    aggression = 1f,
                    selfPreservation = 1f,
                    shopAppetite = 1.3f,
                    logOffFollowers = 4,
                    charactersInReserve = 20,
                ),
            "mi-leon" to
                ReplyGuyWeights(
                    mandateHunger = 1f,
                    aggression = 1.2f,
                    selfPreservation = 1f,
                    shopAppetite = 0.85f,
                    logOffFollowers = 2,
                ),
        )

    /** [leaderId]'s personality, or the plain Reply Guy for a leader with none on record. */
    fun of(leaderId: LeaderId): ReplyGuyWeights = weights[leaderId.value] ?: ReplyGuyWeights()

    /** The leaders with a personality, for the test that keeps this table and the roster in step. */
    val leaders: List<LeaderId> = weights.keys.map(::LeaderId)
}

/**
 * One brain per seat, each with its leader's personality. The engine asks a single function for
 * every bot move, so this looks up whoever is acting and hands the question to that leader's own
 * brain; the brains are built once and reused.
 *
 * At [BotDifficulty.INTERN] the personality changes nothing, because the intern has no
 * preferences to tune, which is the right behaviour for the easiest setting.
 *
 * [reducer] is what [BotDifficulty.TROLL_FARM] searches with. Pass the match's own reducer: a
 * plain [DefaultGameReducer] has no shop, so every purchase the search tries is rejected and the
 * bot never buys a power-up.
 */
class LeaderBots(
    override val difficulty: BotDifficulty,
    private val market: CardMarket = CardMarket.Unknown,
    private val personality: (LeaderId) -> ReplyGuyWeights = LeaderPersonalities::of,
    private val reducer: GameReducer = DefaultGameReducer(),
) : BotBrain {
    private val brains = HashMap<String, BotBrain>()

    override fun decide(
        state: GameState,
        decision: PendingDecision?,
        random: HookRandom,
    ): Action? {
        val actor = BotMoves.actingPlayerId(state) ?: return null
        val leaderId = state.players.firstOrNull { it.id == actor }?.leaderId ?: return null
        return brain(leaderId).decide(state, decision, random)
    }

    /** The brain for [leaderId], built on first use. */
    fun brain(leaderId: LeaderId): BotBrain =
        brains.getOrPut(leaderId.value) {
            val weights = personality(leaderId)
            when (difficulty) {
                BotDifficulty.INTERN -> InternBot(market)
                BotDifficulty.REPLY_GUY -> ReplyGuyBot(market, weights)
                BotDifficulty.TROLL_FARM -> TrollFarmBot(market, weights, reducer = reducer)
            }
        }
}
