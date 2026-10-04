package com.kingofxitter.app.navigation

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kingofxitter.app.game.GameViewModel
import com.kingofxitter.content.LeaderRoster
import com.kingofxitter.engine.MatchSaveFile
import com.kingofxitter.engine.MatchSetup
import com.kingofxitter.engine.model.LeaderId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MatchHostTest {
    private val dir = Files.createTempDirectory("match-host").toFile()
    private val saveFile = File(dir, "match.json")
    private val stores = mutableListOf<ViewModelStore>()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        dir.deleteRecursively()
    }

    @Test
    fun `starts and saves a new match when there is no save`() =
        matchTest {
            val host = host()
            host.resumeOrStart()
            val game = host.awaitGame()

            assertTrue(saveFile.exists())
            assertEquals(
                DefaultLeader,
                game.state.value.players
                    .first { it.id == game.humanPlayerId }
                    .leaderId,
            )
            assertTrue(
                game.state.value.players
                    .all { it.leaderId in LeaderRoster.ids },
            )
        }

    @Test
    fun `resumes the saved match after the process is gone`() =
        matchTest {
            val first = host()
            first.resumeOrStart()
            first.awaitGame()
            stores.removeFirst().clear()
            val saved = assertNotNull(MatchSaveFile(saveFile.toPath()).loadMatch())

            val second = host()
            second.resumeOrStart()
            val resumed = second.awaitGame()

            assertEquals(saved.state, resumed.state.value)
            assertEquals(saved.humanPlayerId, resumed.humanPlayerId)
        }

    @Test
    fun `resuming twice keeps the same match`() =
        matchTest {
            val host = host()
            host.resumeOrStart()
            val game = host.awaitGame()
            host.resumeOrStart()

            assertSame(game, host.awaitGame())
        }

    @Test
    fun `start new replaces the match`() =
        matchTest {
            val host = host()
            host.resumeOrStart()
            val old = host.awaitGame()

            host.startNew()
            val new = host.awaitGame { it !== old }

            assertNotSame(old, new)
            assertEquals(new.state.value, assertNotNull(MatchSaveFile(saveFile.toPath()).loadMatch()).state)
        }

    @Test
    fun `match options choose the leader and the rules a new match starts with`() =
        matchTest {
            val host = host()
            host.startNew(MatchOptions(leader = LeaderId("kim"), characterCap = 280))
            val game = host.awaitGame()

            assertEquals(
                LeaderId("kim"),
                game.state.value.players
                    .first { it.id == game.humanPlayerId }
                    .leaderId,
            )
            assertEquals(280, game.state.value.characterCap)
        }

    @Test
    fun `canceling an old startup cannot replace a newer match session`() =
        matchTest {
            val firstStartEntered = CompletableDeferred<Unit>()
            var starts = 0
            val host =
                host { options ->
                    if (starts++ == 0) {
                        firstStartEntered.complete(Unit)
                        awaitCancellation()
                    }
                    startedMatch(options)
                }
            host.resumeOrStart()
            runCurrent()
            firstStartEntered.await()

            host.startNew()
            val game = host.awaitGame()

            assertEquals(MatchSession.Ready(game), host.session.value)
        }

    @Test
    fun `a corrupt save fails and stays on disk until the player starts over`() =
        matchTest {
            saveFile.writeText("{not a match")
            val host = host()
            host.resumeOrStart()

            assertIs<MatchSession.Failed>(host.session.first { it is MatchSession.Failed || it is MatchSession.Ready })
            assertEquals("{not a match", saveFile.readText())

            host.startNew()
            host.awaitGame()
            assertNotNull(MatchSaveFile(saveFile.toPath()).loadMatch())
        }

    /**
     * Runs [block], then stops every match before the test ends, so no bot moves or saves are left
     * running into the next test.
     */
    private fun matchTest(block: suspend TestScope.() -> Unit) =
        runTest {
            try {
                block()
            } finally {
                stores.forEach { it.clear() }
            }
        }

    private fun host(gameFactory: suspend (MatchOptions) -> StartedMatch = { options -> startedMatch(options) }): MatchHost {
        val store = ViewModelStore().also(stores::add)
        return ViewModelProvider(store, viewModelFactory { initializer { MatchHost(saveFile, gameFactory) } })[MatchHost::class.java]
    }

    /** A match on a fixed seed, with the roll-off the Versus screen would show. */
    private suspend fun startedMatch(options: MatchOptions): StartedMatch {
        val setup = MatchSetup.create(options.leader, LeaderRoster.ids, 42)
        val initial = setup.state.copy(characterCap = options.characterCap)
        val game = GameViewModel.restore(initial, setup.humanPlayerId, MatchSaveFile(saveFile.toPath()))
        val fresh = game.humanPlayerId == setup.humanPlayerId && game.state.value == initial
        return StartedMatch(
            game,
            if (fresh) MatchIntro(setup.humanPlayerId, setup.state.currentPlayerId, setup.state.players, setup.rollOff, 42) else null,
        )
    }

    /** Waits for a live match accepted by [accept]; fails if the match can't start. */
    private suspend fun MatchHost.awaitGame(accept: (GameViewModel) -> Boolean = { true }): GameViewModel =
        when (val ready = session.first { it is MatchSession.Failed || (it is MatchSession.Ready && accept(it.game)) }) {
            is MatchSession.Ready -> ready.game
            else -> error("The match didn't start: $ready")
        }
}
