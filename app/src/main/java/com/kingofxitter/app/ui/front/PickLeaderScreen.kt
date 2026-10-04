package com.kingofxitter.app.ui.front

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kingofxitter.app.R
import com.kingofxitter.app.ui.components.OutlinedText
import com.kingofxitter.app.ui.components.XAvatar
import com.kingofxitter.app.ui.components.XButton
import com.kingofxitter.app.ui.components.XButtonStyle
import com.kingofxitter.app.ui.components.XChip
import com.kingofxitter.app.ui.components.XIcon
import com.kingofxitter.app.ui.components.XIconButton
import com.kingofxitter.app.ui.components.XIconImage
import com.kingofxitter.app.ui.components.XPanel
import com.kingofxitter.app.ui.components.XTag
import com.kingofxitter.app.ui.components.leaderVisual
import com.kingofxitter.app.ui.theme.XColors
import com.kingofxitter.app.ui.theme.XType
import com.kingofxitter.app.ui.theme.XitterTheme
import com.kingofxitter.app.ui.theme.nightBackground
import com.kingofxitter.bots.BotDifficulty
import com.kingofxitter.content.Leader
import com.kingofxitter.content.LeaderRoster
import com.kingofxitter.engine.model.LeaderId

/** The label and the one-line description for a difficulty. */
@StringRes
fun BotDifficulty.label(): Int =
    when (this) {
        BotDifficulty.INTERN -> R.string.difficulty_intern
        BotDifficulty.REPLY_GUY -> R.string.difficulty_reply_guy
        BotDifficulty.TROLL_FARM -> R.string.difficulty_troll_farm
    }

/** The design's explanation under the difficulty chips. */
@StringRes
fun BotDifficulty.help(): Int =
    when (this) {
        BotDifficulty.INTERN -> R.string.difficulty_intern_help
        BotDifficulty.REPLY_GUY -> R.string.difficulty_reply_guy_help
        BotDifficulty.TROLL_FARM -> R.string.difficulty_troll_farm_help
    }

/**
 * Pick your leader (`design/screens/pick-your-leader.html`): the six leaders with their voices,
 * the bot difficulty, and LET'S GO. The three opponents are drawn from the other five when the
 * match starts, so this screen only chooses your own leader.
 */
@Composable
fun PickLeaderScreen(
    onStart: (LeaderId, BotDifficulty) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialLeader: LeaderId = LeaderRoster.macaron.id,
    initialDifficulty: BotDifficulty = BotDifficulty.REPLY_GUY,
) {
    var leader by rememberSaveable { mutableStateOf(initialLeader.value) }
    var difficulty by rememberSaveable { mutableStateOf(initialDifficulty.name) }
    val chosen = LeaderId(leader)
    val chosenDifficulty = BotDifficulty.valueOf(difficulty)

    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            XIconButton(XIcon.Back, stringResource(R.string.route_title), onBack)
            Column {
                OutlinedText(
                    stringResource(R.string.pick_title),
                    XType.DisplayLarge,
                    XColors.Mandates,
                    modifier = Modifier.semantics { heading() },
                )
                Text(stringResource(R.string.pick_subtitle), style = XType.Caption, color = XColors.Muted)
            }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            LeaderRoster.all.forEach { entry ->
                LeaderCard(entry, selected = entry.id == chosen, onSelect = { leader = entry.id.value })
            }
            DifficultySection(chosenDifficulty) { difficulty = it.name }
        }
        XButton(
            stringResource(R.string.pick_go),
            onClick = { onStart(chosen, chosenDifficulty) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            style = XButtonStyle.Hot,
            icon = XIcon.Play,
            minHeight = 64.dp,
        )
    }
}

@Composable
private fun LeaderCard(
    leader: Leader,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val visual = leaderVisual(leader.id)
    val name = stringResource(visual.shortName)
    // TalkBack reads "Macaron selected" rather than only announcing a checked radio button.
    val selectedDescription = stringResource(R.string.pick_leader_selected, name)
    XPanel(
        Modifier
            .fillMaxWidth()
            .semantics { if (selected) stateDescription = selectedDescription }
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
        fill = if (selected) XColors.MainCharacterPanel else XColors.Panel,
        accent = if (selected) visual.color else XColors.Ink,
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            XAvatar(leader.id, size = 56.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(visual.firstName), style = XType.Caption, color = XColors.Muted)
                Text(name, style = XType.DisplaySmall, color = XColors.Paper)
                Text(stringResource(visual.voice), style = XType.Body, color = XColors.Muted)
            }
            if (selected) XTag(stringResource(R.string.pick_you), fill = visual.color)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DifficultySection(
    chosen: BotDifficulty,
    onChoose: (BotDifficulty) -> Unit,
) {
    Column(Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            XIconImage(XIcon.Bot, contentDescription = null, size = 22.dp)
            Text(
                stringResource(R.string.pick_difficulty),
                style = XType.Label,
                color = XColors.Muted,
                modifier = Modifier.semantics { heading() },
            )
            Box(Modifier.weight(1f))
            Text(stringResource(R.string.pick_random_opponents), style = XType.Caption, color = XColors.GhostBorder)
        }
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BotDifficulty.entries.forEach { option ->
                val selected = option == chosen
                XChip(
                    stringResource(option.label()),
                    modifier = Modifier.heightIn(min = 44.dp),
                    fill = if (selected) XColors.Mandates else XColors.Paper,
                    onClick = { onChoose(option) },
                )
            }
        }
        Text(
            stringResource(chosen.help()),
            style = XType.Body,
            color = XColors.Muted,
            textAlign = TextAlign.Start,
        )
    }
}

@Preview(widthDp = 412, heightDp = 915)
@Composable
private fun PickLeaderPreview() {
    XitterTheme { PickLeaderScreen({ _, _ -> }, {}, Modifier.nightBackground()) }
}

@Preview(widthDp = 320, heightDp = 640, fontScale = 1.4f)
@Composable
private fun PickLeaderSmallPreview() {
    XitterTheme {
        PickLeaderScreen({ _, _ -> }, {}, Modifier.nightBackground(), LeaderRoster.mileon.id, BotDifficulty.TROLL_FARM)
    }
}
