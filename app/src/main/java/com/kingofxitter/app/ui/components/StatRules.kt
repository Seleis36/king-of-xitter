package com.kingofxitter.app.ui.components

/** Mandates needed to win (ROADMAP §1). */
const val MANDATES_TO_WIN = 20

/** Characters at which the ring is drawn full; above this it stays full (ROADMAP §1, decision 1). */
const val CHARACTER_RING_CAPACITY = 280

/** Cosmetic certification badge from ROADMAP §1: 0–5 none, 6–14 blue, 15+ gold. */
enum class Certification(
    val minimumMandates: Int,
    val icon: XIcon,
) {
    Unverified(0, XIcon.CheckUnverified),
    Blue(6, XIcon.CheckBlue),
    Gold(15, XIcon.CheckGold),
    ;

    companion object {
        /** Badge for a mandate total. */
        fun of(mandates: Int): Certification = entries.last { mandates >= it.minimumMandates }
    }
}

/** The next milestone on the mandate bar and how far away it is. */
data class MandateGoal(
    val kind: Kind,
    val remaining: Int,
) {
    /** Milestones in the order they are reached. */
    enum class Kind { BlueCheck, GoldCheck, Win }

    companion object {
        /** Next milestone after [mandates]: blue check, then gold check, then 20 to win. */
        fun after(mandates: Int): MandateGoal =
            when {
                mandates < Certification.Blue.minimumMandates -> MandateGoal(Kind.BlueCheck, Certification.Blue.minimumMandates - mandates)
                mandates < Certification.Gold.minimumMandates -> MandateGoal(Kind.GoldCheck, Certification.Gold.minimumMandates - mandates)
                else -> MandateGoal(Kind.Win, (MANDATES_TO_WIN - mandates).coerceAtLeast(0))
            }
    }
}

/** Fraction of the mandate bar that is filled, 0..1. */
fun mandateFraction(mandates: Int): Float = (mandates.toFloat() / MANDATES_TO_WIN).coerceIn(0f, 1f)

/** Fraction of the characters ring that is filled, 0..1. */
fun characterRingFraction(characters: Int): Float = (characters.toFloat() / CHARACTER_RING_CAPACITY).coerceIn(0f, 1f)

/** One follower pip: filled up to the current followers, broken up to the maximum. */
enum class FollowerPip { Full, Lost }

/** Pips for a follower bar. Bot Farm can raise the maximum above 10, which adds pips. */
fun followerPips(
    followers: Int,
    maxFollowers: Int,
): List<FollowerPip> {
    val max = maxFollowers.coerceAtLeast(0)
    val current = followers.coerceIn(0, max)
    return List(max) { index -> if (index < current) FollowerPip.Full else FollowerPip.Lost }
}
