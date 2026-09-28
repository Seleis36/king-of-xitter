package com.kingofxitter.app.feedback

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Whether sounds and haptics play. Both default on. */
data class FeedbackSettings(
    val sound: Boolean = true,
    val haptics: Boolean = true,
)

/**
 * Persists [FeedbackSettings] in app-private preferences. V5's Settings screen reads and writes
 * it (or drops in [FeedbackSettingsSection]).
 */
class FeedbackPreferences(
    context: Context,
) {
    private val preferences: SharedPreferences = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private val mutableSettings = MutableStateFlow(read())

    /** Current settings; updates immediately when [update] is called. */
    val settings: StateFlow<FeedbackSettings> = mutableSettings.asStateFlow()

    /** Stores [settings]. */
    fun update(settings: FeedbackSettings) {
        preferences.edit {
            putBoolean(SOUND, settings.sound)
            putBoolean(HAPTICS, settings.haptics)
        }
        mutableSettings.value = settings
    }

    private fun read() = FeedbackSettings(preferences.getBoolean(SOUND, true), preferences.getBoolean(HAPTICS, true))

    private companion object {
        const val FILE = "feedback"
        const val SOUND = "sound"
        const val HAPTICS = "haptics"
    }
}
