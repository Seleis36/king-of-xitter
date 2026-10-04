package com.kingofxitter.content

import com.kingofxitter.engine.GameFixtures
import com.kingofxitter.engine.model.CardId
import com.kingofxitter.engine.model.DiceFace
import com.kingofxitter.engine.model.Die
import com.kingofxitter.engine.model.DieId
import com.kingofxitter.engine.model.GameEvent
import com.kingofxitter.engine.model.MatchResult
import com.kingofxitter.engine.model.PlayerResource
import com.kingofxitter.engine.model.ResourceChangeReason
import com.kingofxitter.engine.model.VictoryReason
import kotlin.test.Test
import kotlin.test.assertEquals

class TweetMomentsTest {
    private val state = GameFixtures.yourRoll
    private val you = state.currentPlayerId
    private val kim = state.players[1].id

    @Test
    fun `only the first roll of a turn is worth a tweet`() {
        val dice = listOf(Die(DieId(0), DiceFace.TWEET))
        assertEquals(
            listOf(TweetTrigger(you, TweetEvent(TweetMoment.ROLLED))),
            TweetMoments.of(GameEvent.DiceRolled(you, 1, dice), state),
        )
        assertEquals(emptyList(), TweetMoments.of(GameEvent.DiceRolled(you, 2, dice), state))
    }

    @Test
    fun `a landed tweet gives the attacker and the victim a line each`() {
        val hit = GameEvent.ResourceChanged(kim, PlayerResource.FOLLOWERS, -3, 7, ResourceChangeReason.TWEET)
        assertEquals(
            listOf(
                TweetTrigger(you, TweetEvent(TweetMoment.ATTACKED, state.players[1].leaderId)),
                TweetTrigger(kim, TweetEvent(TweetMoment.GOT_HIT, state.players[0].leaderId)),
            ),
            TweetMoments.of(hit, state),
        )
    }

    @Test
    fun `other resource changes are quiet`() {
        val like = GameEvent.ResourceChanged(kim, PlayerResource.FOLLOWERS, 1, 8, ResourceChangeReason.LIKE)
        val selfDamage = GameEvent.ResourceChanged(you, PlayerResource.FOLLOWERS, -3, 4, ResourceChangeReason.TWEET)
        assertEquals(emptyList(), TweetMoments.of(like, state))
        assertEquals(emptyList(), TweetMoments.of(selfDamage, state))
    }

    @Test
    fun `a change of Main Character makes both leaders post`() {
        assertEquals(
            listOf(
                TweetTrigger(kim, TweetEvent(TweetMoment.ENTERED_XITTER, state.players[0].leaderId)),
                TweetTrigger(you, TweetEvent(TweetMoment.LEFT_XITTER, state.players[1].leaderId)),
            ),
            TweetMoments.of(GameEvent.MainCharacterChanged(you, kim), state),
        )
        assertEquals(
            listOf(TweetTrigger(you, TweetEvent(TweetMoment.ENTERED_XITTER))),
            TweetMoments.of(GameEvent.MainCharacterChanged(null, you), state),
        )
    }

    @Test
    fun `buying and suspension have their own moments`() {
        assertEquals(
            listOf(TweetTrigger(you, TweetEvent(TweetMoment.BOUGHT_POWER_UP))),
            TweetMoments.of(GameEvent.PowerUpBought(you, CardId("c12_bot_farm")), state),
        )
        assertEquals(
            listOf(TweetTrigger(kim, TweetEvent(TweetMoment.SUSPENDED))),
            TweetMoments.of(GameEvent.PlayerSuspended(kim), state),
        )
    }

    @Test
    fun `the end of a match gives the winner a win and the survivors a loss`() {
        val finished = GameFixtures.standings
        val triggers = TweetMoments.of(GameEvent.MatchEnded(finished.result!!), finished)
        val winner = (finished.result as MatchResult.Victory).playerId
        assertEquals(TweetMoment.WON, triggers.first { it.playerId == winner }.event.moment)
        assertEquals(
            finished.players
                .filter { it.id != winner && !it.isSuspended }
                .map { it.id }
                .toSet(),
            triggers.filter { it.event.moment == TweetMoment.LOST }.map { it.playerId }.toSet(),
        )
    }

    @Test
    fun `nobody winning still lets the survivors speak`() {
        val noWinner = GameFixtures.noWinner
        assertEquals(emptyList(), TweetMoments.of(GameEvent.MatchEnded(MatchResult.NoWinner), noWinner))
        val living = noWinner.copy(players = noWinner.players.mapIndexed { index, p -> if (index == 0) p.copy(followers = 3) else p })
        assertEquals(
            listOf(TweetTrigger(living.players[0].id, TweetEvent(TweetMoment.LOST))),
            TweetMoments.of(GameEvent.MatchEnded(MatchResult.NoWinner), living),
        )
    }

    @Test
    fun `a victory for the last standing account is still a win`() {
        val survivor = GameFixtures.survivorVictory
        val triggers =
            TweetMoments.of(
                GameEvent.MatchEnded(MatchResult.Victory(survivor.currentPlayerId, VictoryReason.LAST_ACCOUNT_STANDING)),
                survivor,
            )
        assertEquals(listOf(TweetTrigger(survivor.currentPlayerId, TweetEvent(TweetMoment.WON))), triggers)
    }
}
