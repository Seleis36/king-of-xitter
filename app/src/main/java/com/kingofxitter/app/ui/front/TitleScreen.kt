package com.kingofxitter.app.ui.front

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.kingofxitter.app.R
import com.kingofxitter.app.content.rememberContentStrings
import com.kingofxitter.app.ui.components.OutlinedText
import com.kingofxitter.app.ui.components.XButton
import com.kingofxitter.app.ui.components.XButtonStyle
import com.kingofxitter.app.ui.components.XIcon
import com.kingofxitter.app.ui.components.XIconImage
import com.kingofxitter.app.ui.components.XPanel
import com.kingofxitter.app.ui.components.XTextSticker
import com.kingofxitter.app.ui.motion.LocalMotion
import com.kingofxitter.app.ui.motion.bob
import com.kingofxitter.app.ui.motion.wobble
import com.kingofxitter.app.ui.theme.XColors
import com.kingofxitter.app.ui.theme.XType
import com.kingofxitter.app.ui.theme.XitterTheme
import com.kingofxitter.app.ui.theme.nightBackground
import com.kingofxitter.content.TrendingTags
import kotlin.math.roundToInt

/**
 * Title screen (`design/screens/title-screen.html`): sticker bomb, the logo, PLAY SOLO, and the
 * trending marquee scrolling V2's hashtags along the bottom.
 */
@Composable
fun TitleScreen(
    onPlay: () -> Unit,
    onRules: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    tags: List<String> = rememberTrendingTags(),
) {
    Column(modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Stickers()
            Logo()
            Text(
                stringResource(R.string.title_tagline),
                style = XType.Body,
                color = XColors.Muted,
                textAlign = TextAlign.Center,
            )
            MainCharacterBadge()
            XButton(
                stringResource(R.string.title_play_solo),
                onClick = onPlay,
                modifier = Modifier.fillMaxWidth().bob(height = 4.dp),
                style = XButtonStyle.Hot,
                icon = XIcon.Play,
                subtitle = stringResource(R.string.title_play_solo_subtitle),
                minHeight = 74.dp,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                XButton(
                    stringResource(R.string.title_rules),
                    onClick = onRules,
                    modifier = Modifier.weight(1f),
                    style = XButtonStyle.Secondary,
                    icon = XIcon.HowToPlay,
                    minHeight = 52.dp,
                    textStyle = XType.DisplaySmall,
                )
                XButton(
                    stringResource(R.string.title_settings),
                    onClick = onSettings,
                    modifier = Modifier.weight(1f),
                    style = XButtonStyle.Secondary,
                    icon = XIcon.Settings,
                    minHeight = 52.dp,
                    textStyle = XType.DisplaySmall,
                )
            }
            Text(
                stringResource(R.string.title_disclaimer),
                style = XType.Caption,
                color = XColors.GhostBorder,
                textAlign = TextAlign.Center,
            )
        }
        TrendingMarquee(tags, Modifier.fillMaxWidth())
    }
}

/** V2's trending hashtags, resolved in the marquee's order. */
@Composable
fun rememberTrendingTags(): List<String> {
    val strings = rememberContentStrings()
    return remember(strings) { TrendingTags.all.map(strings::text).filter { it.isNotEmpty() } }
}

@Composable
private fun Stickers() {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        XTextSticker(stringResource(R.string.kit_sticker_en_meme_temps), XColors.Mandates, rotation = -6f, textStyle = XType.Tag)
        XTextSticker(stringResource(R.string.kit_sticker_sad), XColors.Followers, rotation = 5f, textStyle = XType.Tag)
        XTextSticker(stringResource(R.string.kit_sticker_afuera), XColors.Characters, rotation = -4f, textStyle = XType.Tag)
    }
}

@Composable
private fun Logo() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedText(
            stringResource(R.string.title_king_of),
            XType.DisplayLarge,
            XColors.Paper,
            outline = 5.dp,
            modifier = Modifier.semantics { heading() },
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedText(stringResource(R.string.title_x), XType.DisplayHero, XColors.Mandates, outline = 7.dp, dropShadow = 5.dp)
            OutlinedText(stringResource(R.string.title_itter), XType.DisplayHero, XColors.Paper, outline = 7.dp, dropShadow = 5.dp)
        }
    }
}

@Composable
private fun MainCharacterBadge() {
    Row(
        Modifier.wrapContentWidth().wobble(degrees = 1.5f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        XIconImage(XIcon.Crown, contentDescription = null, size = 24.dp)
        Text(stringResource(R.string.title_main_character), style = XType.Tag, color = XColors.Mandates)
    }
}

/**
 * The design's scrolling trending bar. It reads as one line to a screen reader rather than a
 * stream of moving words, and it stops moving when motion is off.
 */
@Composable
private fun TrendingMarquee(
    tags: List<String>,
    modifier: Modifier = Modifier,
) {
    if (tags.isEmpty()) return
    val line = tags.joinToString(SEPARATOR)
    val spoken = stringResource(R.string.title_trending_marquee, tags.joinToString(", "))
    val motion = LocalMotion.current
    val width = remember { mutableIntStateOf(0) }
    val shift by rememberInfiniteTransition(label = "marquee").animateFloat(
        initialValue = 0f,
        targetValue = if (motion.enabled) 1f else 0f,
        animationSpec = infiniteRepeatable(tween(motion.duration(SCROLL_MILLIS), easing = LinearEasing), RepeatMode.Restart),
        label = "marqueeShift",
    )
    XPanel(modifier, fill = XColors.Panel) {
        Box(
            Modifier
                .fillMaxWidth()
                .clipToBounds()
                .padding(vertical = 10.dp)
                .clearAndSetSemantics { contentDescription = spoken },
        ) {
            Row(
                Modifier
                    .onSizeChanged { width.intValue = it.width / 2 }
                    .offset { IntOffset(-(shift * width.intValue).roundToInt(), 0) },
            ) {
                // Two copies, so the second slides in as the first slides out.
                repeat(2) {
                    Text(
                        "${stringResource(R.string.title_trending)} $line$SEPARATOR",
                        style = XType.Chip,
                        color = XColors.Mandates,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
            }
        }
    }
}

private const val SEPARATOR = "   ·   "
private const val SCROLL_MILLIS = 18_000

@Suppress("UnusedPrivateMember")
private val previewTags = listOf("#TrumpetRatio", "#PoutineIsServedCold", "#EnMêmeTemps", "#SupremeFun", "#Harmonized", "#AFUERA")

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun TitlePreview() {
    XitterTheme {
        TitleScreen({}, {}, {}, Modifier.nightBackground(), previewTags)
    }
}

@Preview(widthDp = 320, heightDp = 640, fontScale = 1.4f)
@Composable
private fun TitleSmallPreview() {
    XitterTheme {
        TitleScreen({}, {}, {}, Modifier.nightBackground(), previewTags)
    }
}
