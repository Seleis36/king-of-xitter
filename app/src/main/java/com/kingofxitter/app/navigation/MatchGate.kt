package com.kingofxitter.app.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kingofxitter.app.R
import com.kingofxitter.app.game.GameViewModel
import com.kingofxitter.app.ui.components.XButton
import com.kingofxitter.app.ui.components.XButtonStyle
import com.kingofxitter.app.ui.components.XPanel
import com.kingofxitter.app.ui.motion.pulse
import com.kingofxitter.app.ui.theme.XColors
import com.kingofxitter.app.ui.theme.XType
import com.kingofxitter.app.ui.theme.nightBackground

/**
 * Shows [content] for the live match. Starts or resumes one if there is none, shows progress
 * while it loads, and offers a fresh match when the save can't be used. [options] are the
 * player's current settings, used for any match this gate has to start.
 */
@Composable
fun MatchGate(
    host: MatchHost,
    options: MatchOptions = MatchOptions(),
    modifier: Modifier = Modifier,
    content: @Composable (GameViewModel) -> Unit,
) {
    val session by host.session.collectAsState()
    LaunchedEffect(session == null) { if (session == null) host.resumeOrStart(options) }
    when (val current = session) {
        is MatchSession.Ready -> content(current.game)
        is MatchSession.Failed -> MatchMessage(modifier, R.string.match_failed_title, R.string.match_failed_body) { host.startNew(options) }
        MatchSession.Loading, null -> MatchMessage(modifier, R.string.match_loading, null, null)
    }
}

@Composable
private fun MatchMessage(
    modifier: Modifier,
    title: Int,
    body: Int?,
    onStartNew: (() -> Unit)?,
) {
    Box(modifier.fillMaxSize().nightBackground().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                stringResource(title),
                style = XType.DisplayLarge,
                color = XColors.Mandates,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .then(if (onStartNew == null) Modifier.pulse() else Modifier)
                        .semantics {
                            heading()
                            liveRegion = LiveRegionMode.Polite
                        },
            )
            if (body != null) Text(stringResource(body), style = XType.Body, color = XColors.Muted, textAlign = TextAlign.Center)
            if (onStartNew != null) {
                XButton(stringResource(R.string.match_start_new), onClick = onStartNew, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/**
 * Banner for a failed save or bot move (`GameViewModel.failure`). The turn isn't lost: RETRY
 * repeats the failed step.
 */
@Composable
fun MatchFailureBanner(
    game: GameViewModel,
    modifier: Modifier = Modifier,
) {
    val failure by game.failure.collectAsState()
    if (failure == null) return
    XPanel(modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Assertive }, accent = XColors.SendIt) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.match_step_failed), style = XType.DisplaySmall, color = XColors.Paper)
            Text(stringResource(R.string.match_step_failed_body), style = XType.Body, color = XColors.Muted)
            XButton(
                stringResource(R.string.match_retry),
                onClick = game::retry,
                modifier = Modifier.fillMaxWidth(),
                style = XButtonStyle.Hot,
                minHeight = 48.dp,
                textStyle = XType.DisplaySmall,
            )
        }
    }
}
