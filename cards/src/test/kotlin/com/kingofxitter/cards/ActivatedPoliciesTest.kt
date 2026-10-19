package com.kingofxitter.cards

import com.kingofxitter.engine.DefaultGameReducer
import com.kingofxitter.engine.GameTransition
import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.CardEffectRegistry
import com.kingofxitter.engine.model.CardId
import com.kingofxitter.engine.model.CounterId
import com.kingofxitter.engine.model.DecisionKind
import com.kingofxitter.engine.model.DecisionOptionId
import com.kingofxitter.engine.model.DiceFace
import com.kingofxitter.engine.model.PlayerId
import com.kingofxitter.engine.model.PlayerState
import com.kingofxitter.engine.model.TurnPhase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ActivatedPoliciesTest {
    private val reducer = DefaultGameReducer(CardEffectRegistry(ActivatedPolicies.effects))

    /** Rolls with Lurker until a seed shows at least one 3, so the test does not depend on one seed. */
    private fun rollWithAThree(): GameTransition {
        for (seed in 0L..200L) {
            val state =
                TestGame()
                    .withPhase(TurnPhase.ROLLING)
                    .withPolicy(TestGame.HUMAN, CardId("c05_lurker"))
                    .withRollNumber(0)
                    .withSeed(seed)
                    .build()
            val result = reducer.reduce(state, Action.RollDice(TestGame.HUMAN, emptySet()))
            if (result.state.dice.any { it.face == DiceFace.THREE }) return result
        }
        error("no seed in 0..200 rolled a 3")
    }

    @Test
    fun `Lurker offers one option per rolled 3 and a decline`() {
        val rolled = rollWithAThree()
        val threes = rolled.state.dice.filter { it.face == DiceFace.THREE }

        assertEquals(TurnPhase.DECISION, rolled.state.phase)
        val decision = rolled.state.pendingDecisions.first()
        assertEquals(DecisionKind.ACTIVATE_ABILITY, decision.kind)
        val dieOptions = decision.options.filter { it.id.value != "decline" }
        assertEquals(threes.map { it.id.value.toString() }.toSet(), dieOptions.map { it.id.value }.toSet())
        assertTrue(decision.options.any { it.id.value == "decline" })
    }

    @Test
    fun `Lurker rerolls the chosen 3 and returns to rolling`() {
        val rolled = rollWithAThree()
        val decision = rolled.state.pendingDecisions.first()
        val option = decision.options.first { it.id.value != "decline" }
        val dieId = (option.action as Action.ActivateAbility).dieIds.single()

        val activated = reducer.reduce(rolled.state, Action.ChooseDecisionOption(TestGame.HUMAN, decision.id, option.id))

        assertEquals(TurnPhase.ROLLING, activated.state.phase)
        assertTrue(activated.state.pendingDecisions.isEmpty())
        assertNotNull(activated.state.dice.firstOrNull { it.id == dieId })
        assertTrue(activated.state.randomState != rolled.state.randomState)
    }

    private fun rollWith(
        card: String,
        characters: Int = 50,
        followers: Int = 5,
    ): GameTransition {
        val state =
            TestGame()
                .withPhase(TurnPhase.ROLLING)
                .withRollNumber(0)
                .withPolicy(TestGame.HUMAN, CardId(card))
                .withCharacters(TestGame.HUMAN, characters)
                .withFollowers(TestGame.HUMAN, followers)
                .build()
        return reducer.reduce(state, Action.RollDice(TestGame.HUMAN, emptySet()))
    }

    private fun choose(
        rolled: GameTransition,
        optionId: String,
    ): GameTransition {
        val decision = rolled.state.pendingDecisions.first()
        return reducer.reduce(rolled.state, Action.ChooseDecisionOption(TestGame.HUMAN, decision.id, DecisionOptionId(optionId)))
    }

    private fun human(transition: GameTransition): PlayerState = transition.state.players.first { it.id == TestGame.HUMAN }

    /** The first offered way to use the ability, so a test doesn't depend on what one seed rolled. */
    private fun firstUse(rolled: GameTransition): Action.ActivateAbility =
        rolled.state.pendingDecisions
            .first()
            .options
            .first { it.action is Action.ActivateAbility }
            .action as Action.ActivateAbility

    private fun optionFor(
        rolled: GameTransition,
        use: Action.ActivateAbility,
    ): String =
        rolled.state.pendingDecisions
            .first()
            .options
            .first { it.action == use }
            .id.value

    /** The bot rolls while the human holds [card], until a seed shows a tweet. */
    private fun botRollsATweet(
        card: String,
        xitter: PlayerId? = TestGame.BOT,
    ): GameTransition {
        for (seed in 0L..200L) {
            val state =
                TestGame()
                    .withCurrentPlayer(TestGame.BOT)
                    .withPhase(TurnPhase.ROLLING)
                    .withRollNumber(0)
                    .withXitter(xitter)
                    .withPolicy(TestGame.HUMAN, CardId(card))
                    .withCharacters(TestGame.HUMAN, 50)
                    .withFollowers(TestGame.HUMAN, 5)
                    .withSeed(seed)
                    .build()
            val result = reducer.reduce(state, Action.RollDice(TestGame.BOT, emptySet()))
            if (result.state.dice.any { it.face == DiceFace.TWEET }) return result
        }
        error("no seed in 0..200 rolled a tweet")
    }

    /** The bot rolls while the human holds [card]. */
    private fun botRolls(card: String): GameTransition {
        val state =
            TestGame()
                .withCurrentPlayer(TestGame.BOT)
                .withPhase(TurnPhase.ROLLING)
                .withRollNumber(0)
                .withPolicy(TestGame.HUMAN, CardId(card))
                .withCharacters(TestGame.HUMAN, 50)
                .withFollowers(TestGame.HUMAN, 5)
                .build()
        return reducer.reduce(state, Action.RollDice(TestGame.BOT, emptySet()))
    }

    @Test
    fun `Poll Rigger turns a die into a 1 and can be used once per turn`() {
        val rolled = rollWith("c22_poll_rigger")
        val use = firstUse(rolled)
        val result = choose(rolled, optionFor(rolled, use))

        assertEquals(
            DiceFace.ONE,
            result.state.dice
                .first { it.id == use.dieIds.single() }
                .face,
        )
        assertEquals(1, human(result).counters[CounterId("poll_rigger_${result.state.turnNumber}")])
    }

    @Test
    fun `Poll Rigger only offers dice that are not already a 1`() {
        val rolled = rollWith("c22_poll_rigger")
        val offered =
            rolled.state.pendingDecisions
                .first()
                .options
                .mapNotNull { (it.action as? Action.ActivateAbility)?.dieIds?.single() }
        val notOnes =
            rolled.state.dice
                .filter { it.face != DiceFace.ONE }
                .map { it.id }

        assertEquals(notOnes, offered)
    }

    @Test
    fun `Poll Rigger, October Surprise and Spin Doctor are not offered on another leader's roll`() {
        listOf("c22_poll_rigger", "c33_october_surprise", "c44_spin_doctor").forEach { card ->
            assertTrue(botRolls(card).state.pendingDecisions.isEmpty(), "$card was offered on the bot's roll")
        }
    }

    @Test
    fun `October Surprise sets the chosen face and discards itself`() {
        val rolled = rollWith("c33_october_surprise")
        val die = rolled.state.dice.first { it.face != DiceFace.LIKE }
        val result = choose(rolled, "${die.id.value}_LIKE")

        assertEquals(
            DiceFace.LIKE,
            result.state.dice
                .first { it.id == die.id }
                .face,
        )
        assertTrue(CardId("c33_october_surprise") !in human(result).policyIds)
        assertTrue(CardId("c33_october_surprise") in result.state.discardPileCardIds)
    }

    @Test
    fun `Paid Promotion pays 20 chars for 1M`() {
        val result = choose(rollWith("c37_paid_promotion", characters = 50, followers = 5), "buy_followers")

        assertEquals(30, human(result).characters)
        assertEquals(6, human(result).followers)
    }

    @Test
    fun `Paid Promotion is not offered at full followers`() {
        val rolled = rollWith("c37_paid_promotion", characters = 50, followers = 10)

        assertTrue(rolled.state.pendingDecisions.isEmpty())
    }

    @Test
    fun `Paid Promotion can be used on another leader's roll`() {
        val rolled = botRolls("c37_paid_promotion")

        assertEquals(
            TestGame.HUMAN,
            rolled.state.pendingDecisions
                .single()
                .playerId,
        )
    }

    @Test
    fun `Paid Promotion is not offered below 20 chars`() {
        val rolled = rollWith("c37_paid_promotion", characters = 10)

        assertTrue(rolled.state.pendingDecisions.isEmpty())
    }

    @Test
    fun `Spin Doctor pays 20 chars to set a die to any face`() {
        val rolled = rollWith("c44_spin_doctor")
        val die = rolled.state.dice.first { it.face != DiceFace.CHARACTERS }
        val result = choose(rolled, "${die.id.value}_CHARACTERS")

        assertEquals(30, human(result).characters)
        assertEquals(
            DiceFace.CHARACTERS,
            result.state.dice
                .first { it.id == die.id }
                .face,
        )
    }

    @Test
    fun `face changers never offer the face a die already shows`() {
        listOf("c33_october_surprise", "c44_spin_doctor").forEach { card ->
            val rolled = rollWith(card)
            rolled.state.pendingDecisions.first().options.forEach { option ->
                val use = option.action as? Action.ActivateAbility ?: return@forEach
                val die = rolled.state.dice.first { it.id == use.dieIds.single() }
                assertTrue(
                    die.face.name != use.abilityId.substringAfter(":"),
                    "$card offered ${use.abilityId} for a die showing ${die.face}",
                )
            }
        }
    }

    @Test
    fun `Focus Group pays 10 chars for one extra reroll this turn only`() {
        val rolled = rollWith("c45_focus_group")
        val before = rolled.state.turnRollLimit
        val result = choose(rolled, "buy_reroll_option")

        assertEquals(40, human(result).characters)
        assertEquals(before!! + 1, result.state.turnRollLimit)
        assertEquals(0, human(result).extraRerolls)
    }

    @Test
    fun `Touch Grass pays 20 chars and marks the turn`() {
        val rolled = botRollsATweet("c48_touch_grass")
        val decision = rolled.state.pendingDecisions.single()
        val result = reducer.reduce(rolled.state, Action.ChooseDecisionOption(TestGame.HUMAN, decision.id, DecisionOptionId("buy")))

        assertEquals(30, human(result).characters)
        assertEquals(1, human(result).counters[CounterId("touch_grass_${result.state.turnNumber}")])
    }

    @Test
    fun `Touch Grass is only offered when the roll's tweets could reach you`() {
        // Your own roll can't hit you.
        assertTrue(rollWith("c48_touch_grass").state.pendingDecisions.isEmpty())
        // Nobody on Xitter: tweets from outside hit nobody.
        assertTrue(botRollsATweet("c48_touch_grass", xitter = null).state.pendingDecisions.isEmpty())
        // The roller holds Xitter, so their tweets hit you.
        assertEquals(
            TestGame.HUMAN,
            botRollsATweet("c48_touch_grass", xitter = TestGame.BOT)
                .state.pendingDecisions
                .single()
                .playerId,
        )
        // You hold Xitter, so the roller's tweets hit you.
        assertEquals(
            TestGame.HUMAN,
            botRollsATweet("c48_touch_grass", xitter = TestGame.HUMAN)
                .state.pendingDecisions
                .single()
                .playerId,
        )
    }

    @Test
    fun `Phone Tap is offered to the other player's opponent, not the roller`() {
        val rolled = rollWith("c36_phone_tap")

        assertTrue(rolled.state.pendingDecisions.isEmpty())
    }
}
