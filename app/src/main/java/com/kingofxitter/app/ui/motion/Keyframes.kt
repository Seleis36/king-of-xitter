package com.kingofxitter.app.ui.motion

/**
 * Piecewise-linear keyframes over progress 0..1, transcribed from the kit's CSS `@keyframes`.
 * Values before the first key hold the first value, after the last key the last value.
 */
class Keyframes(
    vararg keys: Pair<Float, Float>,
) {
    private val keys = keys.sortedBy { it.first }

    init {
        require(this.keys.isNotEmpty())
    }

    /** Value at progress [t]. */
    fun at(t: Float): Float {
        val first = keys.first()
        if (t <= first.first) return first.second
        keys.zipWithNext().forEach { (a, b) ->
            if (t <= b.first) {
                val span = b.first - a.first
                return if (span <= 0f) b.second else a.second + (b.second - a.second) * ((t - a.first) / span)
            }
        }
        return keys.last().second
    }
}

/** One-shot effects from the kit's MOTION section, each as keyframes over its own duration. */
object KitMotion {
    /** `slam`: a tweet lands. The kit plays it in the first 14% of 5s. */
    const val SLAM_MILLIS = 700
    val slamScale = Keyframes(0f to 1.7f, 0.64f to 0.93f, 1f to 1f)
    val slamRotation = Keyframes(0f to -9f, 0.64f to 2f, 1f to -1.5f)
    val slamAlpha = Keyframes(0f to 0f, 0.64f to 1f)

    /** `shake`: taking a hit. The kit's first half-cycle of 1.8s. */
    const val SHAKE_MILLIS = 900
    val shakeX = Keyframes(0f to 0f, 0.2f to -9f, 0.4f to 8f, 0.6f to -6f, 0.8f to 4f, 1f to 0f)
    val shakeY = Keyframes(0f to 0f, 0.2f to 2f, 0.4f to -2f, 0.6f to 1f, 0.8f to 0f)
    val shakeRotation = Keyframes(0f to 0f, 0.2f to -4f, 0.4f to 3f, 0.6f to -2f, 0.8f to 1f, 1f to 0f)

    /** `rise`: damage and gain numbers float up and fade. */
    const val RISE_MILLIS = 2400
    val riseY = Keyframes(0f to 20f, 0.15f to 0f, 1f to -70f)
    val riseScale = Keyframes(0f to 0.6f, 0.15f to 1.15f, 0.3f to 1f)
    val riseAlpha = Keyframes(0f to 0f, 0.15f to 1f, 0.7f to 1f, 1f to 0f)

    /** `pop`: stickers arrive. The first 18% of 5s. */
    const val POP_MILLIS = 900
    val popScale = Keyframes(0f to 0f, 0.67f to 1.2f, 1f to 1f)
    val popSpin = Keyframes(0f to -25f, 0.67f to 6f, 1f to 0f)

    /** `stampIn`: RATIO'D and SUSPENDED. The first 15% of 5s. */
    const val STAMP_MILLIS = 750
    val stampScale = Keyframes(0f to 3f, 0.67f to 0.9f, 1f to 1f)
    val stampAlpha = Keyframes(0f to 0f, 0.67f to 1f)
}
