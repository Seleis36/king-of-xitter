package com.kingofxitter.engine.model

import kotlinx.serialization.Serializable

/** Stable identity for one participant in a match. */
@JvmInline
@Serializable
value class PlayerId(
    val value: String,
)

/** Stable identity for one playable leader persona. */
@JvmInline
@Serializable
value class LeaderId(
    val value: String,
)

/** Stable identity for one power-up definition. */
@JvmInline
@Serializable
value class CardId(
    val value: String,
)

/** Identity for one die in the current roll. */
@JvmInline
@Serializable
value class DieId(
    val value: Int,
)

/** Identity for one pending decision. */
@JvmInline
@Serializable
value class DecisionId(
    val value: String,
)

/** Identity for one option within a pending decision. */
@JvmInline
@Serializable
value class DecisionOptionId(
    val value: String,
)

/** Identity for a counter added by a power-up effect. */
@JvmInline
@Serializable
value class CounterId(
    val value: String,
)
