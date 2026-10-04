package com.kingofxitter.app.ui.board

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.kingofxitter.app.content.VoiceLog
import com.kingofxitter.content.TweetMoment
import com.kingofxitter.engine.GameController
import com.kingofxitter.engine.model.DecisionKind
import com.kingofxitter.engine.model.PlayerId

/**
 * Incoming-tweet screen bound to a [GameController]. It stays up while the human owns a
 * stay/log-off decision; once the choice is submitted (or if there is none), [onDone] returns
 * to the board. [hit] is the damage from the engine's events; without it the dice estimate it.
 * [voices] supplies the attacker's own tweet (V1); without it the screen uses its fixture line.
 */
@Composable
fun IncomingTweetRoute(
    controller: GameController,
    humanId: PlayerId,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    hit: IncomingHit? = null,
    voices: VoiceLog? = null,
) {
    val state by controller.state.collectAsState()
    val tweets =
        voices
            ?.entries
            ?.collectAsState()
            ?.value
            .orEmpty()
    val decision = state.pendingDecisions.firstOrNull()?.takeIf { it.playerId == humanId && it.kind == DecisionKind.STAY_OR_LOG_OFF }
    if (decision == null) {
        LaunchedEffect(Unit) { onDone() }
        return
    }
    val incoming = hit ?: IncomingHit.estimate(state)
    val attackerTweet = tweets.lastOrNull { it.playerId == incoming.attackerId && it.moment == TweetMoment.ATTACKED }
    IncomingTweetScreen(state, humanId, decision, controller::submit, modifier, hit = incoming, tweet = attackerTweet?.text)
}
