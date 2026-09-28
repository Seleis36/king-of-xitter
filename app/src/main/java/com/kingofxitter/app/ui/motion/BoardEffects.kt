package com.kingofxitter.app.ui.motion

import com.kingofxitter.engine.model.GameEvent
import com.kingofxitter.engine.model.PlayerId
import com.kingofxitter.engine.model.PlayerResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A transient on-screen effect for one player, derived from an engine event. */
data class BoardEffect(
    /** Unique per effect; screens use it as the key that replays a one-shot animation. */
    val id: Long,
    val kind: Kind,
    /** Player the effect is drawn on; null for the Xitter stage itself. */
    val playerId: PlayerId?,
    /** Signed amount for number effects. */
    val amount: Int = 0,
) {
    /** What to draw. */
    enum class Kind {
        /** Lost followers: red "−3M" rising, and the panel shakes. */
        Damage,

        /** Gained followers: "+1M". */
        Heal,

        /** Gained mandates: "+2". */
        Mandates,

        /** Gained or spent characters: "+10". */
        Characters,

        /** The player was suspended: SUSPENDED stamp. */
        Suspended,

        /** A new Main Character (or nobody): the stage slams. */
        NewMainCharacter,
    }

    /** Effects that show a rising number. */
    val isNumber: Boolean get() = kind in setOf(Kind.Damage, Kind.Heal, Kind.Mandates, Kind.Characters)
}

/**
 * Effects for one engine event, before ids are assigned. Events without anything to show
 * (turn changes, decisions, rejections) map to nothing; dice have their own tumble.
 */
fun effectsFor(event: GameEvent): List<Pair<BoardEffect.Kind, Pair<PlayerId?, Int>>> =
    when (event) {
        is GameEvent.ResourceChanged ->
            when {
                event.amount == 0 -> emptyList()
                event.resource == PlayerResource.FOLLOWERS && event.amount < 0 ->
                    listOf(
                        BoardEffect.Kind.Damage to (event.playerId to event.amount),
                    )
                event.resource == PlayerResource.FOLLOWERS -> listOf(BoardEffect.Kind.Heal to (event.playerId to event.amount))
                event.resource == PlayerResource.MANDATES -> listOf(BoardEffect.Kind.Mandates to (event.playerId to event.amount))
                else -> listOf(BoardEffect.Kind.Characters to (event.playerId to event.amount))
            }
        is GameEvent.PlayerSuspended -> listOf(BoardEffect.Kind.Suspended to (event.playerId to 0))
        is GameEvent.MainCharacterChanged -> listOf(BoardEffect.Kind.NewMainCharacter to (event.newPlayerId to 0))
        else -> emptyList()
    }

/**
 * Plays [BoardEffect]s one after another so a burst of events (a whole resolution) reads as a
 * sequence: each effect starts [staggerMillis] after the previous one and stays in [active]
 * for [lifetimeMillis].
 */
class EffectQueue(
    private val scope: CoroutineScope,
    private val staggerMillis: Long = 220,
    private val lifetimeMillis: Long = 2400,
) {
    private val pending = Channel<BoardEffect>(Channel.UNLIMITED)
    private val mutableActive = MutableStateFlow<List<BoardEffect>>(emptyList())
    private var nextId = 0L

    /** Effects on screen right now, oldest first. */
    val active: StateFlow<List<BoardEffect>> = mutableActive.asStateFlow()

    init {
        scope.launch {
            for (effect in pending) {
                mutableActive.update { it + effect }
                scope.launch {
                    delay(lifetimeMillis)
                    mutableActive.update { list -> list.filterNot { it.id == effect.id } }
                }
                delay(staggerMillis)
            }
        }
    }

    /** Queues the effects of [event]. */
    fun offer(event: GameEvent) {
        effectsFor(event).forEach { (kind, target) ->
            pending.trySend(BoardEffect(nextId++, kind, target.first, target.second))
        }
    }
}
