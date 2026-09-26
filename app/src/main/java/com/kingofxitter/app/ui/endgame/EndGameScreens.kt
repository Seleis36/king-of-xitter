@file:Suppress("ktlint:standard:function-naming")

package com.kingofxitter.app.ui.endgame

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingofxitter.app.R
import com.kingofxitter.engine.GameFixtures
import com.kingofxitter.engine.model.MatchStats
import com.kingofxitter.engine.model.PlayerState

private val Ink = Color(0xFF1B1426)
private val Paper = Color(0xFFFFF7E6)
private val Gold = Color(0xFFFFD23F)
private val Pink = Color(0xFFFF3D6E)
private val Muted = Color(0xFFBDB4D4)

/** End-game victory view, fed by the reducer's final standings and match statistics. */
@Composable
fun VictoryScreen(
    winner: PlayerState,
    players: List<PlayerState>,
    stats: MatchStats,
    onPlayAgain: () -> Unit,
    onViewStandings: () -> Unit,
    modifier: Modifier = Modifier,
    displayName: (PlayerState) -> String = PlayerState::endGameName,
) {
    EndGameScaffold(modifier) {
        Text(
            text = stringResource(R.string.endgame_victory_title),
            color = Gold,
            fontSize = 38.sp,
            lineHeight = 42.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.endgame_victory_subtitle),
            color = Paper,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        WinnerPanel(winner = winner, name = displayName(winner))
        MatchStatsPanel(stats)
        StandingsPanel(players, winner.id, displayName)
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onPlayAgain,
            modifier = Modifier.fillMaxWidth().height(58.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink),
        ) {
            Text(stringResource(R.string.endgame_play_again), fontWeight = FontWeight.Black)
        }
        TextButton(onClick = onViewStandings) {
            Text(stringResource(R.string.endgame_view_standings), color = Paper)
        }
    }
}

