package com.kingofxitter.app.feedback

import com.kingofxitter.engine.model.GameEvent
import com.kingofxitter.engine.model.MatchResult
import com.kingofxitter.engine.model.PlayerId
import com.kingofxitter.engine.model.PlayerResource
import com.kingofxitter.engine.model.ResourceChangeReason
import com.kingofxitter.engine.model.VictoryReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CuesTest {
    private val you = PlayerId("you")
    private val kim = PlayerId("kim")

    private fun damage(
        player: PlayerId,
        reason: ResourceChangeReason = ResourceChangeReason.TWEET,
    ) = GameEvent.ResourceChanged(player, PlayerResource.FOLLOWERS, -2, 5, reason)

    @Test
    fun `rolls rattle, tweets land, hits hurt`() {
        assertEquals(listOf(Cue.Dice), cuesFor(GameEvent.DiceRolled(kim, 1, emptyList()), you))
        assertEquals(listOf(Cue.Hit), cuesFor(damage(you), you))
        assertEquals(listOf(Cue.Tweet), cuesFor(damage(kim), you))
    }

    @Test
    fun `only tweet damage makes tweet sounds`() {
        assertEquals(emptyList(), cuesFor(damage(you, ResourceChangeReason.POWER_UP), you))
        assertEquals(emptyList(), cuesFor(GameEvent.ResourceChanged(you, PlayerResource.MANDATES, 2, 8, ResourceChangeReason.OTHER), you))
    }

    @Test
    fun `the end of the match plays your result`() {
        assertEquals(listOf(Cue.Win), cuesFor(GameEvent.MatchEnded(MatchResult.Victory(you, VictoryReason.TWENTY_MANDATES)), you))
        assertEquals(listOf(Cue.Lose), cuesFor(GameEvent.MatchEnded(MatchResult.Victory(kim, VictoryReason.TWENTY_MANDATES)), you))
        assertEquals(listOf(Cue.Lose), cuesFor(GameEvent.MatchEnded(MatchResult.NoWinner), you))
    }

    @Test
    fun `repeats within the window are dropped`() {
        val throttle = CueThrottle(windowMillis = 250)
        assertTrue(throttle.allow(Cue.Tweet, 1000))
        assertFalse(throttle.allow(Cue.Tweet, 1100))
        assertTrue(throttle.allow(Cue.Hit, 1100))
        assertTrue(throttle.allow(Cue.Tweet, 1250))
    }
}
