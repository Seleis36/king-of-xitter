package com.kingofxitter.app.ui.board

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingofxitter.app.R
import com.kingofxitter.app.ui.components.ComposingText
import com.kingofxitter.app.ui.components.DashedFooter
import com.kingofxitter.app.ui.components.Die
import com.kingofxitter.app.ui.components.DieSize
import com.kingofxitter.app.ui.components.OutlinedText
import com.kingofxitter.app.ui.components.TweetAuthor
import com.kingofxitter.app.ui.components.XAvatar
import com.kingofxitter.app.ui.components.XButton
import com.kingofxitter.app.ui.components.XButtonStyle
import com.kingofxitter.app.ui.components.XCard
import com.kingofxitter.app.ui.components.XChip
import com.kingofxitter.app.ui.components.XIcon
import com.kingofxitter.app.ui.components.XIconButton
import com.kingofxitter.app.ui.components.XOptionCard
import com.kingofxitter.app.ui.components.XTag
import com.kingofxitter.app.ui.components.leaderVisual
import com.kingofxitter.app.ui.components.tweetText
import com.kingofxitter.app.ui.theme.XColors
import com.kingofxitter.app.ui.theme.XType
import com.kingofxitter.app.ui.theme.sticker
import com.kingofxitter.engine.model.DiceFace
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.PlayerId
import com.kingofxitter.engine.model.PlayerState

/** How many zingers the screen offers at once. */
private const val ZINGER_CHOICES = 3

/** Zingers from `R.array.compose_placeholder_zingers` until V2 lands. */
@Composable
fun rememberPlaceholderZingers(): ZingerSource {
    val lines = stringArrayResource(R.array.compose_placeholder_zingers).toList()
    val everyone = stringResource(R.string.compose_everyone_handle)
    val context = LocalContext.current
    return remember(lines, everyone) {
        PlaceholderZingers(lines) { target ->
            if (target == null) everyone else context.getString(leaderVisual(target).handle)
        }
    }
}

/**
 * Compose your attack tweet (`design/screens/compose-your-attack-tweet.html`): who the tweets hit
 * and for how much, a tweet built from the chosen zinger, and SEND IT. The tweet is flavour;
 * [onSend] resolves the roll.
 */
@Composable
fun ComposeTweetScreen(
    state: GameState,
    humanId: PlayerId,
    onSend: (zinger: String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    zingers: ZingerSource = rememberPlaceholderZingers(),
) {
    val you = state.players.first { it.id == humanId }
    val preview = RollPreview.of(state, humanId)
    val targets = preview.targets
    val target = (targets as? TweetTargets.MainCharacter)?.let { t -> state.players.first { it.id == t.playerId } }
    var page by rememberSaveable { mutableIntStateOf(0) }
    var choice by rememberSaveable(page) { mutableIntStateOf(0) }
    val options =
        remember(zingers, page, you.leaderId, target?.leaderId) {
            zingers.zingers(you.leaderId, target?.leaderId, page, ZINGER_CHOICES)
        }
    val zinger = options.getOrElse(choice) { "" }

    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                XIconButton(XIcon.Close, stringResource(R.string.compose_cancel), onCancel)
                OutlinedText(
                    stringResource(R.string.compose_title),
                    XType.DisplayLarge,
                    XColors.Paper,
                    modifier = Modifier.semantics { heading() },
                )
                XChip(stringResource(R.string.compose_tweet_count, preview.tweets), icon = XIcon.Tweet, minHeight = 34.dp)
            }
            TargetSticker(preview, target)
            XCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TweetAuthor(you)
                    Text(
                        if (target != null) {
                            tweetText(stringResource(R.string.compose_replying_to, stringResource(leaderVisual(target.leaderId).handle)))
                        } else {
                            tweetText(stringResource(R.string.compose_to_everyone))
                        },
                        style = XType.Body.copy(fontSize = 12.5.sp),
                        color = XColors.InkMuted,
                    )
                    ComposingText(zinger, XType.TweetLarge.copy(fontSize = 18.sp), Modifier.heightIn(min = 100.dp))
                    DashedFooter {
                        Text(
                            stringResource(if (target != null) R.string.compose_rule_outside else R.string.compose_rule_inside),
                            style = XType.Caption,
                            color = XColors.InkMuted,
                            modifier = Modifier.weight(1f),
                        )
                        XTag(stringResource(R.string.compose_damage, preview.tweets), fill = XColors.DamageTint)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    stringResource(R.string.compose_pick_zinger),
                    style = XType.DisplayMedium.copy(fontSize = 18.sp),
                    color = XColors.Paper,
                    modifier = Modifier.semantics { heading() },
                )
                XChip(stringResource(R.string.compose_new_takes), icon = XIcon.Shuffle, minHeight = 32.dp, onClick = { page++ })
            }
            options.forEachIndexed { index, line ->
                XOptionCard(selected = index == choice, onSelect = { choice = index }, modifier = Modifier.fillMaxWidth()) {
                    Text(tweetText(line), style = XType.Body.copy(fontWeight = FontWeight.SemiBold), color = XColors.Ink)
                }
            }
        }
        XButton(
            stringResource(R.string.compose_send),
            onClick = { onSend(zinger) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            style = XButtonStyle.Hot,
            icon = XIcon.Send,
            minHeight = 64.dp,
            textStyle = XType.DisplayButton.copy(fontSize = 28.sp),
        )
    }
}

/** The orange sticker naming the target and the damage. */
@Composable
private fun TargetSticker(
    preview: RollPreview,
    target: PlayerState?,
) {
    val targetCount = (preview.targets as? TweetTargets.Everyone)?.playerIds?.size ?: 1
    Row(
        Modifier
            .padding(horizontal = 4.dp)
            .padding(top = 4.dp)
            .rotate(-1.5f)
            .sticker(fill = XColors.Tweet, shape = RoundedCornerShape(16.dp), halo = 4.dp, shadowOffset = DpOffset(4.dp, 7.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(preview.tweets.coerceAtMost(MAX_TARGET_DICE)) { Die(DiceFace.TWEET, size = DieSize.ExtraSmall) }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val name =
                target?.let { stringResource(leaderVisual(it.leaderId).shortName) }
                    ?: stringResource(R.string.board_preview_target_everyone)
            Text(stringResource(R.string.compose_target, name), style = XType.DisplaySmall.copy(fontSize = 17.sp), color = XColors.Ink)
            Text(
                if (target != null) {
                    stringResource(R.string.compose_target_main_character, preview.tweets)
                } else {
                    pluralStringResource(R.plurals.compose_target_everyone, targetCount, targetCount, preview.tweets)
                },
                style = XType.Caption,
                color = XColors.Ink,
            )
        }
        if (target != null) XAvatar(target.leaderId, size = 40.dp)
    }
}

private const val MAX_TARGET_DICE = 4
