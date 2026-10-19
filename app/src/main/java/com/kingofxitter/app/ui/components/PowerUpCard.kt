package com.kingofxitter.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.kingofxitter.app.R
import com.kingofxitter.app.ui.theme.XColors
import com.kingofxitter.app.ui.theme.XType
import com.kingofxitter.app.ui.theme.sticker
import com.kingofxitter.cards.CardDef
import com.kingofxitter.cards.CardType

/**
 * The shop's card (`design/screens/power-up-shop.html`): an art panel with the cost bubble, then
 * the POLICY or EXECUTIVE ORDER tag, the name, the rules and the flavour line, with [action]
 * (BUY, or how many characters you're short) at the bottom right. A [dimmed] card is one you
 * can't afford yet. [cost] is what you would pay, which Lobbyist can make less than the card's.
 */
@Composable
fun PowerUpCard(
    card: CardDef,
    modifier: Modifier = Modifier,
    cost: Int = card.cost,
    dimmed: Boolean = false,
    action: (@Composable () -> Unit)? = null,
) {
    val text = cardText(card)
    val art = cardArt(card)
    Row(
        modifier
            .padding(bottom = 6.dp)
            .sticker(fill = if (dimmed) XColors.PaperDimmed else XColors.Paper)
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.width(ART_WIDTH)) {
            Box(
                Modifier
                    .padding(top = 6.dp, start = 6.dp)
                    .width(ART_WIDTH - 6.dp)
                    .height(ART_HEIGHT)
                    .sticker(fill = art.color, shape = RoundedCornerShape(14.dp), shadowOffset = DpOffset.Zero),
                contentAlignment = Alignment.Center,
            ) {
                XIconImage(art.icon, contentDescription = null, size = 34.dp, tint = XColors.Ink)
            }
            CostBubble(cost, Modifier.offset(x = (-4).dp, y = (-4).dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TypeTag(card.type)
            Text(text.name, style = XType.DisplayMedium, color = XColors.Ink)
            Text(text.rules, style = XType.BodyStrong, color = XColors.Ink)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text.flavour,
                    style = XType.Caption.copy(fontStyle = FontStyle.Italic),
                    color = XColors.InkMuted,
                    modifier = Modifier.weight(1f),
                )
                action?.invoke()
            }
        }
    }
}

/** A held policy as the design's paper pill ("SPEECHWRITER"), read out with its rules. */
@Composable
fun PolicyPill(
    card: CardDef,
    modifier: Modifier = Modifier,
) {
    val text = cardText(card)
    XChip(
        text.name.uppercase(),
        modifier = modifier.semantics { contentDescription = "${text.name}. ${text.rules}" },
        minHeight = 30.dp,
    )
}

/** A card's display strings, from the keys the catalogue carries. */
private data class CardText(
    val name: String,
    val rules: String,
    val flavour: String,
)

@Composable
private fun cardText(card: CardDef): CardText {
    val context = LocalContext.current

    fun id(key: String) = context.resources.getIdentifier(key, "string", context.packageName)
    val ids = remember(card.id) { Triple(id(card.nameKey), id(card.rulesKey), id(card.flavourKey)) }
    return CardText(
        name =
            if (ids.first != 0) {
                stringResource(ids.first)
            } else {
                card.id.value
                    .replace('_', ' ')
                    .uppercase()
            },
        rules = if (ids.second != 0) stringResource(ids.second) else stringResource(R.string.card_rules_missing),
        flavour = if (ids.third != 0) stringResource(ids.third) else "",
    )
}

@Composable
private fun CostBubble(
    cost: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier.size(40.dp).sticker(fill = XColors.Characters, shape = CircleShape, shadowOffset = DpOffset(0.dp, 3.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(cost.toString(), style = XType.DisplaySmall, color = XColors.Ink, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun TypeTag(type: CardType) {
    val policy = type == CardType.POLICY
    XTag(
        stringResource(if (policy) R.string.card_type_policy else R.string.card_type_order),
        fill = if (policy) XColors.BlueCheck else XColors.OrderTag,
        textColor = XColors.Paper,
    )
}

private val ART_WIDTH = 92.dp
private val ART_HEIGHT = 112.dp

/** The art panel: an icon for what the card does, on that effect's colour. */
internal data class CardArt(
    val icon: XIcon,
    val color: Color,
)

/**
 * Art for each of the 66 cards (ROADMAP §2), grouped by what they mostly do: followers and likes,
 * mandates, dice and rerolls, unfollows, characters, Xitter itself, and the odd ones out.
 */
internal fun cardArt(card: CardDef): CardArt {
    val number =
        card.id.value
            .removePrefix("c")
            .takeWhile { it.isDigit() }
            .toIntOrNull()
    return when (number) {
        4, 7, 20, 37, 38, 48, 59 -> CardArt(XIcon.Like, ArtPink)
        3, 8, 9, 10, 11, 19, 21, 30, 39, 49, 50, 51, 58, 61, 62, 63, 64, 65 -> CardArt(XIcon.Mandates, ArtGold)
        5, 13, 14, 16, 18, 22, 33, 36, 41, 44, 45, 57 -> CardArt(XIcon.Reroll, ArtBlue)
        1, 6, 15, 29, 34, 35, 40, 43, 56 -> CardArt(XIcon.Tweet, ArtOrange)
        2, 17, 25, 26, 28, 31, 32, 42, 47, 53, 66 -> CardArt(XIcon.Characters, ArtLime)
        24, 46, 52 -> CardArt(XIcon.Crown, ArtYellow)
        12, 23 -> CardArt(XIcon.Bot, ArtPurple)
        27 -> CardArt(XIcon.Shuffle, ArtTeal)
        54, 55, 60 -> CardArt(XIcon.Reply, ArtMustard)
        else -> CardArt(XIcon.Characters, ArtLime)
    }
}

private val ArtPink = Color(0xFFFF8FC8)
private val ArtGold = Color(0xFFFFD23F)
private val ArtBlue = Color(0xFF7FB8FF)
private val ArtOrange = Color(0xFFFFA25C)
private val ArtLime = Color(0xFFC6F57A)
private val ArtYellow = Color(0xFFFFE89A)
private val ArtPurple = Color(0xFF9B7BFF)
private val ArtTeal = Color(0xFF5ED6C4)
private val ArtMustard = Color(0xFFD9B44A)
