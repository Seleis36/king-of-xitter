package com.kingofxitter.app.ui.board

import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.DecisionOption
import com.kingofxitter.engine.model.DiceFace
import com.kingofxitter.engine.model.DieId
import com.kingofxitter.engine.model.PendingDecision
import com.kingofxitter.engine.model.PlayerId

/**
 * C9: how the board asks for one of the human's power-up decisions. The cards describe each way
 * to use an ability as a [DecisionOption]; this sorts those options into the question the player
 * actually answers, so a die is picked on the dice tray, a face from a row of dice and a leader
 * from their portraits, instead of from a list of "Die 3 · Like" buttons.
 *
 * [decline] is the option that skips the ability, when the decision has one.
 */
sealed interface AbilityPrompt {
    val decline: DecisionOption?

    /** Pick one of the dice on the tray (Lurker, Poll Rigger, Phone Tap). */
    data class PickDie(
        val byDie: Map<DieId, DecisionOption>,
        override val decline: DecisionOption?,
    ) : AbilityPrompt

    /** Pick a die on the tray, then the face it becomes (October Surprise, Spin Doctor). */
    data class PickDieThenFace(
        val byDie: Map<DieId, Map<DiceFace, DecisionOption>>,
        override val decline: DecisionOption?,
    ) : AbilityPrompt

    /** Pick another leader (Retweet for Retweet). */
    data class PickLeader(
        val byLeader: Map<PlayerId, DecisionOption>,
        override val decline: DecisionOption?,
    ) : AbilityPrompt

    /** Anything else: one button per option, such as a single "use it" or a card to buy. */
    data class Choose(
        val options: List<DecisionOption>,
        override val decline: DecisionOption?,
    ) : AbilityPrompt

    companion object {
        /** Sorts [decision]'s options into the simplest question that covers all of them. */
        fun of(decision: PendingDecision): AbilityPrompt {
            val decline = decision.options.firstOrNull { it.action is Action.DeclineAbility }
            val uses = decision.options.filter { it.action !is Action.DeclineAbility }
            val abilities = uses.map { it.action }.filterIsInstance<Action.ActivateAbility>()
            if (abilities.isEmpty() || abilities.size != uses.size) return Choose(uses, decline)

            if (abilities.all { it.targetPlayerId != null } && abilities.map { it.targetPlayerId }.distinct().size == abilities.size) {
                return PickLeader(uses.associateBy { (it.action as Action.ActivateAbility).targetPlayerId!! }, decline)
            }
            if (abilities.any { it.dieIds.size != 1 || it.targetPlayerId != null }) return Choose(uses, decline)

            val faces = abilities.map { faceOf(it) }
            if (faces.all { it != null }) {
                val byDie =
                    uses
                        .groupBy { (it.action as Action.ActivateAbility).dieIds.single() }
                        .mapValues { (_, options) -> options.associateBy { faceOf(it.action as Action.ActivateAbility)!! } }
                return PickDieThenFace(byDie, decline)
            }
            if (faces.any { it != null }) return Choose(uses, decline)
            val byDie = uses.associateBy { (it.action as Action.ActivateAbility).dieIds.single() }
            // Two ways to use the same die can't be told apart on the tray.
            if (byDie.size != uses.size) return Choose(uses, decline)
            return PickDie(byDie, decline)
        }

        /** The face an ability sets, carried after a colon in its id, for example `use_spin_doctor:LIKE`. */
        internal fun faceOf(action: Action.ActivateAbility): DiceFace? {
            val suffix = action.abilityId.substringAfter(":", missingDelimiterValue = "")
            return DiceFace.entries.firstOrNull { it.name == suffix }
        }
    }
}
