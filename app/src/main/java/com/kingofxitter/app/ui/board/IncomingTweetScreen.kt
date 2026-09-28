package com.kingofxitter.app.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingofxitter.app.R
import com.kingofxitter.app.ui.components.DashedFooter
import com.kingofxitter.app.ui.components.Die
import com.kingofxitter.app.ui.components.DieSize
import com.kingofxitter.app.ui.components.FollowersBar
import com.kingofxitter.app.ui.components.OutlinedText
import com.kingofxitter.app.ui.components.TweetAuthor
import com.kingofxitter.app.ui.components.XAvatar
import com.kingofxitter.app.ui.components.XButton
import com.kingofxitter.app.ui.components.XButtonStyle
import com.kingofxitter.app.ui.components.XCard
import com.kingofxitter.app.ui.components.XChip
import com.kingofxitter.app.ui.components.XIcon
import com.kingofxitter.app.ui.components.XIconImage
import com.kingofxitter.app.ui.components.XPanel
import com.kingofxitter.app.ui.components.XStamp
import com.kingofxitter.app.ui.components.XTag
import com.kingofxitter.app.ui.components.leaderVisual
import com.kingofxitter.app.ui.components.tweetText
import com.kingofxitter.app.ui.theme.XColors
import com.kingofxitter.app.ui.theme.XType
import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.DiceFace
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.PendingDecision
import com.kingofxitter.engine.model.PlayerId

/**
 * What the incoming screen needs to know about the hit. The engine's `ResourceChanged` events
 * are the authority on damage; [IncomingHit.estimate] falls back to the attacker's tweet dice.
 */
data class IncomingHit(
    val attackerId: PlayerId,
    val damage: Int,
) {
    companion object {
        /** Base-rule estimate: one unfollow per tweet die the attacker rolled. */
        fun estimate(state: GameState): IncomingHit = IncomingHit(state.currentPlayerId, state.dice.count { it.face == DiceFace.TWEET })
    }
}

/**
 * Incoming tweet: stay or log off (`design/screens/incoming-tweet-stay-or-log-off.html`). Shown
 * when the human, as Main Character, is hit and owns the STAY_OR_LOG_OFF [decision].
 * [tweet] is the attacker's tweet; V1's TweetWriter supplies it later.
 */
