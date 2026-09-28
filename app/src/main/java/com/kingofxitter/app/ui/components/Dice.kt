package com.kingofxitter.app.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.kingofxitter.app.R
import com.kingofxitter.app.ui.motion.LocalMotion
import com.kingofxitter.app.ui.theme.XColors
import com.kingofxitter.app.ui.theme.XType
import com.kingofxitter.app.ui.theme.sticker
import com.kingofxitter.engine.model.DiceFace

/** Die sizes from the kit: 54dp on the board, 36dp and 28dp in lists. [Compact] fits six on a 320dp phone. */
enum class DieSize(
    internal val side: Dp,
    internal val corner: Dp,
    internal val border: Dp,
    internal val shadow: Dp,
    internal val icon: Dp,
    internal val number: Float,
) {
    Default(54.dp, 15.dp, 3.dp, 5.dp, 34.dp, 34f),
    Compact(44.dp, 12.dp, 3.dp, 4.dp, 28.dp, 28f),
    Small(36.dp, 10.dp, 2.5.dp, 3.dp, 22.dp, 22f),
    ExtraSmall(28.dp, 8.dp, 2.dp, 2.dp, 18.dp, 17f),
}

/** Icon drawn on a symbol face, or null for the number faces. */
fun DiceFace.icon(): XIcon? =
    when (this) {
        DiceFace.TWEET -> XIcon.Tweet
        DiceFace.LIKE -> XIcon.Like
        DiceFace.CHARACTERS -> XIcon.Characters
        DiceFace.ONE, DiceFace.TWO, DiceFace.THREE -> null
    }

/** Number shown on a number face. */
fun DiceFace.number(): Int? =
    when (this) {
        DiceFace.ONE -> 1
        DiceFace.TWO -> 2
        DiceFace.THREE -> 3
        else -> null
    }

/** Accessible name of a face ("Tweet", "3"). */
@StringRes
fun DiceFace.label(): Int =
    when (this) {
        DiceFace.ONE -> R.string.die_one
        DiceFace.TWO -> R.string.die_two
        DiceFace.THREE -> R.string.die_three
        DiceFace.LIKE -> R.string.die_like
        DiceFace.CHARACTERS -> R.string.die_characters
        DiceFace.TWEET -> R.string.die_tweet
    }

// The kit's tumble: cubic-bezier(.3,1.4,.5,1), overshooting as it lands.
private val TumbleEasing = CubicBezierEasing(0.3f, 1.4f, 0.5f, 1f)
private const val TUMBLE_MILLIS = 700

/** Size of the "KEPT" label under a kept die. */
private val KEPT_LABEL = 10.dp

/** Recommended touch target for a tappable die. */
val MIN_TOUCH_TARGET = 48.dp

/**
 * One die. A [kept] die turns yellow, lifts and tilts, with "KEPT" underneath. When [tumbleKey]
 * changes after the first composition, the die tumbles once (pass the roll number for dice that
 * were rerolled).
 * With [onToggleKept], the die is a toggle for keeping it, at least [touchTarget] square (pass less
 * only when dice would otherwise overlap).
 */
