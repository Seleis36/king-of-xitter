package com.kingofxitter.app.ui.front

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kingofxitter.app.R
import com.kingofxitter.app.ui.components.OutlinedText
import com.kingofxitter.app.ui.components.XButton
import com.kingofxitter.app.ui.components.XChip
import com.kingofxitter.app.ui.components.XIcon
import com.kingofxitter.app.ui.components.XIconButton
import com.kingofxitter.app.ui.components.XPanel
import com.kingofxitter.app.ui.theme.XColors
import com.kingofxitter.app.ui.theme.XType
import com.kingofxitter.app.ui.theme.XitterTheme
import com.kingofxitter.app.ui.theme.nightBackground

/**
 * Settings: sound and haptics (U9's flags), animation and bot speed, and the rules toggles from
 * ROADMAP §1. Changes are stored as they are made, so [onDone] is only a way out.
 */
@Composable
fun SettingsScreen(
    settings: GameSettings,
    onChange: (GameSettings) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            XIconButton(XIcon.Back, stringResource(R.string.settings_done), onDone)
            OutlinedText(
                stringResource(R.string.settings_title),
                XType.DisplayLarge,
                XColors.Mandates,
                modifier = Modifier.semantics { heading() },
            )
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Group(R.string.settings_feedback_section) {
                ToggleRow(stringResource(R.string.feedback_sound), settings.feedback.sound) {
                    onChange(settings.copy(feedback = settings.feedback.copy(sound = it)))
                }
                ToggleRow(stringResource(R.string.feedback_haptics), settings.feedback.haptics) {
                    onChange(settings.copy(feedback = settings.feedback.copy(haptics = it)))
                }
            }
            Group(R.string.settings_speed_section) {
                ToggleRow(stringResource(R.string.settings_animations), settings.animations) {
                    onChange(settings.copy(animations = it))
                }
                SpeedRow(R.string.settings_animation_speed, settings.animationSpeed, settings.animations) {
                    onChange(settings.copy(animationSpeed = it))
                }
                SpeedRow(R.string.settings_bot_speed, settings.botSpeed, enabled = true) {
                    onChange(settings.copy(botSpeed = it))
                }
            }
            Group(R.string.settings_rules_section) {
                ToggleRow(stringResource(R.string.settings_character_cap), settings.characterCap) {
                    onChange(settings.copy(characterCap = it))
                }
                Text(stringResource(R.string.settings_character_cap_help), style = XType.Body, color = XColors.Muted)
                Text(stringResource(R.string.settings_next_match), style = XType.Caption, color = XColors.GhostBorder)
            }
        }
        XButton(
            stringResource(R.string.settings_done),
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            minHeight = 58.dp,
        )
    }
}

@Composable
private fun Group(
    titleRes: Int,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            stringResource(titleRes),
            style = XType.Label,
            color = XColors.Muted,
            modifier = Modifier.semantics { heading() },
        )
        XPanel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                content()
            }
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

/** Three chips acting as one choice. The selected chip is yellow, as the design's tabs are. */
@Composable
private fun SpeedRow(
    labelRes: Int,
    choice: SpeedChoice,
    enabled: Boolean,
    onChoose: (SpeedChoice) -> Unit,
) {
    val label = stringResource(labelRes)
    Column(Modifier.padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = XType.DisplaySmall, color = if (enabled) XColors.Paper else XColors.Muted)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SpeedChoice.entries.forEach { option ->
                val selected = option == choice
                XChip(
                    stringResource(option.label),
                    modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                    fill = if (selected) XColors.Mandates else XColors.Paper,
                    onClick = if (enabled) ({ onChoose(option) }) else null,
                    // The row reads as one setting, so each chip says which setting it changes.
                    contentDescription = stringResource(R.string.settings_speed_choice, label, stringResource(option.label)),
                )
            }
        }
    }
}

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun SettingsPreview() {
    var settings by remember { mutableStateOf(GameSettings()) }
    XitterTheme { SettingsScreen(settings, { settings = it }, onDone = {}, modifier = Modifier.nightBackground()) }
}

@Preview(widthDp = 320, heightDp = 640, fontScale = 1.4f)
@Composable
private fun SettingsSmallPreview() {
    var settings by remember { mutableStateOf(GameSettings(characterCap = true, animations = false)) }
    XitterTheme { SettingsScreen(settings, { settings = it }, onDone = {}, modifier = Modifier.nightBackground()) }
}
