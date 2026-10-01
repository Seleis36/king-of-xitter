package com.kingofxitter.cards

import com.kingofxitter.engine.model.CardId

/**
 * Static definition of one power-up card.
 *
 * [id] is the stable [CardId] used by the engine to bind effects.
 * [nameKey] is the `strings.xml` key for the displayed card name.
 * [cost] is the character cost in the shop (already ×10 vs. the KoT original).
 * [type] distinguishes Policies (kept) from Executive Orders (discarded on play).
 * [rulesKey] is the `strings.xml` key for the rules text shown on the card.
 * [flavourKey] is the `strings.xml` key for the italicised flavour line on the card.
 * [count] is how many copies of this card are in the standard deck (usually 1, 2 for duplicates).
 */
data class CardDef(
    val id: CardId,
    val nameKey: String,
    val cost: Int,
    val type: CardType,
    val rulesKey: String,
    val flavourKey: String,
    val count: Int = 1,
)

/** Whether the power-up is kept on the table or discarded immediately after use. */
enum class CardType {
    /** Kept in front of the player; its hooks run every relevant window. */
    POLICY,

    /** Resolved immediately on purchase and sent to the discard pile. */
    EXECUTIVE_ORDER,
}
