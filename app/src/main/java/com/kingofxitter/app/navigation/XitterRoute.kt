package com.kingofxitter.app.navigation

import androidx.annotation.StringRes
import com.kingofxitter.app.R
import kotlinx.serialization.Serializable

/**
 * Every destination from `design/screens/`. Each track replaces its placeholders in
 * [XitterNavHost] with the real screen; the route objects stay stable.
 */
@Serializable
sealed interface XitterRoute {
    /** Placeholder title, also used as the screen's accessibility heading. */
    @get:StringRes
    val titleRes: Int

    /** Title screen. Owner: Track D (V4). */
    @Serializable
    data object Title : XitterRoute {
        override val titleRes get() = R.string.route_title
    }

    /** Pick your leader and bot difficulty. Owner: Track D (V4). */
    @Serializable
    data object PickLeader : XitterRoute {
        override val titleRes get() = R.string.route_pick_leader
    }

    /** Versus intro with the roll-off. Owner: Track D (V4). */
    @Serializable
    data object Versus : XitterRoute {
        override val titleRes get() = R.string.route_versus
    }

    /** Game board. Owner: Track C (U5). */
    @Serializable
    data object Board : XitterRoute {
        override val titleRes get() = R.string.route_board
    }

    /** Compose your attack tweet. Owner: Track C (U6). */
    @Serializable
    data object ComposeTweet : XitterRoute {
        override val titleRes get() = R.string.route_compose_tweet
    }

    /** Incoming tweet: stay or log off. Owner: Track C (U7). */
    @Serializable
    data object IncomingTweet : XitterRoute {
        override val titleRes get() = R.string.route_incoming_tweet
    }

    /** Power-up shop. Owner: Track B (C8). */
    @Serializable
    data object Shop : XitterRoute {
        override val titleRes get() = R.string.route_shop
    }

    /** Match timeline. Owner: Track D (V3). */
    @Serializable
    data object Timeline : XitterRoute {
        override val titleRes get() = R.string.route_timeline
    }

    /** Standings. Owner: Track A (E11). */
    @Serializable
    data object Standings : XitterRoute {
        override val titleRes get() = R.string.route_standings
    }

    /** Victory. Owner: Track A (E11). */
    @Serializable
    data object Victory : XitterRoute {
        override val titleRes get() = R.string.route_victory
    }

    /** Defeat (account suspended). Owner: Track A (E11). */
    @Serializable
    data object Defeat : XitterRoute {
        override val titleRes get() = R.string.route_defeat
    }

    /** How to play. Owner: Track D (V5). */
    @Serializable
    data object HowToPlay : XitterRoute {
        override val titleRes get() = R.string.route_how_to_play
    }

    /** Settings. Owner: Track D (V5). */
    @Serializable
    data object Settings : XitterRoute {
        override val titleRes get() = R.string.route_settings
    }
}
