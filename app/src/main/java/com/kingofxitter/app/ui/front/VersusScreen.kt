package com.kingofxitter.app.ui.front

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kingofxitter.app.R
import com.kingofxitter.app.content.rememberTweetTexts
import com.kingofxitter.app.ui.components.Die
import com.kingofxitter.app.ui.components.DieSize
import com.kingofxitter.app.ui.components.OutlinedText
import com.kingofxitter.app.ui.components.XAvatar
import com.kingofxitter.app.ui.components.XButton
import com.kingofxitter.app.ui.components.XButtonStyle
import com.kingofxitter.app.ui.components.XCard
import com.kingofxitter.app.ui.components.XChip
import com.kingofxitter.app.ui.components.XIcon
import com.kingofxitter.app.ui.components.XPanel
import com.kingofxitter.app.ui.components.XTag
import com.kingofxitter.app.ui.components.leaderVisual
import com.kingofxitter.app.ui.motion.LocalMotion
import com.kingofxitter.app.ui.motion.popOn
import com.kingofxitter.app.ui.motion.pulse
import com.kingofxitter.app.ui.theme.XColors
import com.kingofxitter.app.ui.theme.XType
import com.kingofxitter.app.ui.theme.XitterTheme
import com.kingofxitter.app.ui.theme.nightBackground
import com.kingofxitter.content.TweetEvent
import com.kingofxitter.content.TweetMoment
import com.kingofxitter.engine.RollOffRound
import com.kingofxitter.engine.SplitMix64
import com.kingofxitter.engine.model.DiceFace
import com.kingofxitter.engine.model.PlayerId
import com.kingofxitter.engine.model.PlayerState
import kotlinx.coroutines.delay

/** What the Versus screen shows, so it can be previewed without a live match. */
data class VersusLineUp(
    val humanPlayerId: PlayerId,
    val firstPlayerId: PlayerId,
    val players: List<PlayerState>,
    val rollOff: List<RollOffRound>,
    val matchNumber: Int,
)

/**
 * Versus intro (`design/screens/versus-intro.html`): the four leaders with an opening tweet each,
 * then the roll-off that decided who tweets first, revealed a round at a time.
 *
 * The roll-off is the engine's own (`MatchSetup`), so what the screen shows is what the match was
 * actually set up with. With motion off, every round is shown at once.
 */
@Composable
fun VersusScreen(
    lineUp: VersusLineUp,
    onBegin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val texts = rememberTweetTexts()
    val motion = LocalMotion.current
    var revealed by remember(lineUp) { mutableIntStateOf(if (motion.enabled) 0 else lineUp.rollOff.size) }
    LaunchedEffect(lineUp, motion.enabled) {
        // One round at a time, so a tie-break reads as a tie-break rather than a wall of dice.
        while (revealed < lineUp.rollOff.size) {
            delay(motion.duration(ROUND_MILLIS).toLong())
            revealed++
        }
    }
    val first = lineUp.players.firstOrNull { it.id == lineUp.firstPlayerId }
    val firstName = first?.let { stringResource(leaderVisual(it.leaderId).shortName) }

    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                XChip(stringResource(R.string.versus_match_number, lineUp.matchNumber), minHeight = 32.dp)
                Text(stringResource(R.string.versus_vs), style = XType.DisplayMedium, color = XColors.Mandates)
            }
            OutlinedText(
                stringResource(R.string.versus_headline),
                XType.DisplayLarge,
                XColors.Paper,
                modifier = Modifier.fillMaxWidth().semantics { heading() },
                textAlign = TextAlign.Center,
            )
            lineUp.players.forEachIndexed { index, player ->
                // A stable per-player stream, so reopening the screen shows the same opening tweet.
                val line =
                    texts.write(
                        TweetEvent(TweetMoment.ROLLED),
                        player.leaderId,
                        SplitMix64(lineUp.matchNumber.toLong() * LINE_MIX + index),
                    )
                LeaderIntro(player, isYou = player.id == lineUp.humanPlayerId, line = line)
            }
            RollOff(lineUp, revealed)
            if (revealed >= lineUp.rollOff.size && firstName != null) {
                OutlinedText(
                    stringResource(R.string.versus_starts, firstName),
                    XType.DisplayMedium,
                    XColors.Mandates,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .popOn(revealed)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    textAlign = TextAlign.Center,
                )
            }
        }
        XButton(
            stringResource(if (revealed >= lineUp.rollOff.size) R.string.versus_begin else R.string.versus_skip),
            onClick = onBegin,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).pulse(),
            style = XButtonStyle.Hot,
            icon = XIcon.Tweet,
            minHeight = 62.dp,
        )
    }
}

@Composable
private fun LeaderIntro(
    player: PlayerState,
    isYou: Boolean,
    line: String,
) {
    val visual = leaderVisual(player.leaderId)
    XPanel(Modifier.fillMaxWidth(), accent = visual.color) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            XAvatar(player.leaderId, size = 52.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(visual.shortName), style = XType.DisplaySmall, color = XColors.Paper)
                    XTag(
                        stringResource(if (isYou) R.string.versus_you else R.string.versus_bot),
                        fill = if (isYou) visual.color else XColors.Disabled,
                    )
                }
                if (line.isNotEmpty()) Text(line, style = XType.Body, color = XColors.Muted)
            }
        }
    }
}

/** The roll-off rounds, one card each, newest last; only [revealed] of them are shown. */
@Composable
private fun RollOff(
    lineUp: VersusLineUp,
    revealed: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.versus_roll_off),
                style = XType.Label,
                color = XColors.Mandates,
                modifier = Modifier.semantics { heading() },
            )
            Text(stringResource(R.string.versus_roll_off_rule), style = XType.Caption, color = XColors.Muted)
        }
        lineUp.rollOff.take(revealed).forEachIndexed { round, rolls ->
            XCard(Modifier.fillMaxWidth().popOn(round)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (lineUp.rollOff.size > 1) {
                        Text(stringResource(R.string.versus_round, round + 1), style = XType.Label, color = XColors.InkMuted)
                    }
                    rolls.rolls.forEach { (playerId, dice) ->
                        RollRow(lineUp, playerId, dice)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RollRow(
    lineUp: VersusLineUp,
    playerId: PlayerId,
    dice: List<DiceFace>,
) {
    val player = lineUp.players.firstOrNull { it.id == playerId } ?: return
    val tweets = dice.count { it == DiceFace.TWEET }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(leaderVisual(player.leaderId).shortName), style = XType.DisplayTiny, color = XColors.Ink)
            Box(Modifier.weight(1f))
            XTag(stringResource(R.string.versus_tweets, tweets), fill = if (tweets > 0) XColors.Tweet else XColors.Disabled)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            dice.forEach { face -> Die(face, size = DieSize.ExtraSmall) }
        }
    }
}

private const val ROUND_MILLIS = 900
private const val LINE_MIX = 0x5DEECE66L

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun VersusPreview() {
    XitterTheme { VersusScreen(VersusSamples.design, onBegin = {}, modifier = Modifier.nightBackground()) }
}

@Preview(widthDp = 320, heightDp = 640, fontScale = 1.3f)
@Composable
private fun VersusSmallPreview() {
    XitterTheme { VersusScreen(VersusSamples.tieBreak, onBegin = {}, modifier = Modifier.nightBackground()) }
}
