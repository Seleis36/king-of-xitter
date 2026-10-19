package com.kingofxitter.app.ui.timeline

import com.kingofxitter.app.content.VoiceEntry
import com.kingofxitter.content.TweetMoment
import com.kingofxitter.engine.GameFixtures
import com.kingofxitter.engine.model.CardId
import com.kingofxitter.engine.model.GameState

/**
 * A timeline with one entry per kind of outcome tag, so a preview shows every shape the screen
 * can draw. The text follows the design page's tweets rather than generating V1 lines, which a
 * preview has no resources for.
 */
object TimelineSamples {
    val state: GameState = GameFixtures.timeline
    val humanId = state.players.first().id

    private val you = state.players[0]
    private val kim = state.players[1]
    private val poutine = state.players[2]

    val entries: List<VoiceEntry> =
        listOf(
            VoiceEntry(
                playerId = poutine.id,
                leaderId = poutine.leaderId,
                moment = TweetMoment.BOUGHT_POWER_UP,
                targetId = null,
                text = "Mr. Macaron. Please, sit. The table is only six meters long. We talk.",
                turnNumber = 2,
                cardId = CardId("c04_thick_skin_pr"),
                id = 1,
            ),
            VoiceEntry(
                playerId = kim.id,
                leaderId = kim.leaderId,
                moment = TweetMoment.ROLLED,
                targetId = null,
                text = "The Respected Supreme Leader rolled 3-3-3 on the first try, as he does every time.",
                turnNumber = 3,
                id = 2,
            ),
            VoiceEntry(
                playerId = you.id,
                leaderId = you.leaderId,
                moment = TweetMoment.ATTACKED,
                targetId = kim.id,
                text = "Cher @SupremeFunLeader, en même temps: you are finished. I will explain why in a 47-part thread. 1/47",
                turnNumber = 3,
                unfollows = 3,
                id = 3,
            ),
            VoiceEntry(
                playerId = kim.id,
                leaderId = kim.leaderId,
                moment = TweetMoment.GOT_HIT,
                targetId = you.id,
                text = "@EmmanuelMacaron did not hit the Supreme Leader. The Supreme Leader absorbed a tribute.",
                turnNumber = 3,
                unfollows = 3,
                id = 4,
            ),
            VoiceEntry(
                playerId = you.id,
                leaderId = you.leaderId,
                moment = TweetMoment.LEFT_XITTER,
                targetId = kim.id,
                text = "Mes chers compatriotes, I yield Xitter to @SupremeFunLeader. En même temps, I do not. 1/47",
                turnNumber = 4,
                id = 5,
            ),
            VoiceEntry(
                playerId = kim.id,
                leaderId = kim.leaderId,
                moment = TweetMoment.ENTERED_XITTER,
                targetId = you.id,
                text = "The Supreme Leader has taken Xitter, as written in the five-year plan for Xitter.",
                turnNumber = 4,
                id = 6,
            ),
        )
}
