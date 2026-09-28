package com.kingofxitter.app.ui.theme

import androidx.compose.ui.graphics.Color

/** Colour tokens from `design/screens/ui-kit.html`. Screens use these instead of raw hex values. */
object XColors {
    /** Screen background ("Night"). */
    val Night = Color(0xFF2A1F55)

    /** Dot pattern drawn over [Night]. */
    val NightDot = Color(0xFF3D3080)

    /** Outlines, hard shadows and text on paper ("Ink"). */
    val Ink = Color(0xFF1B1426)

    /** Cards, halos and text on night ("Paper"). */
    val Paper = Color(0xFFFFF7E6)

    /** Dark panels such as bot cards and your panel. */
    val Panel = Color(0xFF1F1742)

    /** Hard shadow under dark panels. */
    val PanelShadow = Color(0xFF120D29)

    /** Secondary text on night. */
    val Muted = Color(0xFFC9C2EE)

    /** Secondary text on paper. */
    val InkMuted = Color(0xFF5A5378)

    /** Disabled fills, empty pips and ring tracks. */
    val Disabled = Color(0xFF4A3F72)

    /** Ghost button outline. */
    val GhostBorder = Color(0xFF8C84B8)

    /** Mandates (victory points) and the primary button. */
    val Mandates = Color(0xFFFFD23F)

    /** Pressed primary button. */
    val MandatesPressed = Color(0xFFFFE06A)

    /** Followers (health) and likes. */
    val Followers = Color(0xFFFF3D6E)

    /** Characters (energy). */
    val Characters = Color(0xFFA6F03A)

    /** Tweet (claw) face. */
    val Tweet = Color(0xFFFF7A1A)

    /** Blue certification check (6–14 mandates). */
    val BlueCheck = Color(0xFF2F8CFF)

    /** Gold certification check (15+ mandates). */
    val GoldCheck = Color(0xFFFFB400)

    /** "Send it" button and hot actions. */
    val SendIt = Color(0xFFD81E54)

    /** Second colour of the incoming alarm bar's flash. */
    val AlarmFlash = Color(0xFFFF4F7E)

    /** RATIO'D and SUSPENDED stamps. */
    val Stamp = Color(0xFFE81E4E)

    /** Mentions and links inside tweets. */
    val Link = Color(0xFF1F5FD1)

    /** Damage preview chips. */
    val DamageTint = Color(0xFFFFB199)

    /** Character gain preview chips. */
    val CharactersTint = Color(0xFFE6FFB8)

    /** Background of the Xitter stage. */
    val Stage = Color(0xFFFFE9A8)

    /** Bot panel background while that bot is Main Character. */
    val MainCharacterPanel = Color(0xFF3A2A12)
}
