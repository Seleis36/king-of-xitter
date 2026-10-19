@file:Suppress("ktlint:standard:function-naming")

package com.kingofxitter.app.ui.endgame

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingofxitter.app.R
import com.kingofxitter.app.content.VoiceEntry
import com.kingofxitter.app.ui.components.CertificationBadge
import com.kingofxitter.app.ui.components.MANDATES_TO_WIN
import com.kingofxitter.app.ui.components.MandateCounter
import com.kingofxitter.app.ui.components.OutlinedText
import com.kingofxitter.app.ui.components.StatValue
import com.kingofxitter.app.ui.components.TweetAuthor
import com.kingofxitter.app.ui.components.XAvatar
import com.kingofxitter.app.ui.components.XButton
import com.kingofxitter.app.ui.components.XButtonStyle
import com.kingofxitter.app.ui.components.XCard
import com.kingofxitter.app.ui.components.XChip
import com.kingofxitter.app.ui.components.XIcon
import com.kingofxitter.app.ui.components.XIconButton
import com.kingofxitter.app.ui.components.XIconImage
import com.kingofxitter.app.ui.components.XPanel
import com.kingofxitter.app.ui.components.XStamp
import com.kingofxitter.app.ui.components.XTag
import com.kingofxitter.app.ui.components.leaderVisual
import com.kingofxitter.app.ui.components.mandateFraction
import com.kingofxitter.app.ui.components.tweetText
import com.kingofxitter.app.ui.theme.XColors
import com.kingofxitter.app.ui.theme.XType
import com.kingofxitter.app.ui.theme.XitterTheme
import com.kingofxitter.app.ui.theme.nightBackground
import com.kingofxitter.app.ui.theme.sticker
import com.kingofxitter.engine.GameFixtures
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.MatchResult
import com.kingofxitter.engine.model.MatchStats
import com.kingofxitter.engine.model.PlayerId
import com.kingofxitter.engine.model.PlayerState
import com.kingofxitter.engine.model.VictoryReason
import kotlin.math.hypot

/**
 * Victory (`design/screens/victory.html`): the winning title, your leader crowned on a sunburst, a
 * sore loser's tweet, the final standings and your match stats. [reaction] is that tweet (V1's
 * LOST line); without it the card is left out.
 */
@Composable
fun VictoryScreen(
    state: GameState,
    onPlayAgain: () -> Unit,
    onShare: () -> Unit,
    onMainMenu: () -> Unit,
    modifier: Modifier = Modifier,
    reaction: VoiceEntry? = null,
) {
    val result = state.result as? MatchResult.Victory ?: return
    val winner = state.players.firstOrNull { it.id == result.playerId } ?: return
    val byMandates = result.reason == VictoryReason.TWENTY_MANDATES
    EndGameScaffold(
        modifier = modifier,
        background = { Sunburst(Modifier.fillMaxSize()) },
        actions = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                XButton(stringResource(R.string.endgame_play_again), onClick = onPlayAgain, modifier = Modifier.weight(1f))
                XIconButton(XIcon.Share, stringResource(R.string.endgame_share), onShare, Modifier.align(Alignment.CenterVertically))
            }
            XButton(
                stringResource(R.string.endgame_main_menu),
                onClick = onMainMenu,
                modifier = Modifier.fillMaxWidth(),
                style = XButtonStyle.Ghost,
                minHeight = 48.dp,
                textStyle = XType.DisplaySmall,
            )
        },
    ) {
        Title(
            stringResource(if (byMandates) R.string.endgame_victory_title else R.string.endgame_survivor_title),
            color = XColors.Mandates,
        )
        OutlinedText(
            stringResource(if (byMandates) R.string.endgame_victory_subtitle else R.string.endgame_survivor_subtitle).uppercase(),
            XType.DisplayMedium,
            XColors.Paper,
            outline = 3.dp,
            textAlign = TextAlign.Center,
        )
        Box(Modifier.padding(top = 18.dp), contentAlignment = Alignment.TopCenter) {
            XAvatar(winner.leaderId, size = 150.dp)
            XIconImage(XIcon.Crown, contentDescription = null, size = 52.dp, modifier = Modifier.offset(y = (-34).dp).rotate(-8f))
            CertificationBadge(winner.mandates, size = 34.dp, modifier = Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = 10.dp))
        }
        reaction?.let { entry ->
            state.players.firstOrNull { it.id == entry.playerId }?.let { author -> EndTweet(author, entry.text, label = null) }
        }
        CompactStandings(state, you = winner.id)
        StatChips(state.matchStats[winner.id] ?: MatchStats())
    }
}

