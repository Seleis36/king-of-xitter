package com.kingofxitter.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.kingofxitter.app.ui.endgame.DefeatScreen
import com.kingofxitter.app.ui.endgame.StandingsScreen
import com.kingofxitter.app.ui.endgame.VictoryScreen
import com.kingofxitter.engine.GameFixtures
import com.kingofxitter.engine.model.MatchResult

/** Debug-only fixture host for emulator screenshots and callback checks. Not included in release builds. */
class EndGamePreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fixture = intent.getStringExtra("fixture") ?: "victory"
        val state =
            when (fixture) {
                "survivor" -> GameFixtures.survivorVictory
                "defeat" -> GameFixtures.defeat
                "nowinner" -> GameFixtures.noWinner
                "standings" -> GameFixtures.liveStandings
                "finalstandings" -> GameFixtures.standings
                "scoredefeat" -> GameFixtures.scoreDefeat
                "ties" -> GameFixtures.liveStandings.copy(players = GameFixtures.liveStandings.players.map { it.copy(mandates = 8) })
                "empty" -> GameFixtures.liveStandings.copy(players = emptyList())
                else -> GameFixtures.victory
            }
        setContent {
            MaterialTheme {
                when (fixture) {
                    "defeat", "nowinner", "scoredefeat" ->
                        DefeatScreen(
                            state,
                            state.players.first().id,
                            ::finish,
                            ::finish,
                            finalTweet =
                                if (fixture ==
                                    "defeat"
                                ) {
                                    getString(R.string.endgame_fixture_last_tweet)
                                } else {
                                    null
                                },
                        )
                    "standings", "finalstandings", "ties", "empty" -> StandingsScreen(state, ::finish)
                    else ->
                        VictoryScreen(state, ::finish, {
                            val winner = state.players.first { it.id == (state.result as MatchResult.Victory).playerId }
                            val share =
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        getString(R.string.endgame_share_summary, winner.leaderId.value, winner.mandates),
                                    )
                                }
                            startActivity(Intent.createChooser(share, null))
                        }, ::finish)
                }
            }
        }
    }
}
