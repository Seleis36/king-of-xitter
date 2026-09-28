package com.kingofxitter.app.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class PlaceholderLinksTest {
    private val allRoutes =
        setOf(
            XitterRoute.Title,
            XitterRoute.PickLeader,
            XitterRoute.Versus,
            XitterRoute.Board,
            XitterRoute.ComposeTweet,
            XitterRoute.IncomingTweet,
            XitterRoute.Shop,
            XitterRoute.Timeline,
            XitterRoute.Standings,
            XitterRoute.Victory,
            XitterRoute.Defeat,
            XitterRoute.HowToPlay,
            XitterRoute.Settings,
        )

    @Test
    fun `every destination has a placeholder entry`() {
        assertEquals(allRoutes, placeholderLinks.keys)
    }

    @Test
    fun `every destination is reachable from the title screen`() {
        val reached = mutableSetOf<XitterRoute>(XitterRoute.Title)
        val queue = ArrayDeque(reached)
        while (queue.isNotEmpty()) {
            placeholderLinks[queue.removeFirst()].orEmpty().filter(reached::add).forEach(queue::addLast)
        }
        assertEquals(allRoutes, reached)
    }

    @Test
    fun `every screen except the title leads somewhere`() {
        placeholderLinks.forEach { (route, links) -> assert(links.isNotEmpty()) { "$route has no way out" } }
    }
}
