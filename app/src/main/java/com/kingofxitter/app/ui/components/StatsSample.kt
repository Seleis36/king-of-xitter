package com.kingofxitter.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.kingofxitter.engine.GameFixtures
import com.kingofxitter.engine.model.LeaderId
import com.kingofxitter.engine.model.PlayerId
import com.kingofxitter.engine.model.PlayerState

/** The kit's STATS and CERTIFICATION blocks plus bot panels. Also shown by the debug design gallery. */
@Composable
internal fun StatsKitSample(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        XPanel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(stringResource(R.string.kit_followers_label), style = XType.Label, color = XColors.Followers)
                FollowersBar(followers = 7, maxFollowers = 10)
                Text(stringResource(R.string.kit_characters_label), style = XType.Label, color = XColors.Characters)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    listOf(0, 60, 140, 300).forEach { CharactersRing(it) }
                }
                Text(stringResource(R.string.kit_mandates_label), style = XType.Label, color = XColors.Mandates)
                MandateCounter(12, showGoal = true)
                listOf(3, 8, 16).forEach { MandateProgress(it, Modifier.fillMaxWidth()) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf(0, 8, 16).forEach { CertificationBadge(it, size = 44.dp) }
        }
        BotPanelRow(designBots, mainCharacter = PlayerId("trumpet"))
        BotPanelRow(
            designBots.map { if (it.id.value == "kim") it.copy(followers = 0) else it },
            mainCharacter = null,
            acting = PlayerId("poutine"),
        )
    }
}

/** The three opponents from `game-board-your-roll.html`. */
private val designBots =
    listOf(
        PlayerState(PlayerId("trumpet"), LeaderId("trumpet"), followers = 5, characters = 30, mandates = 13),
        PlayerState(PlayerId("poutine"), LeaderId("poutine"), followers = 9, characters = 120, mandates = 4),
        PlayerState(PlayerId("kim"), LeaderId("kim"), followers = 3, characters = 20, mandates = 16),
    )

@Composable
private fun BotPanelRow(
    bots: List<PlayerState>,
    mainCharacter: PlayerId?,
    acting: PlayerId? = null,
) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        bots.forEach { bot ->
            BotPanel(
                bot,
                Modifier.weight(1f),
                highlight =
                    when (bot.id) {
                        mainCharacter -> BotPanelHighlight.MainCharacter
                        acting -> BotPanelHighlight.Acting
                        else -> BotPanelHighlight.None
                    },
            )
        }
    }
}

@Preview(widthDp = 412, heightDp = 820)
@Composable
private fun StatsKitPreview() {
    XitterTheme { StatsKitSample(Modifier.nightBackground()) }
}

@Preview(name = "Bot panels · yourRoll fixture", widthDp = 412)
@Composable
private fun BotPanelsYourRollPreview() {
    val state = GameFixtures.yourRoll
    XitterTheme {
        Column(Modifier.nightBackground().padding(16.dp)) {
            BotPanelRow(state.players.drop(1), state.xitterPlayerId, acting = state.currentPlayerId)
        }
    }
}

@Preview(name = "Bot panels · defeat fixture", widthDp = 412)
@Composable
private fun BotPanelsDefeatPreview() {
    val state = GameFixtures.defeat
    XitterTheme {
        Column(Modifier.nightBackground().padding(16.dp)) {
            BotPanelRow(state.players.drop(1), state.xitterPlayerId)
        }
    }
}
