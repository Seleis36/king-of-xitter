package com.kingofxitter.app.ui.front

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import com.kingofxitter.app.R
import com.kingofxitter.app.feedback.FeedbackPreferences
import com.kingofxitter.app.feedback.FeedbackSettings
import com.kingofxitter.app.ui.motion.MotionSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** How fast animations play and how long bots pause between moves. */
enum class SpeedChoice(
    @param:StringRes val label: Int,
    /** Multiplier for motion durations; 2 means twice as fast. */
    val motionSpeed: Float,
    /** Pause before a bot's move, matching `RealGameController`'s default at [NORMAL]. */
    val botDelayMillis: Long,
) {
    SLOW(R.string.settings_speed_slow, motionSpeed = 0.6f, botDelayMillis = 900),
    NORMAL(R.string.settings_speed_normal, motionSpeed = 1f, botDelayMillis = 400),
    FAST(R.string.settings_speed_fast, motionSpeed = 1.8f, botDelayMillis = 120),
}

/** The character cap the Settings toggle turns on (ROADMAP §1, decision 1). */
const val CHARACTER_CAP = 280

/**
 * Everything the Settings screen owns. [feedback] keeps the sound and haptics flags U9 already
 * persists, so both screens read one source.
 *
 * [characterCap] is a rules toggle, so it only takes effect when the next match starts; the
 * engine reads it from `GameState.characterCap`.
 */
data class GameSettings(
    val feedback: FeedbackSettings = FeedbackSettings(),
    val animations: Boolean = true,
    val animationSpeed: SpeedChoice = SpeedChoice.NORMAL,
    val botSpeed: SpeedChoice = SpeedChoice.NORMAL,
    val characterCap: Boolean = false,
) {
    /** Motion settings for [com.kingofxitter.app.ui.motion.LocalMotion]. */
    fun motion(systemAllowsMotion: Boolean): MotionSettings =
        MotionSettings(enabled = animations && systemAllowsMotion, speed = animationSpeed.motionSpeed)

    /** The cap to put on a new match, or null for the uncapped base rules. */
    val characterCapValue: Int?
        get() = if (characterCap) CHARACTER_CAP else null
}

/**
 * Persists [GameSettings] in app-private preferences. Sound and haptics are delegated to
 * [FeedbackPreferences] so the audio layer keeps observing the flags it already owns.
 */
class GamePreferences(
    context: Context,
    /** The sound and haptics flags, shared with the audio layer that already observes them. */
    val feedback: FeedbackPreferences = FeedbackPreferences(context),
) {
    private val preferences: SharedPreferences = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private val mutableSettings = MutableStateFlow(read())

    /** Current settings; updates as soon as [update] is called. */
    val settings: StateFlow<GameSettings> = mutableSettings.asStateFlow()

    /** Stores [settings], writing sound and haptics through to [FeedbackPreferences]. */
    fun update(settings: GameSettings) {
        if (settings.feedback != mutableSettings.value.feedback) feedback.update(settings.feedback)
        preferences.edit {
            putBoolean(ANIMATIONS, settings.animations)
            putString(ANIMATION_SPEED, settings.animationSpeed.name)
            putString(BOT_SPEED, settings.botSpeed.name)
            putBoolean(CHARACTER_CAP_KEY, settings.characterCap)
        }
        mutableSettings.value = settings
    }

    private fun read(): GameSettings {
        val defaults = GameSettings()
        return GameSettings(
            feedback = feedback.settings.value,
            animations = preferences.getBoolean(ANIMATIONS, defaults.animations),
            animationSpeed = speed(ANIMATION_SPEED, defaults.animationSpeed),
            botSpeed = speed(BOT_SPEED, defaults.botSpeed),
            characterCap = preferences.getBoolean(CHARACTER_CAP_KEY, defaults.characterCap),
        )
    }

    // A stored name that no longer exists (a renamed choice) falls back instead of crashing.
    private fun speed(
        key: String,
        default: SpeedChoice,
    ) = preferences.getString(key, null)?.let { name -> SpeedChoice.entries.firstOrNull { it.name == name } } ?: default

    private companion object {
        const val FILE = "game-settings"
        const val ANIMATIONS = "animations"
        const val ANIMATION_SPEED = "animation-speed"
        const val BOT_SPEED = "bot-speed"
        const val CHARACTER_CAP_KEY = "character-cap"
    }
}

/** A [GamePreferences] for the composition's context. */
@Composable
fun rememberGamePreferences(): GamePreferences {
    val context = LocalContext.current
    return remember(context) { GamePreferences(context) }
}
