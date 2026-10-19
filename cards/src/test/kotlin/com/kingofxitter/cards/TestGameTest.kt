package com.kingofxitter.cards

import com.kingofxitter.engine.model.CardId
import com.kingofxitter.engine.model.DiceFace
import com.kingofxitter.engine.model.LeaderId
import com.kingofxitter.engine.model.PlayerId
import com.kingofxitter.engine.model.PlayerState
import com.kingofxitter.engine.model.TurnPhase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TestGameTest {
    private val third = PlayerId("third")

    @Test
    fun `the default is the human against one bot, rolling, on a fixed seed`() {
        val state = TestGame().build()

        assertEquals(listOf(TestGame.HUMAN, TestGame.BOT), state.players.map { it.id })
        assertEquals(TestGame.HUMAN, state.currentPlayerId)
        assertEquals(TurnPhase.ROLLING, state.phase)
        assertEquals(TestGame.DEFAULT_SEED, state.randomState)
        assertEquals(TestGame().build(), state)
    }

    @Test
    fun `extra players sit after the bot and take the same builders`() {
        val state =
            TestGame()
                .withPlayer(PlayerState(third, LeaderId("kim")))
                .withFollowers(third, 3)
                .withCharacters(third, 40)
                .withMandates(third, 7)
                .withPolicy(third, CardId("c02_lobbyist"))
                .build()

        assertEquals(listOf(TestGame.HUMAN, TestGame.BOT, third), state.players.map { it.id })
        val seated = state.players.last()
        assertEquals(listOf(3, 40, 7), listOf(seated.followers, seated.characters, seated.mandates))
        assertEquals(listOf(CardId("c02_lobbyist")), seated.policyIds)
    }

    @Test
    fun `seating the same id again replaces that player`() {
        val state =
            TestGame()
                .withPlayer(PlayerState(third, LeaderId("kim")))
                .withPlayer(PlayerState(third, LeaderId("xi")))
                .build()

        assertEquals(LeaderId("xi"), state.players.single { it.id == third }.leaderId)
    }

    @Test
    fun `the human and the bot are set with their own builders`() {
        assertFailsWith<IllegalArgumentException> { TestGame().withPlayer(PlayerState(TestGame.BOT, LeaderId("kim"))) }
    }

    @Test
    fun `dice, phase and Xitter come through as set`() {
        val state =
            TestGame()
                .withPhase(TurnPhase.SHOP)
                .withXitter(TestGame.BOT)
                .withDice(DiceFace.TWEET, DiceFace.LIKE)
                .build()

        assertEquals(TurnPhase.SHOP, state.phase)
        assertEquals(TestGame.BOT, state.xitterPlayerId)
        assertEquals(listOf(DiceFace.TWEET, DiceFace.LIKE), state.dice.map { it.face })
    }
}
