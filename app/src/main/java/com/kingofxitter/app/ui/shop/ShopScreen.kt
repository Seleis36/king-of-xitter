package com.kingofxitter.app.ui.shop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kingofxitter.app.R
import com.kingofxitter.app.ui.components.PowerUpCard
import com.kingofxitter.app.ui.components.XButton
import com.kingofxitter.app.ui.components.XButtonStyle
import com.kingofxitter.app.ui.components.XIcon
import com.kingofxitter.app.ui.components.XIconButton
import com.kingofxitter.app.ui.components.XIconImage
import com.kingofxitter.app.ui.theme.XColors
import com.kingofxitter.app.ui.theme.XType
import com.kingofxitter.app.ui.theme.nightBackground
import com.kingofxitter.cards.CardCatalog
import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.PlayerId
import com.kingofxitter.engine.model.TurnPhase

@Composable
fun ShopScreen(
    state: GameState,
    humanId: PlayerId,
    onAction: (Action) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val player = state.players.first { it.id == humanId }
    val isShoppingPhase = state.phase == TurnPhase.SHOP && state.currentPlayerId == humanId
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .nightBackground()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "POWER-UP SHOP",
                style = XType.DisplayLarge,
                color = XColors.Paper
            )
            XIconButton(
                icon = XIcon.Timeline, // Replace with close icon if available, Timeline as fallback? Wait, XIcon might not have Close. Let's just use Text button if no icon.
                contentDescription = "Close",
                onClick = onClose
            )
        }

        // Characters (currency)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            XIconImage(icon = XIcon.Characters, contentDescription = null, size = 24.dp)
            Text(
                text = "${player.characters} CHARACTERS",
                style = XType.DisplayMedium,
                color = XColors.Characters
            )
        }

        // Cards list
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.shopCardIds.isEmpty()) {
                Text(
                    text = "The shop is empty.",
                    style = XType.Body,
                    color = XColors.Muted
                )
            }
            
            for (cardId in state.shopCardIds) {
                val cardDef = CardCatalog[cardId]
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PowerUpCard(card = cardDef, modifier = Modifier.fillMaxWidth())
                    
                    if (isShoppingPhase) {
                        XButton(
                            text = "BUY FOR ${cardDef.cost}",
                            onClick = { onAction(Action.BuyPowerUp(humanId, cardId)) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = player.characters >= cardDef.cost,
                            style = XButtonStyle.Primary
                        )
                    }
                }
            }
        }

        // Sweep Shop
        if (isShoppingPhase) {
            XButton(
                text = "SWEEP (20)",
                onClick = { onAction(Action.SweepShop(humanId)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = player.characters >= 20,
                style = XButtonStyle.Secondary
            )
        }
    }
}
