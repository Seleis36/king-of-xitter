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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.kingofxitter.app.R
import com.kingofxitter.app.ui.components.CertificationBadge
import com.kingofxitter.app.ui.components.Die
import com.kingofxitter.app.ui.components.DieSize
import com.kingofxitter.app.ui.components.OutlinedText
import com.kingofxitter.app.ui.components.XButton
import com.kingofxitter.app.ui.components.XCard
import com.kingofxitter.app.ui.components.XIcon
import com.kingofxitter.app.ui.components.XIconImage
import com.kingofxitter.app.ui.components.XPanel
import com.kingofxitter.app.ui.motion.bob
import com.kingofxitter.app.ui.theme.XColors
import com.kingofxitter.app.ui.theme.XType
import com.kingofxitter.app.ui.theme.XitterTheme
import com.kingofxitter.app.ui.theme.nightBackground
import com.kingofxitter.app.ui.theme.sticker
import com.kingofxitter.engine.model.DiceFace

/**
 * How to play (`design/screens/how-to-play.html`): the dice, what each face does, the Main
 * Character rules and how a match ends. [onDone] closes it back to wherever it was opened from.
 */
@Composable
fun HowToPlayScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 20.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Title()
            DiceRow()
            FaceRule(null, R.string.how_to_mandates_title, R.string.how_to_mandates, XColors.Mandates)
            FaceRule(XIcon.Tweet, R.string.how_to_tweet_title, R.string.how_to_tweet, XColors.Tweet)
            FaceRule(XIcon.Like, R.string.how_to_like_title, R.string.how_to_like, XColors.Followers)
            FaceRule(XIcon.Characters, R.string.how_to_characters_title, R.string.how_to_characters, XColors.Characters)
            Section(R.string.how_to_main_character_title, R.string.how_to_main_character) {
                XIconImage(XIcon.Crown, contentDescription = null, size = 26.dp, modifier = Modifier.bob())
            }
            Section(R.string.how_to_win_title, R.string.how_to_win) {
                XIconImage(XIcon.Mandates, contentDescription = null, size = 24.dp)
            }
            Certifications()
        }
        XButton(
            stringResource(R.string.how_to_got_it),
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            minHeight = 60.dp,
        )
    }
}

@Composable
private fun Title() {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedText(stringResource(R.string.how_to_title_lead), XType.DisplayLarge, XColors.Paper)
            // The design sets the X apart, as the platform's own logo does.
            OutlinedText(stringResource(R.string.how_to_title_x), XType.DisplayLarge, XColors.Mandates)
            OutlinedText(stringResource(R.string.how_to_title_tail), XType.DisplayLarge, XColors.Paper)
        }
        Text(
            stringResource(R.string.how_to_subtitle),
            style = XType.Body,
            color = XColors.Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/** The six faces, with the "roll, keep, reroll twice" rule beside them. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiceRow() {
    XCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                DiceFace.entries.forEach { face -> Die(face, size = DieSize.Small) }
            }
            Text(stringResource(R.string.how_to_dice), style = XType.Body, color = XColors.Ink)
        }
    }
}

/** One die face and the rule it triggers. */
@Composable
private fun FaceRule(
    icon: XIcon?,
    titleRes: Int,
    bodyRes: Int,
    accent: androidx.compose.ui.graphics.Color,
) {
    XPanel(Modifier.fillMaxWidth(), accent = accent) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .sticker(fill = accent, shape = CircleShape, borderWidth = 2.5.dp, shadowOffset = DpOffset.Zero),
                contentAlignment = Alignment.Center,
            ) {
                if (icon != null) {
                    XIconImage(icon, contentDescription = null, size = 22.dp)
                } else {
                    Text(stringResource(R.string.how_to_dice_count), style = XType.DisplaySmall, color = XColors.Ink)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(titleRes),
                    style = XType.DisplaySmall,
                    color = XColors.Paper,
                    modifier = Modifier.semantics { heading() },
                )
                Text(stringResource(bodyRes), style = XType.Body, color = XColors.Muted)
            }
        }
    }
}

/** A rules block with an icon in its heading. */
@Composable
private fun Section(
    titleRes: Int,
    bodyRes: Int,
    icon: @Composable () -> Unit,
) {
    XCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                icon()
                Text(
                    stringResource(titleRes),
                    style = XType.DisplaySmall,
                    color = XColors.Ink,
                    modifier = Modifier.semantics { heading() },
                )
            }
            Text(stringResource(bodyRes), style = XType.Body, color = XColors.Ink)
        }
    }
}

/** The three certification tiers, which are cosmetic (ROADMAP §1). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Certifications() {
    FlowRow(
        Modifier.fillMaxWidth().padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(0 to R.string.how_to_check_none, 6 to R.string.how_to_check_blue, 15 to R.string.how_to_check_gold)
            .forEach { (mandates, label) -> Certification(mandates, label) }
    }
}

@Composable
private fun Certification(
    mandates: Int,
    labelRes: Int,
) {
    Row(
        Modifier
            .sticker(shape = CircleShape, borderWidth = 2.5.dp, shadowOffset = DpOffset.Zero)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CertificationBadge(mandates, size = 18.dp)
        Text(stringResource(labelRes), style = XType.Chip, color = XColors.Ink)
    }
}

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun HowToPlayPreview() {
    XitterTheme { HowToPlayScreen(onDone = {}, modifier = Modifier.nightBackground()) }
}

@Preview(widthDp = 320, heightDp = 640, fontScale = 1.4f)
@Composable
private fun HowToPlaySmallPreview() {
    XitterTheme { HowToPlayScreen(onDone = {}, modifier = Modifier.nightBackground()) }
}
