package com.kingofxitter.app.navigation

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kingofxitter.app.R
import com.kingofxitter.app.content.rememberVoiceLog
import com.kingofxitter.app.feedback.GameFeedback
import com.kingofxitter.app.game.GameViewModel
import com.kingofxitter.app.ui.board.BoardCallbacks
import com.kingofxitter.app.ui.board.BoardRoute
import com.kingofxitter.app.ui.board.ComposeTweetRoute
import com.kingofxitter.app.ui.board.IncomingHit
import com.kingofxitter.app.ui.board.IncomingTweetRoute
import com.kingofxitter.app.ui.board.rememberEffectQueue
import com.kingofxitter.app.ui.components.leaderVisual
import com.kingofxitter.app.ui.endgame.DefeatScreen
import com.kingofxitter.app.ui.endgame.StandingsScreen
import com.kingofxitter.app.ui.endgame.VictoryScreen
import com.kingofxitter.app.ui.front.ContinueOffer
import com.kingofxitter.app.ui.front.HowToPlayScreen
import com.kingofxitter.app.ui.front.PickLeaderScreen
import com.kingofxitter.app.ui.front.SettingsScreen
import com.kingofxitter.app.ui.front.TitleScreen
import com.kingofxitter.app.ui.front.VersusLineUp
import com.kingofxitter.app.ui.front.VersusScreen
import com.kingofxitter.app.ui.front.rememberGamePreferences
import com.kingofxitter.app.ui.shop.ShopScreen
import com.kingofxitter.app.ui.theme.nightBackground
import com.kingofxitter.app.ui.timeline.TimelineScreen
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.MatchResult

/** Where each placeholder leads. Real screens keep their entry so the reachability test covers them. */
internal val placeholderLinks: Map<XitterRoute, List<XitterRoute>> =
    mapOf(
        XitterRoute.Title to listOf(XitterRoute.PickLeader, XitterRoute.HowToPlay, XitterRoute.Settings),
        XitterRoute.PickLeader to listOf(XitterRoute.Versus),
        XitterRoute.Versus to listOf(XitterRoute.Board),
        XitterRoute.Board to
            listOf(
                XitterRoute.ComposeTweet,
                XitterRoute.IncomingTweet,
                XitterRoute.Shop,
                XitterRoute.Timeline,
                XitterRoute.Standings,
                XitterRoute.Victory,
                XitterRoute.Defeat,
            ),
        XitterRoute.ComposeTweet to listOf(XitterRoute.Board),
        XitterRoute.IncomingTweet to listOf(XitterRoute.Board),
        XitterRoute.Shop to listOf(XitterRoute.Board),
        XitterRoute.Timeline to listOf(XitterRoute.Board),
        XitterRoute.Standings to listOf(XitterRoute.Board),
        XitterRoute.Victory to listOf(XitterRoute.Standings, XitterRoute.Title),
        XitterRoute.Defeat to listOf(XitterRoute.Standings, XitterRoute.Title),
        XitterRoute.HowToPlay to listOf(XitterRoute.Title),
        XitterRoute.Settings to listOf(XitterRoute.Title),
    )

/**
 * App navigation graph. Starts on the title screen. The board and the screens around it play
 * the real match from [MatchHost]; the front door's placeholders lead into it.
 */
