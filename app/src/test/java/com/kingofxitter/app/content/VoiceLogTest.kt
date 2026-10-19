package com.kingofxitter.app.content

import com.kingofxitter.content.TweetMoment
import com.kingofxitter.engine.GameFixtures
import com.kingofxitter.engine.model.CardId
import com.kingofxitter.engine.model.DiceFace
import com.kingofxitter.engine.model.Die
import com.kingofxitter.engine.model.DieId
import com.kingofxitter.engine.model.GameEvent
import com.kingofxitter.engine.model.PlayerResource
import com.kingofxitter.engine.model.ResourceChangeReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VoiceLogTest {
    private val state = GameFixtures.yourRoll
    private val you = state.currentPlayerId
    private val kim = state.players[1].id

    /** Stands in for the resource lookup: the key and the draw are enough to tell lines apart. */
    private fun log(write: (String) -> String = { it }) =
        VoiceLog { event, leaderId, random -> write("${leaderId.value}/${event.moment}/${random.nextInt(5)}") }

    @Test
    fun `a roll becomes one line by the roller`() {
        val log = log()
        log.record(GameEvent.DiceRolled(you, 1, listOf(Die(DieId(0), DiceFace.TWEET))), state)
        val entries = log.entries.value
        assertEquals(1, entries.size)
        assertEquals(you, entries.single().playerId)
        assertEquals(TweetMoment.ROLLED, entries.single().moment)
        assertEquals(state.turnNumber, entries.single().turnNumber)
    }

    @Test
    fun `a landed tweet logs the attacker and the victim, and names the target`() {
        val log = log()
        log.record(GameEvent.ResourceChanged(kim, PlayerResource.FOLLOWERS, -3, 7, ResourceChangeReason.TWEET), state)
        val entries = log.entries.value
        assertEquals(listOf(TweetMoment.ATTACKED, TweetMoment.GOT_HIT), entries.map { it.moment })
        assertEquals(listOf(kim, you), entries.map { it.targetId })
        // Two lines in one batch draw separately, so the attacker and the victim don't echo.
        assertEquals(2, entries.map { it.text }.toSet().size)
    }

    @Test
    fun `quiet events add nothing`() {
        val log = log()
        log.record(GameEvent.DiceRolled(you, 2, emptyList()), state)
        log.record(GameEvent.TurnStarted(you, 4), state)
        assertEquals(emptyList(), log.entries.value)
        assertNull(log.latest(you))
    }

    @Test
    fun `an unwritten line is dropped instead of logged blank`() {
        val log = log { "" }
        log.record(GameEvent.PowerUpBought(you, CardId("c12_bot_farm")), state)
        assertEquals(emptyList(), log.entries.value)
    }

    @Test
    fun `latest returns the newest line by that leader`() {
        val log = log()
        log.record(GameEvent.PowerUpBought(you, CardId("c12_bot_farm")), state)
        log.record(GameEvent.PlayerSuspended(kim), state)
        log.record(GameEvent.DiceRolled(you, 1, emptyList()), state)
        assertEquals(TweetMoment.ROLLED, log.latest(you)?.moment)
        assertEquals(TweetMoment.SUSPENDED, log.latest(kim)?.moment)
        assertNull(log.latest(null))
    }

    @Test
    fun `the log is capped so a long match can't grow without bound`() {
        val log = log()
        repeat(500) { log.record(GameEvent.PowerUpBought(you, CardId("c12_bot_farm")), state) }
        assertTrue(log.entries.value.size <= 200, "${log.entries.value.size}")
    }

    @Test
    fun `the zinger you picked is the attack tweet, posted once however many it hits`() {
        val macaron = state.players[2].id
        val log = log()
        log.draft(you, "My chosen zinger", state.turnNumber)
        log.record(GameEvent.ResourceChanged(kim, PlayerResource.FOLLOWERS, -1, 9, ResourceChangeReason.TWEET), state)
        log.record(GameEvent.ResourceChanged(macaron, PlayerResource.FOLLOWERS, -1, 9, ResourceChangeReason.TWEET), state)

        val attacks = log.entries.value.filter { it.moment == TweetMoment.ATTACKED }
        assertEquals(listOf("My chosen zinger"), attacks.map { it.text })
        // Both victims still answer.
        assertEquals(
            listOf(kim, macaron),
            log.entries.value
                .filter { it.moment == TweetMoment.GOT_HIT }
                .map { it.playerId },
        )
    }

    @Test
    fun `a draft from an earlier turn is never posted`() {
        val log = log()
        log.draft(you, "Stale zinger", state.turnNumber - 1)
        log.record(GameEvent.ResourceChanged(kim, PlayerResource.FOLLOWERS, -1, 9, ResourceChangeReason.TWEET), state)

        assertTrue(log.entries.value.none { it.text == "Stale zinger" })
        assertEquals(1, log.entries.value.count { it.moment == TweetMoment.ATTACKED })
    }
}