/** Defeat view for a suspended player, with the winner and personal match stats. */
@Composable
fun DefeatScreen(
    suspendedPlayer: PlayerState,
    winner: PlayerState?,
    stats: MatchStats,
    onBackToGame: () -> Unit,
    modifier: Modifier = Modifier,
    displayName: (PlayerState) -> String = PlayerState::endGameName,
) {
    EndGameScaffold(modifier) {
        Text(
            text = stringResource(R.string.endgame_defeat_title),
            color = Pink,
            fontSize = 36.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.endgame_defeat_subtitle),
            color = Paper,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        PlayerPanel(
            label = stringResource(R.string.endgame_your_account),
            player = suspendedPlayer,
            name = displayName(suspendedPlayer),
            accent = Pink,
        )
        if (winner != null) {
            PlayerPanel(
                label = stringResource(R.string.endgame_winner),
                player = winner,
                name = displayName(winner),
                accent = Gold,
            )
        } else {
            Text(
                text = stringResource(R.string.endgame_no_winner),
                color = Paper,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        MatchStatsPanel(stats)
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onBackToGame,
            modifier = Modifier.fillMaxWidth().height(58.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink),
        ) {
            Text(stringResource(R.string.endgame_back_to_game), fontWeight = FontWeight.Black)
        }
    }
}

/** Final ranked roster for the standings route. Ties retain their shared rank. */
@Composable
fun StandingsScreen(
    players: List<PlayerState>,
    statsByPlayer: Map<com.kingofxitter.engine.model.PlayerId, MatchStats>,
    onBackToGame: () -> Unit,
    modifier: Modifier = Modifier,
    displayName: (PlayerState) -> String = PlayerState::endGameName,
) {
    val rankedPlayers = players.sortedWith(compareByDescending<PlayerState> { it.mandates }.thenBy { it.id.value })
    EndGameScaffold(modifier) {
        Text(
            text = stringResource(R.string.endgame_standings_title),
            color = Gold,
            fontSize = 34.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.endgame_standings_subtitle),
            color = Paper,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        if (rankedPlayers.isEmpty()) {
            Text(stringResource(R.string.endgame_empty_standings), color = Paper)
        } else {
            StandingsPanel(rankedPlayers, winnerId = null, displayName = displayName, statsByPlayer = statsByPlayer)
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onBackToGame,
            modifier = Modifier.fillMaxWidth().height(58.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink),
        ) {
            Text(stringResource(R.string.endgame_back_to_game), fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun EndGameScaffold(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(modifier = modifier.fillMaxSize(), color = Ink, contentColor = Paper) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
private fun WinnerPanel(
    winner: PlayerState,
    name: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Paper,
        contentColor = Ink,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Text(
                text = stringResource(R.string.endgame_mandates_total, winner.mandates),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun PlayerPanel(
    label: String,
    player: PlayerState,
    name: String,
    accent: Color,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Paper,
        contentColor = Ink,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(modifier = Modifier.size(14.dp), shape = CircleShape, color = accent, content = {})
            Column(modifier = Modifier.weight(1f)) {
                Text(label, color = Color(0xFF5A5378), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            }
            Text(
                text = stringResource(R.string.endgame_mandates_short, player.mandates),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun MatchStatsPanel(stats: MatchStats) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF302743),
        contentColor = Paper,
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.endgame_match_stats), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip(stringResource(R.string.endgame_tweets_sent, stats.tweetsSent), Modifier.weight(1f))
                StatChip(stringResource(R.string.endgame_unfollows_dealt, stats.unfollowsDealt), Modifier.weight(1f))
            }
            StatChip(stringResource(R.string.endgame_turns_as_main_character, stats.turnsAsMainCharacter), Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun StatChip(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = Ink, contentColor = Paper) {
        Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun StandingsPanel(
    players: List<PlayerState>,
    winnerId: com.kingofxitter.engine.model.PlayerId?,
    displayName: (PlayerState) -> String,
    statsByPlayer: Map<com.kingofxitter.engine.model.PlayerId, MatchStats> = emptyMap(),
) {
    var previousMandates: Int? = null
    var rank = 0
    val rankedPlayers =
        players.mapIndexed { index, player ->
            if (player.mandates != previousMandates) rank = index + 1
            previousMandates = player.mandates
            player to rank
        }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF302743),
        contentColor = Paper,
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rankedPlayers.forEach { (player, playerRank) ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                Ink,
                                RoundedCornerShape(12.dp),
                            ).padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(stringResource(R.string.endgame_rank, playerRank), color = Gold, fontWeight = FontWeight.Black)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(displayName(player), color = Paper, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        if (player.isSuspended) {
                            Text(stringResource(R.string.endgame_suspended), color = Pink, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(stringResource(R.string.endgame_mandates_short, player.mandates), color = Gold, fontWeight = FontWeight.Black)
                        if (winnerId == player.id) {
                            Text(stringResource(R.string.endgame_winner_badge), color = Gold, style = MaterialTheme.typography.labelSmall)
                        }
                        statsByPlayer[player.id]?.let { stats ->
                            Text(
                                stringResource(R.string.endgame_tweet_count_short, stats.tweetsSent),
                                color = Muted,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun PlayerState.endGameName(): String = leaderId.value.replace('-', ' ').replaceFirstChar { it.uppercase() }

@Preview(showBackground = true, backgroundColor = 0xFF1B1426)
@Composable
private fun VictoryScreenPreview() {
    val winner = GameFixtures.victory.players.first()
    MaterialTheme {
        VictoryScreen(
            winner = winner,
            players = GameFixtures.victory.players,
            stats =
                GameFixtures.victory.matchStats.values
                    .firstOrNull()
                    ?: MatchStats(tweetsSent = 19, unfollowsDealt = 23, turnsAsMainCharacter = 4),
            onPlayAgain = {},
            onViewStandings = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1B1426)
@Composable
private fun DefeatScreenPreview() {
    val suspended = GameFixtures.defeat.players.first()
    val winner = GameFixtures.defeat.players.firstOrNull { it.id != suspended.id }
    MaterialTheme {
        DefeatScreen(suspended, winner, MatchStats(tweetsSent = 6, unfollowsDealt = 11), onBackToGame = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1B1426)
@Composable
private fun StandingsScreenPreview() {
    MaterialTheme {
        StandingsScreen(GameFixtures.standings.players, GameFixtures.standings.matchStats, onBackToGame = {})
    }
}