@Composable
fun XitterNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    host: MatchHost = viewModel(factory = MatchHost.Factory),
) {
    val navigate: (XitterRoute) -> Unit = { route ->
        // Returning to a screen already in the back stack pops back to it instead of stacking a copy.
        val inBackStack = runCatching { navController.getBackStackEntry(route) }.isSuccess
        navController.navigate(route) {
            if (inBackStack) popUpTo(route) { inclusive = true }
            launchSingleTop = true
        }
    }
    // Entering a match clears the way there (pick a leader, versus), so Back from the board
    // returns to the title, where CONTINUE picks the match up again.
    val enterMatch: () -> Unit = {
        navController.navigate(XitterRoute.Board) {
            popUpTo(XitterRoute.Title)
            launchSingleTop = true
        }
    }
    val context = LocalContext.current
    val preferences = rememberGamePreferences()
    val settings by preferences.settings.collectAsState()
    val options =
        MatchOptions(
            leader = settings.leader,
            difficulty = settings.difficulty,
            botDelayMillis = settings.botSpeed.botDelayMillis,
            characterCap = settings.characterCapValue,
        )
    val session by host.session.collectAsState()
    val resumable by host.resumable.collectAsState()
    val game = (session as? MatchSession.Ready)?.game
    // Effects and sounds follow the live match, above navigation so no screen misses them.
    val effects = game?.let { rememberEffectQueue(it) }
    // V1 lines follow the match next to the effects, so the board and the incoming screen agree.
    val voices = game?.let { rememberVoiceLog(it) }
    if (game != null) GameFeedback(game, game.humanPlayerId, preferences.feedback)
    val screen = Modifier.fillMaxSize().nightBackground().safeDrawingPadding()

    NavHost(navController = navController, startDestination = XitterRoute.Title, modifier = modifier) {
        composable<XitterRoute.Title> {
            TitleScreen(
                onPlay = { navigate(XitterRoute.PickLeader) },
                onRules = { navigate(XitterRoute.HowToPlay) },
                onSettings = { navigate(XitterRoute.Settings) },
                modifier = screen,
                resumable = resumable?.let { ContinueOffer(it.turnNumber, it.leader) },
                onContinue = enterMatch,
            )
        }
        composable<XitterRoute.PickLeader> {
            PickLeaderScreen(
                onStart = { leader, difficulty ->
                    // The pick is remembered, so a replay or a restart uses the same opponents.
                    preferences.update(settings.copy(leader = leader, difficulty = difficulty))
                    host.startNew(options.copy(leader = leader, difficulty = difficulty))
                    navigate(XitterRoute.Versus)
                },
                onBack = { navController.navigateUp() },
                modifier = screen,
                initialLeader = settings.leader,
                initialDifficulty = settings.difficulty,
            )
        }
        composable<XitterRoute.Versus> {
            val intro by host.intro.collectAsState()
            val lineUp = intro
            // A resumed save has no roll-off to show, so the intro is skipped entirely.
            if (lineUp == null) {
                LaunchedEffect(Unit) { enterMatch() }
            } else {
                VersusScreen(
                    VersusLineUp(lineUp.humanPlayerId, lineUp.firstPlayerId, lineUp.players, lineUp.rollOff, lineUp.matchNumber),
                    onBegin = enterMatch,
                    modifier = screen,
                )
            }
        }
        composable<XitterRoute.Board> {
            MatchGate(host, options, screen) { live ->
                Column(screen) {
                    MatchFailureBanner(live, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    BoardRoute(
                        controller = live,
                        humanId = live.humanPlayerId,
                        callbacks =
                            BoardCallbacks(
                                onOpenTimeline = { navigate(XitterRoute.Timeline) },
                                onOpenStandings = { navigate(XitterRoute.Standings) },
                                onOpenShop = { navigate(XitterRoute.Shop) },
                                onCompose = { navigate(XitterRoute.ComposeTweet) },
                                onIncomingTweet = { navigate(XitterRoute.IncomingTweet) },
                                onMatchFinished = { navigate(resultRoute(live.state.value, live)) },
                            ),
                        modifier = Modifier.weight(1f),
                        effects = effects,
                        voices = voices,
                    )
                }
            }
        }
        composable<XitterRoute.ComposeTweet> {
            MatchGate(host, options, screen) { live ->
                ComposeTweetRoute(
                    live,
                    live.humanPlayerId,
                    onDone = { navigate(XitterRoute.Board) },
                    modifier = screen,
                    // The zinger you picked is the tweet the timeline and the board show.
                    onSent = { zinger -> voices?.draft(live.humanPlayerId, zinger, live.state.value.turnNumber) },
                )
            }
        }
        composable<XitterRoute.IncomingTweet> {
            MatchGate(host, options, screen) { live ->
                val lastHit by host.lastHitOnHuman.collectAsState()
                IncomingTweetRoute(
                    live,
                    live.humanPlayerId,
                    onDone = { navigate(XitterRoute.Board) },
                    modifier = screen,
                    hit = lastHit?.let { (attacker, damage) -> IncomingHit(attacker, damage) },
                    voices = voices,
                )
            }
        }
        composable<XitterRoute.Shop> {
            MatchGate(host, options, screen) { live ->
                val state by live.state.collectAsState()
                ShopScreen(state, live.humanPlayerId, live::submit, onClose = { navigate(XitterRoute.Board) }, modifier = screen)
            }
        }
        composable<XitterRoute.Timeline> {
            MatchGate(host, options, screen) { live ->
                val state by live.state.collectAsState()
                val tweets =
                    voices
                        ?.entries
                        ?.collectAsState()
                        ?.value
                        .orEmpty()
                TimelineScreen(state, live.humanPlayerId, tweets, onBack = { navigate(XitterRoute.Board) }, modifier = screen)
            }
        }
        composable<XitterRoute.Standings> {
            MatchGate(host, options, screen) { live ->
                val state by live.state.collectAsState()
                StandingsScreen(state, onBack = { navigate(if (state.result == null) XitterRoute.Board else resultRoute(state, live)) })
            }
        }
        composable<XitterRoute.Victory> {
            MatchGate(host, options, screen) { live ->
                val state by live.state.collectAsState()
                VictoryScreen(
                    state,
                    onPlayAgain = { playAgain(host, options, enterMatch) },
                    onShare = { shareWin(context, state) },
                    onMainMenu = { navigate(XitterRoute.Title) },
                )
            }
        }
        composable<XitterRoute.Defeat> {
            MatchGate(host, options, screen) { live ->
                val state by live.state.collectAsState()
                DefeatScreen(
                    state,
                    live.humanPlayerId,
                    onPlayAgain = { playAgain(host, options, enterMatch) },
                    onMainMenu = { navigate(XitterRoute.Title) },
                )
            }
        }
        composable<XitterRoute.HowToPlay> {
            HowToPlayScreen(onDone = { navController.navigateUp() }, modifier = screen)
        }
        composable<XitterRoute.Settings> {
            SettingsScreen(settings, preferences::update, onDone = { navController.navigateUp() }, modifier = screen)
        }
    }
}

/** Victory when the human won, Defeat otherwise (including nobody winning). */
private fun resultRoute(
    state: GameState,
    game: GameViewModel,
): XitterRoute = if ((state.result as? MatchResult.Victory)?.playerId == game.humanPlayerId) XitterRoute.Victory else XitterRoute.Defeat

private fun playAgain(
    host: MatchHost,
    options: MatchOptions,
    enterMatch: () -> Unit,
) {
    host.startNew(options)
    enterMatch()
}

private fun shareWin(
    context: Context,
    state: GameState,
) {
    val winner = state.players.firstOrNull { it.id == (state.result as? MatchResult.Victory)?.playerId } ?: return
    val name = context.getString(leaderVisual(winner.leaderId).fullName)
    val send =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, context.getString(R.string.endgame_share_summary, name, winner.mandates))
        }
    context.startActivity(Intent.createChooser(send, null))
}

private inline fun <reified T : XitterRoute> NavGraphBuilder.placeholder(
    route: XitterRoute,
    noinline navigate: (XitterRoute) -> Unit,
) {
    composable<T> {
        PlaceholderScreen(route, placeholderLinks[route].orEmpty(), navigate)
    }
}
