package com.kingofxitter.app.ui.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.kingofxitter.app.R
import com.kingofxitter.app.ui.components.XAvatar
import com.kingofxitter.app.ui.components.XCard
import com.kingofxitter.app.ui.components.XIcon
import com.kingofxitter.app.ui.components.XIconImage
import com.kingofxitter.app.ui.components.XSpeechBubble
import com.kingofxitter.app.ui.components.XTag
import com.kingofxitter.app.ui.motion.bob
import com.kingofxitter.app.ui.motion.wobble
import com.kingofxitter.app.ui.theme.StickerShape
import com.kingofxitter.app.ui.theme.XColors
import com.kingofxitter.app.ui.theme.XType
import com.kingofxitter.engine.model.PlayerState
import kotlin.math.hypot

/**
 * The Xitter stage: a tilted card with the Main Character on a sunburst, their latest tweet, and
 * what holding Xitter means. Shows an empty stage when nobody holds it. The design's height is a
 * minimum: at large font scales the stage grows with its text.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun XitterStage(
    mainCharacter: PlayerState?,
    isYou: Boolean,
    line: String,
    modifier: Modifier = Modifier,
) {
    XCard(modifier.rotate(-1f).fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(3.dp).clip(StickerShape)) {
            FlowRow(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp)
                    .background(XColors.Ink)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = XColors.Followers)) { append(stringResource(R.string.board_xitter_x)) }
                        append(stringResource(R.string.board_xitter_rest))
                    },
                    style = XType.DisplayMedium.copy(fontSize = XType.DisplayMedium.fontSize * 1.1f),
                    color = XColors.Paper,
                    modifier = Modifier.padding(top = 3.dp).semantics { heading() },
                )
                if (mainCharacter != null) XTag(stringResource(R.string.board_main_character), icon = XIcon.Crown)
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .clipToBounds()
                    .background(XColors.Stage),
            ) {
                Sunburst(Modifier.matchParentSize())
                if (mainCharacter == null) EmptyStage() else OccupiedStage(mainCharacter, isYou, line)
            }
        }
    }
}

@Composable
private fun OccupiedStage(
    mainCharacter: PlayerState,
    isYou: Boolean,
    line: String,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // The bust takes the design's 150dp, or less on narrow phones and at large font scales so
        // the tweet keeps room.
        val bust = (maxWidth * BUST_SHARE / LocalDensity.current.fontScale.coerceAtLeast(1f)).coerceAtMost(150.dp)
        Row(Modifier.fillMaxWidth().heightIn(min = STAGE_HEIGHT).padding(end = 12.dp), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.width(bust), horizontalAlignment = Alignment.CenterHorizontally) {
                XIconImage(XIcon.Crown, contentDescription = null, size = bust * 0.29f, modifier = Modifier.bob().rotate(-8f))
                // Stand-in for the leader bust until S5 supplies portraits.
                XAvatar(mainCharacter.leaderId, size = bust * 0.79f, modifier = Modifier.padding(bottom = 10.dp))
            }
            StageText(line, isYou, Modifier.weight(1f).heightIn(min = STAGE_HEIGHT))
        }
    }
}

@Composable
private fun StageText(
    line: String,
    isYou: Boolean,
    modifier: Modifier = Modifier,
) {
    // Bubble at the top and tags at the bottom, as in the design, pushed apart when there's room.
    Column(modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.SpaceBetween) {
        XSpeechBubble(
            line,
            Modifier.wobble(degrees = 1f),
            textStyle = XType.BodyStrong.copy(fontSize = XType.BodyStrong.fontSize * 0.96f),
        )
        Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            XTag(stringResource(R.string.board_stage_mandates), fill = XColors.Paper, icon = XIcon.Mandates, maxLines = TAG_LINES)
            XTag(
                stringResource(if (isYou) R.string.board_stage_your_tweets else R.string.board_stage_their_tweets),
                fill = XColors.Paper,
                icon = XIcon.Tweet,
                maxLines = TAG_LINES,
            )
            if (isYou) {
                XTag(
                    stringResource(R.string.board_stage_no_likes),
                    fill = XColors.Paper,
                    icon = XIcon.LikeLost,
                    maxLines = TAG_LINES,
                )
            }
        }
    }
}

@Composable
private fun EmptyStage() {
    Column(
        Modifier.fillMaxWidth().heightIn(min = STAGE_HEIGHT).padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.board_stage_empty), style = XType.DisplayLarge, color = XColors.Ink)
        XTag(
            stringResource(R.string.board_stage_empty_hint),
            fill = XColors.Paper,
            icon = XIcon.Crown,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

/** The stage's `repeating-conic-gradient`: 10° gold rays from 26% across, 70% down. */
@Composable
private fun Sunburst(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val center = Offset(size.width * 0.26f, size.height * 0.7f)
        val radius = hypot(size.width, size.height)
        for (ray in 0 until 36 step 2) {
            drawArc(
                color = XColors.Mandates,
                startAngle = ray * 10f - 90f,
                sweepAngle = 10f,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
            )
        }
    }
}

/** Height of the stage below its header in the design (238dp card, 40dp header, 3dp inset). */
private val STAGE_HEIGHT = 192.dp

/** Share of the stage's width the Main Character's bust takes on narrow phones. */
private const val BUST_SHARE = 0.42f

/** Stage tags wrap rather than cut off at large font scales. */
private const val TAG_LINES = 3
