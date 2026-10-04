package com.kingofxitter.app.ui.components

import androidx.annotation.DrawableRes
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.kingofxitter.app.R
import com.kingofxitter.app.ui.theme.XColors
import com.kingofxitter.app.ui.theme.XType
import com.kingofxitter.app.ui.theme.sticker
import com.kingofxitter.content.LeaderRoster
import com.kingofxitter.engine.model.LeaderId

/**
 * Presentation data for a leader, taken from S5's `LeaderRoster` in `:content`. The roster is the
 * single source of identity, colour and portrait; this type only turns its `strings.xml` keys into
 * the resource IDs Compose needs.
 */
data class LeaderVisual(
    val color: Color,
    @param:StringRes val firstName: Int,
    @param:StringRes val shortName: Int,
    @param:StringRes val fullName: Int,
    @param:StringRes val handle: Int,
    @param:StringRes val voice: Int,
    @param:DrawableRes val portrait: Int?,
)

/**
 * The roster's string and drawable IDs. Resource IDs can't be named from a pure-Kotlin module, so
 * they are listed here once per leader; `LeaderVisualTest` fails if the roster gains a leader this
 * table doesn't cover.
 */
private val leaderResources =
    mapOf(
        "trumpet" to
            LeaderResources(
                R.string.leader_first_trumpet,
                R.string.leader_short_trumpet,
                R.string.leader_full_trumpet,
                R.string.leader_handle_trumpet,
                R.string.leader_voice_trumpet,
                R.drawable.portrait_trumpet,
            ),
        "poutine" to
            LeaderResources(
                R.string.leader_first_poutine,
                R.string.leader_short_poutine,
                R.string.leader_full_poutine,
                R.string.leader_handle_poutine,
                R.string.leader_voice_poutine,
                R.drawable.portrait_poutine,
            ),
        "kim" to
            LeaderResources(
                R.string.leader_first_kim,
                R.string.leader_short_kim,
                R.string.leader_full_kim,
                R.string.leader_handle_kim,
                R.string.leader_voice_kim,
                R.drawable.portrait_kim,
            ),
        "macaron" to
            LeaderResources(
                R.string.leader_first_macaron,
                R.string.leader_short_macaron,
                R.string.leader_full_macaron,
                R.string.leader_handle_macaron,
                R.string.leader_voice_macaron,
                R.drawable.portrait_macaron,
            ),
        "xi" to
            LeaderResources(
                R.string.leader_first_xi,
                R.string.leader_short_xi,
                R.string.leader_full_xi,
                R.string.leader_handle_xi,
                R.string.leader_voice_xi,
                R.drawable.portrait_xi,
            ),
        "mi-leon" to
            LeaderResources(
                R.string.leader_first_mileon,
                R.string.leader_short_mileon,
                R.string.leader_full_mileon,
                R.string.leader_handle_mileon,
                R.string.leader_voice_mileon,
                R.drawable.portrait_mileon,
            ),
    )

/** The resource IDs behind one roster entry. */
private data class LeaderResources(
    @param:StringRes val firstName: Int,
    @param:StringRes val shortName: Int,
    @param:StringRes val fullName: Int,
    @param:StringRes val handle: Int,
    @param:StringRes val voice: Int,
    @param:DrawableRes val portrait: Int,
)

private val leaderVisuals =
    LeaderRoster.all
        .mapNotNull { leader ->
            leaderResources[leader.id.value]?.let { resources ->
                leader.id.value to
                    LeaderVisual(
                        color = Color(leader.colorArgb),
                        firstName = resources.firstName,
                        shortName = resources.shortName,
                        fullName = resources.fullName,
                        handle = resources.handle,
                        voice = resources.voice,
                        portrait = resources.portrait,
                    )
            }
        }.toMap()

private val unknownLeader =
    LeaderVisual(
        color = XColors.Muted,
        firstName = R.string.leader_first_unknown,
        shortName = R.string.leader_short_unknown,
        fullName = R.string.leader_full_unknown,
        handle = R.string.leader_handle_unknown,
        voice = R.string.leader_voice_unknown,
        portrait = null,
    )

/** Colours and names for [leaderId]; unknown IDs get a neutral fallback rather than crashing. */
fun leaderVisual(leaderId: LeaderId): LeaderVisual = leaderVisuals[leaderId.value] ?: unknownLeader

/**
 * Round leader avatar (`.avc`): leader-coloured disc with ink outline, paper halo and hard shadow.
 * Shows the leader's S5 portrait, or the leader's initial for an unknown ID. Pass [portrait] to
 * override it (previews of a leader that has no portrait yet).
 */
@Composable
fun XAvatar(
    leaderId: LeaderId,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    portrait: Painter? = leaderVisual(leaderId).portrait?.let { painterResource(it) },
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
                }.clearAndSetSemantics { contentDescription = name },
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
