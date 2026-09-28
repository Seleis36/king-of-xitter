package com.kingofxitter.app.ui.board

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingofxitter.app.R
import com.kingofxitter.app.ui.components.CertificationBadge
import com.kingofxitter.app.ui.components.CharactersRing
import com.kingofxitter.app.ui.components.FollowersBar
import com.kingofxitter.app.ui.components.MandateProgress
import com.kingofxitter.app.ui.components.XAvatar
import com.kingofxitter.app.ui.components.XChip
import com.kingofxitter.app.ui.components.XPanel
import com.kingofxitter.app.ui.components.XTag
import com.kingofxitter.app.ui.components.leaderVisual
import com.kingofxitter.app.ui.theme.XColors
import com.kingofxitter.app.ui.theme.XType
import com.kingofxitter.engine.model.CardId
import com.kingofxitter.engine.model.PlayerState

/**
 * Readable fallback for a power-up name ("alpha-poster" → "ALPHA POSTER") until C1's catalogue
 * supplies real names.
 */
fun fallbackPolicyName(cardId: CardId): String = cardId.value.replace('-', ' ').uppercase()

/** Your leader: avatar, followers, characters ring, mandate progress and owned policies. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun YourPanel(
    player: PlayerState,
    modifier: Modifier = Modifier,
    policyName: (CardId) -> String = ::fallbackPolicyName,
) {
    val visual = leaderVisual(player.leaderId)
    XPanel(modifier.fillMaxWidth(), accent = visual.color) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                XAvatar(player.leaderId, size = 54.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        XTag(stringResource(R.string.board_you), fill = XColors.Characters, textStyle = XType.Tag.copy(fontSize = 10.sp))
                        // Long names shrink to fit instead of being cut off on narrow phones.
                        BasicText(
                            stringResource(visual.shortName),
                            style = XType.DisplayMedium.copy(fontSize = 19.sp, color = XColors.Paper),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = 19.sp),
                            modifier = Modifier.weight(1f, fill = false).padding(top = 3.dp).semantics { heading() },
                        )
                        CertificationBadge(player.mandates, size = 18.dp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // The hearts give way to the count, which the bar already speaks.
                        FollowersBar(player.followers, player.maxFollowers, Modifier.weight(1f, fill = false), pipSize = 15.dp)
                        Text(
                            stringResource(R.string.board_followers_short, player.followers),
                            style = XType.Caption.copy(fontSize = 12.sp),
                            color = XColors.Paper,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.clearAndSetSemantics { },
                        )
                    }
                }
                CharactersRing(player.characters)
            }
            MandateProgress(player.mandates, Modifier.fillMaxWidth())
            if (player.policyIds.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    player.policyIds.forEach { card ->
                        XChip(policyName(card), textStyle = XType.Chip.copy(fontSize = 11.sp), minHeight = 26.dp)
                    }
                }
            }
        }
    }
}
