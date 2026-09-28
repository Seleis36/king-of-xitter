package com.kingofxitter.app.ui.motion

import com.kingofxitter.engine.model.GameEvent
import com.kingofxitter.engine.model.PlayerId
import com.kingofxitter.engine.model.PlayerResource
import com.kingofxitter.engine.model.ResourceChangeReason
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class BoardEffectsTest {
    private val you = PlayerId("you")
    private val kim = PlayerId("kim")

    private fun changed(
        player: PlayerId,
        resource: PlayerResource,
        amount: Int,
    ) = GameEvent.ResourceChanged(player, resource, amount, totalAfter = 5, reason = ResourceChangeReason.TWEET)

    @Test
    fun `resource changes become number effects`() {
        assertEquals(listOf(BoardEffect.Kind.Damage to (you to -3)), effectsFor(changed(you, PlayerResource.FOLLOWERS, -3)))
        assertEquals(listOf(BoardEffect.Kind.Heal to (you to 2)), effectsFor(changed(you, PlayerResource.FOLLOWERS, 2)))
        assertEquals(listOf(BoardEffect.Kind.Mandates to (kim to 2)), effectsFor(changed(kim, PlayerResource.MANDATES, 2)))
        assertEquals(listOf(BoardEffect.Kind.Characters to (kim to -20)), effectsFor(changed(kim, PlayerResource.CHARACTERS, -20)))
        assertEquals(emptyList(), effectsFor(changed(kim, PlayerResource.MANDATES, 0)))
    }

    @Test
    fun `suspensions and Xitter changes have effects, turn bookkeeping doesn't`() {
        assertEquals(listOf(BoardEffect.Kind.Suspended to (kim to 0)), effectsFor(GameEvent.PlayerSuspended(kim)))
        assertEquals(listOf(BoardEffect.Kind.NewMainCharacter to (you to 0)), effectsFor(GameEvent.MainCharacterChanged(kim, you)))
        assertEquals(listOf(BoardEffect.Kind.NewMainCharacter to (null to 0)), effectsFor(GameEvent.MainCharacterChanged(kim, null)))
        assertEquals(emptyList(), effectsFor(GameEvent.TurnStarted(you, 3)))
    }

    @Test
    fun `a burst plays in sequence and each effect expires`() =
        runTest {
            val scope = TestScope(testScheduler)
            val queue = EffectQueue(scope, staggerMillis = 100, lifetimeMillis = 1000)
            queue.offer(changed(kim, PlayerResource.FOLLOWERS, -3))
            queue.offer(changed(you, PlayerResource.MANDATES, 1))
            scope.runCurrent()
            assertEquals(listOf(BoardEffect.Kind.Damage), queue.active.value.map { it.kind })
            scope.advanceTimeBy(100)
            scope.runCurrent()
            assertEquals(listOf(BoardEffect.Kind.Damage, BoardEffect.Kind.Mandates), queue.active.value.map { it.kind })
            scope.advanceTimeBy(900)
            scope.runCurrent()
            assertEquals(listOf(BoardEffect.Kind.Mandates), queue.active.value.map { it.kind })
            scope.advanceTimeBy(100)
            scope.runCurrent()
            assertEquals(emptyList(), queue.active.value)
            scope.cancel()
        }
}
