package com.kingofxitter.app.content

import com.kingofxitter.engine.GameFixtures
import com.kingofxitter.engine.model.CardId
import com.kingofxitter.engine.model.GameEvent
import kotlin.test.Test
import kotlin.test.assertEquals

/** The timeline keys its rows on [VoiceEntry.id], so ids must never repeat within one log. */
class VoiceEntryIdTest {
    private val state = GameFixtures.yourRoll
    private val you = state.currentPlayerId

    @Test
    fun `two identical buy lines in one turn get distinct ids`() {
        // The writer always returns the same text and a buy line has no target, so every other
        // field of the two entries is equal. That is the case that used to crash the timeline.
        val log = VoiceLog { _, _, _ -> "Bought it." }
        log.record(GameEvent.PowerUpBought(you, CardId("c12_bot_farm")), state)
        log.record(GameEvent.PowerUpBought(you, CardId("c12_bot_farm")), state)

        val entries = log.entries.value
        assertEquals(2, entries.size)
        assertEquals(entries[0].copy(id = 0), entries[1].copy(id = 0))
        assertEquals(2, entries.map { it.id }.toSet().size)
    }

    @Test
    fun `ids stay unique once the log is trimmed`() {
        val log = VoiceLog { _, _, _ -> "Bought it." }
        repeat(500) { log.record(GameEvent.PowerUpBought(you, CardId("c12_bot_farm")), state) }

        val ids = log.entries.value.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertEquals(ids.sorted(), ids)
    }
}
