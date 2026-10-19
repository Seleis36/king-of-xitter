package com.kingofxitter.app.ui.shop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kingofxitter.app.R
import com.kingofxitter.app.ui.components.CharactersRing
import com.kingofxitter.app.ui.components.OutlinedText
import com.kingofxitter.app.ui.components.PolicyPill
import com.kingofxitter.app.ui.components.PowerUpCard
import com.kingofxitter.app.ui.components.XButton
import com.kingofxitter.app.ui.components.XButtonStyle
import com.kingofxitter.app.ui.components.XIcon
import com.kingofxitter.app.ui.components.XIconButton
import com.kingofxitter.app.ui.components.XPanel
import com.kingofxitter.app.ui.theme.XColors
import com.kingofxitter.app.ui.theme.XType
import com.kingofxitter.app.ui.theme.XitterTheme
import com.kingofxitter.app.ui.theme.nightBackground
import com.kingofxitter.cards.CardCatalog
import com.kingofxitter.cards.ShopLogic
import com.kingofxitter.engine.GameFixtures
import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.PlayerId
import com.kingofxitter.engine.model.TurnPhase

/**
 * Power-up shop (`design/screens/power-up-shop.html`): your characters, the three cards on the
 * shelf with what each costs you, the sweep, and the policies you hold. Buying and sweeping are
 * only offered in your shop phase; at any other time the shop can be browsed.
 */
@Composable
fun ShopScreen(
    state: GameState,
    humanId: PlayerId,
    onAction: (Action) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val player = state.players.first { it.id == humanId }
    val isShoppingPhase = state.phase == TurnPhase.SHOP && state.currentPlayerId == humanId && state.pendingDecisions.isEmpty()

    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                XIconButton(XIcon.Back, stringResource(R.string.shop_close), onClose)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedText(
                        stringResource(R.string.shop_title),
                        XType.DisplayLarge,
                        XColors.Paper,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        stringResource(if (isShoppingPhase) R.string.shop_subtitle else R.string.shop_subtitle_browsing),
                        style = XType.Body,
                        color = XColors.Muted,
                    )
                }
                CharactersRing(player.characters)
            }

            if (state.shopCardIds.isEmpty()) Text(stringResource(R.string.shop_empty), style = XType.Body, color = XColors.Muted)

            state.shopCardIds.forEachIndexed { index, cardId ->
                // A card the catalogue doesn't know (an old save, a test fixture) can't be shown or bought.
                val card = CardCatalog.byId[cardId] ?: return@forEachIndexed
                // The price you pay, which Lobbyist lowers below the card's printed cost.
                val price = ShopLogic.costFor(state, player, card)
                val short = (price - player.characters).coerceAtLeast(0)
                PowerUpCard(
                    card = card,
                    modifier = Modifier.fillMaxWidth().rotate(if (index % 2 == 0) -0.8f else 0.6f),
                    cost = price,
                    dimmed = short > 0,
                    action =
                        if (isShoppingPhase) {
                            {
                                XButton(
                                    if (short > 0) stringResource(R.string.shop_short, short) else stringResource(R.string.shop_buy_short),
                                    onClick = { onAction(Action.BuyPowerUp(humanId, cardId)) },
                                    enabled = short == 0,
                                    minHeight = 48.dp,
                                    textStyle = XType.DisplaySmall,
                                )
                            }
                        } else {
                            null
                        },
                )
            }

            if (isShoppingPhase) {
                XButton(
                    stringResource(R.string.shop_sweep_timeline),
                    onClick = { onAction(Action.SweepShop(humanId)) },
                    modifier = Modifier.fillMaxWidth(),
                    style = XButtonStyle.Ghost,
                    enabled = player.characters >= ShopLogic.SWEEP_COST && state.shopCardIds.isNotEmpty(),
                    icon = XIcon.Sweep,
                    subtitle = stringResource(R.string.shop_sweep_cost, ShopLogic.SWEEP_COST),
                    minHeight = 56.dp,
                    textStyle = XType.DisplaySmall,
                )
            }

            YourPolicies(state, humanId)
        }
        XButton(
            stringResource(if (isShoppingPhase) R.string.shop_done else R.string.shop_back),
            onClick = onClose,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        )
    }
}

/** The policies you hold, as the design's pills; nothing when you hold none. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun YourPolicies(
    state: GameState,
    humanId: PlayerId,
) {
    val held =
        state.players
            .firstOrNull { it.id == humanId }
            ?.policyIds
            .orEmpty()
            .mapNotNull { CardCatalog.byId[it] }
    if (held.isEmpty()) return
    XPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.shop_your_policies), style = XType.DisplaySmall, color = XColors.Paper)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                held.forEach { PolicyPill(it) }
            }
        }
    }
}

@Composable
private fun ShopPreview(
    state: GameState,
    humanId: PlayerId,
) {
    XitterTheme { ShopScreen(state, humanId, onAction = {}, onClose = {}, modifier = Modifier.nightBackground()) }
}

@Preview(name = "Design: power-up-shop.html", widthDp = 412, heightDp = 915)
@Composable
private fun ShopDesignPreview() = ShopPreview(GameFixtures.Design.shop, GameFixtures.Design.shop.currentPlayerId)

@Preview(name = "Fixture: shop", widthDp = 320, heightDp = 640, fontScale = 1.5f)
@Composable
private fun ShopFixturePreview() = ShopPreview(GameFixtures.shop, GameFixtures.shop.currentPlayerId)

@Preview(name = "Browsing on another turn", widthDp = 412, heightDp = 915)
@Composable
private fun ShopBrowsingPreview() = ShopPreview(GameFixtures.Design.board, GameFixtures.Design.board.currentPlayerId)
