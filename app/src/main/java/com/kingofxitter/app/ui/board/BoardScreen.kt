package com.kingofxitter.app.ui.board

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingofxitter.app.R
import com.kingofxitter.app.ui.components.BotPanel
import com.kingofxitter.app.ui.components.BotPanelHighlight
import com.kingofxitter.app.ui.components.XButton
import com.kingofxitter.app.ui.components.XButtonStyle
import com.kingofxitter.app.ui.components.XChip
import com.kingofxitter.app.ui.components.XIcon
import com.kingofxitter.app.ui.components.XIconButton
import com.kingofxitter.app.ui.components.XIconImage
import com.kingofxitter.app.ui.components.XPanel
import com.kingofxitter.app.ui.components.XTag
import com.kingofxitter.app.ui.components.leaderVisual
import com.kingofxitter.app.ui.motion.BoardEffect
import com.kingofxitter.app.ui.motion.EffectNumbers
import com.kingofxitter.app.ui.motion.latest
import com.kingofxitter.app.ui.motion.pulse
import com.kingofxitter.app.ui.motion.shakeOn
import com.kingofxitter.app.ui.motion.slamOn
import com.kingofxitter.app.ui.motion.stampOn
import com.kingofxitter.app.ui.theme.XColors
import com.kingofxitter.app.ui.theme.XType
import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.CardId
import com.kingofxitter.engine.model.DecisionOption
import com.kingofxitter.engine.model.DieId
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.PlayerId
import com.kingofxitter.engine.model.TurnPhase

/** Navigation and side effects the board hands back to its host. */
data class BoardCallbacks(
    val onOpenTimeline: () -> Unit = {},
    val onOpenStandings: () -> Unit = {},
    val onOpenShop: () -> Unit = {},
    /** Called instead of resolving directly when the roll has tweets with a target. Null resolves at once. */
    val onCompose: (() -> Unit)? = null,
    val onIncomingTweet: () -> Unit = {},
    val onMatchFinished: () -> Unit = {},
)

/**
 * Game board (`design/screens/game-board-your-roll.html`). Stateless apart from which dice the
 * human has picked to keep; every rule change goes through [onAction].
 *
 * [mainCharacterLine] is the Main Character's latest tweet; V1's TweetWriter supplies it later.
 */
@Composable
fun BoardScreen(
    state: GameState,
    humanId: PlayerId,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier,
    callbacks: BoardCallbacks = BoardCallbacks(),
    mainCharacterLine: String? = null,
    policyName: (CardId) -> String = ::fallbackPolicyName,
    effects: List<BoardEffect> = emptyList(),
) {
    val mode = BoardMode.of(state, humanId)
    val human = state.players.first { it.id == humanId }
    val mainCharacter = state.players.firstOrNull { it.id == state.xitterPlayerId }
    val preview = if (state.dice.isEmpty()) null else RollPreview.of(state, state.currentPlayerId)
    // Dice picked to keep. Starts from the engine's kept flags and resets on every new roll.
    var selected by rememberSaveable(state.turnNumber, state.rollNumber, saver = DieIdSetSaver) {
        mutableStateOf(
            state.dice
                .filter { it.isKept }
                .map { it.id }
                .toSet(),
        )
    }
    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TopBar(state, humanId, callbacks)
            BotRow(state, humanId, effects)
            XitterStage(
                modifier = Modifier.slamOn(effects.latest(BoardEffect.Kind.NewMainCharacter, state.xitterPlayerId)),
                mainCharacter = mainCharacter,
                isYou = mainCharacter?.id == humanId,
                line =
                    mainCharacterLine
                        ?: stringResource(if (mainCharacter?.id == humanId) R.string.board_mc_line_you else R.string.board_mc_line_default),
            )
            Box(Modifier.shakeOn(effects.latest(BoardEffect.Kind.Damage, humanId))) {
                YourPanel(human, policyName = policyName)
                EffectNumbers(effects.filter { it.playerId == humanId }, Modifier.align(Alignment.TopCenter))
            }
            DiceHeader(state, humanId, mode)
            DiceTray(
                state,
                selected =
                    if (mode is BoardMode.Rolling) {
                        selected
                    } else {
                        state.dice
                            .filter { it.isKept }
                            .map { it.id }
                            .toSet()
                    },
                onToggle =
                    if (mode is BoardMode.Rolling && mode.canReroll) {
                        { id, keep -> selected = if (keep) selected + id else selected - id }
                    } else {
                        null
                    },
            )
            if (preview != null && state.phase == TurnPhase.ROLLING) {
                val label =
                    if (state.currentPlayerId == humanId) {
                        stringResource(R.string.board_if_you_stop_now)
                    } else {
                        stringResource(R.string.board_if_they_stop_now, rollerName(state))
                    }
                StopNowPreview(state, preview, label)
            }
            PowerUpShelf(state, mode, callbacks.onOpenShop)
        }
        Controls(
            state = state,
            mode = mode,
            humanId = humanId,
            selected = selected,
            preview = preview,
            onAction = onAction,
            callbacks = callbacks,
            modifier = Modifier.padding(bottom = 16.dp),
        )
    }
}

