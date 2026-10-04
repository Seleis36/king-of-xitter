package com.kingofxitter.app.ui.board

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingofxitter.app.R
import com.kingofxitter.app.content.engagementCounts
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
import com.kingofxitter.app.ui.motion.flash
import com.kingofxitter.app.ui.motion.rise
import com.kingofxitter.app.ui.motion.shakeOn
import com.kingofxitter.app.ui.motion.slamOn
import com.kingofxitter.app.ui.motion.stampOn
import com.kingofxitter.app.ui.theme.XColors
import com.kingofxitter.app.ui.theme.XType
import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.DecisionId
import com.kingofxitter.engine.model.DecisionOption
import com.kingofxitter.engine.model.DiceFace
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.LeaderId
import com.kingofxitter.engine.model.PendingDecision
import com.kingofxitter.engine.model.PlayerId
import com.kingofxitter.engine.model.PlayerState

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
@OptIn(ExperimentalLayoutApi::class)
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

    val choose: (DecisionOption?) -> Unit = { option ->
        option?.let { onAction(Action.ChooseDecisionOption(decision.playerId, decision.id, it.id)) }
    }
    BoxWithConstraints(modifier.fillMaxSize()) {
        // The choice stays pinned under the scrolling tweet when there's room. On small phones at
        // large font scales it would leave the tweet a sliver, so it scrolls with the rest instead.
        val pinned = maxHeight >= PIN_MIN_HEIGHT * LocalDensity.current.fontScale.coerceAtLeast(1f)
        val scroll = rememberScrollState()
        Column(Modifier.fillMaxSize()) {
            // The alarm bar across the top (`.a-flash`).
            Box(Modifier.fillMaxWidth().height(6.dp).flash(XColors.SendIt, XColors.AlarmFlash))
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(scroll)
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IncomingHeader(state, attacker.leaderId, attackerNameCase)
                OutlinedText(
                    stringResource(R.string.incoming_title),
                    XType.MemeHero,
                    XColors.Followers,
                    modifier = Modifier.fillMaxWidth().shakeOn(decision.id).semantics { heading() },
                    outline = 6.dp,
                    dropShadow = 6.dp,
                    textAlign = TextAlign.Center,
                )
                XCard(Modifier.fillMaxWidth().slamOn(decision.id)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        TweetAuthor(
                            attacker,
                            avatarSize = 46.dp,
                            time =
                                stringResource(
                                    R.string.incoming_time,
                                    3,
                                    (state.turnNumber * 7) % 60,
                                ),
                        )
                        Text(tweetText(tweetText), style = XType.TweetLarge, color = XColors.Ink)
                        Engagement(attacker.leaderId, state.turnNumber)
                        DashedFooter {
                            // The dice and the damage tag, which moves to its own line when space is short.
                            FlowRow(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                itemVerticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val tweetDice = state.dice.count { it.face == DiceFace.TWEET }
                                    repeat(tweetDice.coerceAtMost(MAX_FOOTER_DICE)) { Die(DiceFace.TWEET, size = DieSize.ExtraSmall) }
                                    // Damage beyond the dice came from the attacker's power-ups.
                                    if (hit.damage > tweetDice) {
                                        Text(stringResource(R.string.incoming_plus_power_ups), style = XType.Caption, color = XColors.Ink)
                                    }
                                }
                                XTag(pluralStringResource(R.plurals.incoming_unfollows, hit.damage, hit.damage), fill = XColors.DamageTint)
                            }
                        }
                    }
                }
                YourFollowers(you, before, hit.damage, decision.id)
                if (!pinned) DecisionPanel(attackerNameCase, stay, logOff, choose)
            }
            if (pinned) DecisionPanel(attackerNameCase, stay, logOff, choose, Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp))
        }
    }
}

@Composable
private fun IncomingHeader(
    state: GameState,
    attacker: LeaderId,
    attackerName: String,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        XChip(
            stringResource(R.string.board_turn, state.turnNumber),
            textStyle = XType.DisplaySmall.copy(fontSize = 15.sp),
            minHeight = 34.dp,
        )
        Row(
            Modifier.weight(1f, fill = false).padding(start = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            XAvatar(attacker, size = 30.dp)
            Text(
                stringResource(R.string.incoming_on_a_roll, attackerName),
                style = XType.Body.copy(fontSize = 13.sp),
                color = XColors.Paper,
            )
        }
    }
}

