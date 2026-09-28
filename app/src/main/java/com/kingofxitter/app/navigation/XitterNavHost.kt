package com.kingofxitter.app.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kingofxitter.app.ui.board.BoardCallbacks
import com.kingofxitter.app.ui.board.BoardRoute
import com.kingofxitter.app.ui.board.BoardSamples
import com.kingofxitter.app.ui.board.ComposeTweetRoute
import com.kingofxitter.app.ui.board.IncomingTweetRoute
import com.kingofxitter.app.ui.theme.nightBackground
import com.kingofxitter.engine.FakeGameController

/** Where each placeholder leads. The board keeps its entry so the reachability test still covers it. */
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

/** App navigation graph. Starts on the title screen. */
@Composable
fun XitterNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val navigate: (XitterRoute) -> Unit = { route ->
        navController.navigate(route) {
            // Returning to a screen already in the back stack pops back to it instead of stacking a copy.
            popUpTo(route) { inclusive = true }
            launchSingleTop = true
        }
    }
    // Until U10 connects the real controller, the board plays the design's situation on the fake.
    val controller = remember { FakeGameController(BoardSamples.design) }
    NavHost(navController = navController, startDestination = XitterRoute.Title, modifier = modifier) {
        placeholder<XitterRoute.Title>(XitterRoute.Title, navigate)
        placeholder<XitterRoute.PickLeader>(XitterRoute.PickLeader, navigate)
        placeholder<XitterRoute.Versus>(XitterRoute.Versus, navigate)
        composable<XitterRoute.Board> {
            BoardRoute(
                controller = controller,
                humanId = BoardSamples.you,
                callbacks =
                    BoardCallbacks(
                        onOpenTimeline = { navigate(XitterRoute.Timeline) },
                        onOpenStandings = { navigate(XitterRoute.Standings) },
                        onOpenShop = { navigate(XitterRoute.Shop) },
                        onCompose = { navigate(XitterRoute.ComposeTweet) },
                        onIncomingTweet = { navigate(XitterRoute.IncomingTweet) },
                        onMatchFinished = { navigate(XitterRoute.Standings) },
                    ),
                modifier = Modifier.fillMaxSize().nightBackground().safeDrawingPadding(),
            )
        }
        composable<XitterRoute.ComposeTweet> {
            ComposeTweetRoute(
                controller = controller,
                humanId = BoardSamples.you,
                onDone = { navigate(XitterRoute.Board) },
                modifier = Modifier.fillMaxSize().nightBackground().safeDrawingPadding(),
            )
        }
        composable<XitterRoute.IncomingTweet> {
            IncomingTweetRoute(
                controller = controller,
                humanId = BoardSamples.you,
                onDone = { navigate(XitterRoute.Board) },
                modifier = Modifier.fillMaxSize().nightBackground().safeDrawingPadding(),
            )
        }
        placeholder<XitterRoute.Shop>(XitterRoute.Shop, navigate)
        placeholder<XitterRoute.Timeline>(XitterRoute.Timeline, navigate)
        placeholder<XitterRoute.Standings>(XitterRoute.Standings, navigate)
        placeholder<XitterRoute.Victory>(XitterRoute.Victory, navigate)
        placeholder<XitterRoute.Defeat>(XitterRoute.Defeat, navigate)
        placeholder<XitterRoute.HowToPlay>(XitterRoute.HowToPlay, navigate)
        placeholder<XitterRoute.Settings>(XitterRoute.Settings, navigate)
    }
}

private inline fun <reified T : XitterRoute> androidx.navigation.NavGraphBuilder.placeholder(
    route: XitterRoute,
    noinline navigate: (XitterRoute) -> Unit,
) {
    composable<T> {
        PlaceholderScreen(route, placeholderLinks[route].orEmpty(), navigate)
    }
}