@Composable
fun Die(
    face: DiceFace,
    modifier: Modifier = Modifier,
    size: DieSize = DieSize.Default,
    kept: Boolean = false,
    tumbleKey: Any? = null,
    onToggleKept: ((Boolean) -> Unit)? = null,
    touchTarget: Dp = MIN_TOUCH_TARGET,
) {
    val lift by animateDpAsState(if (kept) (-12).dp else 0.dp, tween(180), label = "lift")
    val tilt by animateFloatAsState(if (kept) -7f else 0f, tween(180), label = "tilt")
    val tumble = remember { Animatable(0f) }
    // Only a change tumbles: showing the board again doesn't replay the last roll.
    var shownKey by remember { mutableStateOf(tumbleKey) }
    val motion = LocalMotion.current
    LaunchedEffect(tumbleKey) {
        if (tumbleKey != null && tumbleKey != shownKey) {
            shownKey = tumbleKey
            if (motion.enabled) {
                tumble.snapTo(0f)
                tumble.animateTo(1f, tween(motion.duration(TUMBLE_MILLIS), easing = TumbleEasing))
            }
        }
    }
    val faceName = stringResource(face.label())
    val description = stringResource(R.string.die_description, faceName)
    val keptState = stringResource(if (kept) R.string.die_kept_state else R.string.die_rerolled_state)
    val numberStyle = XType.DisplayLarge.copy(fontSize = with(LocalDensity.current) { size.number.dp.toSp() })
    Box(
        modifier =
            modifier
                .then(
                    if (onToggleKept != null) {
                        // Smaller dice still get a [touchTarget]-sized touch area; the die sits at its top.
                        Modifier
                            .sizeIn(minWidth = touchTarget, minHeight = touchTarget)
                            .toggleable(value = kept, role = Role.Checkbox, onValueChange = onToggleKept)
                    } else {
                        Modifier
                    },
                ).clearAndSetSemantics {
                    contentDescription = description
                    if (onToggleKept != null) {
                        role = Role.Checkbox
                        toggleableState = ToggleableState(kept)
                        stateDescription = keptState
                    }
                },
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier =
                Modifier
                    .offset(y = lift)
                    .graphicsLayer {
                        val t = tumble.value
                        // 0 → 25%: spin to 160° and grow; 55%: 330° and settle; 70%+: rest.
                        rotationZ = tilt + tumbleRotation(t)
                        val scale = tumbleScale(t)
                        scaleX = scale
                        scaleY = scale
                        translationY = -8.dp.toPx() * tumbleHop(t)
                    }.size(size.side)
                    .sticker(
                        fill = if (kept) XColors.Mandates else XColors.Paper,
                        shape = RoundedCornerShape(size.corner),
                        borderWidth = size.border,
                        shadowOffset = DpOffset(0.dp, size.shadow),
                    ),
            contentAlignment = Alignment.Center,
        ) {
            val icon = face.icon()
            if (icon != null) {
                XIconImage(icon, contentDescription = null, size = size.icon)
            } else {
                Text(face.number().toString(), style = numberStyle, color = XColors.Ink, modifier = Modifier.padding(top = size.border * 2))
            }
        }
        if (kept && (size == DieSize.Default || size == DieSize.Compact)) {
            // Sized with the die rather than the font scale, so labels of neighbouring dice don't
            // run together; the kept state is also spoken and shown by the die itself.
            Text(
                stringResource(R.string.die_kept),
                style = XType.Label.copy(fontSize = with(LocalDensity.current) { KEPT_LABEL.toSp() }),
                color = XColors.Mandates,
                modifier =
                    Modifier
                        .offset(y = size.side + lift + 12.dp)
                        .graphicsLayer { rotationZ = 7f + tilt },
            )
        }
    }
}

/** Tumble rotation in degrees at progress [t] (0..1). */
internal fun tumbleRotation(t: Float): Float =
    when {
        t <= 0f || t >= 1f -> 0f
        t < 0.25f -> 160f * (t / 0.25f)
        t < 0.55f -> 160f + 170f * ((t - 0.25f) / 0.3f)
        t < 0.7f -> 330f + 30f * ((t - 0.55f) / 0.15f)
        else -> 360f
    } % 360f

/** Tumble scale at progress [t]: grows to 1.18, dips to 0.94, settles at 1. */
internal fun tumbleScale(t: Float): Float =
    when {
        t <= 0f || t >= 0.7f -> 1f
        t < 0.25f -> 1f + 0.18f * (t / 0.25f)
        t < 0.55f -> 1.18f - 0.24f * ((t - 0.25f) / 0.3f)
        else -> 0.94f + 0.06f * ((t - 0.55f) / 0.15f)
    }

/** Upward hop (0..1 of 8dp) at progress [t], peaking at 25%. */
internal fun tumbleHop(t: Float): Float =
    when {
        t <= 0f || t >= 0.55f -> 0f
        t < 0.25f -> t / 0.25f
        else -> 1f - (t - 0.25f) / 0.3f
    }
