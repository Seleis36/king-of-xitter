package com.kingofxitter.engine

import com.kingofxitter.engine.model.AbilityOffer
import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.CardEffect
import com.kingofxitter.engine.model.CardEffectIntent
import com.kingofxitter.engine.model.CardEffectRegistry
import com.kingofxitter.engine.model.CardHookContext
import com.kingofxitter.engine.model.CardId
import com.kingofxitter.engine.model.CounterId
import com.kingofxitter.engine.model.DamageAdjustment
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
import com.kingofxitter.engine.model.ResourceChangeReason
import com.kingofxitter.engine.model.TurnPhase
import com.kingofxitter.engine.model.VictoryReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CardHookReducerTest {
    private val player = PlayerId("player")
    private val target = PlayerId("target")
    private val policyId = CardId("policy")
    private val purchasedId = CardId("order")

    @Test
    fun `turn start and dice hooks add resources dice and rerolls once`() {
        val effect =
            object : CardEffect {
                override val cardId = policyId

                override fun onTurnStart(context: CardHookContext) =
                    listOf(CardEffectIntent.ChangeResource(player, PlayerResource.CHARACTERS, 10))

                override fun diceCount(context: CardHookContext) = 1

                override fun rerollCount(context: CardHookContext) = 1
            }
        val reducer = reducer(effect)

        var transition = reducer.reduce(state(TurnPhase.TURN_START), Action.RollDice(player))
        assertEquals(7, transition.state.dice.size)
        assertEquals(
            10,
            transition.state.players
                .first()
                .characters,
        )
        assertEquals(1, transition.state.rollNumber)

        repeat(3) {
            transition =
                reducer.reduce(
                    transition.state,
                    Action.RollDice(
                        player,
                        transition.state.dice
                            .map { die -> die.id }
                            .toSet(),
                    ),
                )
        }
        assertEquals(4, transition.state.rollNumber)

        val rejected =
            reducer.reduce(
                transition.state,
                Action.RollDice(
                    player,
                    transition.state.dice
                        .map { die -> die.id }
                        .toSet(),
                ),
            )
        assertEquals(transition.state, rejected.state)
        assertIs<GameEvent.ActionRejected>(rejected.events.single())
    }

    @Test
    fun `purchase dispatch runs buyer policies and purchased card in stable order`() {
        val active =
            object : CardEffect {
                override val cardId = CardId("a-active")

                override fun onBuy(context: CardHookContext) =
                    listOf(CardEffectIntent.ChangeResource(player, PlayerResource.CHARACTERS, 10))
            }
        val order =
            object : CardEffect {
                override val cardId = purchasedId

                override fun onBuy(context: CardHookContext) =
                    listOf(CardEffectIntent.ChangeResource(player, PlayerResource.CHARACTERS, 20))
            }
        val reducer = DefaultGameReducer(CardEffectRegistry(listOf(order, active)))
        val initial =
            state(TurnPhase.SHOP).copy(
                players =
                    listOf(
                        PlayerState(player, LeaderId("leader"), policyIds = listOf(active.cardId)),
                        PlayerState(target, LeaderId("target")),
                    ),
            )

        val transition = reducer.dispatchOnBuy(initial, player, purchasedId)

        assertEquals(
            30,
            transition.state.players
                .first()
                .characters,
        )
        assertEquals(listOf(10, 20), transition.events.map { (it as GameEvent.ResourceChanged).amount })
    }

    @Test
    fun `purchase effect that reaches twenty mandates ends the match`() {
        val order =
            object : CardEffect {
                override val cardId = purchasedId

                override fun onBuy(context: CardHookContext) = listOf(CardEffectIntent.ChangeResource(player, PlayerResource.MANDATES, 1))
            }
        val reducer = reducer(order)
        val initial =
            state(TurnPhase.SHOP).copy(
                players =
                    listOf(
                        PlayerState(player, LeaderId("leader"), mandates = 19),
                        PlayerState(target, LeaderId("target")),
                    ),
            )

        val transition = reducer.dispatchOnBuy(initial, player, purchasedId)

        assertEquals(MatchResult.Victory(player, VictoryReason.TWENTY_MANDATES), transition.state.result)
        assertTrue(transition.events.any { it is GameEvent.MatchEnded })
    }

    @Test
    fun `turn start queued decision resumes rolling after its option is chosen`() {
        val decisionId =
            com.kingofxitter.engine.model
                .DecisionId("start-choice")
        val optionId = DecisionOptionId("continue")
        val effect =
            object : CardEffect {
                override val cardId = policyId

                override fun onTurnStart(context: CardHookContext) =
                    listOf(
                        CardEffectIntent.QueueDecision(
                            PendingDecision(
                                decisionId,
                                player,
                                DecisionKind.ACTIVATE_ABILITY,
                                listOf(DecisionOption(optionId, Action.RollDice(player))),
                            ),
                        ),
                    )
            }
        val reducer = reducer(effect)
        val waiting =
            reducer.reduce(
                state(TurnPhase.TURN_START).copy(
                    players =
                        listOf(
                            PlayerState(player, LeaderId("leader"), policyIds = listOf(policyId)),
                            PlayerState(target, LeaderId("target")),
                        ),
                ),
                Action.RollDice(player),
            )

        val resumed =
            reducer.reduce(
                waiting.state,
                Action.ChooseDecisionOption(player, decisionId, optionId),
            )

        assertEquals(TurnPhase.ROLLING, resumed.state.phase)
        assertEquals(1, resumed.state.rollNumber)
        assertTrue(resumed.events.any { it is GameEvent.DecisionResolved })
    }

    @Test
    fun `resolve and turn end hooks apply their intents`() {
        val effect =
            object : CardEffect {
                override val cardId = policyId

                override fun onResolve(context: CardHookContext) =
                    listOf(CardEffectIntent.ChangeResource(player, PlayerResource.CHARACTERS, 10))

                override fun onTurnEnd(context: CardHookContext) =
                    listOf(CardEffectIntent.ChangeResource(player, PlayerResource.MANDATES, 1))
            }
        val reducer = reducer(effect)
        val rolling =
            state(TurnPhase.ROLLING).copy(
                dice = listOf(Die(DieId(0), DiceFace.CHARACTERS)),
                rollNumber = 1,
            )

        val resolved = reducer.reduce(rolling, Action.ResolveRoll(player))
        val ended = reducer.reduce(resolved.state.copy(phase = TurnPhase.END_TURN), Action.EndTurn(player))

        assertEquals(
            20,
            ended.state.players
                .first()
                .characters,
        )
        assertEquals(
            1,
            ended.state.players
                .first()
                .mandates,
        )
    }

    @Test
    fun `damage hooks combine outgoing bonus and incoming reduction`() {
        val attackerEffect =
            object : CardEffect {
                override val cardId = policyId

                override fun modifyOutgoingDamage(context: CardHookContext) = DamageAdjustment(bonusDamage = 1)
            }
        val defenderEffect =
            object : CardEffect {
                override val cardId = CardId("defense")

                override fun modifyIncomingDamage(context: CardHookContext) = DamageAdjustment(damageReduction = 1)
            }
        val initial =
            GameState(
                players =
                    listOf(
                        PlayerState(player, LeaderId("leader"), policyIds = listOf(policyId)),
                        PlayerState(target, LeaderId("target"), followers = 10, policyIds = listOf(defenderEffect.cardId)),
                    ),
                currentPlayerId = player,
                phase = TurnPhase.ROLLING,
                xitterPlayerId = target,
                dice = List(3) { index -> Die(DieId(index), DiceFace.TWEET) },
                rollNumber = 1,
                randomState = 17L,
            )

        val transition = reducer(attackerEffect, defenderEffect).reduce(initial, Action.ResolveRoll(player))

        assertEquals(
            7,
            transition.state.players
                .first { it.id == target }
                .followers,
        )
    }

    @Test
    fun `offered activated ability applies its intent through a decision`() {
        val action = Action.ActivateAbility(player, policyId, "bonus")
        val effect =
            object : CardEffect {
                override val cardId = policyId

                override fun activatedAbilities(context: CardHookContext) =
                    if (context.window.name == "AFTER_DICE_ROLL") {
                        listOf(
                            AbilityOffer(
                                "bonus",
                                "ability.bonus.prompt",
                                listOf(DecisionOption(DecisionOptionId("use"), action)),
                            ),
                        )
                    } else {
                        emptyList()
                    }

                override fun activate(
                    context: CardHookContext,
                    action: Action.ActivateAbility,
                ) = listOf(CardEffectIntent.ChangeCounter(player, CounterId("uses"), 1))
            }
        val reducer = reducer(effect)
        val rolled =
            reducer.reduce(
                state(TurnPhase.TURN_START).copy(
                    players =
                        listOf(
                            PlayerState(player, LeaderId("leader"), policyIds = listOf(policyId)),
                            PlayerState(target, LeaderId("target")),
                        ),
                ),
                Action.RollDice(player),
            )
        val decision = rolled.state.pendingDecisions.single()

        val chosen =
            reducer.reduce(
                rolled.state,
                Action.ChooseDecisionOption(player, decision.id, decision.options.single().id),
            )

        assertEquals(TurnPhase.ROLLING, chosen.state.phase)
        assertEquals(
            1,
            chosen.state.players
                .first()
                .counters[CounterId("uses")],
        )
        assertTrue(chosen.events.any { it is GameEvent.DecisionResolved })
    }

    @Test
    fun `after resolve ability is offered before shopping`() {
        val action = Action.ActivateAbility(player, policyId, "after-resolve")
        val effect =
            object : CardEffect {
                override val cardId = policyId

                override fun activatedAbilities(context: CardHookContext) =
                    if (context.window.name == "AFTER_RESOLVE") {
                        listOf(
                            AbilityOffer(
                                "after-resolve",
                                "ability.after-resolve.prompt",
                                listOf(DecisionOption(DecisionOptionId("use"), action)),
                            ),
                        )
                    } else {
                        emptyList()
                    }
            }
        val reducer = reducer(effect)
        val rolling =
            state(TurnPhase.ROLLING).copy(
                players =
                    listOf(
                        PlayerState(player, LeaderId("leader"), policyIds = listOf(policyId)),
                        PlayerState(target, LeaderId("target")),
                    ),
                dice = listOf(Die(DieId(0), DiceFace.CHARACTERS)),
                rollNumber = 1,
            )

        val waiting = reducer.reduce(rolling, Action.ResolveRoll(player))

        assertEquals(TurnPhase.DECISION, waiting.state.phase)
        assertEquals(
            "ability.after-resolve.prompt",
            waiting.state.pendingDecisions
                .single()
                .promptKey,
        )
        assertEquals(TurnPhase.SHOP, waiting.state.phaseAfterDecisions)
    }

    @Test
    fun `elimination hook can restore followers before suspension`() {
        val rescue =
            object : CardEffect {
                override val cardId = policyId

                override fun onEliminated(context: CardHookContext) =
                    listOf(CardEffectIntent.ChangeResource(target, PlayerResource.FOLLOWERS, 1, ResourceChangeReason.POWER_UP))
            }
        val initial =
            GameState(
                players =
                    listOf(
                        PlayerState(player, LeaderId("leader")),
                        PlayerState(target, LeaderId("target"), followers = 1, policyIds = listOf(policyId)),
                    ),
                currentPlayerId = player,
                phase = TurnPhase.ROLLING,
                xitterPlayerId = target,
                dice = listOf(Die(DieId(0), DiceFace.TWEET)),
                rollNumber = 1,
            )

        val transition = reducer(rescue).reduce(initial, Action.ResolveRoll(player))

        assertEquals(
            1,
            transition.state.players
                .first { it.id == target }
                .followers,
        )
        assertTrue(transition.events.none { it == GameEvent.PlayerSuspended(target) })
    }

    @Test
    fun `follower loss from another hook still dispatches elimination effects`() {
        val damageCard = CardId("damage")
        val damageEffect =
            object : CardEffect {
                override val cardId = damageCard

                override fun onResolve(context: CardHookContext) =
                    listOf(CardEffectIntent.ChangeResource(target, PlayerResource.FOLLOWERS, -1))
            }
        val rescueEffect =
            object : CardEffect {
                override val cardId = policyId

                override fun onEliminated(context: CardHookContext) =
                    listOf(CardEffectIntent.ChangeResource(target, PlayerResource.FOLLOWERS, 1))
            }
        val initial =
            GameState(
                players =
                    listOf(
                        PlayerState(player, LeaderId("leader"), policyIds = listOf(damageCard)),
                        PlayerState(target, LeaderId("target"), followers = 1, policyIds = listOf(policyId)),
                    ),
                currentPlayerId = player,
                phase = TurnPhase.ROLLING,
                xitterPlayerId = target,
                dice = listOf(Die(DieId(0), DiceFace.CHARACTERS)),
                rollNumber = 1,
            )

        val transition = reducer(damageEffect, rescueEffect).reduce(initial, Action.ResolveRoll(player))

        assertEquals(
            1,
            transition.state.players
                .first { it.id == target }
                .followers,
        )
        assertEquals(target, transition.state.xitterPlayerId)
        assertTrue(transition.events.none { it == GameEvent.PlayerSuspended(target) })
    }

    @Test
    fun `extra turn is consumed and keeps the same player active`() {
        val initial =
            state(TurnPhase.END_TURN).copy(
                players =
                    listOf(
                        PlayerState(player, LeaderId("leader"), extraTurns = 1),
                        PlayerState(target, LeaderId("target")),
                    ),
            )

        val transition = DefaultGameReducer().reduce(initial, Action.EndTurn(player))

        assertEquals(player, transition.state.currentPlayerId)
        assertEquals(
            0,
            transition.state.players
                .first()
                .extraTurns,
        )
        assertEquals(TurnPhase.TURN_START, transition.state.phase)
        assertTrue(transition.events.any { it is GameEvent.ExtraTurnsChanged })
    }

    private fun reducer(vararg effects: CardEffect) = DefaultGameReducer(CardEffectRegistry(effects.toList()))

    private fun state(phase: TurnPhase) =
        GameState(
            players = listOf(PlayerState(player, LeaderId("leader"), policyIds = listOf(policyId))),
            currentPlayerId = player,
            phase = phase,
            randomState = 42L,
        )
}
