package com.kingofxitter.engine

import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.MatchResult
import com.kingofxitter.engine.model.PlayerId
import com.kingofxitter.engine.model.TurnPhase
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/** Versioned save codec. Unknown versions and invalid state are rejected without modifying the source. */
@Serializable
data class SavedMatch(
    val state: GameState,
    val humanPlayerId: PlayerId? = null,
)

/** Serializes snapshots and local player ownership together. */
object MatchSaveCodec {
    private val json = Json { encodeDefaults = true }

    /** Encodes the full deterministic snapshot, including decisions and RNG continuation. */
    fun encode(
        state: GameState,
        humanPlayerId: PlayerId? = null,
    ): String {
        validate(state)
        require(humanPlayerId == null || state.players.any { it.id == humanPlayerId })
        return json.encodeToString(Snapshot(state = state, humanPlayerId = humanPlayerId))
    }

    /** Throws on malformed, unsupported, or internally inconsistent saves. */
    fun decode(text: String): GameState = decodeMatch(text).state

    /** Restores player ownership along with the snapshot. */
    fun decodeMatch(text: String): SavedMatch {
        val snapshot = json.decodeFromString<Snapshot>(text)
        require(snapshot.version == 1) { "Unsupported match save version: ${snapshot.version}" }
        validate(snapshot.state)
        require(snapshot.humanPlayerId == null || snapshot.state.players.any { it.id == snapshot.humanPlayerId })
        return SavedMatch(snapshot.state, snapshot.humanPlayerId)
    }

    private fun validate(state: GameState) {
        val ids = state.players.map { it.id }.toSet()
        require(state.players.isNotEmpty() && ids.size == state.players.size)
        require(state.currentPlayerId in ids && (state.xitterPlayerId == null || state.xitterPlayerId in ids))
        require(state.turnNumber >= 0 && state.rollNumber >= 0)
        require(state.characterCap == null || state.characterCap > 0)
        require(state.phase != TurnPhase.DECISION || state.pendingDecisions.isNotEmpty())
        require(state.players.all { it.extraDice >= 0 && it.extraRerolls >= 0 })
        require(
            state.players.all {
                it.maxFollowers > 0 &&
                    it.followers in 0..it.maxFollowers &&
                    it.mandates >= 0 &&
                    it.characters >= 0 &&
                    it.extraTurns >= 0 &&
                    it.counters.values.all { value -> value >= 0 }
            },
        )
        require(
            state.dice
                .map { it.id }
                .distinct()
                .size == state.dice.size,
        )
        require(state.matchStats.keys.all { it in ids })
        require(
            state.pendingDecisions
                .map { it.id }
                .distinct()
                .size == state.pendingDecisions.size,
        )
        require(
            state.pendingDecisions.all { decision ->
                decision.playerId in ids &&
                    decision.options.isNotEmpty() &&
                    decision.options
                        .map { it.id }
                        .distinct()
                        .size == decision.options.size &&
                    decision.options.all { it.action.playerId == decision.playerId }
            },
        )
        require(state.pendingDecisions.isEmpty() || (state.phase == TurnPhase.DECISION && state.phaseAfterDecisions != null))
        require((state.result != null) == (state.phase == TurnPhase.FINISHED))
        (state.result as? MatchResult.Victory)?.let { require(it.playerId in ids) }
    }

    @Serializable
    private data class Snapshot(
        val version: Int = 1,
        val state: GameState,
        val humanPlayerId: PlayerId? = null,
    )
}

/** App-private save file. Writes replace the previous save atomically after syncing the temporary file. */
class MatchSaveFile(
    private val path: Path,
) {
    /** Returns null only when no save exists. Corruption is reported to the caller and preserved. */
    fun load(): GameState? = loadMatch()?.state

    /** Loads the ownership metadata needed to resume the original human account. */
    fun loadMatch(): SavedMatch? =
        if (Files.exists(path)) MatchSaveCodec.decodeMatch(String(Files.readAllBytes(path), Charsets.UTF_8)) else null

    /** Call from an IO dispatcher. Failed serialization or writes preserve the previous save. */
    fun save(
        state: GameState,
        humanPlayerId: PlayerId? = null,
    ) {
        val bytes = MatchSaveCodec.encode(state, humanPlayerId).toByteArray(Charsets.UTF_8)
        val target = path.toAbsolutePath()
        Files.createDirectories(target.parent)
        val temporary = Files.createTempFile(target.parent, "match-", ".tmp")
        try {
            FileOutputStream(temporary.toFile()).use { stream ->
                stream.write(bytes)
                stream.fd.sync()
            }
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } finally {
            Files.deleteIfExists(temporary)
        }
    }
}