@Composable
fun IncomingTweetScreen(
    state: GameState,
    humanId: PlayerId,
    decision: PendingDecision,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier,
    hit: IncomingHit = IncomingHit.estimate(state),
    tweet: String? = null,
) {
    val you = state.players.first { it.id == humanId }
    val attacker = state.players.first { it.id == hit.attackerId }
    val attackerName = stringResource(leaderVisual(attacker.leaderId).shortName)
    val attackerNameCase = attackerName.lowercase().replaceFirstChar { it.titlecase() }
    val before = (you.followers + hit.damage).coerceAtMost(you.maxFollowers)
    val tweetLines = stringArrayResource(R.array.incoming_placeholder_tweets)
    val tweetText =
        tweet ?: tweetLines[Math.floorMod(state.turnNumber, tweetLines.size)].format(stringResource(leaderVisual(you.leaderId).handle))
    val stay = decision.options.firstOrNull { it.action is Action.StayInXitter }
    val logOff = decision.options.firstOrNull { it.action is Action.LogOffXitter }

    Column(modifier.fillMaxSize()) {
        // The alarm bar across the top (`.a-flash`); U8 makes it flash.
        Box(Modifier.fillMaxWidth().height(6.dp).background(XColors.SendIt))
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                XChip(
                    stringResource(R.string.board_turn, state.turnNumber),
                    textStyle = XType.DisplaySmall.copy(fontSize = 15.sp),
                    minHeight = 34.dp,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    XAvatar(attacker.leaderId, size = 30.dp)
                    Text(
                        stringResource(R.string.incoming_on_a_roll, attackerNameCase),
                        style = XType.Body.copy(fontSize = 13.sp),
                        color = XColors.Paper,
                    )
                }
            }
            OutlinedText(
                stringResource(R.string.incoming_title),
                XType.MemeHero,
                XColors.Followers,
                modifier = Modifier.fillMaxWidth().semantics { heading() },
                outline = 6.dp,
                dropShadow = 6.dp,
                textAlign = TextAlign.Center,
            )
            XCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TweetAuthor(attacker, avatarSize = 46.dp, time = stringResource(R.string.incoming_time, 3, (state.turnNumber * 7) % 60))
                    Text(tweetText(tweetText), style = XType.TweetLarge, color = XColors.Ink)
                    Engagement(state.turnNumber)
                    DashedFooter {
                        val tweetDice = state.dice.count { it.face == DiceFace.TWEET }
                        repeat(tweetDice.coerceAtMost(MAX_FOOTER_DICE)) { Die(DiceFace.TWEET, size = DieSize.ExtraSmall) }
                        // Damage beyond the dice came from the attacker's power-ups.
                        if (hit.damage > tweetDice) {
                            Text(stringResource(R.string.incoming_plus_power_ups), style = XType.Caption, color = XColors.Ink)
                        }
                        Box(Modifier.weight(1f))
                        XTag(pluralStringResource(R.plurals.incoming_unfollows, hit.damage, hit.damage), fill = XColors.DamageTint)
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().height(206.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(Modifier.width(180.dp).fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    // Stand-in for your leader's bust until S5 supplies portraits.
                    XAvatar(you.leaderId, size = 150.dp, modifier = Modifier.padding(bottom = 12.dp))
                    XStamp(stringResource(R.string.incoming_ratioed), Modifier.align(Alignment.BottomStart).padding(bottom = 28.dp))
                    OutlinedText(
                        stringResource(R.string.compose_damage, hit.damage),
                        XType.DisplayLarge.copy(fontSize = 30.sp),
                        XColors.Followers,
                        modifier = Modifier.align(Alignment.TopEnd).padding(top = 24.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.incoming_your_followers),
                        style = XType.DisplayMedium.copy(fontSize = 18.sp),
                        color = XColors.Paper,
                    )
                    FollowersBar(you.followers, you.maxFollowers, pipSize = 15.dp)
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.board_followers_short, before),
                            style = XType.DisplaySmall.copy(textDecoration = TextDecoration.LineThrough),
                            color = XColors.Muted,
                        )
                        Text(
                            stringResource(R.string.board_followers_short, you.followers),
                            style = XType.DisplayLarge.copy(fontSize = 30.sp),
                            color = XColors.Followers,
                        )
                    }
                }
            }
        }
        XPanel(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp), accent = XColors.Mandates) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    XIconImage(XIcon.Crown, contentDescription = null, size = 26.dp)
                    Text(
                        stringResource(R.string.incoming_you_are_main_character),
                        style = XType.DisplayMedium.copy(fontSize = 21.sp),
                        color = XColors.Paper,
                        modifier = Modifier.padding(top = 3.dp).semantics { heading() },
                    )
                }
                Text(stringResource(R.string.incoming_choice, attackerNameCase), style = XType.Body, color = XColors.Muted)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    XButton(
                        stringResource(R.string.incoming_stay),
                        onClick = { stay?.let { onAction(Action.ChooseDecisionOption(decision.playerId, decision.id, it.id)) } },
                        modifier = Modifier.weight(1f),
                        enabled = stay != null,
                        subtitle = stringResource(R.string.incoming_stay_subtitle),
                        minHeight = 72.dp,
                        textStyle = XType.DisplayButton.copy(fontSize = 22.sp),
                    )
                    XButton(
                        stringResource(R.string.incoming_log_off),
                        onClick = { logOff?.let { onAction(Action.ChooseDecisionOption(decision.playerId, decision.id, it.id)) } },
                        modifier = Modifier.weight(1f),
                        style = XButtonStyle.Secondary,
                        enabled = logOff != null,
                        icon = XIcon.LogOff,
                        subtitle = stringResource(R.string.incoming_log_off_subtitle, attackerNameCase),
                        minHeight = 72.dp,
                        textStyle = XType.DisplayButton.copy(fontSize = 22.sp),
                    )
                }
            }
        }
    }
}

/** Reply, retweet, like and view counts. Placeholder numbers until V2's fake engagement lands. */
@Composable
private fun Engagement(seed: Int) {
    val counts = fakeEngagement(seed)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf(XIcon.Reply, XIcon.Retweet, XIcon.LikeOutline, XIcon.Views).zip(counts).forEachIndexed { index, (icon, count) ->
            val color = if (index == 2) XColors.SendIt else XColors.InkMuted
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                XIconImage(icon, contentDescription = null, size = 16.dp, tint = color)
                Text(count, style = XType.Caption.copy(fontSize = 12.5.sp), color = color)
            }
        }
    }
}

/** Deterministic, plausibly viral counts ("12.4K", "9.9M") for a seed. */
internal fun fakeEngagement(seed: Int): List<String> {
    val base = Math.floorMod(seed * 7919, 900) + 100
    return listOf(base * 124L, base * 481L, base * 2_110L, base * 99_000L).map(::compactCount)
}

/** 12400 → "12.4K", 9900000 → "9.9M". */
internal fun compactCount(value: Long): String =
    when {
        value >= 1_000_000 -> "%.1fM".format(java.util.Locale.ROOT, value / 1_000_000.0)
        value >= 1_000 -> "%.1fK".format(java.util.Locale.ROOT, value / 1_000.0)
        else -> value.toString()
    }

private const val MAX_FOOTER_DICE = 6
