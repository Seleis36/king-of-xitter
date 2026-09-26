@file:Suppress("ktlint:standard:function-naming")

package com.kingofxitter.app.ui.endgame

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kingofxitter.app.R
import com.kingofxitter.engine.GameFixtures
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.MatchResult
import com.kingofxitter.engine.model.MatchStats
import com.kingofxitter.engine.model.PlayerId
import com.kingofxitter.engine.model.PlayerState
import com.kingofxitter.engine.model.VictoryReason

private val Ink = Color(0xFF1B1426)
private val Paper = Color(0xFFFFF7E6)
private val Gold = Color(0xFFFFD23F)
private val Pink = Color(0xFFFF7192)
private val Panel = Color(0xFF302743)

/** Victory route. The caller replaces the finished match when replaying or returning to the menu. */
@Composable
fun VictoryScreen(
    state: GameState,
    onPlayAgain: () -> Unit,
    onShare: () -> Unit,
    onMainMenu: () -> Unit,
    modifier: Modifier = Modifier,
    displayName: @Composable (PlayerState) -> String = { leaderName(it) },
) {
    val result = state.result as? MatchResult.Victory ?: return
    val winner = state.players.first { it.id == result.playerId }
    EndGameScaffold(modifier) {
        Heading(
            stringResource(
                if (result.reason ==
                    VictoryReason.TWENTY_MANDATES
                ) {
                    R.string.endgame_victory_title
                } else {
                    R.string.endgame_survivor_title
                },
            ),
        )
        Text(
            stringResource(
                if (winner.mandates >=
                    15
                ) {
                    R.string.endgame_victory_subtitle
                } else {
                    R.string.endgame_survivor_subtitle
                },
            ),
            textAlign = TextAlign.Center,
        )
        Surface(shape = RoundedCornerShape(20.dp), color = Paper, contentColor = Ink) {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(displayName(winner), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text(stringResource(R.string.endgame_mandates_total, winner.mandates))
            }
        }
        CompactStandings(state, displayName)
        MatchStatsPanel(state.matchStats[winner.id] ?: MatchStats())
        PrimaryAction(stringResource(R.string.endgame_play_again), onPlayAgain)
        TextButton(onClick = onShare) { Text(stringResource(R.string.endgame_share), color = Gold) }
        TextButton(onClick = onMainMenu) { Text(stringResource(R.string.endgame_main_menu), color = Paper) }
    }
}

/** Covers suspension, a rival reaching 20 first, and simultaneous elimination without a winner. */
@Composable
fun DefeatScreen(
    state: GameState,
    playerId: PlayerId,
    onPlayAgain: () -> Unit,
    onMainMenu: () -> Unit,
    modifier: Modifier = Modifier,
    finalTweet: String? = null,
    displayName: @Composable (PlayerState) -> String = { leaderName(it) },
) {
    val player = state.players.first { it.id == playerId }
    val winnerId = (state.result as? MatchResult.Victory)?.playerId
    EndGameScaffold(modifier) {
        Heading(stringResource(if (player.isSuspended) R.string.endgame_defeat_title else R.string.endgame_score_defeat_title), Pink)
        Text(displayName(player), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.endgame_defeat_subtitle), textAlign = TextAlign.Center)
        Text(stringResource(R.string.endgame_turn_number, state.turnNumber))
        val winner = state.players.firstOrNull { it.id == winnerId }
        if (winner != null) {
            Text(stringResource(R.string.endgame_winner), color = Gold)
            Text(displayName(winner), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.endgame_mandates_total, winner.mandates))
        } else if (state.result == MatchResult.NoWinner) {
            Text(stringResource(R.string.endgame_no_winner), textAlign = TextAlign.Center)
        } else {
            Text(stringResource(R.string.endgame_match_continues), textAlign = TextAlign.Center)
        }
        if (finalTweet != null) {
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Paper, contentColor = Ink) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.endgame_last_tweet), fontWeight = FontWeight.Bold)
                    Text(finalTweet)
                }
            }
        }
        MatchStatsPanel(state.matchStats[playerId] ?: MatchStats())
        PrimaryAction(stringResource(R.string.endgame_appeal), onPlayAgain)
        TextButton(onClick = onMainMenu) { Text(stringResource(R.string.endgame_main_menu), color = Paper) }
    }
}

/** Standings support both a running match and its final result. */
@Composable
fun StandingsScreen(
    state: GameState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    displayName: @Composable (PlayerState) -> String = { leaderName(it) },
) {
    EndGameScaffold(modifier) {
        Heading(stringResource(R.string.endgame_standings_title))
        Text(stringResource(R.string.endgame_badge_thresholds), textAlign = TextAlign.Center)
        if (state.players.isEmpty()) Text(stringResource(R.string.endgame_empty_standings))
        RankedPlayers(state, displayName)
        PrimaryAction(stringResource(if (state.result == null) R.string.endgame_back_to_game else R.string.endgame_main_menu), onBack)
    }
}

