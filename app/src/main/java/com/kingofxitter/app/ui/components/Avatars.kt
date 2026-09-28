package com.kingofxitter.app.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.kingofxitter.app.R
import com.kingofxitter.app.ui.theme.XColors
import com.kingofxitter.app.ui.theme.XType
import com.kingofxitter.app.ui.theme.sticker
import com.kingofxitter.engine.model.LeaderId

/**
 * Presentation data for a leader. **Stand-in** until S5's `Leader` roster lands in `:content`;
 * then this lookup should read from it. Values follow the README's leader table.
 */
data class LeaderVisual(
    val color: Color,
    @param:StringRes val shortName: Int,
    @param:StringRes val fullName: Int,
    @param:StringRes val handle: Int,
)

/** Leader IDs used by the engine fixtures and `MatchSetup`. */
private val leaderVisuals =
    mapOf(
        "trumpet" to
            LeaderVisual(Color(0xFFFF8A1F), R.string.leader_short_trumpet, R.string.leader_full_trumpet, R.string.leader_handle_trumpet),
        "poutine" to
            LeaderVisual(Color(0xFF9C7BFF), R.string.leader_short_poutine, R.string.leader_full_poutine, R.string.leader_handle_poutine),
        "kim" to LeaderVisual(Color(0xFFFF4F4F), R.string.leader_short_kim, R.string.leader_full_kim, R.string.leader_handle_kim),
        "macaron" to
            LeaderVisual(Color(0xFFFF8FC8), R.string.leader_short_macaron, R.string.leader_full_macaron, R.string.leader_handle_macaron),
        "xi" to LeaderVisual(Color(0xFF22C08A), R.string.leader_short_xi, R.string.leader_full_xi, R.string.leader_handle_xi),
        "mi-leon" to
            LeaderVisual(Color(0xFF5FB4F0), R.string.leader_short_mileon, R.string.leader_full_mileon, R.string.leader_handle_mileon),
    )

private val unknownLeader =
    LeaderVisual(XColors.Muted, R.string.leader_short_unknown, R.string.leader_full_unknown, R.string.leader_handle_unknown)

/** Colours and names for [leaderId]; unknown IDs get a neutral fallback rather than crashing. */
fun leaderVisual(leaderId: LeaderId): LeaderVisual = leaderVisuals[leaderId.value] ?: unknownLeader

/**
 * Round leader avatar (`.avc`): leader-coloured disc with ink outline, paper halo and hard shadow.
 * Shows [portrait] when given (S5 supplies them), otherwise the leader's initial.
 */
@Composable
fun XAvatar(
    leaderId: LeaderId,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    portrait: Painter? = null,
) {
    val visual = leaderVisual(leaderId)
    val name = stringResource(visual.fullName)
    // The monogram fills a fixed-size disc, so it ignores the user's font scale.
    val monogramSize = with(LocalDensity.current) { (size * 0.5f).toSp() }
    Box(
        modifier =
            modifier
                .size(size)
                .sticker(
                    fill = visual.color,
                    shape = CircleShape,
                    halo = 3.dp,
                    shadowOffset = DpOffset(3.dp, 5.dp),
                ).clip(CircleShape)
                // Redraw the outline over the portrait, which is wider than the disc.
                .drawWithContent {
                    drawContent()
                    val border = 3.dp.toPx()
                    drawCircle(XColors.Ink, radius = (this.size.minDimension - border) / 2, style = Stroke(border))
                }.semantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) {
        if (portrait != null) {
            Image(portrait, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(size * 1.24f))
        } else {
            Text(
                stringResource(visual.shortName).take(1),
                style = XType.DisplayLarge.copy(fontSize = monogramSize),
                color = XColors.Ink,
            )
        }
    }
}