/**
 * Defeat (`design/screens/defeat-account-suspended.html`). Covers suspension, a rival reaching 20
 * first, everybody going down together, and your suspension while the bots play on. [finalTweet]
 * is the tweet that suspended you (or, lost on score, the winner's victory tweet).
 */
@Composable
fun DefeatScreen(
    state: GameState,
    playerId: PlayerId,
    onPlayAgain: () -> Unit,
    onMainMenu: () -> Unit,
    modifier: Modifier = Modifier,
    finalTweet: VoiceEntry? = null,
) {
    val you = state.players.firstOrNull { it.id == playerId } ?: return
    val winner = state.players.firstOrNull { it.id == (state.result as? MatchResult.Victory)?.playerId }
    EndGameScaffold(
        modifier = modifier,
        actions = {
            XButton(stringResource(R.string.endgame_appeal), onClick = onPlayAgain, modifier = Modifier.fillMaxWidth())
            XButton(
                stringResource(R.string.endgame_main_menu),
                onClick = onMainMenu,
                modifier = Modifier.fillMaxWidth(),
                style = XButtonStyle.Ghost,
                minHeight = 48.dp,
                textStyle = XType.DisplaySmall,
            )
        },
    ) {
        Title(
            stringResource(if (you.isSuspended) R.string.endgame_defeat_title else R.string.endgame_score_defeat_title),
            color = XColors.Paper,
        )
        Text(
            when {
                you.isSuspended -> stringResource(R.string.endgame_suspended_subtitle, state.turnNumber)
                winner != null -> stringResource(R.string.endgame_outposted_subtitle, leaderName(winner), state.turnNumber)
                else -> stringResource(R.string.endgame_defeat_subtitle)
            },
            style = XType.Body,
            color = XColors.Muted,
            textAlign = TextAlign.Center,
        )
        Box(Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
            XAvatar(you.leaderId, size = 150.dp, modifier = if (you.isSuspended) Modifier.grayscale() else Modifier)
            XStamp(stringResource(if (you.isSuspended) R.string.endgame_suspended else R.string.endgame_stamp_outposted), rotation = -14f)
        }
        finalTweet?.let { entry ->
            state.players.firstOrNull { it.id == entry.playerId }?.let { author ->
                EndTweet(
                    author,
                    entry.text,
                    label = stringResource(if (you.isSuspended) R.string.endgame_last_tweet else R.string.endgame_winning_tweet),
                )
            }
        }
        when {
            winner != null -> WinnerPanel(winner)
            state.result == MatchResult.NoWinner -> Note(stringResource(R.string.endgame_no_winner))
            state.result == null -> Note(stringResource(R.string.endgame_match_continues))
        }
        StatChips(state.matchStats[playerId] ?: MatchStats())
    }
}

/**
 * Standings (`design/screens/standings.html`), for a running match and its final result: the
 * certification thresholds, then each leader ranked with their followers, characters, mandate bar
 * and what their position means. [humanId] marks your card.
 */
@Composable
fun StandingsScreen(
    state: GameState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    humanId: PlayerId? = null,
) {
    val live = state.result == null
    val backLabel = stringResource(if (live) R.string.endgame_back_to_game else R.string.endgame_main_menu)
    EndGameScaffold(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        actions = { XButton(backLabel, onClick = onBack, modifier = Modifier.fillMaxWidth()) },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            XIconButton(XIcon.Back, backLabel, onBack)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(if (live) R.string.endgame_standings_live_title else R.string.endgame_standings_final_title),
                    style = XType.DisplayLarge,
                    color = XColors.Paper,
                    modifier = Modifier.semantics { heading() },
                )
                Text(stringResource(R.string.endgame_standings_rules, state.turnNumber), style = XType.Caption, color = XColors.Muted)
            }
        }
        CertificationLegend()
        if (state.players.isEmpty()) Note(stringResource(R.string.endgame_empty_standings))
        standings(state).forEach { standing -> StandingCard(state, standing, isYou = standing.player.id == humanId) }
    }
}

