package com.kingofxitter.app.ui.board

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.sizeIn
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingofxitter.app.R
import com.kingofxitter.app.ui.components.BotPanel
import com.kingofxitter.app.ui.components.BotPanelHighlight
import com.kingofxitter.app.ui.components.Die
import com.kingofxitter.app.ui.components.DieSize
import com.kingofxitter.app.ui.components.MIN_TOUCH_TARGET
import com.kingofxitter.app.ui.components.XAvatar
import com.kingofxitter.app.ui.components.XButton
import com.kingofxitter.app.ui.components.XButtonStyle
import com.kingofxitter.app.ui.components.XChip
import com.kingofxitter.app.ui.components.XIcon
import com.kingofxitter.app.ui.components.XIconButton
import com.kingofxitter.app.ui.components.XIconImage
import com.kingofxitter.app.ui.components.XPanel
import com.kingofxitter.app.ui.components.XTag
import com.kingofxitter.app.ui.components.label
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
import com.kingofxitter.cards.CardCatalog
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
    // C9: the power-up decision the human is answering, and the die picked for it on the tray.
    val prompt = (mode as? BoardMode.Deciding)?.let { AbilityPrompt.of(it.decision) }
    var pickedDie by rememberSaveable((mode as? BoardMode.Deciding)?.decision?.id?.value) { mutableStateOf<Int?>(null) }
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
            Box(
                Modifier
                    .shakeOn(effects.latest(BoardEffect.Kind.Damage, humanId))
                    .stampOn(effects.latest(BoardEffect.Kind.Suspended, humanId)),
            ) {
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
            prompt = prompt,
            pickedDie = pickedDie,
            onPickDie = { pickedDie = it?.value },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TopBar(
    state: GameState,
    humanId: PlayerId,
    callbacks: BoardCallbacks,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        // The turn tag wraps under the turn chip on narrow phones and at large font scales.
        FlowRow(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            XChip(stringResource(R.string.board_turn, state.turnNumber), textStyle = XType.DisplaySmall, minHeight = 34.dp)
            if (state.result == null) {
                val yours = state.currentPlayerId == humanId
                val actor = state.players.first { it.id == state.currentPlayerId }
                // One tag whose text changes, so TalkBack announces each new turn.
                XTag(
                    if (yours) {
                        stringResource(R.string.board_your_turn)
                    } else {
                        stringResource(R.string.board_their_turn, stringResource(leaderVisual(actor.leaderId).shortName))
                    },
                    fill = if (yours) XColors.Characters else XColors.Paper,
                    modifier =
                        Modifier
                            .semantics { liveRegion = LiveRegionMode.Polite }
                            .then(if (yours) Modifier.pulse() else Modifier),
                )
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

@OptIn(ExperimentalLayoutApi::class)
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
            // 10dp top and bottom keep the shelf a 48dp touch target.
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            XIconImage(XIcon.Characters, contentDescription = null, size = 22.dp)
            // The hint moves under the title when the row is too narrow.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.board_power_ups),
                    style = XType.DisplaySmall.copy(fontSize = 15.sp),
                    color = XColors.Paper,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Text("$shelf · $hint", style = XType.Caption, color = XColors.Muted)
            }
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
    prompt: AbilityPrompt? = null,
    pickedDie: Int? = null,
    onPickDie: (DieId?) -> Unit = {},
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
        is BoardMode.Deciding ->
            DecisionControls(
                mode,
                prompt ?: AbilityPrompt.of(mode.decision),
                state,
                pickedDie?.let(::DieId),
                onPickDie,
                onAction,
                modifier,
            )
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
 * C9: the human's power-up decision. The card names itself and asks its question; the answer
 * matches the [prompt]: a die picked from the dice shown in the panel, then a face from a row of
 * dice, a leader from their portraits, or one button per option. The dice are repeated here
 * because the board's tray can be scrolled out of sight. SKIP is always on screen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DecisionControls(
    mode: BoardMode.Deciding,
    prompt: AbilityPrompt,
    state: GameState,
    pickedDie: DieId?,
    onPickDie: (DieId?) -> Unit,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier,
) {
    val decision = mode.decision
    val pickTray: @Composable (Set<DieId>) -> Unit = { pickable ->
        DiceTray(
            state,
            selected = setOfNotNull(pickedDie),
            onToggle = { id, pick -> onPickDie(if (pick) id else null) },
            pickable = pickable,
        )
    }
    val choose = { option: DecisionOption -> onAction(Action.ChooseDecisionOption(decision.playerId, decision.id, option.id)) }
    val buttonText = XType.DisplaySmall
    XPanel(modifier.fillMaxWidth(), accent = XColors.Mandates) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                decisionPrompt(decision),
                style = buttonText,
                color = XColors.Paper,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            decisionQuestion(decision)?.let { Text(it, style = XType.Body, color = XColors.Muted) }
            when (prompt) {
                is AbilityPrompt.PickDie -> {
                    val use = pickedDie?.let(prompt.byDie::get)
                    if (use == null) Text(stringResource(R.string.board_decision_pick_die), style = XType.Caption, color = XColors.Muted)
                    pickTray(prompt.byDie.keys)
                    DecisionButtons(
                        primary = optionLabel(prompt.byDie.values.first(), R.string.board_decision_use),
                        onPrimary = use?.let { { choose(it) } },
                        decline = prompt.decline,
                        onDecline = choose,
                        textStyle = buttonText,
                    )
                }
                is AbilityPrompt.PickDieThenFace -> {
                    val faces = pickedDie?.let(prompt.byDie::get)
                    if (faces == null) {
                        Text(stringResource(R.string.board_decision_pick_die_then_face), style = XType.Caption, color = XColors.Muted)
                    }
                    pickTray(prompt.byDie.keys)
                    if (faces != null) {
                        Text(stringResource(R.string.board_decision_pick_face), style = XType.Caption, color = XColors.Muted)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            faces.forEach { (face, option) ->
                                val faceName = stringResource(face.label())
                                Box(
                                    Modifier
                                        .sizeIn(minWidth = MIN_TOUCH_TARGET, minHeight = MIN_TOUCH_TARGET)
                                        .clickable(
                                            role = Role.Button,
                                            onClickLabel = stringResource(R.string.board_decision_face, faceName),
                                        ) { choose(option) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Die(face = face, size = DieSize.Small)
                                }
                            }
                        }
                    }
                    DecisionButtons(primary = null, onPrimary = null, decline = prompt.decline, onDecline = choose, textStyle = buttonText)
                }
                is AbilityPrompt.PickLeader -> {
                    Text(stringResource(R.string.board_decision_pick_leader), style = XType.Caption, color = XColors.Muted)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        prompt.byLeader.forEach { (playerId, option) ->
                            val leader = state.players.firstOrNull { it.id == playerId } ?: return@forEach
                            val name = stringResource(leaderVisual(leader.leaderId).shortName)
                            Column(
                                Modifier
                                    .sizeIn(minWidth = MIN_TOUCH_TARGET, minHeight = MIN_TOUCH_TARGET)
                                    .clickable(role = Role.Button, onClickLabel = name) { choose(option) },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                XAvatar(leader.leaderId, size = 48.dp)
                                Text(name, style = XType.Label, color = XColors.Paper)
                            }
                        }
                    }
                    DecisionButtons(primary = null, onPrimary = null, decline = prompt.decline, onDecline = choose, textStyle = buttonText)
                }
                is AbilityPrompt.Choose -> {
                    val single = prompt.options.singleOrNull()
                    if (single != null) {
                        DecisionButtons(
                            primary = decisionOptionLabel(single, state),
                            onPrimary = { choose(single) },
                            decline = prompt.decline,
                            onDecline = choose,
                            textStyle = buttonText,
                        )
                    } else {
                        Column(
                            Modifier.heightIn(max = 200.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            prompt.options.forEach { option ->
                                XButton(
                                    decisionOptionLabel(option, state),
                                    onClick = { choose(option) },
                                    modifier = Modifier.fillMaxWidth(),
                                    minHeight = 48.dp,
                                    textStyle = buttonText,
                                )
                            }
                        }
                        DecisionButtons(
                            primary = null,
                            onPrimary = null,
                            decline = prompt.decline,
                            onDecline = choose,
                            textStyle = buttonText,
                        )
                    }
                }
            }
        }
    }
}