@Composable
private fun TopBar(
    state: GameState,
    humanId: PlayerId,
    callbacks: BoardCallbacks,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            XChip(stringResource(R.string.board_turn, state.turnNumber), textStyle = XType.DisplaySmall, minHeight = 34.dp)
            if (state.result == null) {
                if (state.currentPlayerId == humanId) {
                    XTag(stringResource(R.string.board_your_turn), fill = XColors.Characters, modifier = Modifier.pulse())
                } else {
                    val actor = state.players.first { it.id == state.currentPlayerId }
                    XTag(
                        stringResource(R.string.board_their_turn, stringResource(leaderVisual(actor.leaderId).shortName)),
                        fill = XColors.Paper,
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            XIconButton(XIcon.Timeline, stringResource(R.string.action_timeline), callbacks.onOpenTimeline)
            XIconButton(XIcon.Standings, stringResource(R.string.action_standings), callbacks.onOpenStandings)
        }
    }
}

@Composable
private fun BotRow(
    state: GameState,
    humanId: PlayerId,
    effects: List<BoardEffect>,
) {
    val humanIndex = state.players.indexOfFirst { it.id == humanId }
    // Opponents in turn order after you.
    val bots = (1 until state.players.size).map { state.players[(humanIndex + it) % state.players.size] }
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        bots.forEach { bot ->
            Box(Modifier.weight(1f).shakeOn(effects.latest(BoardEffect.Kind.Damage, bot.id))) {
                BotPanel(
                    bot,
                    highlight =
                        when {
                            bot.id == state.xitterPlayerId -> BotPanelHighlight.MainCharacter
                            bot.id == state.currentPlayerId && state.result == null -> BotPanelHighlight.Acting
                            else -> BotPanelHighlight.None
                        },
                    stampModifier = Modifier.stampOn(effects.latest(BoardEffect.Kind.Suspended, bot.id)),
                )
                EffectNumbers(effects.filter { it.playerId == bot.id }, Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun rollerName(state: GameState): String =
    stringResource(leaderVisual(state.players.first { it.id == state.currentPlayerId }.leaderId).shortName)

@Composable
private fun DiceHeader(
    state: GameState,
    humanId: PlayerId,
    mode: BoardMode,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        when (mode) {
            is BoardMode.Rolling -> RollCounter(mode.rollNumber, mode.rollLimit)
            BoardMode.ReadyToRoll -> Text(stringResource(R.string.board_ready_to_roll), style = XType.DisplayMedium, color = XColors.Paper)
            else ->
                Text(
                    if (state.currentPlayerId == humanId) {
                        stringResource(R.string.board_your_dice)
                    } else {
                        stringResource(R.string.board_their_dice, rollerName(state))
                    },
                    style = XType.DisplayMedium,
                    color = XColors.Paper,
                )
        }
        if (mode is BoardMode.Rolling && mode.canReroll) {
            Text(stringResource(R.string.board_tap_to_keep), style = XType.Caption, color = XColors.Muted)
        }
    }
}

@Composable
private fun PowerUpShelf(
    state: GameState,
    mode: BoardMode,
    onOpenShop: () -> Unit,
) {
    val shelf = pluralStringResource(R.plurals.board_shelf_count, state.shopCardIds.size, state.shopCardIds.size)
    val hint = stringResource(if (mode == BoardMode.Shopping) R.string.board_shelf_open else R.string.board_shelf_after_resolve)
    XPanel(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.board_open_shop), onClick = onOpenShop),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            XIconImage(XIcon.Characters, contentDescription = null, size = 22.dp)
            Text(
                stringResource(R.string.board_power_ups),
                style = XType.DisplaySmall.copy(fontSize = 15.sp),
                color = XColors.Paper,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text("$shelf · $hint", style = XType.Caption, color = XColors.Muted, maxLines = 1)
        }
    }
}

@Composable
private fun Controls(
    state: GameState,
    mode: BoardMode,
    humanId: PlayerId,
    selected: Set<DieId>,
    preview: RollPreview?,
    onAction: (Action) -> Unit,
    callbacks: BoardCallbacks,
    modifier: Modifier = Modifier,
) {
    val buttonText = XType.DisplayButton.copy(fontSize = 21.sp)
    when (mode) {
        BoardMode.ReadyToRoll ->
            XButton(
                stringResource(R.string.board_roll),
                onClick = { onAction(Action.RollDice(humanId)) },
                modifier = modifier.fillMaxWidth().pulse(),
                icon = XIcon.Reroll,
                textStyle = buttonText,
            )
        is BoardMode.Rolling ->
            Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                XButton(
                    stringResource(R.string.board_reroll),
                    onClick = { onAction(Action.RollDice(humanId, keptDice = selected)) },
                    modifier = Modifier.weight(1f),
                    style = XButtonStyle.Secondary,
                    enabled = mode.canReroll && selected.size < state.dice.size,
                    icon = XIcon.Reroll,
                    textStyle = buttonText,
                )
                XButton(
                    stringResource(R.string.board_resolve),
                    onClick = {
                        val compose = callbacks.onCompose
                        if (compose != null && preview != null && preview.tweets > 0 && preview.targets != TweetTargets.Nobody) {
                            compose()
                        } else {
                            onAction(Action.ResolveRoll(humanId))
                        }
                    },
                    modifier = Modifier.weight(1f).pulse(),
                    textStyle = buttonText,
                )
            }
        BoardMode.Shopping ->
            Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                XButton(
                    stringResource(R.string.board_power_ups),
                    onClick = callbacks.onOpenShop,
                    modifier = Modifier.weight(1f),
                    style = XButtonStyle.Secondary,
                    textStyle = buttonText,
                )
                XButton(
                    stringResource(R.string.board_end_turn),
                    onClick = { onAction(Action.EndTurn(humanId)) },
                    modifier = Modifier.weight(1f),
                    textStyle = buttonText,
                )
            }
        is BoardMode.IncomingTweet ->
            XButton(
                stringResource(R.string.board_incoming),
                onClick = callbacks.onIncomingTweet,
                modifier = modifier.fillMaxWidth(),
                style = XButtonStyle.Hot,
                textStyle = buttonText,
            )
        is BoardMode.Deciding -> DecisionControls(mode, onAction, modifier)
        is BoardMode.Watching -> {
            val actor = state.players.first { it.id == mode.actorId }
            val name = stringResource(leaderVisual(actor.leaderId).shortName)
            XButton(
                // A bot answering a decision on someone else's turn is deciding, not rolling.
                if (mode.actorId == state.currentPlayerId) {
                    stringResource(R.string.board_watching, name)
                } else {
                    stringResource(R.string.board_deciding, name)
                },
                onClick = {},
                modifier = modifier.fillMaxWidth(),
                enabled = false,
                textStyle = buttonText,
            )
        }
        BoardMode.Finished ->
            XButton(
                stringResource(R.string.board_see_results),
                onClick = callbacks.onMatchFinished,
                modifier = modifier.fillMaxWidth(),
                textStyle = buttonText,
            )
    }
}