/** Your bust taking the hit, and your followers before and after. */
@Composable
private fun YourFollowers(
    you: PlayerState,
    before: Int,
    damage: Int,
    decisionId: DecisionId,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // The design's 180dp bust, narrower on small phones so the followers keep room.
        val bust = (maxWidth * BUST_SHARE).coerceAtMost(180.dp)
        Row(
            Modifier.fillMaxWidth().heightIn(min = bust * BUST_ASPECT),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.width(bust).height(bust * BUST_ASPECT), contentAlignment = Alignment.BottomCenter) {
                // Stand-in for your leader's bust until S5 supplies portraits.
                XAvatar(you.leaderId, size = bust * AVATAR_SHARE, modifier = Modifier.padding(bottom = 12.dp).shakeOn(decisionId))
                XStamp(
                    stringResource(R.string.incoming_ratioed),
                    Modifier.align(Alignment.BottomStart).padding(bottom = 28.dp).stampOn(decisionId),
                )
                OutlinedText(
                    stringResource(R.string.compose_damage, damage),
                    XType.DisplayLarge.copy(fontSize = 30.sp),
                    XColors.Followers,
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 24.dp)
                            .rise()
                            .clearAndSetSemantics { },
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.incoming_your_followers),
                    style = XType.DisplayMedium.copy(fontSize = 18.sp),
                    color = XColors.Paper,
                )
                FollowersBar(you.followers, you.maxFollowers, pipSize = 15.dp)
                val change = stringResource(R.string.incoming_followers_change_spoken, before, you.followers)
                Row(
                    Modifier.clearAndSetSemantics { contentDescription = change },
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
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
}

/** "You're the Main Character": STAY or LOG OFF, side by side or stacked when they'd be cramped. */
@Composable
private fun DecisionPanel(
    attackerName: String,
    stay: DecisionOption?,
    logOff: DecisionOption?,
    choose: (DecisionOption?) -> Unit,
    modifier: Modifier = Modifier,
) {
    XPanel(modifier.fillMaxWidth(), accent = XColors.Mandates) {
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
            Text(stringResource(R.string.incoming_choice, attackerName), style = XType.Body, color = XColors.Muted)
            BoxWithConstraints {
                val stacked = maxWidth < MIN_BUTTON_WIDTH * 2 * LocalDensity.current.fontScale.coerceAtLeast(1f)
                val stayButton = @Composable { buttonModifier: Modifier ->
                    XButton(
                        stringResource(R.string.incoming_stay),
                        onClick = { choose(stay) },
                        modifier = buttonModifier,
                        enabled = stay != null,
                        subtitle = stringResource(R.string.incoming_stay_subtitle),
                        minHeight = 72.dp,
                        textStyle = XType.DisplayButton.copy(fontSize = 22.sp),
                    )
                }
                val logOffButton = @Composable { buttonModifier: Modifier ->
                    XButton(
                        stringResource(R.string.incoming_log_off),
                        onClick = { choose(logOff) },
                        modifier = buttonModifier,
                        style = XButtonStyle.Secondary,
                        enabled = logOff != null,
                        icon = XIcon.LogOff,
                        subtitle = stringResource(R.string.incoming_log_off_subtitle, attackerName),
                        minHeight = 72.dp,
                        textStyle = XType.DisplayButton.copy(fontSize = 22.sp),
                    )
                }
                if (stacked) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        stayButton(Modifier.fillMaxWidth())
                        logOffButton(Modifier.fillMaxWidth())
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        stayButton(Modifier.weight(1f))
                        logOffButton(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** Reply, retweet, like and view counts, from V2's fake engagement. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Engagement(
    leaderId: LeaderId,
    seed: Int,
) {
    val counts = engagementCounts(leaderId, seed)
    val labels =
        listOf(R.string.incoming_replies, R.string.incoming_retweets, R.string.incoming_likes, R.string.incoming_views)
            .zip(counts) { label, count -> stringResource(label, count) }
    // Wraps onto a second line on small phones at large font scales.
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(XIcon.Reply, XIcon.Retweet, XIcon.LikeOutline, XIcon.Views).zip(counts).forEachIndexed { index, (icon, count) ->
            val color = if (index == 2) XColors.SendIt else XColors.InkMuted
            Row(
                Modifier.clearAndSetSemantics { contentDescription = labels[index] },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                XIconImage(icon, contentDescription = null, size = 16.dp, tint = color)
                Text(count, style = XType.Caption.copy(fontSize = 12.5.sp), color = color)
            }
        }
    }
}

private const val MAX_FOOTER_DICE = 6

/** Below this height (at font scale 1) the choice scrolls with the tweet instead of staying pinned. */
private val PIN_MIN_HEIGHT = 560.dp

/** Narrowest a STAY / LOG OFF button gets (at font scale 1) before they stack. */
private val MIN_BUTTON_WIDTH = 110.dp

/** Share of the width your bust takes on narrow phones, its height to width, and the avatar in it. */
private const val BUST_SHARE = 0.45f
private const val BUST_ASPECT = 206f / 180f
private const val AVATAR_SHARE = 150f / 180f