/** Night background with an optional layer behind, scrolling content, and actions pinned below. */
@Composable
private fun EndGameScaffold(
    modifier: Modifier,
    actions: @Composable ColumnScope.() -> Unit,
    background: @Composable BoxScope.() -> Unit = {},
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier.fillMaxSize()) {
        background()
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 20.dp, bottom = 12.dp),
                horizontalAlignment = horizontalAlignment,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = content,
            )
            Column(Modifier.padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = actions)
        }
    }
}

@Composable
private fun Title(
    text: String,
    color: Color,
) {
    OutlinedText(
        text,
        XType.DisplayHero.copy(fontSize = 46.sp, lineHeight = 48.sp),
        color,
        modifier = Modifier.fillMaxWidth().semantics { heading() },
        outline = 6.dp,
        dropShadow = 4.dp,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun Note(text: String) {
    Text(text, style = XType.Body, color = XColors.Muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
}

/** A tweet on a paper card, tilted like the design's, with an optional red label above it. */
@Composable
private fun EndTweet(
    author: PlayerState,
    text: String,
    label: String?,
) {
    XCard(Modifier.fillMaxWidth().rotate(-1.5f)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (label != null) Text(label, style = XType.Tag, color = XColors.SendIt)
            TweetAuthor(author, avatarSize = 38.dp)
            Text(tweetText(text), style = XType.Tweet, color = XColors.Ink)
        }
    }
}

/** The winner's strip on the Defeat screen. */
@Composable
private fun WinnerPanel(winner: PlayerState) {
    XPanel(Modifier.fillMaxWidth(), accent = XColors.Mandates) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            XAvatar(winner.leaderId, size = 52.dp)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.endgame_winner_badge), style = XType.Tag, color = XColors.Mandates)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(leaderName(winner), style = XType.DisplayMedium, color = XColors.Paper)
                    CertificationBadge(winner.mandates, size = 20.dp)
                }
            }
            MandateCounter(winner.mandates)
        }
    }
}

/** The Victory screen's ranked list: rank, leader, badge, and mandates. */
@Composable
private fun CompactStandings(
    state: GameState,
    you: PlayerId,
) {
    XPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            standings(state).forEach { (rank, player) ->
                Row(
                    Modifier.alpha(if (player.isSuspended) SUSPENDED_ALPHA else 1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(rank.toString(), style = XType.DisplayMedium, color = XColors.Paper)
                    XAvatar(player.leaderId, size = 34.dp)
                    Text(
                        if (player.id == you) stringResource(R.string.endgame_you_name, leaderName(player)) else leaderName(player),
                        style = XType.DisplaySmall,
                        color = XColors.Paper,
                        maxLines = 1,
                    )
                    CertificationBadge(player.mandates, size = 16.dp)
                    if (player.isSuspended) XTag(stringResource(R.string.endgame_suspended), fill = XColors.Followers)
                    Box(Modifier.weight(1f))
                    MandateCounter(player.mandates, iconSize = 20.dp, valueStyle = XType.DisplayMedium)
                }
            }
        }
    }
}

/** Match stats as the design's white pills. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatChips(stats: MatchStats) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        XChip(pluralStringResource(R.plurals.endgame_tweets_sent, stats.tweetsSent, stats.tweetsSent))
        XChip(stringResource(R.string.endgame_unfollows_dealt, stats.unfollowsDealt))
        XChip(pluralStringResource(R.plurals.endgame_turns_as_main_character, stats.turnsAsMainCharacter, stats.turnsAsMainCharacter))
    }
}

/** The three certification bands and where they start. */
@Composable
private fun CertificationLegend() {
    XPanel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.endgame_certification), style = XType.DisplaySmall, color = XColors.Paper)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(XColors.GhostBorder to 6f, XColors.BlueCheck to 9f, XColors.GoldCheck to 5f).forEach { (color, weight) ->
                    Box(Modifier.weight(weight).height(10.dp).bar(color))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                LegendEntry(0, R.string.endgame_band_unverified)
                LegendEntry(6, R.string.endgame_band_blue)
                LegendEntry(15, R.string.endgame_band_gold)
            }
        }
    }
}

@Composable
private fun LegendEntry(
    mandates: Int,
    label: Int,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        CertificationBadge(mandates, size = 14.dp)
        Text(stringResource(label), style = XType.Caption, color = XColors.Muted)
    }
}

