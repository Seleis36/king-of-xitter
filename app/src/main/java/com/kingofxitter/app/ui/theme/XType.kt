package com.kingofxitter.app.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.kingofxitter.app.R

/** Luckiest Guy: titles, buttons and big numbers. */
val LuckiestGuy = FontFamily(Font(R.font.luckiest_guy))

/** Anton: meme captions, stickers and stamps. Always upper case in the design. */
val Anton = FontFamily(Font(R.font.anton))

/** Rubik (variable weight): tweets and UI text. */
@OptIn(ExperimentalTextApi::class)
val Rubik =
    FontFamily(
        listOf(400, 500, 600, 700, 800, 900).map { weight ->
            Font(
                R.font.rubik,
                FontWeight(weight),
                variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
            )
        },
    )

/**
 * Text styles from `design/screens/ui-kit.html`. Sizes follow the 412dp design frame and are in
 * `sp`, so they scale with the user's font size.
 */
object XType {
    private val display = TextStyle(fontFamily = LuckiestGuy, fontWeight = FontWeight.Normal, letterSpacing = 0.02.em, lineHeight = 1.em)
    private val meme = TextStyle(fontFamily = Anton, fontWeight = FontWeight.Normal, letterSpacing = 0.03.em, lineHeight = 1.em)
    private val body = TextStyle(fontFamily = Rubik)

    /** Screen hero titles ("THE KIT", "INCOMING!" sized titles). */
    val DisplayHero = display.copy(fontSize = 64.sp)

    /** Screen titles ("TWEET IT"). */
    val DisplayLarge = display.copy(fontSize = 28.sp)

    /** Primary button labels. */
    val DisplayButton = display.copy(fontSize = 24.sp, letterSpacing = 0.03.em)

    /** Section headings ("ROLL 2 OF 4", "PICK YOUR ZINGER"). */
    val DisplayMedium = display.copy(fontSize = 20.sp)

    /** Panel names ("MACARON"). */
    val DisplaySmall = display.copy(fontSize = 16.sp)

    /** Compact names in bot panels. */
    val DisplayTiny = display.copy(fontSize = 12.5.sp)

    /** Large meme captions ("INCOMING!"). */
    val MemeHero = meme.copy(fontSize = 64.sp)

    /** Sticker text ("SAD!", "NYET."). */
    val Sticker = meme.copy(fontSize = 18.sp, letterSpacing = 0.04.em)

    /** Stamps ("RATIO'D", "SUSPENDED"). */
    val Stamp = meme.copy(fontSize = 24.sp, letterSpacing = 0.06.em)

    /** Tweet body. */
    val Tweet = body.copy(fontSize = 14.5.sp, fontWeight = FontWeight.Medium, lineHeight = 1.38.em)

    /** Large tweet body on the compose and incoming screens. */
    val TweetLarge = body.copy(fontSize = 16.5.sp, fontWeight = FontWeight.SemiBold, lineHeight = 1.38.em)

    /** Default UI text. */
    val Body = body.copy(fontSize = 13.5.sp, fontWeight = FontWeight.Medium, lineHeight = 1.35.em)

    /** Emphasised UI text (bubbles, zinger options). */
    val BodyStrong = body.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, lineHeight = 1.3.em)

    /** Small helper text ("Tap a die to keep it"). */
    val Caption = body.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold)

    /** Stat values next to icons. */
    val Stat = body.copy(fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 1.em)

    /** Chips. */
    val Chip = body.copy(fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)

    /** Tags (upper case in the design). */
    val Tag = body.copy(fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.04.em)

    /** Spaced labels ("IF YOU STOP NOW", "FOLLOWERS · HP"). */
    val Label = body.copy(fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.08.em)
}
