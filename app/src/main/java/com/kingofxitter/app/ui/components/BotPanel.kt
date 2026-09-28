package com.kingofxitter.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingofxitter.app.R
import com.kingofxitter.app.ui.theme.XColors
import com.kingofxitter.app.ui.theme.XType
import com.kingofxitter.engine.model.PlayerState

/** How a bot panel is highlighted. */
enum class BotPanelHighlight { None, MainCharacter, Acting }

/**
 * Compact opponent panel for the top of the board: avatar, short name with certification, "BOT",
 * and followers, characters and mandates. The Main Character gets the gold "on xitter" look;
 * a suspended bot is dimmed and stamped. [stampModifier] animates the stamp (U8). Narrow panels
 * (small phones, large font scales) stack the header and wrap the stats.
 */
@Composable
fun BotPanel(
    player: PlayerState,
    modifier: Modifier = Modifier,
    highlight: BotPanelHighlight = BotPanelHighlight.None,
    stampModifier: Modifier = Modifier,
) {
    val isMainCharacter = highlight == BotPanelHighlight.MainCharacter
    val state =
        when {
            player.isSuspended -> stringResource(R.string.stat_suspended)
            isMainCharacter -> stringResource(R.string.stat_on_xitter)
            highlight == BotPanelHighlight.Acting -> stringResource(R.string.stat_acting)
            else -> ""
        }
    BoxWithConstraints(modifier.semantics(mergeDescendants = true) { stateDescription = state }) {
        // Three panels share the width: on narrow phones or at large font scales the design's
        // side-by-side header doesn't fit, so the panel stacks instead.
        val compact = maxWidth < COMPACT_BELOW * LocalDensity.current.fontScale.coerceAtLeast(1f)
        XPanel(
            modifier = Modifier.fillMaxWidth().alpha(if (player.isSuspended) 0.55f else 1f),
            fill = if (isMainCharacter) XColors.MainCharacterPanel else XColors.Panel,
            accent =
                when (highlight) {
                    BotPanelHighlight.MainCharacter -> XColors.Mandates
                    BotPanelHighlight.Acting -> XColors.Characters
                    BotPanelHighlight.None -> XColors.Ink
                },
        ) {
            if (compact) CompactContent(player) else RegularContent(player)
        }
        // Stickers on the panel keep their size: they're decoration, and the panel's state is spoken.
        val sticker = with(LocalDensity.current) { if (compact) 9.dp.toSp() else 9.sp }
        when {
            player.isSuspended ->
                XStamp(
                    stringResource(R.string.stat_suspended),
                    Modifier.align(Alignment.Center).then(stampModifier),
                    textStyle = XType.Stamp.copy(fontSize = sticker * STAMP_SCALE),
                    borderWidth = 3.dp,
                )
            isMainCharacter ->
                XTag(
                    stringResource(R.string.stat_on_xitter),
                    Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-12).dp).rotate(5f),
                    icon = XIcon.Crown,
                    textStyle = XType.Tag.copy(fontSize = sticker),
                )
        }
    }
}

/** The design's panel: avatar beside name and "BOT", stats in a row. */
@Composable
private fun RegularContent(player: PlayerState) {
    Column(Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            XAvatar(player.leaderId, size = 36.dp)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    BotName(player, Modifier.weight(1f, fill = false))
                    CertificationBadge(player.mandates, size = 15.dp)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    XIconImage(XIcon.Bot, contentDescription = null, size = 12.dp)
                    Text(stringResource(R.string.stat_bot), style = XType.Label.copy(fontSize = 10.sp), color = XColors.Muted)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { BotStats(player) }
    }
}

/** Narrow panel: avatar and badge, the name under them, stats wrapping onto a second line. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompactContent(player: PlayerState) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            XAvatar(player.leaderId, size = 32.dp)
            CertificationBadge(player.mandates, size = 15.dp)
        }
        BotName(player, Modifier.fillMaxWidth(), TextAlign.Center)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) { BotStats(player) }
    }
}

/** Luckiest Guy is wide: long names shrink to fit instead of being cut off. */
@Composable
private fun BotName(
    player: PlayerState,
    modifier: Modifier = Modifier,
    align: TextAlign = TextAlign.Start,
) {
    BasicText(
        stringResource(leaderVisual(player.leaderId).shortName),
        style = XType.DisplayTiny.copy(color = XColors.Paper, textAlign = align),
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = XType.DisplayTiny.fontSize),
        modifier = modifier,
    )
}

@Composable
private fun BotStats(player: PlayerState) {
    val stat = XType.Stat.copy(fontSize = 13.sp)
    StatValue(
        XIcon.Like,
        player.followers.toString(),
        stringResource(R.string.stat_followers_of_max, player.followers, player.maxFollowers),
        style = stat,
        iconSize = 15.dp,
    )
    StatValue(
        XIcon.Characters,
        player.characters.toString(),
        pluralStringResource(R.plurals.stat_characters, player.characters, player.characters),
        style = stat,
        iconSize = 15.dp,
    )
    StatValue(
        XIcon.Mandates,
        player.mandates.toString(),
        pluralStringResource(R.plurals.stat_mandates, player.mandates, player.mandates),
        style = stat,
        iconSize = 15.dp,
    )
}

/** Panels narrower than this (at font scale 1) use the compact layout. */
private val COMPACT_BELOW = 116.dp

private const val STAMP_SCALE = 13f / 9f
