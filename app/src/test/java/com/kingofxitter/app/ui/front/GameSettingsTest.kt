package com.kingofxitter.app.ui.front

import com.kingofxitter.app.feedback.FeedbackSettings
import com.kingofxitter.app.navigation.DEFAULT_BOT_DELAY_MILLIS
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameSettingsTest {
    @Test
    fun `the defaults are the base rules at the controller's own pacing`() {
        val defaults = GameSettings()
        assertEquals(FeedbackSettings(), defaults.feedback)
        assertTrue(defaults.animations)
        assertEquals(SpeedChoice.NORMAL, defaults.animationSpeed)
        assertEquals(SpeedChoice.NORMAL, defaults.botSpeed)
        // ROADMAP §1 decision 1: the cap is configurable and off by default.
        assertFalse(defaults.characterCap)
        assertNull(defaults.characterCapValue)
        assertEquals(DEFAULT_BOT_DELAY_MILLIS, SpeedChoice.NORMAL.botDelayMillis)
    }

    @Test
    fun `the cap toggle produces the 280 characters the roadmap decided on`() {
        assertEquals(280, GameSettings(characterCap = true).characterCapValue)
        assertEquals(CHARACTER_CAP, GameSettings(characterCap = true).characterCapValue)
    }

    @Test
    fun `slow and fast really are slower and faster`() {
        assertTrue(SpeedChoice.SLOW.motionSpeed < SpeedChoice.NORMAL.motionSpeed)
        assertTrue(SpeedChoice.NORMAL.motionSpeed < SpeedChoice.FAST.motionSpeed)
        assertTrue(SpeedChoice.SLOW.botDelayMillis > SpeedChoice.NORMAL.botDelayMillis)
        assertTrue(SpeedChoice.NORMAL.botDelayMillis > SpeedChoice.FAST.botDelayMillis)
        assertTrue(SpeedChoice.entries.all { it.botDelayMillis >= 0 && it.motionSpeed > 0 })
    }

    @Test
    fun `motion follows both the setting and the system`() {
        val settings = GameSettings(animationSpeed = SpeedChoice.FAST)
        assertTrue(settings.motion(systemAllowsMotion = true).enabled)
        assertEquals(SpeedChoice.FAST.motionSpeed, settings.motion(systemAllowsMotion = true).speed)
        // "Remove animations" wins over the app's own switch.
        assertFalse(settings.motion(systemAllowsMotion = false).enabled)
        assertFalse(GameSettings(animations = false).motion(systemAllowsMotion = true).enabled)
    }

    @Test
    fun `the animation speed reaches the durations motion uses`() {
        assertEquals(500, GameSettings().motion(true).duration(500))
        val fast = GameSettings(animationSpeed = SpeedChoice.FAST).motion(true)
        val slow = GameSettings(animationSpeed = SpeedChoice.SLOW).motion(true)
        assertTrue(fast.duration(900) < 900)
        assertTrue(slow.duration(900) > 900)
    }
}
