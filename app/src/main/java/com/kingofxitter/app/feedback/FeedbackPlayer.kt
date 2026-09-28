package com.kingofxitter.app.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.kingofxitter.engine.GameController
import com.kingofxitter.engine.model.PlayerId

/** Plays [Cue]s: sounds through a [SoundPool] on the game stream, haptics through the view. */
class FeedbackPlayer(
    context: Context,
    private val view: View,
) : AutoCloseable {
    private val pool =
        SoundPool
            .Builder()
            .setMaxStreams(MAX_STREAMS)
            .setAudioAttributes(
                AudioAttributes
                    .Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            ).build()
    private val throttle = CueThrottle()
    private val soundLock = Any()
    private val soundLoadQueue = SoundLoadQueue()
    private var closed = false
    private val sounds: Map<Cue, Int>

    init {
        pool.setOnLoadCompleteListener { loadedPool, sampleId, status ->
            synchronized(soundLock) {
                if (!closed) {
                    soundLoadQueue.complete(sampleId, status == 0).forEach { readyId ->
                        loadedPool.play(readyId, 1f, 1f, 1, 0, 1f)
                    }
                }
            }
        }
        sounds = Cue.entries.associateWith { pool.load(context, it.sound, 1) }
    }

    /** Plays [cue] as far as [settings] allow. */
    fun play(
        cue: Cue,
        settings: FeedbackSettings,
    ) {
        if (!throttle.allow(cue, SystemClock.uptimeMillis())) return
        synchronized(soundLock) {
            if (closed) return
            if (settings.sound) {
                sounds[cue]?.let { sampleId ->
                    if (soundLoadQueue.request(sampleId)) pool.play(sampleId, 1f, 1f, 1, 0, 1f)
                }
            }
            if (settings.haptics) haptic(cue.haptic)?.let(view::performHapticFeedback)
        }
    }

    private fun haptic(haptic: Haptic): Int? =
        when (haptic) {
            Haptic.None -> null
            Haptic.Tick -> HapticFeedbackConstants.CLOCK_TICK
            Haptic.Heavy -> HapticFeedbackConstants.LONG_PRESS
            Haptic.Confirm ->
                if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.R
                ) {
                    HapticFeedbackConstants.CONFIRM
                } else {
                    HapticFeedbackConstants.LONG_PRESS
                }
            Haptic.Reject ->
                if (Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.R
                ) {
                    HapticFeedbackConstants.REJECT
                } else {
                    HapticFeedbackConstants.LONG_PRESS
                }
        }

    override fun close() {
        synchronized(soundLock) {
            if (closed) return
            closed = true
            soundLoadQueue.clear()
            pool.release()
        }
    }

    private companion object {
        const val MAX_STREAMS = 4
    }
}

/**
 * Plays sounds and haptics for [controller]'s events while composed, as the human [humanId]
 * experiences them. Hoist it above navigation, next to the controller.
 */
@Composable
fun GameFeedback(
    controller: GameController,
    humanId: PlayerId,
    preferences: FeedbackPreferences,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val player = remember(context, view) { FeedbackPlayer(context, view) }
    DisposableEffect(player) { onDispose { player.close() } }
    LaunchedEffect(controller, humanId, player) {
        controller.events.collect { event ->
            cuesFor(event, humanId, controller.state.value.currentPlayerId).forEach { player.play(it, preferences.settings.value) }
        }
    }
}
