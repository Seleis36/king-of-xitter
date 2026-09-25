package com.kingofxitter.engine.model

/** Stable identity for one participant in a match. */
@JvmInline
value class PlayerId(
    val value: String,
)

/** Stable identity for one playable leader persona. */
@JvmInline
value class LeaderId(
    val value: String,
)

/** Stable identity for one power-up definition. */
@JvmInline
value class CardId(
    val value: String,
)

/** Identity for one die in the current roll. */
@JvmInline
value class DieId(
    val value: Int,
)

/** Identity for one pending decision. */
@JvmInline
value class DecisionId(
    val value: String,
)

/** Identity for one option within a pending decision. */
@JvmInline
value class DecisionOptionId(
    val value: String,
)

/** Identity for a counter added by a power-up effect. */
@JvmInline
value class CounterId(
    val value: String,
)
