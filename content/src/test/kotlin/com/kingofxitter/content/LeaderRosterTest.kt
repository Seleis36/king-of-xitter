package com.kingofxitter.content

import com.kingofxitter.engine.MatchSetup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class LeaderRosterTest {
    @Test
    fun `roster holds the six leaders of the cast`() {
        assertEquals(6, LeaderRoster.all.size)
        assertEquals(6, LeaderRoster.ids.toSet().size)
        assertEquals(LeaderRoster.all.map { it.id }, LeaderRoster.ids)
    }

    @Test
    fun `every leader has its own colour, portrait and string keys`() {
        assertEquals(
            6,
            LeaderRoster.all
                .map { it.colorArgb }
                .toSet()
                .size,
        )
        assertEquals(
            6,
            LeaderRoster.all
                .map { it.portraitKey }
                .toSet()
                .size,
        )
        assertEquals(
            6,
            LeaderRoster.all
                .map { it.shortNameKey }
                .toSet()
                .size,
        )
        LeaderRoster.all.forEach { leader ->
            // Opaque ARGB, so the app can build a Compose colour without a dependency on it.
            assertEquals(0xFFL, leader.colorArgb ushr 24, leader.id.value)
            listOf(leader.firstNameKey, leader.shortNameKey, leader.fullNameKey, leader.handleKey, leader.voiceKey).forEach { key ->
                assertTrue(key.isNotEmpty() && key.none { it.isUpperCase() }, key)
            }
        }
    }

    @Test
    fun `lookup finds roster members and rejects unknown leaders`() {
        LeaderRoster.all.forEach { leader -> assertSame(leader, LeaderRoster.byId(leader.id)) }
        assertNull(
            LeaderRoster.byId(
                com.kingofxitter.engine.model
                    .LeaderId("nobody"),
            ),
        )
    }

    @Test
    fun `the roster satisfies the engine's match setup contract`() {
        val start = MatchSetup.create(LeaderRoster.macaron.id, LeaderRoster.ids, seed = 7)
        assertEquals(4, start.state.players.size)
        assertEquals(
            LeaderRoster.macaron.id,
            start.state.players
                .first()
                .leaderId,
        )
        assertTrue(start.state.players.all { it.leaderId in LeaderRoster.ids })
    }
}
