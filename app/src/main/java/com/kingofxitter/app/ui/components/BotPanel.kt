package com.kingofxitter.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
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
 * a suspended bot is dimmed and stamped. [stampModifier] animates the stamp (U8).
 */
@Composable
fun BotPanel(
    player: PlayerState,
    modifier: Modifier = Modifier,
    highlight: BotPanelHighlight = BotPanelHighlight.None,
    stampModifier: Modifier = Modifier,
) {
    val visual = leaderVisual(player.leaderId)
    val isMainCharacter = highlight == BotPanelHighlight.MainCharacter
    val state =
        when {
            player.isSuspended -> stringResource(R.string.stat_suspended)
            isMainCharacter -> stringResource(R.string.stat_on_xitter)
            highlight == BotPanelHighlight.Acting -> stringResource(R.string.stat_acting)
            else -> ""
        }
    Box(modifier.semantics(mergeDescendants = true) { stateDescription = state }) {
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
            Column(Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    XAvatar(player.leaderId, size = 36.dp)
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            // Luckiest Guy is wide: long names shrink to fit instead of being cut off.
                            BasicText(
                                stringResource(visual.shortName),
                                style = XType.DisplayTiny.copy(color = XColors.Paper),
                                maxLines = 1,
                                autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = XType.DisplayTiny.fontSize),
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            CertificationBadge(player.mandates, size = 15.dp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            XIconImage(XIcon.Bot, contentDescription = null, size = 12.dp)
                            Text(stringResource(R.string.stat_bot), style = XType.Label.copy(fontSize = 10.sp), color = XColors.Muted)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
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
            }
        }
        when {
            player.isSuspended ->
                XStamp(
                    stringResource(R.string.stat_suspended),
                    Modifier.align(Alignment.Center).then(stampModifier),
                    textStyle = XType.Stamp.copy(fontSize = 13.sp),
                    borderWidth = 3.dp,
                )
            isMainCharacter ->
                XTag(
                    stringResource(R.string.stat_on_xitter),
                    Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-12).dp).rotate(5f),
                    icon = XIcon.Crown,
                    textStyle = XType.Tag.copy(fontSize = 9.sp),
                )
        }
    }
}
