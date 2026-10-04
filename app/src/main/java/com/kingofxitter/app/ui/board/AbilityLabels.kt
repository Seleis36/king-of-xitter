package com.kingofxitter.app.ui.board

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.kingofxitter.app.R
import com.kingofxitter.cards.CardCatalog
import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.CardId
import com.kingofxitter.engine.model.DiceFace
import com.kingofxitter.engine.model.PendingDecision

/** The display name of [cardId], falling back to its id when the string is missing. */
@Composable
internal fun cardName(cardId: CardId): String {
    val context = LocalContext.current
    val key = CardCatalog.byId[cardId]?.nameKey ?: return cardId.value
    val resource = context.resources.getIdentifier(key, "string", context.packageName)
    return if (resource != 0) stringResource(resource) else cardId.value
}

/** The prompt above a power-up decision: the card that is asking, when there is one. */
@Composable
internal fun decisionPrompt(decision: PendingDecision): String {
    val cardId =
        decision.options
            .map { it.action }
            .filterIsInstance<Action.ActivateAbility>()
            .firstOrNull()
            ?.cardId
    return if (cardId != null) {
        stringResource(R.string.board_decision_prompt_card, cardName(cardId))
    } else {
        stringResource(R.string.board_decision_prompt)
    }
}

/**
 * Describes one way to use an ability: the card, then the die, face, card or leader it picks.
 * Ability ids carry their choice after a colon, for example `use_spin_doctor:LIKE`.
 */
@Composable
internal fun abilityLabel(action: Action.ActivateAbility): String {
    val parts = mutableListOf(cardName(action.cardId))
    action.dieIds.firstOrNull()?.let { parts += stringResource(R.string.board_ability_die, it.value + 1) }
    action.abilityId
        .split(":")
        .drop(1)
        .forEach { segment ->
            val face = DiceFace.entries.firstOrNull { it.name == segment }
            val card = CardCatalog.byId[CardId(segment)]
            when {
                face != null -> parts += face.name.lowercase().replaceFirstChar { it.uppercase() }
                card != null -> parts += cardName(card.id)
            }
        }
    action.targetPlayerId?.let { parts += it.value.replaceFirstChar { c -> c.uppercase() } }
    return parts.joinToString(" \u00B7 ")
}
