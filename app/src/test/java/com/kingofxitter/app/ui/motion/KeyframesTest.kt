package com.kingofxitter.app.ui.motion

import kotlin.test.Test
import kotlin.test.assertEquals

class KeyframesTest {
    private val frames = Keyframes(0f to 0f, 0.5f to 10f, 1f to 4f)

    @Test
    fun `interpolates between keys and holds outside them`() {
        assertEquals(0f, frames.at(-1f))
        assertEquals(5f, frames.at(0.25f))
        assertEquals(10f, frames.at(0.5f))
        assertEquals(7f, frames.at(0.75f))
        assertEquals(4f, frames.at(2f))
    }

    @Test
    fun `keys can be given in any order`() {
        assertEquals(5f, Keyframes(1f to 10f, 0f to 0f).at(0.5f))
    }

    @Test
    fun `one-shot effects end at rest`() {
        assertEquals(1f, KitMotion.slamScale.at(1f))
        assertEquals(1f, KitMotion.slamAlpha.at(1f))
        assertEquals(0f, KitMotion.shakeX.at(1f))
        assertEquals(0f, KitMotion.shakeRotation.at(1f))
        assertEquals(1f, KitMotion.popScale.at(1f))
        assertEquals(1f, KitMotion.stampScale.at(1f))
        assertEquals(0f, KitMotion.riseAlpha.at(1f))
    }

    @Test
    fun `effects start from the kit's first frame`() {
        assertEquals(1.7f, KitMotion.slamScale.at(0f))
        assertEquals(0f, KitMotion.popScale.at(0f))
        assertEquals(3f, KitMotion.stampScale.at(0f))
    }
}
