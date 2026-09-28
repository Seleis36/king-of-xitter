package com.kingofxitter.app.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.kingofxitter.app.R

/** Colours, type and sticker variants on one screen. Also shown by the debug design gallery. */
@Composable
internal fun ThemeKitSample(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.nightBackground().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(stringResource(R.string.theme_preview_display), style = XType.DisplayLarge, color = XColors.Mandates)
        Text(stringResource(R.string.theme_preview_meme), style = XType.MemeHero, color = XColors.Followers)
        Text(stringResource(R.string.theme_preview_tweet), style = XType.Tweet, color = XColors.Paper)
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            listOf(XColors.Paper, XColors.Mandates, XColors.Followers, XColors.Characters).forEach { fill ->
                Box(Modifier.size(56.dp).sticker(fill = fill))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Box(Modifier.size(64.dp).sticker(fill = XColors.Panel, shadowColor = XColors.PanelShadow))
            Box(Modifier.size(64.dp).sticker(fill = XColors.Mandates, halo = 4.dp, shadowOffset = DpOffset(4.dp, 7.dp)))
        }
    }
}

@Preview(widthDp = 412, heightDp = 520)
@Composable
private fun ThemeKitPreview() {
    XitterTheme { ThemeKitSample() }
}
