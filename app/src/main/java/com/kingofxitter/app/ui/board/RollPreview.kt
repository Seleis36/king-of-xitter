package com.kingofxitter.app.ui.board

import com.kingofxitter.engine.model.DiceFace
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.PlayerId

/** Who the current tweets would hit. */
sealed interface TweetTargets {
    /** Xitter is empty: tweets hit nobody, and the roller takes Xitter. */
    data object Nobody : TweetTargets

    /** The roller is outside Xitter: tweets hit only the Main Character. */
    data class MainCharacter(
        val playerId: PlayerId,
    ) : TweetTargets

    /** The roller is the Main Character: tweets hit every other living leader. */
    data class Everyone(
        val playerIds: List<PlayerId>,
    ) : TweetTargets
}

/**
 * What resolving the current dice would do under the base rules (ROADMAP §1), for the board's
 * "IF YOU STOP NOW" line and the compose screen. Power-ups can change the real outcome; the
 * engine stays the authority.
 */
data class RollPreview(
    /** Mandates from sets of 1s, 2s and 3s. */
    val setMandates: Int,
    /** Followers the likes would restore, after the maximum. */
    val followersHealed: Int,
    /** Likes rolled while on Xitter, which heal nothing. */
    val likesBlocked: Boolean,
    /** Characters gained, after the optional cap. */
    val characters: Int,
    /** Tweets rolled; each is 1M unfollows per target. */
    val tweets: Int,
    val targets: TweetTargets,
    /** Xitter is empty, so resolving enters it for +1 mandate. */
    val entersXitter: Boolean,
) {
    /** Resolving would change nothing visible. */
    val isEmpty: Boolean
        get() = setMandates == 0 && followersHealed == 0 && !likesBlocked && characters == 0 && tweets == 0 && !entersXitter

    companion object {
        /** Preview of [playerId] resolving the dice currently in [state]. */
        fun of(
            state: GameState,
            playerId: PlayerId,
        ): RollPreview {
            val player = state.players.first { it.id == playerId }
            val counts = state.dice.groupingBy { it.face }.eachCount()

            fun count(face: DiceFace) = counts[face] ?: 0
            val setMandates =
                listOf(DiceFace.ONE to 1, DiceFace.TWO to 2, DiceFace.THREE to 3).sumOf { (face, value) ->
                    val n = count(face)
                    if (n >= 3) value + n - 3 else 0
                }
            val onXitter = state.xitterPlayerId == playerId
            val likes = count(DiceFace.LIKE)
            val healed = if (onXitter) 0 else minOf(likes, (player.maxFollowers - player.followers).coerceAtLeast(0))
            val rawCharacters = count(DiceFace.CHARACTERS) * CHARACTERS_PER_FACE
            val characters =
                state.characterCap?.let { cap -> rawCharacters.coerceAtMost((cap - player.characters).coerceAtLeast(0)) } ?: rawCharacters
            val targets =
                when (val mainCharacter = state.xitterPlayerId) {
                    null -> TweetTargets.Nobody
                    playerId -> TweetTargets.Everyone(state.players.filter { it.id != playerId && !it.isSuspended }.map { it.id })
                    else -> TweetTargets.MainCharacter(mainCharacter)
                }
            return RollPreview(
                setMandates = setMandates,
                followersHealed = healed,
                likesBlocked = onXitter && likes > 0,
                characters = characters,
                tweets = count(DiceFace.TWEET),
                targets = targets,
                entersXitter = state.xitterPlayerId == null,
            )
        }

        private const val CHARACTERS_PER_FACE = 10
    }
}
