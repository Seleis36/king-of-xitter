package com.kingofxitter.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kingofxitter.app.R
import com.kingofxitter.app.ui.theme.XColors
import com.kingofxitter.app.ui.theme.XType

/** Certification badge for a mandate total (unverified, blue or gold check). */
@Composable
fun CertificationBadge(
    mandates: Int,
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
) {
    val certification = Certification.of(mandates)
    XIconImage(certification.icon, stringResource(certification.label()), modifier, size = size)
}

/** Accessible name of a certification. */
fun Certification.label(): Int =
    when (this) {
        Certification.Unverified -> R.string.stat_unverified
        Certification.Blue -> R.string.stat_blue_check
        Certification.Gold -> R.string.stat_gold_check
    }

/** Followers as a row of hearts: full for current followers, broken for lost ones (`.hearts`). */
@Composable
fun FollowersBar(
    followers: Int,
    maxFollowers: Int,
    modifier: Modifier = Modifier,
    pipSize: Dp = 15.dp,
) {
    val description = stringResource(R.string.stat_followers_of_max, followers, maxFollowers)
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        followerPips(followers, maxFollowers).forEach { pip ->
            XIconImage(if (pip == FollowerPip.Full) XIcon.Like else XIcon.LikeLost, contentDescription = null, size = pipSize)
        }
    }
}

/** Characters (energy) ring with the value in the middle. Fills to [CHARACTER_RING_CAPACITY]. */
@Composable
fun CharactersRing(
    characters: Int,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
) {
    val description = pluralStringResource(R.plurals.stat_characters, characters, characters)
    val fraction = characterRingFraction(characters)
    // The value sits in a fixed-size ring, so it ignores the user's font scale.
    val valueStyle = XType.DisplaySmall.copy(fontSize = with(LocalDensity.current) { (size * 0.29f).toSp() })
    Box(modifier.size(size).clearAndSetSemantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            // Design: 40-unit viewbox, radius 15.5, stroke 5.
            val unit = this.size.minDimension / 40f
            val stroke = 5f * unit
            val radius = 15.5f * unit
            drawCircle(XColors.Ink, radius = radius)
            drawCircle(XColors.Disabled, radius = radius, style = Stroke(stroke))
            if (fraction > 0f) {
                drawArc(
                    color = XColors.Characters,
                    startAngle = -90f,
                    sweepAngle = 360f * fraction,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        Text(characters.toString(), style = valueStyle, color = XColors.Characters, modifier = Modifier.padding(top = 3.dp))
    }
}

/** Mandate icon and total, optionally followed by "of 20" (`.stat` in the kit's MANDATES block). */
@Composable
fun MandateCounter(
    mandates: Int,
    modifier: Modifier = Modifier,
    iconSize: Dp = 26.dp,
    valueStyle: TextStyle = XType.DisplayLarge,
    showGoal: Boolean = false,
) {
    val description = pluralStringResource(R.plurals.stat_mandates, mandates, mandates)
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        XIconImage(XIcon.Mandates, contentDescription = null, size = iconSize)
        Text(mandates.toString(), style = valueStyle, color = XColors.Paper, modifier = Modifier.padding(top = 3.dp))
        if (showGoal) Text(stringResource(R.string.stat_of_twenty), style = XType.Caption, color = XColors.Muted)
    }
}

/**
 * Mandate progress bar with blue-check and gold-check markers, and how far the next milestone is
 * ("7 to gold").
 */
@Composable
fun MandateProgress(
    mandates: Int,
    modifier: Modifier = Modifier,
) {
    val goal = MandateGoal.after(mandates)
    val goalText =
        when (goal.kind) {
            MandateGoal.Kind.BlueCheck -> stringResource(R.string.stat_to_blue, goal.remaining)
            MandateGoal.Kind.GoldCheck -> stringResource(R.string.stat_to_gold, goal.remaining)
            MandateGoal.Kind.Win -> stringResource(R.string.stat_to_win, goal.remaining)
        }
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MandateCounter(mandates, iconSize = 22.dp, valueStyle = XType.DisplaySmall)
        Box(
            Modifier
                .weight(1f)
                .height(12.dp)
                .clip(CircleShape)
                .background(XColors.PanelShadow)
                .border(2.dp, XColors.Ink, CircleShape),
        ) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(mandateFraction(mandates)).background(XColors.BlueCheck))
            Marker(Certification.Blue.minimumMandates, XColors.Paper)
            Marker(Certification.Gold.minimumMandates, XColors.GoldCheck)
        }
        Text(goalText, style = XType.Caption, color = XColors.Muted, maxLines = 1)
    }
}

@Composable
private fun Marker(
    mandates: Int,
    color: androidx.compose.ui.graphics.Color,
) {
    Row(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(mandateFraction(mandates)))
        Box(Modifier.fillMaxHeight().width(2.dp).background(color))
    }
}

/** A stat icon with its value, as in bot panels. */
@Composable
fun StatValue(
    icon: XIcon,
    value: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    iconSize: Dp = 16.dp,
    style: TextStyle = XType.Stat,
) {
    Row(
        modifier = modifier.clearAndSetSemantics { this.contentDescription = contentDescription },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        XIconImage(icon, contentDescription = null, size = iconSize)
        Text(value, style = style, color = XColors.Paper, maxLines = 1, softWrap = false)
    }
}
