package com.kingofxitter.app.ui.components

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingofxitter.app.ui.theme.XColors
import com.kingofxitter.app.ui.theme.XType
import com.kingofxitter.engine.model.PlayerState

private val Mention = Regex("[@#][\\p{L}\\p{N}_]+")

/** Tweet text with @mentions and #hashtags in link blue, as in the design's tweets. */
fun tweetText(text: String): AnnotatedString =
    buildAnnotatedString {
        var last = 0
        Mention.findAll(text).forEach { match ->
            append(text, last, match.range.first)
            withStyle(SpanStyle(color = XColors.Link)) { append(match.value) }
            last = match.range.last + 1
        }
        append(text, last, text.length)
    }

/** Author line of a tweet: avatar, full name with certification badge, handle and optional time. */
@Composable
fun TweetAuthor(
    author: PlayerState,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 44.dp,
    time: String? = null,
) {
    val visual = leaderVisual(author.leaderId)
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        XAvatar(author.leaderId, size = avatarSize)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(visual.fullName),
                    style = XType.Body.copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold),
                    color = XColors.Ink,
                )
                CertificationBadge(author.mandates, size = 16.dp)
            }
            val handle = stringResource(visual.handle)
            Text(
                if (time == null) handle else "$handle · $time",
                style = XType.Body.copy(fontSize = 12.5.sp),
                color = XColors.InkMuted,
            )
        }
    }
}

// The kit blinks with steps(2): hard on/off, no fade.
private val StepEasing = Easing { fraction -> if (fraction < 0.5f) 0f else 1f }

/** Blinking text cursor (`.a-blink`): a 3dp ink bar, shown while composing. */
@Composable
fun BlinkingCursor(modifier: Modifier = Modifier) {
    val alpha by rememberInfiniteTransition(label = "cursor").animateFloat(
        initialValue = 0.15f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(550, easing = StepEasing), RepeatMode.Reverse),
        label = "cursorAlpha",
    )
    Box(modifier.fillMaxSize().padding(horizontal = 1.dp).background(XColors.Ink.copy(alpha = alpha)))
}

private const val CURSOR = "cursor"

/** Tweet text being typed: [text] with mentions highlighted and a blinking cursor after it. */
@Composable
fun ComposingText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    Text(
        buildAnnotatedString {
            append(tweetText(text))
            appendInlineContent(CURSOR, "|")
        },
        style = style,
        color = XColors.Ink,
        modifier = modifier,
        inlineContent =
            mapOf(
                CURSOR to
                    InlineTextContent(Placeholder(5.sp, style.fontSize * 1.15f, PlaceholderVerticalAlign.TextCenter)) {
                        BlinkingCursor()
                    },
            ),
    )
}

/** A footer row above a dashed rule, as at the bottom of the compose and incoming cards. */
@Composable
fun DashedFooter(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    XColors.Muted,
                    Offset(0f, 0f),
                    Offset(size.width, 0f),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
                )
            }.padding(top = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) { content() }
}
