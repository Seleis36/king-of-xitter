package com.kingofxitter.engine

import com.kingofxitter.engine.model.AbilityOffer
import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.CardEffect
import com.kingofxitter.engine.model.CardEffectIntent
import com.kingofxitter.engine.model.CardEffectRegistry
import com.kingofxitter.engine.model.CardHookContext
import com.kingofxitter.engine.model.CardId
import com.kingofxitter.engine.model.DamageAdjustment
import com.kingofxitter.engine.model.DecisionId
import com.kingofxitter.engine.model.DecisionKind
import com.kingofxitter.engine.model.DecisionOption
import com.kingofxitter.engine.model.DecisionOptionId
import com.kingofxitter.engine.model.DiceFace
import com.kingofxitter.engine.model.Die
import com.kingofxitter.engine.model.DieId
import com.kingofxitter.engine.model.GameEvent
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.LeaderId
import com.kingofxitter.engine.model.MatchResult
import com.kingofxitter.engine.model.PendingDecision
import com.kingofxitter.engine.model.PlayerId
import com.kingofxitter.engine.model.PlayerResource
import com.kingofxitter.engine.model.PlayerState
import com.kingofxitter.engine.model.TurnPhase
import com.kingofxitter.engine.model.VictoryReason
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewRegressionTest {
    private val a = PlayerId("a")
    private val b = PlayerId("b")
    private val c = PlayerId("c")
    private val card = CardId("test")

    private fun state(phase: TurnPhase = TurnPhase.ROLLING) =
        GameState(
            players =
                listOf(
                    PlayerState(a, LeaderId("a"), policyIds = listOf(card)),
                    PlayerState(b, LeaderId("b")),
                    PlayerState(c, LeaderId("c")),
                ),
            currentPlayerId = a,
            phase = phase,
            dice = if (phase == TurnPhase.ROLLING) listOf(Die(DieId(0), DiceFace.LIKE)) else emptyList(),
            rollNumber = if (phase == TurnPhase.ROLLING) 1 else 0,
        )

    private fun reducer(effect: CardEffect) = DefaultGameReducer(CardEffectRegistry(listOf(effect)))

    @Test fun `Xitter turn-start win stops before dice and card hooks`() {
        val initial =
            state(TurnPhase.TURN_START).let {
                it.copy(
                    xitterPlayerId = a,
                    players =
                        it.players.map { p ->
                            if (p.id ==
                                a
                            ) {
                                p.copy(mandates = 18)
                            } else {
                                p
                            }
                        },
                )
            }
        val result = DefaultGameReducer().reduce(initial, Action.RollDice(a))
        assertEquals(MatchResult.Victory(a, VictoryReason.TWENTY_MANDATES), result.state.result)
        assertTrue(result.events.none { it is GameEvent.DiceRolled })
    }

    @Test fun `lethal resolve hook cannot be undone by likes and skips dead actor`() {
        val effect =
            object : CardEffect {
                override val cardId = card

                override fun onResolve(context: CardHookContext) = listOf(CardEffectIntent.ChangeResource(a, PlayerResource.FOLLOWERS, -10))
            }
        val result = reducer(effect).reduce(state(), Action.ResolveRoll(a))
        assertEquals(
            0,
            result.state.players
                .first()
                .followers,
        )
        assertEquals(b, result.state.currentPlayerId)
        assertEquals(TurnPhase.TURN_START, result.state.phase)
        assertTrue(result.events.contains(GameEvent.PlayerSuspended(a)))
    }

    @Test fun `lethal turn-start hook persists and advances instead of rejecting original state`() {
        val effect =
            object : CardEffect {
                override val cardId = card

                override fun onTurnStart(context: CardHookContext) =
                    listOf(CardEffectIntent.ChangeResource(a, PlayerResource.FOLLOWERS, -10))
            }
        val result = reducer(effect).reduce(state(TurnPhase.TURN_START), Action.RollDice(a))
        assertEquals(
            0,
            result.state.players
                .first()
                .followers,
        )
        assertEquals(b, result.state.currentPlayerId)
        assertTrue(result.events.none { it is GameEvent.ActionRejected })
    }

    @Test fun `passive outgoing damage fires without tweet dice`() {
        val effect =
            object : CardEffect {
                override val cardId = card

                override fun modifyOutgoingDamage(context: CardHookContext) = DamageAdjustment(bonusDamage = 1)
            }
        val result = reducer(effect).reduce(state().copy(xitterPlayerId = b), Action.ResolveRoll(a))
        assertEquals(9, result.state.players[1].followers)
        assertEquals(1, result.state.matchStats[a]?.unfollowsDealt)
    }

    @Test fun `hook resource deltas aggregate and follower limit precedes healing`() {
        val effect =
            object : CardEffect {
                override val cardId = card

                override fun onBuy(context: CardHookContext) =
                    listOf(
                        CardEffectIntent.ChangeResource(a, PlayerResource.FOLLOWERS, 4),
                        CardEffectIntent.ChangeResource(a, PlayerResource.FOLLOWERS, -2),
                        CardEffectIntent.ChangeFollowerLimit(a, 2),
                    )
            }
        val result = reducer(effect).dispatchOnBuy(state(TurnPhase.SHOP), a, card)
        assertEquals(
            12,
            result.state.players
                .first()
                .followers,
        )
        assertEquals(
            12,
            result.state.players
                .first()
                .maxFollowers,
        )
    }

    @Test fun `optional reroll ability can be declined or extend current roll limit`() {
        val effect =
            object : CardEffect {
                override val cardId = card

                override fun activatedAbilities(context: CardHookContext) =
                    listOf(
                        AbilityOffer(
                            "extra",
                            "extra",
                            listOf(DecisionOption(DecisionOptionId("use"), Action.ActivateAbility(a, card, "extra"))),
                        ),
                    )

                override fun activate(
                    context: CardHookContext,
                    action: Action.ActivateAbility,
                ) = listOf(CardEffectIntent.ChangeRerollCount(a, 1))
            }
        val reducer = reducer(effect)
        val waiting = reducer.reduce(state().copy(rollNumber = 2, turnRollLimit = 3, turnDiceCount = 1), Action.RollDice(a)).state
        val decision = waiting.pendingDecisions.first()
        val declined = reducer.reduce(waiting, Action.ChooseDecisionOption(a, decision.id, DecisionOptionId("decline")))
        assertEquals(TurnPhase.ROLLING, declined.state.phase)
        assertEquals(3, declined.state.turnRollLimit)
        val used = reducer.reduce(waiting, Action.ChooseDecisionOption(a, decision.id, DecisionOptionId("use")))
        assertEquals(4, used.state.turnRollLimit)
        assertTrue(reducer.reduce(used.state, Action.RollDice(a)).events.any { it is GameEvent.DiceRolled })
    }

    @Test fun `purchase decision becomes answerable and restores shop`() {
        val effect =
            object : CardEffect {
                override val cardId = card

                override fun onBuy(context: CardHookContext) =
                    listOf(
                        CardEffectIntent.QueueDecision(
                            PendingDecision(
                                DecisionId("buy"),
                                a,
                                DecisionKind.ACTIVATE_ABILITY,
                                listOf(DecisionOption(DecisionOptionId("skip"), Action.DeclineAbility(a))),
                            ),
                        ),
                    )
            }
        val reducer = reducer(effect)
        val waiting = reducer.dispatchOnBuy(state(TurnPhase.SHOP), a, card).state
        assertEquals(TurnPhase.DECISION, waiting.phase)
        val resumed = reducer.reduce(waiting, Action.ChooseDecisionOption(a, DecisionId("buy"), DecisionOptionId("skip")))
        assertEquals(TurnPhase.SHOP, resumed.state.phase)
        assertTrue(resumed.state.pendingDecisions.isEmpty())
    }

    @Test fun `end turn leaves shop through reducer and unsolicited logoff is rejected`() {
        val initial = state(TurnPhase.SHOP).copy(xitterPlayerId = b)
        val reducer = DefaultGameReducer()
        assertEquals(initial, reducer.reduce(initial, Action.LogOffXitter(b)).state)
        val next = reducer.reduce(initial, Action.EndTurn(a))
        assertEquals(b, next.state.currentPlayerId)
        assertEquals(TurnPhase.TURN_START, next.state.phase)
    }

    @Test fun `fatal hooks discard dead choices and finish living choices before next turn`() {
        for (window in listOf(TurnPhase.TURN_START, TurnPhase.ROLLING)) {
            for (decisionOwner in listOf(a, b)) {
                val effect =
                    object : CardEffect {
                        override val cardId = card

                        private fun intents() =
                            listOf(
                                CardEffectIntent.ChangeResource(a, PlayerResource.FOLLOWERS, -10),
                                CardEffectIntent.QueueDecision(
                                    PendingDecision(
                                        DecisionId("last"),
                                        decisionOwner,
                                        DecisionKind.ACTIVATE_ABILITY,
                                        listOf(DecisionOption(DecisionOptionId("skip"), Action.DeclineAbility(decisionOwner))),
                                    ),
                                ),
                            )

                        override fun onTurnStart(context: CardHookContext) = intents()

                        override fun onResolve(context: CardHookContext) = intents()
                    }
                val reducer = reducer(effect)
                var result =
                    reducer.reduce(
                        state(window),
                        if (window ==
                            TurnPhase.TURN_START
                        ) {
                            Action.RollDice(a)
                        } else {
                            Action.ResolveRoll(a)
                        },
                    )
                assertEquals(result.state, MatchSaveCodec.decode(MatchSaveCodec.encode(result.state)))
                if (decisionOwner == b) {
                    assertEquals(TurnPhase.DECISION, result.state.phase)
                    result = reducer.reduce(result.state, Action.ChooseDecisionOption(b, DecisionId("last"), DecisionOptionId("skip")))
                }
                assertEquals(TurnPhase.TURN_START, result.state.phase)
                assertEquals(b, result.state.currentPlayerId)
                assertTrue(result.state.pendingDecisions.isEmpty())
            }
        }
    }

    @Test fun `queued paid abilities cannot spend the same characters twice and can be declined`() {
        val effect =
            object : CardEffect {
                override val cardId = card

                override fun activatedAbilities(context: CardHookContext) =
                    listOf("one", "two").map { id ->
                        AbilityOffer(id, id, listOf(DecisionOption(DecisionOptionId("use"), Action.ActivateAbility(a, card, id))))
                    }

                override fun activate(
                    context: CardHookContext,
                    action: Action.ActivateAbility,
                ) = listOf(
                    CardEffectIntent.ChangeResource(a, PlayerResource.CHARACTERS, -20),
                    CardEffectIntent.ChangeResource(a, PlayerResource.MANDATES, 1),
                )
            }
        val reducer = reducer(effect)
        val initial =
            state(TurnPhase.TURN_START).let {
                it.copy(
                    players =
                        it.players.map { p ->
                            if (p.id ==
                                a
                            ) {
                                p.copy(characters = 20)
                            } else {
                                p
                            }
                        },
                )
            }
        var waiting = reducer.reduce(initial, Action.RollDice(a)).state
        waiting =
            reducer.reduce(waiting, Action.ChooseDecisionOption(a, waiting.pendingDecisions.first().id, DecisionOptionId("use"))).state
        assertEquals(1, waiting.players.first().mandates)
        val rejected = reducer.reduce(waiting, Action.ChooseDecisionOption(a, waiting.pendingDecisions.first().id, DecisionOptionId("use")))
        assertEquals(waiting, rejected.state)
        assertTrue(rejected.events.any { it is GameEvent.ActionRejected })
        val declined =
            reducer.reduce(
                waiting,
                Action.ChooseDecisionOption(a, waiting.pendingDecisions.first().id, DecisionOptionId("decline")),
            )
        assertEquals(TurnPhase.ROLLING, declined.state.phase)
        runTest {
            val controller = RealGameController(initial, backgroundScope, setOf(a), reducer, botDelayMillis = 10)
            runCurrent()
            advanceTimeBy(30)
            runCurrent()
            assertEquals(TurnPhase.ROLLING, controller.state.value.phase)
            assertTrue(
                controller.state.value.pendingDecisions
                    .isEmpty(),
            )
            assertNull(controller.failure.value)
            controller.close()
        }
    }

    @Test fun `eliminating the final queued owner resumes the original phase`() {
        val effect =
            object : CardEffect {
                override val cardId = card

                override fun activate(
                    context: CardHookContext,
                    action: Action.ActivateAbility,
                ) = listOf(CardEffectIntent.ChangeResource(b, PlayerResource.FOLLOWERS, -10))
            }
        val first =
            PendingDecision(
                DecisionId("kill"),
                a,
                DecisionKind.ACTIVATE_ABILITY,
                listOf(DecisionOption(DecisionOptionId("use"), Action.ActivateAbility(a, card, "kill"))),
            )
        val second =
            PendingDecision(
                DecisionId("other"),
                b,
                DecisionKind.ACTIVATE_ABILITY,
                listOf(DecisionOption(DecisionOptionId("decline"), Action.DeclineAbility(b))),
            )
        val waiting =
            state().copy(
                phase = TurnPhase.DECISION,
                phaseAfterDecisions = TurnPhase.ROLLING,
                pendingDecisions = listOf(first, second),
            )
        val result = reducer(effect).reduce(waiting, Action.ChooseDecisionOption(a, first.id, first.options.first().id))
        assertEquals(TurnPhase.ROLLING, result.state.phase)
        assertTrue(result.state.pendingDecisions.isEmpty())
        assertEquals(result.state, MatchSaveCodec.decode(MatchSaveCodec.encode(result.state)))
    }

    @Test fun `end turn victory clears optional queued choices immediately`() {
        val effect =
            object : CardEffect {
                override val cardId = card

                override fun onTurnEnd(context: CardHookContext) =
                    listOf(
                        CardEffectIntent.ChangeResource(a, PlayerResource.MANDATES, 20),
                        CardEffectIntent.QueueDecision(
                            PendingDecision(
                                DecisionId("late"),
                                a,
                                DecisionKind.ACTIVATE_ABILITY,
                                listOf(DecisionOption(DecisionOptionId("decline"), Action.DeclineAbility(a))),
                            ),
                        ),
                    )
            }
        val result = reducer(effect).reduce(state(TurnPhase.SHOP), Action.EndTurn(a))
        assertEquals(MatchResult.Victory(a, VictoryReason.TWENTY_MANDATES), result.state.result)
        assertTrue(result.state.pendingDecisions.isEmpty())
        assertEquals(TurnPhase.FINISHED, result.state.phase)
    }

    @Test fun `character cap is optional and applies to dice and hooks`() {
        val initial =
            state().copy(dice = listOf(Die(DieId(0), DiceFace.CHARACTERS)), characterCap = 280).let {
                it.copy(
                    players =
                        it.players.map { p ->
                            if (p.id ==
                                a
                            ) {
                                p.copy(characters = 275)
                            } else {
                                p
                            }
                        },
                )
            }
        val reducer = DefaultGameReducer()
        assertEquals(
            280,
            reducer
                .reduce(initial, Action.ResolveRoll(a))
                .state.players
                .first()
                .characters,
        )
        assertEquals(
            285,
            reducer
                .reduce(initial.copy(characterCap = null), Action.ResolveRoll(a))
                .state.players
                .first()
                .characters,
        )
    }
}
