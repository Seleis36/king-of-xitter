package com.kingofxitter.content

import com.kingofxitter.engine.model.LeaderId

/**
 * One playable leader persona. The module stays pure Kotlin, so it carries `strings.xml` keys
 * instead of display text (ROADMAP §7) and an ARGB value instead of a Compose colour.
 *
 * [shortNameKey] is the panel name ("TRUMPET"), [firstNameKey] the small line above it
 * ("Donald J."), [fullNameKey] the tweet byline, and [handleKey] the `@handle`.
 * [voiceKey] is the one-line voice summary shown on Pick your leader.
 * [portraitKey] names the portrait asset; `:app` maps it to a drawable.
 */
data class Leader(
    val id: LeaderId,
    val firstNameKey: String,
    val shortNameKey: String,
    val fullNameKey: String,
    val handleKey: String,
    val voiceKey: String,
    val colorArgb: Long,
    val portraitKey: String,
)

/**
 * The six leaders of `design/screens/the-cast-and-their-tweet-voices.html`, in cast order.
 * [all] is the roster `MatchSetup.create` draws from, so its size and order are stable.
 */
object LeaderRoster {
    val trumpet = leader("trumpet", 0xFFFF8A1F)
    val poutine = leader("poutine", 0xFF9C7BFF)
    val kim = leader("kim", 0xFFFF4F4F)
    val macaron = leader("macaron", 0xFFFF8FC8)
    val xi = leader("xi", 0xFF22C08A)
    val mileon = leader("mi-leon", 0xFF5FB4F0, portraitKey = "mileon", stringSuffix = "mileon")

    /** Every leader, in the order the design lists the cast. */
    val all = listOf(trumpet, poutine, kim, macaron, xi, mileon)

    /** Leader IDs for `MatchSetup.create`, which requires exactly six distinct leaders. */
    val ids = all.map { it.id }

    private val byIdValue = all.associateBy { it.id }

    /** The leader with [id], or null when a save or fixture names an unknown one. */
    fun byId(id: LeaderId): Leader? = byIdValue[id]

    private fun leader(
        id: String,
        colorArgb: Long,
        portraitKey: String = id,
        stringSuffix: String = id,
    ) = Leader(
        id = LeaderId(id),
        firstNameKey = "leader_first_$stringSuffix",
        shortNameKey = "leader_short_$stringSuffix",
        fullNameKey = "leader_full_$stringSuffix",
        handleKey = "leader_handle_$stringSuffix",
        voiceKey = "leader_voice_$stringSuffix",
        colorArgb = colorArgb,
        portraitKey = portraitKey,
    )
}
