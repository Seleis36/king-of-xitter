package com.kingofxitter.app.ui.timeline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingofxitter.app.R
import com.kingofxitter.app.content.VoiceEntry
import com.kingofxitter.app.content.engagementCounts
import com.kingofxitter.app.content.rememberContentStrings
import com.kingofxitter.app.ui.components.DashedFooter
import com.kingofxitter.app.ui.components.OutlinedText
import com.kingofxitter.app.ui.components.TweetAuthor
import com.kingofxitter.app.ui.components.XButton
import com.kingofxitter.app.ui.components.XButtonStyle
import com.kingofxitter.app.ui.components.XCard
import com.kingofxitter.app.ui.components.XChip
import com.kingofxitter.app.ui.components.XIcon
import com.kingofxitter.app.ui.components.XIconImage
import com.kingofxitter.app.ui.components.XTag
import com.kingofxitter.app.ui.components.leaderVisual
import com.kingofxitter.app.ui.components.tweetText
import com.kingofxitter.app.ui.theme.XColors
import com.kingofxitter.app.ui.theme.XType
import com.kingofxitter.app.ui.theme.XitterTheme
import com.kingofxitter.app.ui.theme.nightBackground
import com.kingofxitter.cards.CardCatalog
import com.kingofxitter.content.TweetMoment
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.PlayerId

/**
 * Match timeline (`design/screens/match-timeline.html`): every line the leaders said this match,
 * newest first, with the design's three filters.
 *
 * The entries come from V1's [com.kingofxitter.app.content.VoiceLog], so the timeline is a record
 * of the match rather than a second source of flavour text.
 */
@Composable
fun TimelineScreen(
    state: GameState,
    humanId: PlayerId,
    entries: List<VoiceEntry>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var filterName by rememberSaveable { mutableStateOf(TimelineFilter.FOR_YOU.name) }
    val filter = TimelineFilter.valueOf(filterName)
    val shown = remember(entries, filter, humanId) { entries.onTimeline(filter, humanId) }

    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Header(filter) { filterName = it.name }
        if (shown.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(filter.empty),
                    style = XType.Body,
                    color = XColors.Muted,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 12.dp),
            ) {
                items(shown, key = { "${it.playerId.value}-${it.moment}-${it.turnNumber}-${it.text.hashCode()}" }) { entry ->
                    TimelineTweet(state, entry)
                }
            }
        }
        XButton(
            stringResource(R.string.timeline_back),
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            style = XButtonStyle.Secondary,
            icon = XIcon.Back,
            minHeight = 56.dp,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Header(
    filter: TimelineFilter,
    onFilter: (TimelineFilter) -> Unit,
) {
    Column(Modifier.padding(top = 16.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            XIconImage(XIcon.Timeline, contentDescription = null, size = 24.dp, tint = XColors.Mandates)
            OutlinedText(stringResource(R.string.timeline_title_x), XType.DisplayLarge, XColors.Mandates)
            OutlinedText(
                stringResource(R.string.timeline_title),
                XType.DisplayLarge,
                XColors.Paper,
                modifier = Modifier.semantics { heading() },
            )
        }
        Text(stringResource(R.string.timeline_subtitle), style = XType.Caption, color = XColors.Muted)
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TimelineFilter.entries.forEach { option ->
                val selected = option == filter
                XChip(
                    stringResource(option.label),
                    modifier = Modifier.heightIn(min = 40.dp),
                    fill = if (selected) XColors.Mandates else XColors.Paper,
                    onClick = { onFilter(option) },
                    contentDescription = stringResource(R.string.timeline_filter_selected, stringResource(option.label)),
                )
            }
        }
    }
}

@Composable
private fun TimelineTweet(
    state: GameState,
    entry: VoiceEntry,
) {
    val author = state.players.firstOrNull { it.id == entry.playerId } ?: return
    val (hour, minute) = timelineClock(entry.turnNumber)
    XCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TweetAuthor(author, avatarSize = 42.dp, time = stringResource(R.string.timeline_time, hour, minute))
            Text(tweetText(entry.text), style = XType.Tweet, color = XColors.Ink)
            Outcome(state, entry)
            DashedFooter { Engagement(entry) }
        }
    }
}