/** One leader on the Standings screen. Your card is on paper, as in the design. */
@Composable
private fun StandingCard(
    state: GameState,
    standing: Standing,
    isYou: Boolean,
) {
    val player = standing.player
    val ink = if (isYou) XColors.Ink else XColors.Paper
    val muted = if (isYou) XColors.InkMuted else XColors.Muted
    val name = leaderName(player)
    XPanel(
        Modifier.fillMaxWidth().alpha(if (player.isSuspended) SUSPENDED_ALPHA else 1f),
        fill = if (isYou) XColors.Paper else XColors.Panel,
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RankBadge(standing.rank)
                XAvatar(player.leaderId, size = 46.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            if (isYou) stringResource(R.string.endgame_you_name, name) else name,
                            style = XType.DisplaySmall,
                            color = ink,
                            maxLines = 1,
                        )
                        CertificationBadge(player.mandates, size = 18.dp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        StatValue(
                            XIcon.Like,
                            stringResource(R.string.endgame_followers_short, player.followers),
                            pluralStringResource(R.plurals.endgame_followers_spoken, player.followers, player.followers),
                            color = ink,
                        )
                        StatValue(
                            XIcon.Characters,
                            player.characters.toString(),
                            pluralStringResource(R.plurals.endgame_characters_spoken, player.characters, player.characters),
                            color = ink,
                        )
                    }
                }
                MandateCounter(player.mandates, iconSize = 26.dp, color = ink)
            }
            MandateBar(player.mandates)
            Text(standingNote(state, player), style = XType.Caption, color = muted)
        }
    }
}

/** What a leader's position means, in the design's words. */
@Composable
private fun standingNote(
    state: GameState,
    player: PlayerState,
): String {
    val toWin = (MANDATES_TO_WIN - player.mandates).coerceAtLeast(0)
    return when {
        player.isSuspended -> stringResource(R.string.endgame_suspended_note)
        (state.result as? MatchResult.Victory)?.playerId == player.id -> stringResource(R.string.endgame_winner_note)
        player.id == state.xitterPlayerId && state.result == null -> stringResource(R.string.endgame_on_xitter)
        player.followers <= DANGER_FOLLOWERS -> pluralStringResource(R.plurals.endgame_danger_note, player.followers, player.followers)
        player.mandates >= GOLD -> pluralStringResource(R.plurals.endgame_gold_note, toWin, toWin)
        player.mandates >= BLUE -> pluralStringResource(R.plurals.endgame_blue_note, GOLD - player.mandates, GOLD - player.mandates)
        else -> pluralStringResource(R.plurals.endgame_unverified_note, BLUE - player.mandates, BLUE - player.mandates)
    }
}

@Composable
private fun RankBadge(rank: Int) {
    Box(Modifier.size(34.dp).bar(if (rank == 1) XColors.Mandates else XColors.Paper, corner = 9.dp), contentAlignment = Alignment.Center) {
        Text(rank.toString(), style = XType.DisplayMedium, color = XColors.Ink, modifier = Modifier.padding(top = 3.dp))
    }
}

/** The mandate bar, coloured by certification, with markers at the blue and gold checks. */
@Composable
private fun MandateBar(mandates: Int) {
    val fill =
        when {
            mandates >= GOLD -> XColors.GoldCheck
            mandates >= BLUE -> XColors.BlueCheck
            else -> XColors.GhostBorder
        }
    Box(
        Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(CircleShape)
            .background(XColors.PanelShadow)
            .border(2.dp, XColors.Ink, CircleShape),
    ) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(mandateFraction(mandates)).background(fill))
        listOf(BLUE, GOLD).forEach { mark ->
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(mandateFraction(mark)))
                Box(Modifier.fillMaxHeight().width(2.dp).background(XColors.Ink))
            }
        }
    }
}

/** Purple rays behind the Victory screen, from the design's conic gradient. */
@Composable
private fun Sunburst(modifier: Modifier = Modifier) {
    Canvas(modifier.nightBackground()) {
        val center = Offset(size.width / 2, size.height * 0.28f)
        val radius = hypot(size.width, size.height)
        for (ray in 0 until RAYS step 2) {
            drawArc(
                color = XColors.NightDot,
                startAngle = ray * (360f / RAYS),
                sweepAngle = 360f / RAYS,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
            )
        }
    }
}

/** Draws the content in greys, for a suspended account. */
private fun Modifier.grayscale(): Modifier =
    drawWithCache {
        val paint = Paint().apply { colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) }
        onDrawWithContent {
            drawIntoCanvas { canvas ->
                canvas.saveLayer(Rect(Offset.Zero, size), paint)
                drawContent()
                canvas.restore()
            }
        }
    }