/** SKIP and, when there is one, the button that uses the ability. A null [onPrimary] shows it disabled. */
@Composable
private fun DecisionButtons(
    primary: String?,
    onPrimary: (() -> Unit)?,
    decline: DecisionOption?,
    onDecline: (DecisionOption) -> Unit,
    textStyle: TextStyle,
) {
    if (primary == null && decline == null) return
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (decline != null) {
            XButton(
                stringResource(R.string.board_decision_skip),
                onClick = { onDecline(decline) },
                modifier = Modifier.weight(1f),
                style = XButtonStyle.Secondary,
                minHeight = 48.dp,
                textStyle = textStyle,
            )
        }
        if (primary != null) {
            XButton(
                primary,
                onClick = { onPrimary?.invoke() },
                modifier = Modifier.weight(1f),
                enabled = onPrimary != null,
                minHeight = 48.dp,
                textStyle = textStyle,
            )
        }
    }
}

/** The card's own label for [option] (its `labelKey`), or [fallback]. */
@Composable
private fun optionLabel(
    option: DecisionOption,
    fallback: Int,
): String = stringByName(option.labelKey) ?: stringResource(fallback)

@Composable
private fun decisionOptionLabel(
    option: DecisionOption,
    state: GameState,
): String =
    when (val action = option.action) {
        is Action.DeclineAbility -> stringResource(R.string.board_decision_skip)
        is Action.ActivateAbility -> stringByName(option.labelKey) ?: abilityLabel(action, state)
        is Action.BuyPowerUp ->
            CardCatalog.byId[action.cardId]?.let { stringResource(R.string.board_decision_buy_cost, cardName(action.cardId), it.cost) }
                ?: stringResource(R.string.board_decision_buy, cardName(action.cardId))
        else -> stringResource(R.string.board_decision_ok)
    }

private val DieIdSetSaver =
    listSaver<MutableState<Set<DieId>>, Int>(
        save = { state -> state.value.map { it.value } },
        restore = { values -> mutableStateOf(values.map(::DieId).toSet()) },
    )
