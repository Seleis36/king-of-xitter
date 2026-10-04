package com.kingofxitter.content

import com.kingofxitter.engine.model.LeaderId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ZingerBookTest {
    @Test
    fun `every attacker has three lines for every other leader and for the whole timeline`() {
        LeaderRoster.all.forEach { attacker ->
            LeaderRoster.all.filter { it.id != attacker.id }.forEach { target ->
                val keys = ZingerBook.keys(attacker.id, target.id)
                assertTrue(keys.size >= 3, "${attacker.id.value}->${target.id.value} has ${keys.size}")
                assertEquals(keys.size, keys.toSet().size)
            }
            assertEquals(ZingerBook.PER_PAIR, ZingerBook.keys(attacker.id, null).size)
        }
    }

    @Test
    fun `the book has a key per pair plus one set per attacker, all distinct`() {
        val leaders = LeaderRoster.all.size
        assertEquals(leaders * (leaders - 1 + 1) * ZingerBook.PER_PAIR, ZingerBook.all.size)
        assertEquals(ZingerBook.all.size, ZingerBook.all.toSet().size)
    }

    @Test
    fun `a key names the attacker and the target`() {
        assertEquals(
            listOf("zinger_macaron_kim_0", "zinger_macaron_kim_1", "zinger_macaron_kim_2"),
            ZingerBook.keys(LeaderRoster.macaron.id, LeaderRoster.kim.id),
        )
        assertEquals(
            listOf("zinger_mileon_everyone_0", "zinger_mileon_everyone_1", "zinger_mileon_everyone_2"),
            ZingerBook.keys(LeaderRoster.mileon.id, null),
        )
    }

    @Test
    fun `nobody tweets at themselves and unknown leaders get nothing`() {
        assertEquals(emptyList(), ZingerBook.keys(LeaderRoster.xi.id, LeaderRoster.xi.id))
        assertEquals(emptyList(), ZingerBook.keys(LeaderId("nobody"), LeaderRoster.xi.id))
        assertEquals(emptyList(), ZingerBook.keys(LeaderRoster.xi.id, LeaderId("nobody")))
    }
}