/** A rounded, ink-edged fill with no shadow: the kit's bars and badges. */
private fun Modifier.bar(
    color: Color,
    corner: Dp = 6.dp,
): Modifier = sticker(fill = color, shape = RoundedCornerShape(corner), borderWidth = 2.dp, shadowOffset = DpOffset.Zero)

@Composable
private fun leaderName(player: PlayerState): String = stringResource(leaderVisual(player.leaderId).shortName)

private const val SUSPENDED_ALPHA = 0.55f
private const val DANGER_FOLLOWERS = 2
private const val BLUE = 6
private const val GOLD = 15
private const val RAYS = 24

@Preview(name = "Victory", widthDp = 412, heightDp = 915)
@Composable
private fun VictoryPreview() = XitterTheme { VictoryScreen(GameFixtures.victory, {}, {}, {}, Modifier.nightBackground()) }

@Preview(name = "Last survivor", widthDp = 320, heightDp = 640, fontScale = 1.5f)
@Composable
private fun SurvivorPreview() = XitterTheme { VictoryScreen(GameFixtures.survivorVictory, {}, {}, {}, Modifier.nightBackground()) }

@Preview(name = "Suspended", widthDp = 412, heightDp = 915)
@Composable
private fun DefeatPreview() =
    XitterTheme {
        DefeatScreen(
            GameFixtures.defeat,
            GameFixtures.defeat.players
                .first()
                .id,
            {},
            {},
            Modifier.nightBackground(),
        )
    }

@Preview(name = "No winner", widthDp = 320, heightDp = 640)
@Composable
private fun NoWinnerPreview() =
    XitterTheme {
        DefeatScreen(
            GameFixtures.noWinner,
            GameFixtures.noWinner.players
                .first()
                .id,
            {},
            {},
            Modifier.nightBackground(),
        )
    }

@Preview(name = "Lost to score", widthDp = 412, heightDp = 915)
@Composable
private fun ScoreDefeatPreview() =
    XitterTheme {
        DefeatScreen(
            GameFixtures.scoreDefeat,
            GameFixtures.scoreDefeat.players
                .first()
                .id,
            {},
            {},
            Modifier.nightBackground(),
        )
    }

@Preview(name = "Live standings", widthDp = 412, heightDp = 915)
@Composable
private fun StandingsPreview() =
    XitterTheme {
        StandingsScreen(GameFixtures.liveStandings, {}, Modifier.nightBackground(), GameFixtures.liveStandings.currentPlayerId)
    }

@Preview(name = "Tied standings", widthDp = 320, heightDp = 640, fontScale = 1.5f)
@Composable
private fun TiedPreview() =
    XitterTheme {
        StandingsScreen(
            GameFixtures.liveStandings.copy(players = GameFixtures.liveStandings.players.map { it.copy(mandates = 8) }),
            {},
            Modifier.nightBackground(),
        )
    }

@Preview(name = "Empty standings")
@Composable
private fun EmptyPreview() =
    XitterTheme {
        StandingsScreen(GameFixtures.liveStandings.copy(players = emptyList()), {}, Modifier.nightBackground())
    }

@Preview(name = "Final standings", widthDp = 412, heightDp = 915)
@Composable
private fun FinalStandingsPreview() = XitterTheme { StandingsScreen(GameFixtures.standings, {}, Modifier.nightBackground()) }

@Preview(name = "Design: victory.html", widthDp = 412, heightDp = 915)
@Composable
private fun VictoryDesignPreview() = XitterTheme { VictoryScreen(GameFixtures.Design.victory, {}, {}, {}, Modifier.nightBackground()) }

@Preview(name = "Design: defeat-account-suspended.html", widthDp = 412, heightDp = 915)
@Composable
private fun DefeatDesignPreview() =
    XitterTheme {
        val state = GameFixtures.Design.defeat
        DefeatScreen(state, state.players.first().id, {}, {}, Modifier.nightBackground())
    }

@Preview(name = "Design: standings.html", widthDp = 412, heightDp = 915)
@Composable
private fun StandingsDesignPreview() =
    XitterTheme {
        val state = GameFixtures.Design.standings
        StandingsScreen(state, {}, Modifier.nightBackground(), state.players.first().id)
    }
