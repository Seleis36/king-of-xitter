package com.kingofxitter.app.ui.motion

import android.provider.Settings
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * How much the UI moves. [enabled] false turns idle loops off and makes one-shot effects jump to
 * their end. [speed] scales durations (V5's animation-speed setting: 2 is twice as fast).
 */
@Immutable
data class MotionSettings(
    val enabled: Boolean = true,
    val speed: Float = 1f,
) {
    /** [millis] adjusted for [speed]. */
    fun duration(millis: Int): Int = (millis / speed.coerceAtLeast(MIN_SPEED)).roundToInt()

    private companion object {
        const val MIN_SPEED = 0.25f
    }
}

/** Motion settings for the screens below. Provide V5's settings here; defaults follow the system. */
val LocalMotion = staticCompositionLocalOf { MotionSettings() }

/** The system's "Remove animations" (animator duration scale 0) turns motion off. */
@Composable
fun rememberSystemMotion(): MotionSettings {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        val scale = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        MotionSettings(enabled = scale > 0f)
    }
}

// ---- Idle loops ---------------------------------------------------------------------------

/** `wobble`: rocks between [degrees] and −[degrees] (stickers, speech bubbles). */
fun Modifier.wobble(
    degrees: Float = 3f,
    periodMillis: Int = 3200,
): Modifier =
    composed {
        val motion = LocalMotion.current
        if (!motion.enabled) return@composed graphicsLayer { rotationZ = degrees }
        val angle by rememberInfiniteTransition(label = "wobble").animateFloat(
            initialValue = degrees,
            targetValue = -degrees,
            animationSpec = infiniteRepeatable(tween(motion.duration(periodMillis / 2), easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "wobbleAngle",
        )
        graphicsLayer { rotationZ = angle }
    }

/** `bob`: floats up [height] and back (avatars, the Main Character's crown). */
fun Modifier.bob(
    height: Dp = 9.dp,
    periodMillis: Int = 2600,
): Modifier =
    composed {
        val motion = LocalMotion.current
        if (!motion.enabled) return@composed this
        val lift by rememberInfiniteTransition(label = "bob").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(motion.duration(periodMillis / 2), easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "bobLift",
        )
        graphicsLayer { translationY = -height.toPx() * lift }
    }

/** `pulse`: breathes to 103.5% (the call to action, "Your turn", the gold check). */
fun Modifier.pulse(
    scale: Float = 1.035f,
    periodMillis: Int = 1600,
): Modifier =
    composed {
        val motion = LocalMotion.current
        if (!motion.enabled) return@composed this
        val current by rememberInfiniteTransition(label = "pulse").animateFloat(
            initialValue = 1f,
            targetValue = scale,
            animationSpec = infiniteRepeatable(tween(motion.duration(periodMillis / 2), easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "pulseScale",
        )
        graphicsLayer {
            scaleX = current
            scaleY = current
        }
    }

/** `flash`: the incoming alarm bar, alternating between [from] and [to]. */
fun Modifier.flash(
    from: Color,
    to: Color,
    periodMillis: Int = 1000,
): Modifier =
    composed {
        val motion = LocalMotion.current
        if (!motion.enabled) return@composed drawBehind { drawRect(from) }
        val color by rememberInfiniteTransition(label = "flash").animateColor(
            initialValue = from,
            targetValue = to,
            animationSpec = infiniteRepeatable(tween(motion.duration(periodMillis / 2), easing = LinearEasing), RepeatMode.Reverse),
            label = "flashColor",
        )
        drawBehind { drawRect(color) }
    }

// ---- One-shot effects ---------------------------------------------------------------------

/**
 * Plays a one-shot effect whenever [key] changes to a non-null value, including on first
 * composition. Returns the progress state (0..1); 1 means at rest.
 */
@Composable
private fun oneShot(
    key: Any?,
    millis: Int,
): () -> Float {
    val motion = LocalMotion.current
    val progress = remember { Animatable(1f) }
    var played by remember { mutableStateOf<Any?>(null) }
    LaunchedEffect(key) {
        if (key != null && key != played) {
            played = key
            if (motion.enabled) {
                progress.snapTo(0f)
                progress.animateTo(1f, tween(motion.duration(millis), easing = LinearEasing))
            }
        }
    }
    return { progress.value }
}

/** `slam`: lands from 170% with a twist whenever [key] changes (a tweet arriving). */
fun Modifier.slamOn(key: Any?): Modifier =
    composed {
        val progress = oneShot(key, KitMotion.SLAM_MILLIS)
        graphicsLayer {
            val t = progress()
            val scale = KitMotion.slamScale.at(t)
            scaleX = scale
            scaleY = scale
            rotationZ = if (t >= 1f) 0f else KitMotion.slamRotation.at(t)
            alpha = KitMotion.slamAlpha.at(t)
        }
    }

/** `shake`: jolts left and right whenever [key] changes (taking a hit). */
fun Modifier.shakeOn(key: Any?): Modifier =
    composed {
        val progress = oneShot(key, KitMotion.SHAKE_MILLIS)
        graphicsLayer {
            val t = progress()
            translationX =
                KitMotion.shakeX
                    .at(t)
                    .dp
                    .toPx()
            translationY =
                KitMotion.shakeY
                    .at(t)
                    .dp
                    .toPx()
            rotationZ = KitMotion.shakeRotation.at(t)
        }
    }

/** `pop`: springs in from nothing whenever [key] changes (stickers). */
fun Modifier.popOn(key: Any?): Modifier =
    composed {
        val progress = oneShot(key, KitMotion.POP_MILLIS)
        graphicsLayer {
            val t = progress()
            val scale = KitMotion.popScale.at(t)
            scaleX = scale
            scaleY = scale
            rotationZ = KitMotion.popSpin.at(t)
        }
    }

/** `stampIn`: slams down from 300% whenever [key] changes (RATIO'D, SUSPENDED). */
fun Modifier.stampOn(key: Any?): Modifier =
    composed {
        val progress = oneShot(key, KitMotion.STAMP_MILLIS)
        graphicsLayer {
            val t = progress()
            val scale = KitMotion.stampScale.at(t)
            scaleX = scale
            scaleY = scale
            alpha = KitMotion.stampAlpha.at(t)
        }
    }

/**
 * `rise`: floats up and fades out once, from when it enters composition. Used for damage and
 * gain numbers; with motion off it simply shows.
 */
fun Modifier.rise(): Modifier =
    composed {
        val motion = LocalMotion.current
        val progress = remember { Animatable(if (motion.enabled) 0f else 0.5f) }
        LaunchedEffect(Unit) {
            if (motion.enabled) progress.animateTo(1f, tween(motion.duration(KitMotion.RISE_MILLIS), easing = LinearEasing))
        }
        graphicsLayer {
            val t = progress.value
            translationY =
                KitMotion.riseY
                    .at(t)
                    .dp
                    .toPx()
            val scale = KitMotion.riseScale.at(t)
            scaleX = scale
            scaleY = scale
            alpha = KitMotion.riseAlpha.at(t)
        }
    }
