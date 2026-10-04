package com.kingofxitter.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.kingofxitter.app.R
import com.kingofxitter.app.ui.theme.StickerShape
import com.kingofxitter.app.ui.theme.XColors
import com.kingofxitter.app.ui.theme.XType
import com.kingofxitter.app.ui.theme.sticker
import com.kingofxitter.cards.CardDef
import com.kingofxitter.cards.CardType

/**
 * Sticker card component: cost bubble, POLICY/ORDER tag, text.
 */
@Composable
fun PowerUpCard(
    card: CardDef,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val context = LocalContext.current
    val nameRes = remember(card.nameKey) { context.resources.getIdentifier(card.nameKey, "string", context.packageName) }
    val rulesRes = remember(card.rulesKey) { context.resources.getIdentifier(card.rulesKey, "string", context.packageName) }
    val flavourRes = remember(card.flavourKey) { context.resources.getIdentifier(card.flavourKey, "string", context.packageName) }

    val name =
        if (nameRes != 0) {
            stringResource(nameRes)
        } else {
            card.id.value
                .replace('-', ' ')
                .uppercase()
        }
    val rules = if (rulesRes != 0) stringResource(rulesRes) else stringResource(R.string.card_rules_missing)
    val flavour = if (flavourRes != 0) stringResource(flavourRes) else ""

    Box(
        modifier =
            modifier
                .padding(bottom = 6.dp) // Room for shadow
                .sticker(
                    fill = XColors.Paper,
                    shape = StickerShape,
                    halo = 0.dp,
                ).padding(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header: Cost Bubble + Type Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Cost bubble
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .background(XColors.Night, CircleShape)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    XIconImage(
                        icon = XIcon.Characters,
                        contentDescription = null,
                        size = 14.dp,
                    )
                    Text(
                        text = card.cost.toString(),
                        style = XType.Stat,
                        color = XColors.Paper,
                    )
                }

                // POLICY/ORDER Tag
                val typeText = stringResource(if (card.type == CardType.POLICY) R.string.card_type_policy else R.string.card_type_order)
                val typeColor = if (card.type == CardType.POLICY) XColors.Mandates else XColors.Followers
                XTag(
                    text = typeText,
                    fill = typeColor,
                    textStyle = XType.Tag,
                )
            }

            // Title
            Text(
                text = name,
                style = if (compact) XType.DisplaySmall else XType.DisplayMedium,
                color = XColors.Ink,
            )

            // Rules text
            if (!compact) {
                Text(
                    text = rules,
                    style = XType.Body,
                    color = XColors.Ink,
                )

                if (flavour.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = flavour,
                        style = XType.Caption.copy(fontStyle = FontStyle.Italic),
                        color = XColors.InkMuted,
                    )
                }
            }
        }
    }
}
