package com.kingofxitter.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kingofxitter.app.R
import com.kingofxitter.app.ui.theme.XColors
import com.kingofxitter.app.ui.theme.XType
import com.kingofxitter.app.ui.theme.XitterTheme
import com.kingofxitter.app.ui.theme.nightBackground
import com.kingofxitter.engine.model.DiceFace

/** The kit's DICE block, plus an interactive keep-and-roll row. Also shown by the debug design gallery. */
@Composable
internal fun DiceKitSample(modifier: Modifier = Modifier) {
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            DiceFace.entries.forEach { Die(it) }
        }
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Bottom) {
            Die(DiceFace.TWEET, kept = true)
            Die(DiceFace.THREE, kept = true)
            Die(DiceFace.LIKE, size = DieSize.Small)
            Die(DiceFace.TWO, size = DieSize.Small)
            Die(DiceFace.CHARACTERS, size = DieSize.ExtraSmall)
            Die(DiceFace.ONE, size = DieSize.ExtraSmall)
        }
        Text(stringResource(R.string.kit_dice_caption), style = XType.Caption, color = XColors.Muted)
        InteractiveDice()
    }
}

@Composable
private fun InteractiveDice() {
    var roll by remember { mutableIntStateOf(1) }
    var faces by remember { mutableStateOf(List(6) { DiceFace.entries[it] }) }
    var kept by remember { mutableStateOf(emptySet<Int>()) }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(stringResource(R.string.kit_dice_roll), style = XType.Caption, color = XColors.Muted)
        Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 22.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            faces.forEachIndexed { index, face ->
                Die(
                    face,
                    kept = index in kept,
                    tumbleKey = if (index in kept) null else roll,
                    onToggleKept = { keep -> kept = if (keep) kept + index else kept - index },
                )
            }
        }
        XButton(stringResource(R.string.kit_button_primary), onClick = {
            roll++
            faces = faces.mapIndexed { index, face -> if (index in kept) face else DiceFace.entries.random() }
        }, modifier = Modifier.fillMaxWidth(), icon = XIcon.Reroll)
    }
}

@Preview(widthDp = 412, heightDp = 560)
@Composable
private fun DiceKitPreview() {
    XitterTheme { DiceKitSample(Modifier.nightBackground()) }
}