/**
 * Generic prompt for decisions other than stay/log off, such as optional power-up abilities.
 * C9 replaces this with card-specific prompts.
 */
@Composable
private fun DecisionControls(
    mode: BoardMode.Deciding,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    XPanel(modifier.fillMaxWidth(), accent = XColors.Mandates) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.board_decision_prompt), style = XType.DisplaySmall, color = XColors.Paper)
            mode.decision.options.forEach { option ->
                XButton(
                    decisionOptionLabel(option),
                    onClick = { onAction(Action.ChooseDecisionOption(mode.decision.playerId, mode.decision.id, option.id)) },
                    modifier = Modifier.fillMaxWidth(),
                    style = if (option.action is Action.DeclineAbility) XButtonStyle.Secondary else XButtonStyle.Primary,
                    minHeight = 48.dp,
                    textStyle = XType.DisplaySmall,
                )
            }
        }
    }
}

@Composable
private fun decisionOptionLabel(option: DecisionOption): String =
    when (option.action) {
        is Action.DeclineAbility -> stringResource(R.string.board_decision_skip)
        is Action.ActivateAbility -> stringResource(R.string.board_decision_use)
        else -> stringResource(R.string.board_decision_ok)
    }

private val DieIdSetSaver =
    listSaver<MutableState<Set<DieId>>, Int>(
        save = { state -> state.value.map { it.value } },
        restore = { values -> mutableStateOf(values.map(::DieId).toSet()) },
    )
