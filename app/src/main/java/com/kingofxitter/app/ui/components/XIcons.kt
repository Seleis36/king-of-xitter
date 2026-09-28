package com.kingofxitter.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kingofxitter.app.R
import com.kingofxitter.app.ui.theme.XColors
import com.kingofxitter.app.ui.theme.XType
import com.kingofxitter.app.ui.theme.XitterTheme
import com.kingofxitter.app.ui.theme.nightBackground

/**
 * The icon set from `design/screens/`, generated into VectorDrawables by `tools/extract_icons.py`.
 * [tintable] icons are single-colour line icons drawn in the caller's colour; the others keep the
 * design's colours.
 */
enum class XIcon(
    @param:DrawableRes val drawable: Int,
    val tintable: Boolean,
) {
    Tweet(R.drawable.ic_tweet, tintable = false),
    Like(R.drawable.ic_like, tintable = false),
    LikeLost(R.drawable.ic_like_lost, tintable = false),
    Characters(R.drawable.ic_characters, tintable = false),
    Mandates(R.drawable.ic_mandates, tintable = false),
    Crown(R.drawable.ic_crown, tintable = false),
    Bot(R.drawable.ic_bot, tintable = false),
    CheckUnverified(R.drawable.ic_check_unverified, tintable = false),
    CheckBlue(R.drawable.ic_check_blue, tintable = false),
    CheckGold(R.drawable.ic_check_gold, tintable = false),
    Timeline(R.drawable.ic_timeline, tintable = true),
    Standings(R.drawable.ic_standings, tintable = true),
    Reroll(R.drawable.ic_reroll, tintable = true),
    Close(R.drawable.ic_close, tintable = true),
    Back(R.drawable.ic_back, tintable = true),
    Send(R.drawable.ic_send, tintable = true),
    Shuffle(R.drawable.ic_shuffle, tintable = true),
    LogOff(R.drawable.ic_log_off, tintable = true),
    Sweep(R.drawable.ic_sweep, tintable = true),
    Reply(R.drawable.ic_reply, tintable = true),
    Retweet(R.drawable.ic_retweet, tintable = true),
    LikeOutline(R.drawable.ic_like_outline, tintable = true),
    Views(R.drawable.ic_views, tintable = true),
    HowToPlay(R.drawable.ic_how_to_play, tintable = true),
    Settings(R.drawable.ic_settings, tintable = true),
    Play(R.drawable.ic_play, tintable = true),
    Share(R.drawable.ic_share, tintable = true),
}

/**
 * Draws [icon] at [size]. [tint] applies to tintable icons only. Pass a [contentDescription] when
 * the icon carries meaning on its own; leave it null when adjacent text already says it.
 */
@Composable
fun XIconImage(
    icon: XIcon,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = XColors.Ink,
) {
    val painter = painterResource(icon.drawable)
    if (icon.tintable) {
        Icon(painter, contentDescription, modifier.size(size), tint = tint)
    } else {
        Image(painter, contentDescription, modifier.size(size))
    }
}

/** Every icon with its name. Also shown by the debug design gallery. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun IconSetSample(modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        XIcon.entries.forEach { icon ->
            Column(Modifier.width(88.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                XIconImage(icon, contentDescription = null, size = 40.dp, tint = XColors.Paper)
                Text(icon.name, style = XType.Caption, color = XColors.Muted, textAlign = TextAlign.Center)
            }
        }
    }
}

@Preview(widthDp = 412, heightDp = 700)
@Composable
private fun IconSetPreview() {
    XitterTheme { IconSetSample(Modifier.nightBackground()) }
}
