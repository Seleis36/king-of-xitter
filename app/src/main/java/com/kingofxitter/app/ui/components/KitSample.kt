package com.kingofxitter.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kingofxitter.app.R
import com.kingofxitter.app.ui.theme.XColors
import com.kingofxitter.app.ui.theme.XType
import com.kingofxitter.app.ui.theme.XitterTheme
import com.kingofxitter.app.ui.theme.nightBackground
import com.kingofxitter.engine.model.LeaderId

/** The shared components as laid out in the kit's right column. Also shown by the debug design gallery. */
@Composable
internal fun ComponentKitSample(modifier: Modifier = Modifier) {
    var selected by remember { mutableIntStateOf(0) }
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedText(stringResource(R.string.kit_title), XType.DisplayHero, XColors.Mandates, outline = 7.dp, dropShadow = 6.dp)
        XButton(stringResource(R.string.kit_button_primary), onClick = {}, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val small = XType.DisplayButton.copy(fontSize = XType.DisplayMedium.fontSize)
            XButton(stringResource(R.string.kit_button_secondary), {}, Modifier.weight(1f), XButtonStyle.Secondary, textStyle = small)
            XButton(
                stringResource(R.string.kit_button_hot),
                {},
                Modifier.weight(1f),
                XButtonStyle.Hot,
                icon = XIcon.Send,
                textStyle = small,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val small = XType.DisplayButton.copy(fontSize = XType.DisplayMedium.fontSize)
            XButton(stringResource(R.string.kit_button_ghost), {}, Modifier.weight(1f), XButtonStyle.Ghost, textStyle = small)
            XButton(stringResource(R.string.kit_button_disabled), {}, Modifier.weight(1f), enabled = false, textStyle = small)
        }
        XButton(
            stringResource(R.string.kit_button_stay),
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            subtitle = stringResource(R.string.kit_button_stay_subtitle),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            XIconButton(XIcon.Timeline, stringResource(R.string.action_timeline), onClick = {})
            XIconButton(XIcon.Standings, stringResource(R.string.action_standings), onClick = {})
            XChip(stringResource(R.string.kit_chip))
            XTag(stringResource(R.string.kit_tag), icon = XIcon.Crown)
        }
        listOf(R.string.kit_option_selected, R.string.kit_option_other).forEachIndexed { index, text ->
            XOptionCard(selected = selected == index, onSelect = { selected = index }) {
                Text(stringResource(text), style = XType.BodyStrong, color = XColors.Ink)
            }
        }
        XSpeechBubble(stringResource(R.string.kit_bubble))
        Box(Modifier.fillMaxWidth().height(170.dp)) {
            XTextSticker(stringResource(R.string.kit_sticker_sad), XColors.Mandates, Modifier.offset(4.dp, 10.dp), rotation = -8f)
            XTextSticker(
                stringResource(R.string.kit_sticker_nyet),
                Color(0xFF9C7BFF),
                Modifier.offset(110.dp, 4.dp),
                rotation = 6f,
                textColor = XColors.Paper,
            )
            XTextSticker(stringResource(R.string.kit_sticker_afuera), XColors.Characters, Modifier.offset(220.dp, 14.dp), rotation = -4f)
            XTextSticker(
                stringResource(R.string.kit_sticker_en_meme_temps),
                Color(0xFFFF8FC8),
                Modifier.offset(10.dp, 76.dp),
                rotation = 5f,
            )
            XTextSticker(stringResource(R.string.kit_sticker_harmonized), Color(0xFF22C08A), Modifier.offset(200.dp, 82.dp), rotation = -6f)
            XStamp(stringResource(R.string.kit_stamp_ratioed), Modifier.offset(24.dp, 124.dp))
            XTextSticker(stringResource(R.string.kit_sticker_covfefe), Color(0xFFFF8A1F), Modifier.offset(210.dp, 132.dp), rotation = 8f)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("trumpet", "poutine", "kim", "macaron", "xi", "mi-leon").forEach { id ->
                XAvatar(LeaderId(id), size = 44.dp)
            }
        }
    }
}

@Preview(widthDp = 412, heightDp = 1100)
@Composable
private fun ComponentKitPreview() {
    XitterTheme { ComponentKitSample(Modifier.nightBackground()) }
}
