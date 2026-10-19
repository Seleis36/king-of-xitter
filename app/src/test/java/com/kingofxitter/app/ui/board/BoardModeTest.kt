package com.kingofxitter.app.ui.board

import com.kingofxitter.engine.GameFixtures
import com.kingofxitter.engine.model.PlayerId
import com.kingofxitter.engine.model.TurnPhase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BoardModeTest {
    private val you = PlayerId("you")

    @Test
    fun `your roll can reroll until the limit`() {
        val mode = BoardMode.of(GameFixtures.yourRoll, you)
        assertEquals(BoardMode.Rolling(rollNumber = 2, rollLimit = 3), mode)
        assertTrue((mode as BoardMode.Rolling).canReroll)
        assertFalse(BoardMode.Rolling(3, 3).canReroll)
    }

    @Test
    fun `turn start rolls and shop phases shop`() {
        assertEquals(BoardMode.ReadyToRoll, BoardMode.of(GameFixtures.yourRoll.copy(phase = TurnPhase.TURN_START, rollNumber = 0), you))
        assertEquals(BoardMode.Shopping, BoardMode.of(GameFixtures.shop, you))
    }

    @Test
    fun `extra rerolls raise the limit before the turn freezes it`() {
        val state =
            GameFixtures.yourRoll.copy(
                players = GameFixtures.yourRoll.players.map { if (it.id == you) it.copy(extraRerolls = 1) else it },
            )
        assertEquals(BoardMode.Rolling(2, 4), BoardMode.of(state, you))
        assertEquals(BoardMode.Rolling(2, 5), BoardMode.of(state.copy(turnRollLimit = 5), you))
    }

    @Test
    fun `the human answers an incoming tweet even on someone else's turn`() {
        val mode = BoardMode.of(GameFixtures.incomingTweet, you)
        assertIs<BoardMode.IncomingTweet>(mode)
    }

    @Test
    fun `other players' turns and decisions are watched`() {
        assertEquals(BoardMode.Watching(PlayerId("kim")), BoardMode.of(GameFixtures.opponentTurn, you))
        assertEquals(BoardMode.Watching(you), BoardMode.of(GameFixtures.incomingTweet, PlayerId("kim")))
    }

    @Test
    fun `a result finishes the board`() {
        assertEquals(BoardMode.Finished, BoardMode.of(GameFixtures.victory, you))
    }

    @Test
    fun `a suspended human is done even while the bots play on`() {
        val suspended =
            GameFixtures.opponentTurn.copy(
                players =
                    GameFixtures.opponentTurn.players.map {
                        if (it.id ==
                            you
                        ) {
                            it.copy(followers = 0)
                        } else {
                            it
                        }
                    },
            )
        assertEquals(BoardMode.Finished, BoardMode.of(suspended, you))
    }
}
