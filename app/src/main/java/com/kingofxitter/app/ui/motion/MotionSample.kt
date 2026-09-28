package com.kingofxitter.app.ui.motion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingofxitter.app.R
import com.kingofxitter.app.ui.components.Die
import com.kingofxitter.app.ui.components.OutlinedText
import com.kingofxitter.app.ui.components.XAvatar
import com.kingofxitter.app.ui.components.XButton
import com.kingofxitter.app.ui.components.XCard
import com.kingofxitter.app.ui.components.XIcon
import com.kingofxitter.app.ui.components.XPanel
import com.kingofxitter.app.ui.components.XStamp
import com.kingofxitter.app.ui.components.XTextSticker
import com.kingofxitter.app.ui.theme.XColors
import com.kingofxitter.app.ui.theme.XType
import com.kingofxitter.app.ui.theme.XitterTheme
import com.kingofxitter.app.ui.theme.nightBackground
import com.kingofxitter.engine.model.DiceFace
import com.kingofxitter.engine.model.LeaderId

/** The kit's MOTION tiles, with a button to replay the one-shot effects. Also in the debug gallery. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MotionKitSample(modifier: Modifier = Modifier) {
    var replay by remember { mutableIntStateOf(1) }
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            maxItemsInEachRow = 4,
        ) {
            val trumpet = LeaderId("trumpet")
            Tile(R.string.motion_wobble) {
                XTextSticker(stringResource(R.string.kit_sticker_sad), XColors.Mandates, Modifier.wobble(6f))
            }
            Tile(R.string.motion_bob) { XAvatar(trumpet, Modifier.bob(), size = 52.dp) }
            Tile(R.string.motion_tumble) { Die(DiceFace.TWEET, tumbleKey = replay) }
            Tile(R.string.motion_slam) {
                XCard(Modifier.size(84.dp, 48.dp).slamOn(replay)) {
                    Text(
                        stringResource(R.string.motion_tweet),
                        Modifier.align(Alignment.Center),
                        style = XType.Caption,
                        color = XColors.Ink,
                    )
                }
            }
            Tile(R.string.motion_shake) { XAvatar(trumpet, Modifier.shakeOn(replay), size = 52.dp) }
            Tile(R.string.motion_rise) {
                key(replay) {
                    OutlinedText(
                        stringResource(R.string.effect_followers, signed(-3)),
                        XType.DisplayLarge.copy(fontSize = 26.sp),
                        XColors.Followers,
                        Modifier.rise(),
                    )
                }
            }
            Tile(R.string.motion_pop) {
                XTextSticker(stringResource(R.string.kit_sticker_sad), XColors.Mandates, Modifier.popOn(replay), rotation = -6f)
            }
            Tile(R.string.motion_stamp) {
                XStamp(
                    stringResource(R.string.kit_stamp_ratioed),
                    Modifier.stampOn(replay),
                    textStyle = XType.Stamp.copy(fontSize = 11.sp),
                    borderWidth = 3.dp,
                )
            }
        }
        XButton(
            stringResource(R.string.motion_replay),
            onClick = { replay++ },
            modifier = Modifier.fillMaxWidth().pulse(),
            icon = XIcon.Play,
        )
    }
}

@Composable
private fun Tile(
    label: Int,
    content: @Composable () -> Unit,
) {
    XPanel(Modifier.size(width = 84.dp, height = 112.dp)) {
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth().height(72.dp).clipToBounds(), contentAlignment = Alignment.Center) { content() }
            Text(stringResource(label), style = XType.DisplaySmall.copy(fontSize = 13.sp), color = XColors.Paper)
        }
    }
}

@Preview(widthDp = 412, heightDp = 420)
@Composable
private fun MotionKitPreview() {
    XitterTheme { MotionKitSample(Modifier.nightBackground()) }
}
