package com.kingofxitter.app.ui.board

import com.kingofxitter.engine.model.DiceFace
import com.kingofxitter.engine.model.Die
import com.kingofxitter.engine.model.DieId
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.LeaderId
import com.kingofxitter.engine.model.PlayerId
import com.kingofxitter.engine.model.PlayerState
import com.kingofxitter.engine.model.TurnPhase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RollPreviewTest {
    private val you = PlayerId("you")
    private val a = PlayerId("a")
    private val b = PlayerId("b")

    private fun state(
        vararg faces: DiceFace,
        xitter: PlayerId? = a,
        followers: Int = 7,
        characters: Int = 0,
        cap: Int? = null,
        bSuspended: Boolean = false,
    ) = GameState(
        players =
            listOf(
                PlayerState(you, LeaderId("macaron"), followers = followers, characters = characters),
                PlayerState(a, LeaderId("trumpet")),
                PlayerState(b, LeaderId("kim"), followers = if (bSuspended) 0 else 10),
            ),
        currentPlayerId = you,
        phase = TurnPhase.ROLLING,
        xitterPlayerId = xitter,
        dice = faces.mapIndexed { index, face -> Die(DieId(index), face) },
        rollNumber = 1,
        characterCap = cap,
    )

    @Test
    fun `number sets score their face plus one per extra die`() {
        assertEquals(3, RollPreview.of(state(DiceFace.THREE, DiceFace.THREE, DiceFace.THREE), you).setMandates)
        assertEquals(3, RollPreview.of(state(DiceFace.ONE, DiceFace.ONE, DiceFace.ONE, DiceFace.ONE, DiceFace.ONE), you).setMandates)
        assertEquals(0, RollPreview.of(state(DiceFace.TWO, DiceFace.TWO), you).setMandates)
        assertEquals(
            3,
            RollPreview.of(state(DiceFace.ONE, DiceFace.ONE, DiceFace.ONE, DiceFace.TWO, DiceFace.TWO, DiceFace.TWO), you).setMandates,
        )
    }

    @Test
    fun `likes heal up to the maximum and are blocked on Xitter`() {
        assertEquals(2, RollPreview.of(state(DiceFace.LIKE, DiceFace.LIKE), you).followersHealed)
        assertEquals(1, RollPreview.of(state(DiceFace.LIKE, DiceFace.LIKE, followers = 9), you).followersHealed)
        val onXitter = RollPreview.of(state(DiceFace.LIKE, xitter = you), you)
        assertEquals(0, onXitter.followersHealed)
        assertTrue(onXitter.likesBlocked)
    }

    @Test
    fun `characters respect the optional cap`() {
        assertEquals(20, RollPreview.of(state(DiceFace.CHARACTERS, DiceFace.CHARACTERS), you).characters)
        assertEquals(10, RollPreview.of(state(DiceFace.CHARACTERS, DiceFace.CHARACTERS, characters = 270, cap = 280), you).characters)
    }

    @Test
    fun `tweets target the Main Character, everyone, or nobody`() {
        assertEquals(TweetTargets.MainCharacter(a), RollPreview.of(state(DiceFace.TWEET), you).targets)
        assertEquals(TweetTargets.Everyone(listOf(a, b)), RollPreview.of(state(DiceFace.TWEET, xitter = you), you).targets)
        assertEquals(TweetTargets.Everyone(listOf(a)), RollPreview.of(state(DiceFace.TWEET, xitter = you, bSuspended = true), you).targets)
        val empty = RollPreview.of(state(DiceFace.TWEET, xitter = null), you)
        assertEquals(TweetTargets.Nobody, empty.targets)
        assertTrue(empty.entersXitter)
        assertFalse(RollPreview.of(state(DiceFace.TWEET), you).entersXitter)
    }

    @Test
    fun `a roll that does nothing is empty`() {
        assertTrue(RollPreview.of(state(DiceFace.ONE, DiceFace.TWO, DiceFace.LIKE, followers = 10), you).isEmpty)
    }
}
