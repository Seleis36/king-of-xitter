package com.kingofxitter.app.ui.endgame

import com.kingofxitter.engine.GameFixtures
import com.kingofxitter.engine.model.MatchResult
import kotlin.test.Test
import kotlin.test.assertEquals

class StandingsTest {
    @Test fun `unsorted roster displays decreasing scores and shares tied ranks`() {
        val state = GameFixtures.liveStandings
        assertEquals(listOf(16, 13, 12, 4), standings(state).map { it.player.mandates })
        val tied = state.copy(players = state.players.map { it.copy(mandates = 5) })
        assertEquals(listOf(1, 1, 1, 1), standings(tied).map { it.rank })
        assertEquals(emptyList(), standings(state.copy(players = emptyList())))
    }

    @Test fun `last surviving winner ranks ahead of suspended players with more mandates`() {
        val state = GameFixtures.survivorVictory
        val winner = (state.result as MatchResult.Victory).playerId
        assertEquals(winner, standings(state).first().player.id)
        assertEquals(1, standings(state).first().rank)
    }
}
