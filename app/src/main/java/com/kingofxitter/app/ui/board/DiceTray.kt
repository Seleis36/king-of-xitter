package com.kingofxitter.app.ui.board

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingofxitter.app.R
import com.kingofxitter.app.ui.components.Die
import com.kingofxitter.app.ui.components.DieSize
import com.kingofxitter.app.ui.components.XChip
import com.kingofxitter.app.ui.components.XIcon
import com.kingofxitter.app.ui.components.leaderVisual
import com.kingofxitter.app.ui.theme.XColors
import com.kingofxitter.app.ui.theme.XType
import com.kingofxitter.app.ui.theme.sticker
import com.kingofxitter.engine.model.DieId
import com.kingofxitter.engine.model.GameState

/** "ROLL 2 OF 3" with one pip per roll, filled for rolls used. */
@Composable
fun RollCounter(
    rollNumber: Int,
    rollLimit: Int,
    modifier: Modifier = Modifier,
) {
    val text = stringResource(R.string.board_roll_of, rollNumber, rollLimit)
    Row(
        modifier.clearAndSetSemantics { contentDescription = text }.semantics { heading() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(text, style = XType.DisplayMedium, color = XColors.Paper, modifier = Modifier.padding(top = 3.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(rollLimit) { index ->
                Box(
                    Modifier
                        .size(11.dp)
                        .sticker(
                            fill = if (index < rollNumber) XColors.Mandates else XColors.Disabled,
                            shape = CircleShape,
                            borderWidth = 2.dp,
                            shadowOffset = DpOffset.Zero,
                        ),
                )
            }
        }
    }
}

/**
 * The six (or more) dice. Before the first roll, empty slots stand in. [selected] dice are shown
 * kept; [onToggle] makes them tappable. Dice rerolled on the latest roll tumble.
 */
@Composable
fun DiceTray(
    state: GameState,
    selected: Set<DieId>,
    modifier: Modifier = Modifier,
    onToggle: ((DieId, Boolean) -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().padding(top = 8.dp, bottom = 22.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.dice.isEmpty()) {
            repeat(state.turnDiceCount ?: BASE_DICE) { EmptyDieSlot() }
        } else {
            val size = if (state.dice.size > BASE_DICE) DieSize.Small else DieSize.Default
            state.dice.forEach { die ->
                Die(
                    face = die.face,
                    size = size,
                    kept = die.id in selected,
                    tumbleKey = if (die.isKept) null else state.rollNumber,
                    onToggleKept = onToggle?.let { toggle -> { keep -> toggle(die.id, keep) } },
                )
            }
        }
    }
}

@Composable
private fun EmptyDieSlot() {
    Box(
        Modifier
            .size(54.dp)
            .sticker(
                fill = XColors.Panel,
                shape = RoundedCornerShape(15.dp),
                borderColor = XColors.Disabled,
                shadowOffset = DpOffset.Zero,
            ),
    )
}

/** "IF YOU STOP NOW" line: what resolving the current dice would do. [label] names who resolves. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StopNowPreview(
    state: GameState,
    preview: RollPreview,
    label: String,
    modifier: Modifier = Modifier,
) {
    val chipText = XType.Chip.copy(fontSize = 11.5.sp)
    FlowRow(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = XType.Label, color = XColors.Muted)
        if (preview.tweets > 0) {
            val tweets = pluralStringResource(R.plurals.board_preview_tweets, preview.tweets, preview.tweets)
            val target =
                when (val targets = preview.targets) {
                    TweetTargets.Nobody -> stringResource(R.string.board_preview_target_nobody)
                    is TweetTargets.Everyone -> stringResource(R.string.board_preview_target_everyone)
                    is TweetTargets.MainCharacter ->
                        stringResource(leaderVisual(state.players.first { it.id == targets.playerId }.leaderId).shortName)
                }
            XChip(
                stringResource(R.string.board_preview_tweet_target, tweets, target),
                icon = XIcon.Tweet,
                fill = XColors.DamageTint,
                textStyle = chipText,
                minHeight = 26.dp,
            )
        }
        if (preview.setMandates > 0) {
            XChip(
                stringResource(R.string.board_preview_plus, preview.setMandates),
                icon = XIcon.Mandates,
                textStyle = chipText,
                minHeight = 26.dp,
            )
        }
        if (preview.entersXitter) {
            XChip(
                stringResource(R.string.board_preview_take_xitter),
                icon = XIcon.Crown,
                fill = XColors.Mandates,
                textStyle = chipText,
                minHeight = 26.dp,
            )
        }
        if (preview.followersHealed > 0) {
            XChip(
                stringResource(R.string.board_preview_heal, preview.followersHealed),
                icon = XIcon.Like,
                textStyle = chipText,
                minHeight = 26.dp,
            )
        }
        if (preview.likesBlocked) {
            XChip(stringResource(R.string.board_preview_likes_blocked), icon = XIcon.LikeLost, textStyle = chipText, minHeight = 26.dp)
        }
        if (preview.characters > 0) {
            XChip(
                stringResource(R.string.board_preview_plus, preview.characters),
                icon = XIcon.Characters,
                fill = XColors.CharactersTint,
                textStyle = chipText,
                minHeight = 26.dp,
            )
        }
        if (preview.isEmpty) Text(stringResource(R.string.board_preview_nothing), style = XType.Caption, color = XColors.Muted)
    }
}

private const val BASE_DICE = 6