/** The design's small tag under a tweet saying what it did. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Outcome(
    state: GameState,
    entry: VoiceEntry,
) {
    val target = entry.targetId?.let { id -> state.players.firstOrNull { it.id == id } }
    val handle = target?.let { stringResource(leaderVisual(it.leaderId).handle) }
    val text =
        when (entry.moment) {
            TweetMoment.ATTACKED ->
                if (handle != null && entry.unfollows > 0) stringResource(R.string.timeline_unfollows_to, entry.unfollows, handle) else null

            TweetMoment.GOT_HIT ->
                if (handle != null && entry.unfollows > 0) {
                    stringResource(R.string.timeline_unfollows_from, entry.unfollows, handle)
                } else {
                    null
                }

            TweetMoment.ENTERED_XITTER -> stringResource(R.string.timeline_took_xitter)
            TweetMoment.LEFT_XITTER -> stringResource(R.string.timeline_logged_off)
            TweetMoment.BOUGHT_POWER_UP -> boughtLabel(entry)
            TweetMoment.SUSPENDED -> stringResource(R.string.timeline_suspended)
            TweetMoment.WON -> stringResource(R.string.timeline_won)
            TweetMoment.LOST -> stringResource(R.string.timeline_lost)
            TweetMoment.BURNER -> stringResource(R.string.timeline_burner)
            TweetMoment.ROLLED -> null
        }
    if (text == null) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        XTag(text, fill = outcomeFill(entry.moment), maxLines = 2)
    }
}

/** The card's own name when the catalogue knows it, so the tag reads like the design's. */
@Composable
private fun boughtLabel(entry: VoiceEntry): String {
    val strings = rememberContentStrings()
    val name =
        entry.cardId
            ?.let { CardCatalog.byId[it]?.nameKey }
            ?.let(strings::text)
            ?.takeIf { it.isNotEmpty() }
    return if (name == null) stringResource(R.string.timeline_bought_unknown) else stringResource(R.string.timeline_bought, name)
}

private fun outcomeFill(moment: TweetMoment) =
    when (moment) {
        TweetMoment.ATTACKED, TweetMoment.GOT_HIT, TweetMoment.SUSPENDED -> XColors.DamageTint
        TweetMoment.ENTERED_XITTER, TweetMoment.WON -> XColors.Mandates
        TweetMoment.BOUGHT_POWER_UP -> XColors.CharactersTint
        else -> XColors.Paper
    }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Engagement(entry: VoiceEntry) {
    val counts = engagementCounts(entry.leaderId, entry.turnNumber * 10 + entry.moment.ordinal)
    val icons = listOf(XIcon.Reply, XIcon.Retweet, XIcon.LikeOutline, XIcon.Views)
    val labels = listOf(R.string.incoming_replies, R.string.incoming_retweets, R.string.incoming_likes, R.string.incoming_views)
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        icons.zip(counts).forEachIndexed { index, (icon, count) ->
            val color = if (index == 2) XColors.SendIt else XColors.InkMuted
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                XIconImage(icon, contentDescription = stringResource(labels[index], count), size = 15.dp, tint = color)
                Text(count, style = XType.Caption.copy(fontSize = 12.sp), color = color)
            }
        }
    }
}

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun TimelinePreview() {
    XitterTheme {
        TimelineScreen(
            TimelineSamples.state,
            TimelineSamples.humanId,
            TimelineSamples.entries,
            onBack = {},
            modifier = Modifier.nightBackground(),
        )
    }
}

@Preview(widthDp = 320, heightDp = 640, fontScale = 1.3f)
@Composable
private fun TimelineEmptyPreview() {
    XitterTheme {
        TimelineScreen(TimelineSamples.state, TimelineSamples.humanId, emptyList(), onBack = {}, modifier = Modifier.nightBackground())
    }
}
