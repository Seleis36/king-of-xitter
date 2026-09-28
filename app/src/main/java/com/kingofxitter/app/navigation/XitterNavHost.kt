package com.kingofxitter.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/** Where each placeholder leads. A route disappears from here once its real screen lands. */
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
    NavHost(navController = navController, startDestination = XitterRoute.Title, modifier = modifier) {
        placeholder<XitterRoute.Title>(XitterRoute.Title, navigate)
        placeholder<XitterRoute.PickLeader>(XitterRoute.PickLeader, navigate)
        placeholder<XitterRoute.Versus>(XitterRoute.Versus, navigate)
        placeholder<XitterRoute.Board>(XitterRoute.Board, navigate)
        placeholder<XitterRoute.ComposeTweet>(XitterRoute.ComposeTweet, navigate)
        placeholder<XitterRoute.IncomingTweet>(XitterRoute.IncomingTweet, navigate)
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
