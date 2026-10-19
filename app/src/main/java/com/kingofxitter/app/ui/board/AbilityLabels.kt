package com.kingofxitter.app.ui.board

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.kingofxitter.app.R
import com.kingofxitter.app.ui.components.label
import com.kingofxitter.app.ui.components.leaderVisual
import com.kingofxitter.cards.CardCatalog
import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.CardId
import com.kingofxitter.engine.model.DiceFace
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.PendingDecision
import com.kingofxitter.engine.model.PlayerId

/** The string resource called [name], or null when there is none (or no name at all). */
@Composable
internal fun stringByName(name: String?): String? {
    if (name.isNullOrEmpty()) return null
    val context = LocalContext.current
    val resource = context.resources.getIdentifier(name, "string", context.packageName)
    return if (resource != 0) stringResource(resource) else null
}

/** The display name of [cardId], falling back to its id when the string is missing. */
@Composable
internal fun cardName(cardId: CardId): String = stringByName(CardCatalog.byId[cardId]?.nameKey) ?: cardId.value

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

/** What the card is asking (its `promptKey`), or null for a decision without a question. */
@Composable
internal fun decisionQuestion(decision: PendingDecision): String? = stringByName(decision.promptKey)

/**
 * Describes one way to use an ability: the card, then the die, face, card or leader it picks.
 * Ability ids carry their choices after colons, for example `use_spin_doctor:LIKE` or
 * Hostile Takeover's `buy_policy:<leader>:<card>`.
 */
@Composable
internal fun abilityLabel(
    action: Action.ActivateAbility,
    state: GameState,
): String {
    val segments = action.abilityId.split(":").drop(1)
    // Hostile Takeover names the card, whose leader holds it, and its price.
    if (action.abilityId.startsWith("buy_policy:") && segments.size == 2) {
        val owner = state.players.firstOrNull { it.id == PlayerId(segments[0]) }
        val card = CardCatalog.byId[CardId(segments[1])]
        if (owner != null && card != null) {
            return stringResource(R.string.board_decision_takeover, cardName(card.id), leaderName(owner.id, state), card.cost)
        }
    }
    // Leaked Memo buys the card it peeked at, so say which one.
    if (action.abilityId == "buy_top_card") {
        val top = state.drawPileCardIds.firstOrNull()?.let { CardCatalog.byId[it] }
        if (top != null) return stringResource(R.string.board_decision_buy_cost, cardName(top.id), top.cost)
    }
    val parts = mutableListOf<String>()
    action.dieIds.firstOrNull()?.let { parts += stringResource(R.string.board_ability_die, it.value + 1) }
    segments.forEach { segment ->
        val face = DiceFace.entries.firstOrNull { it.name == segment }
        val card = CardCatalog.byId[CardId(segment)]
        when {
            face != null -> parts += stringResource(face.label())
            card != null -> parts += cardName(card.id)
        }
    }
    action.targetPlayerId?.let { parts += leaderName(it, state) }
    return parts.ifEmpty { listOf(stringResource(R.string.board_decision_use)) }.joinToString(" · ")
}

@Composable
private fun leaderName(
    playerId: PlayerId,
    state: GameState,
): String {
    val leader = state.players.firstOrNull { it.id == playerId } ?: return playerId.value
    return stringResource(leaderVisual(leader.leaderId).shortName)
}
