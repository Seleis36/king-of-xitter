package com.kingofxitter.engine

import com.kingofxitter.engine.model.Action
import com.kingofxitter.engine.model.DiceFace
import com.kingofxitter.engine.model.GameState
import com.kingofxitter.engine.model.LeaderId
import com.kingofxitter.engine.model.TurnPhase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ControllerSetupSaveTest {
    private val roster = (1..6).map { LeaderId("leader-$it") }

    @Test fun `setup uses distinct leaders deterministic rolls and only rerolls ties`() {
        for (seed in 0L..30L) {
            val start = MatchSetup.create(roster.first(), roster, seed)
            assertEquals(start, MatchSetup.create(roster.first(), roster, seed))
            assertEquals(
                4,
                start.state.players
                    .map { it.leaderId }
                    .distinct()
                    .size,
            )
            assertEquals(roster.first().value, start.humanPlayerId.value)
            var candidates =
                start.state.players
                    .map { it.id }
                    .toSet()
            start.rollOff.forEach { round ->
                assertEquals(candidates, round.rolls.keys)
                assertTrue(round.rolls.values.all { it.size == 6 })
                val high = round.rolls.values.maxOf { dice -> dice.count { it == DiceFace.TWEET } }
                candidates = round.rolls.filterValues { dice -> dice.count { it == DiceFace.TWEET } == high }.keys
            }
            assertEquals(setOf(start.state.currentPlayerId), candidates)
        }
        assertTrue((0L..30L).any { MatchSetup.create(roster.first(), roster, it).rollOff.size > 1 })
        assertFailsWith<IllegalArgumentException> { MatchSetup.create(roster.first(), roster.take(5), 0) }
    }

    @Test fun `bot pacing advances legal phases and stops for human`() =
        runTest {
            val start = MatchSetup.create(roster.first(), roster, 42).state
            val bot = start.players.first().id
            val initial = start.copy(currentPlayerId = bot)
            val saved = mutableListOf<GameState>()
            val controller = RealGameController(initial, backgroundScope, setOf(bot), botDelayMillis = 100, save = { saved += it })
            runCurrent()
            advanceTimeBy(99)
            assertEquals(initial, controller.state.value)
            advanceTimeBy(1)
            runCurrent()
            assertEquals(TurnPhase.ROLLING, controller.state.value.phase)
            advanceTimeBy(201)
            runCurrent()
            assertEquals(TurnPhase.TURN_START, controller.state.value.phase)
            assertNotEquals(bot, controller.state.value.currentPlayerId)
            val humanTurn = controller.state.value
            advanceTimeBy(1000)
            assertEquals(humanTurn, controller.state.value)
            assertEquals(controller.state.value, saved.last())
            controller.close()
        }

    @Test fun `human pending decision pauses an opponent turn and close cancels delayed bot`() =
        runTest {
            val initial = GameFixtures.incomingTweet
            val human = initial.pendingDecisions.first().playerId
            val controller = RealGameController(initial, backgroundScope, initial.players.map { it.id }.toSet() - human)
            runCurrent()
            advanceTimeBy(1000)
            assertEquals(initial, controller.state.value)
            val decision = initial.pendingDecisions.first()
            controller.submit(Action.ChooseDecisionOption(human, decision.id, decision.options.first().id))
            runCurrent()
            assertEquals(TurnPhase.SHOP, controller.state.value.phase)
            val saved = controller.state.value
            controller.close()
            advanceTimeBy(1000)
            assertEquals(saved, controller.state.value)
            assertFailsWith<IllegalStateException> { controller.submit(Action.EndTurn(human)) }
        }

    @Test fun `failed save is exposed and does not publish an unpersisted state`() =
        runTest {
            val initial = MatchSetup.create(roster.first(), roster, 0).state
            val controller = RealGameController(initial, backgroundScope, emptySet(), save = { error("disk unavailable") })
            controller.submit(Action.RollDice(initial.currentPlayerId))
            runCurrent()
            assertEquals(initial, controller.state.value)
            assertEquals("disk unavailable", controller.failure.value?.message)
            controller.close()
        }

    @Test fun `save roundtrips queued choices game results and deterministic continuation`() {
        val reducer = DefaultGameReducer()
        for (initial in listOf(GameFixtures.incomingTweet, GameFixtures.victory, GameFixtures.yourRoll)) {
            val restored = MatchSaveCodec.decode(MatchSaveCodec.encode(initial))
            assertEquals(initial, restored)
            val action = firstAvailableAction(initial)
            if (action != null) assertEquals(reducer.reduce(initial, action), reducer.reduce(restored, action))
        }
        val initial = MatchSetup.create(roster.first(), roster, 43).state
        val folder = Files.createTempDirectory("king-of-xitter-save-test")
        try {
            val path = folder.resolve("match.json")
            val save = MatchSaveFile(path)
            assertNull(save.load())
            save.save(initial)
            val previousBytes = Files.readAllBytes(path)
            assertFailsWith<IllegalArgumentException> { save.save(initial.copy(players = emptyList())) }
            assertContentEquals(previousBytes, Files.readAllBytes(path))
            assertEquals(initial, MatchSaveFile(path).load())
            val future = MatchSaveCodec.encode(initial).replace("\"version\":1", "\"version\":99")
            assertFailsWith<IllegalArgumentException> { MatchSaveCodec.decode(future) }
            Files.writeString(path, "broken")
            assertFails { save.load() }
            assertEquals("broken", Files.readString(path))
        } finally {
            Files.deleteIfExists(folder.resolve("match.json"))
            Files.delete(folder)
        }
    }

    @Test fun `bot save can recover after transient failure without recreating controller`() =
        runTest {
            val initial = MatchSetup.create(roster.first(), roster, 4).state
            var fail = true
            val controller =
                RealGameController(
                    initial,
                    backgroundScope,
                    setOf(initial.currentPlayerId),
                    botDelayMillis = 10,
                    save = { if (fail) error("temporary") },
                )
            runCurrent()
            advanceTimeBy(11)
            runCurrent()
            assertEquals(initial, controller.state.value)
            assertEquals("temporary", controller.failure.value?.message)
            fail = false
            controller.retry()
            runCurrent()
            assertEquals(TurnPhase.ROLLING, controller.state.value.phase)
            assertNull(controller.failure.value)
            controller.close()
        }

    @Test fun `ownership survives restore and invalid decision phases are rejected`() {
        val start = MatchSetup.create(roster.first(), roster, 9)
        val saved = MatchSaveCodec.decodeMatch(MatchSaveCodec.encode(start.state, start.humanPlayerId))
        assertEquals(start.humanPlayerId, saved.humanPlayerId)
        assertNotEquals(start.state.players[1].id, saved.humanPlayerId)
        assertFailsWith<IllegalArgumentException> { MatchSaveCodec.encode(start.state.copy(phase = TurnPhase.DECISION)) }
        assertFailsWith<IllegalArgumentException> {
            MatchSaveCodec.encode(start.state.copy(players = start.state.players.map { it.copy(extraRerolls = -1) }))
        }
    }

    @Test fun `four stand-in bots complete matches through public controller actions`() =
        runTest {
            for (seed in listOf(10L, 20L, 30L)) {
                val initial = MatchSetup.create(roster.first(), roster, seed).state
                val controller = RealGameController(initial, backgroundScope, initial.players.map { it.id }.toSet(), botDelayMillis = 1)
                runCurrent()
                var steps = 0
                while (controller.state.value.result == null && steps++ < 2000) {
                    advanceTimeBy(1)
                    runCurrent()
                }
                assertNull(controller.failure.value)
                assertNotNull(controller.state.value.result)
                controller.close()
            }
        }
}
