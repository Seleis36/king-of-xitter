package com.kingofxitter.app.ui.front

import com.kingofxitter.bots.BotDifficulty
import com.kingofxitter.content.LeaderRoster
import com.kingofxitter.engine.model.DiceFace
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VersusSamplesTest {
    @Test
    fun `a line-up is a real match with four distinct leaders and you first`() {
        listOf(VersusSamples.design, VersusSamples.tieBreak).forEach { lineUp ->
            assertEquals(4, lineUp.players.size)
            assertEquals(
                4,
                lineUp.players
                    .map { it.leaderId }
                    .toSet()
                    .size,
            )
            assertEquals(lineUp.humanPlayerId, lineUp.players.first().id)
            assertEquals(LeaderRoster.macaron.id, lineUp.players.first().leaderId)
            assertTrue(lineUp.players.all { it.leaderId in LeaderRoster.ids })
            assertTrue(lineUp.matchNumber in 0 until 10_000)
        }
    }

    @Test
    fun `the first player is the one who rolled the most tweets`() {
        listOf(VersusSamples.design, VersusSamples.tieBreak).forEach { lineUp ->
            val lastRound = lineUp.rollOff.last()
            val tweets = VersusSamples.tweetsIn(lastRound)
            assertEquals(tweets.values.max(), tweets.getValue(lineUp.firstPlayerId))
            assertTrue(lineUp.players.any { it.id == lineUp.firstPlayerId })
        }
    }

    @Test
    fun `every round rolls six dice for each leader still tied`() {
        listOf(VersusSamples.design, VersusSamples.tieBreak).forEach { lineUp ->
            assertTrue(lineUp.rollOff.isNotEmpty())
            lineUp.rollOff.forEach { round ->
                assertTrue(round.rolls.isNotEmpty())
                round.rolls.values.forEach { dice ->
                    assertEquals(6, dice.size)
                    assertTrue(dice.all { it in DiceFace.entries })
                }
            }
            // Later rounds only involve the leaders that tied in the one before.
            lineUp.rollOff.zipWithNext().forEach { (earlier, later) ->
                assertTrue(later.rolls.keys.all { it in earlier.rolls.keys })
                assertTrue(later.rolls.size < earlier.rolls.size)
            }
        }
    }

    @Test
    fun `the tie-break sample really needs more than one round`() {
        assertTrue(VersusSamples.tieBreak.rollOff.size > 1, "${VersusSamples.tieBreak.rollOff.size}")
        assertEquals(1, VersusSamples.design.rollOff.size)
    }

    @Test
    fun `every difficulty has a label and an explanation`() {
        val labels = BotDifficulty.entries.map { it.label() }
        val helps = BotDifficulty.entries.map { it.help() }
        assertEquals(BotDifficulty.entries.size, labels.toSet().size)
        assertEquals(BotDifficulty.entries.size, helps.toSet().size)
        assertTrue((labels + helps).all { it != 0 })
    }
}
