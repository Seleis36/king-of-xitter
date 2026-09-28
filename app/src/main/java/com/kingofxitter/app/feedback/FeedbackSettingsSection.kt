package com.kingofxitter.app.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kingofxitter.app.R
import com.kingofxitter.app.ui.components.XPanel
import com.kingofxitter.app.ui.theme.XColors
import com.kingofxitter.app.ui.theme.XType
import com.kingofxitter.app.ui.theme.XitterTheme
import com.kingofxitter.app.ui.theme.nightBackground

/** Sound and haptics toggles, ready for V5's Settings screen. */
@Composable
fun FeedbackSettingsSection(
    settings: FeedbackSettings,
    onChange: (FeedbackSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    XPanel(modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            ToggleRow(stringResource(R.string.feedback_sound), settings.sound) { onChange(settings.copy(sound = it)) }
            ToggleRow(stringResource(R.string.feedback_haptics), settings.haptics) { onChange(settings.copy(haptics = it)) }
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = XType.DisplaySmall, color = XColors.Paper, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors =
                SwitchDefaults.colors(
                    checkedThumbColor = XColors.Ink,
                    checkedTrackColor = XColors.Characters,
                    uncheckedThumbColor = XColors.Muted,
                    uncheckedTrackColor = XColors.Disabled,
                    uncheckedBorderColor = XColors.Ink,
                    checkedBorderColor = XColors.Ink,
                ),
        )
    }
}

@Preview(widthDp = 412)
@Composable
private fun FeedbackSettingsPreview() {
    var settings by remember { mutableStateOf(FeedbackSettings(sound = true, haptics = false)) }
    XitterTheme {
        FeedbackSettingsSection(settings, { settings = it }, Modifier.nightBackground().padding(16.dp))
    }
}
