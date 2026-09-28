package com.kingofxitter.app.ui.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
 * what holding Xitter means. Shows an empty stage when nobody holds it.
 */
@Composable
fun XitterStage(
    mainCharacter: PlayerState?,
    isYou: Boolean,
    line: String,
    modifier: Modifier = Modifier,
) {
    XCard(modifier.rotate(-1f).fillMaxWidth().height(238.dp)) {
        Column(Modifier.fillMaxSize().padding(3.dp).clip(StickerShape)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(XColors.Ink)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
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
                    .weight(1f)
                    .clipToBounds()
                    .background(XColors.Stage),
            ) {
                Sunburst()
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
    Row(Modifier.fillMaxSize().padding(end = 12.dp)) {
        Column(
            Modifier.width(150.dp).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
        ) {
            XIconImage(XIcon.Crown, contentDescription = null, size = 44.dp, modifier = Modifier.bob().rotate(-8f))
            // Stand-in for the leader bust until S5 supplies portraits.
            XAvatar(mainCharacter.leaderId, size = 118.dp, modifier = Modifier.padding(bottom = 10.dp))
        }
        Column(
            Modifier.fillMaxHeight().weight(1f).padding(vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            XSpeechBubble(
                line,
                Modifier.wobble(degrees = 1f),
                textStyle =
                    XType.BodyStrong.copy(
                        fontSize =
                            XType.BodyStrong.fontSize * 0.96f,
                    ),
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                XTag(stringResource(R.string.board_stage_mandates), fill = XColors.Paper, icon = XIcon.Mandates)
                XTag(
                    stringResource(if (isYou) R.string.board_stage_your_tweets else R.string.board_stage_their_tweets),
                    fill = XColors.Paper,
                    icon = XIcon.Tweet,
                )
                if (isYou) XTag(stringResource(R.string.board_stage_no_likes), fill = XColors.Paper, icon = XIcon.LikeLost)
            }
        }
    }
}

@Composable
private fun EmptyStage() {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
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
private fun Sunburst() {
    Canvas(Modifier.fillMaxSize()) {
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
