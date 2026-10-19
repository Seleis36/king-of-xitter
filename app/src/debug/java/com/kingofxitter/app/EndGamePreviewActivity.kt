package com.kingofxitter.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import com.kingofxitter.app.content.VoiceEntry
import com.kingofxitter.app.ui.endgame.DefeatScreen
import com.kingofxitter.app.ui.endgame.StandingsScreen
import com.kingofxitter.app.ui.endgame.VictoryScreen
import com.kingofxitter.app.ui.theme.XitterTheme
import com.kingofxitter.app.ui.theme.nightBackground
import com.kingofxitter.content.TweetMoment
import com.kingofxitter.engine.GameFixtures
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.MatchResult

/** Debug-only fixture host for emulator screenshots and callback checks. Not included in release builds. */
class EndGamePreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val bars = SystemBarStyle.dark(Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
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
        val you = state.players.firstOrNull()?.id
        setContent {
            XitterTheme {
                val modifier = Modifier.fillMaxSize().nightBackground().safeDrawingPadding()
                when (fixture) {
                    "defeat", "nowinner", "scoredefeat" ->
                        DefeatScreen(
                            state,
                            state.players.first().id,
                            ::finish,
                            ::finish,
                            modifier,
                            finalTweet =
                                if (fixture ==
                                    "defeat"
                                ) {
                                    fixtureTweet(state, getString(R.string.endgame_fixture_last_tweet))
                                } else {
                                    null
                                },
                        )
                    "standings", "finalstandings", "ties", "empty" -> StandingsScreen(state, ::finish, modifier, you)
                    else ->
                        VictoryScreen(
                            state,
                            ::finish,
                            {
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
                            },
                            ::finish,
                            modifier,
                            reaction = fixtureTweet(state, getString(R.string.tweet_trumpet_lost_0), TweetMoment.LOST),
                        )
                }
            }
        }
    }

    /** A tweet by the first leader who isn't the fixture's own player. */
    private fun fixtureTweet(
        state: GameState,
        text: String,
        moment: TweetMoment = TweetMoment.ATTACKED,
    ): VoiceEntry? {
        val author = state.players.drop(1).firstOrNull() ?: return null
        return VoiceEntry(author.id, author.leaderId, moment, state.players.first().id, text, state.turnNumber)
    }
}