@Composable
private fun EndGameScaffold(
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(modifier.fillMaxSize(), color = Ink, contentColor = Paper) {
        Column(
            Modifier
                .safeDrawingPadding()
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

@Composable
private fun Heading(
    text: String,
    color: Color = Gold,
) {
    Text(
        text,
        Modifier.semantics {
            heading()
        },
        color = color,
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Black,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun PrimaryAction(
    label: String,
    onClick: () -> Unit,
) {
    Button(
        onClick,
        Modifier.fillMaxWidth().heightIn(min = 56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink),
    ) {
        Text(label, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
    }
}

@Composable
private fun MatchStatsPanel(stats: MatchStats) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Panel, contentColor = Paper) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.endgame_match_stats), fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.endgame_tweets_sent, stats.tweetsSent))
            Text(stringResource(R.string.endgame_unfollows_dealt, stats.unfollowsDealt))
            Text(stringResource(R.string.endgame_turns_as_main_character, stats.turnsAsMainCharacter))
        }
    }
}

@Composable
private fun CompactStandings(
    state: GameState,
    displayName: @Composable (PlayerState) -> String,
) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Panel, contentColor = Paper) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            standings(state).forEach { (rank, player) ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.endgame_rank, rank), color = Gold, fontWeight = FontWeight.Bold)
                    Column(Modifier.weight(1f)) {
                        Text(displayName(player), fontWeight = FontWeight.Bold)
                        if (player.isSuspended) {
                            Text(
                                stringResource(R.string.endgame_suspended),
                                color = Pink,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                    Text(stringResource(R.string.endgame_score_number, player.mandates), color = Gold)
                }
            }
        }
    }
}

@Composable
private fun RankedPlayers(
    state: GameState,
    displayName: @Composable (PlayerState) -> String,
) {
    standings(state).forEach { (rank, player) ->
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Panel, contentColor = Paper) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.endgame_rank, rank), color = Gold, fontWeight = FontWeight.Black)
                    Text(displayName(player), Modifier.weight(1f), fontWeight = FontWeight.Bold)
                }
                Text(stringResource(R.string.endgame_mandates_total, player.mandates), color = Gold)
                Text(stringResource(R.string.endgame_resources, player.followers, player.characters))
                LinearProgressIndicator(progress = {
                    (player.mandates / 20f).coerceIn(0f, 1f)
                }, modifier = Modifier.fillMaxWidth(), color = Gold, trackColor = Ink)
                Text(
                    stringResource(
                        when {
                            player.mandates >= 15 -> R.string.endgame_gold_badge
                            player.mandates >= 6 -> R.string.endgame_blue_badge
                            else -> R.string.endgame_unverified
                        },
                    ),
                )
                if (player.isSuspended) Text(stringResource(R.string.endgame_suspended), color = Pink)
                if (player.id == state.xitterPlayerId &&
                    state.result == null
                ) {
                    Text(stringResource(R.string.endgame_on_xitter), color = Gold)
                }
            }
        }
    }
}

/** Temporary labels until the content roster supplies localized display names. */
@Composable
private fun leaderName(player: PlayerState): String =
    stringResource(
        when (player.leaderId.value) {
            "macaron" -> R.string.leader_macaron
            "kim" -> R.string.leader_kim
            "poutine" -> R.string.leader_poutine
            "mi-leon" -> R.string.leader_mileon
            "xi" -> R.string.leader_xi
            "trumpet" -> R.string.leader_trumpet
            else -> R.string.endgame_unknown_leader
        },
    )

@Preview(name = "Victory", widthDp = 412, heightDp = 915)
@Composable
private fun VictoryPreview() = MaterialTheme { VictoryScreen(GameFixtures.victory, {}, {}, {}) }

@Preview(name = "Last survivor", widthDp = 320, heightDp = 640, fontScale = 1.5f)
@Composable
private fun SurvivorPreview() = MaterialTheme { VictoryScreen(GameFixtures.survivorVictory, {}, {}, {}) }

@Preview(name = "Suspended", widthDp = 412, heightDp = 915)
@Composable
private fun DefeatPreview() =
    MaterialTheme {
        DefeatScreen(
            GameFixtures.defeat,
            GameFixtures.defeat.players
                .first()
                .id,
            {},
            {},
        )
    }

@Preview(name = "No winner", widthDp = 320, heightDp = 640)
@Composable
private fun NoWinnerPreview() =
    MaterialTheme {
        DefeatScreen(
            GameFixtures.noWinner,
            GameFixtures.noWinner.players
                .first()
                .id,
            {},
            {},
        )
    }

@Preview(name = "Live standings", widthDp = 412, heightDp = 915)
@Composable
private fun StandingsPreview() = MaterialTheme { StandingsScreen(GameFixtures.liveStandings, {}) }

@Preview(name = "Tied standings", widthDp = 320, heightDp = 640, fontScale = 1.5f)
@Composable
private fun TiedPreview() =
    MaterialTheme {
        StandingsScreen(GameFixtures.liveStandings.copy(players = GameFixtures.liveStandings.players.map { it.copy(mandates = 8) }), {})
    }

@Preview(name = "Empty standings")
@Composable
private fun EmptyPreview() = MaterialTheme { StandingsScreen(GameFixtures.liveStandings.copy(players = emptyList()), {}) }

@Preview(name = "Lost to score", widthDp = 412, heightDp = 915)
@Composable
private fun ScoreDefeatPreview() =
    MaterialTheme {
        DefeatScreen(
            GameFixtures.scoreDefeat,
            GameFixtures.scoreDefeat.players
                .first()
                .id,
            {},
            {},
        )
    }

@Preview(name = "Final standings", widthDp = 412, heightDp = 915)
@Composable
private fun FinalStandingsPreview() = MaterialTheme { StandingsScreen(GameFixtures.standings, {}) }
