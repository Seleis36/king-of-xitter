package com.kingofxitter.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.kingofxitter.app.ui.theme.StickerShape
import com.kingofxitter.app.ui.theme.XColors
import com.kingofxitter.app.ui.theme.XType
import com.kingofxitter.app.ui.theme.sticker

/** Sticker-bomb text sticker (`.sb`): "SAD!", "NYET.", "¡AFUERA!". Upper-cased Anton on a haloed tile. */
@Composable
fun XTextSticker(
    text: String,
    fill: Color,
    modifier: Modifier = Modifier,
    rotation: Float = 0f,
    textColor: Color = XColors.Ink,
    textStyle: TextStyle = XType.Sticker,
) {
    Box(
        modifier =
            modifier
                .rotate(rotation)
                .sticker(
                    fill = fill,
                    shape = RoundedCornerShape(12.dp),
                    halo = 4.dp,
                    shadowOffset = DpOffset(4.dp, 6.dp),
                ).padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(text.uppercase(), style = textStyle, color = textColor, maxLines = 1)
    }
}

/** Rubber stamp (`.stamp`): "RATIO'D", "SUSPENDED". Tilted by default like the design. */
@Composable
fun XStamp(
    text: String,
    modifier: Modifier = Modifier,
    rotation: Float = -8f,
    textStyle: TextStyle = XType.Stamp,
    borderWidth: Dp = 5.dp,
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier =
            modifier
                .rotate(rotation)
                .background(XColors.Paper.copy(alpha = 0.92f), shape)
                .border(borderWidth, XColors.Stamp, shape)
                .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(text.uppercase(), style = textStyle, color = XColors.Stamp, maxLines = 1)
    }
}

/**
 * Display text with the kit's ink outline (`.outl` 7px, `.outl-s` 4px) and optional hard drop
 * shadow (`.drop`). Screen readers get the text once.
 */
@Composable
fun OutlinedText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    outline: Dp = 4.dp,
    dropShadow: Dp = 0.dp,
    textAlign: TextAlign? = null,
) {
    val strokePx = with(LocalDensity.current) { outline.toPx() }
    val outlined = style.copy(drawStyle = Stroke(width = strokePx, join = StrokeJoin.Round))
    Box(modifier.clearAndSetSemantics { contentDescription = text }) {
        if (dropShadow > 0.dp) {
            Text(text, style = outlined, color = XColors.Ink, textAlign = textAlign, modifier = Modifier.offset(y = dropShadow))
            Text(text, style = style, color = XColors.Ink, textAlign = textAlign, modifier = Modifier.offset(y = dropShadow))
        }
        Text(text, style = outlined, color = XColors.Ink, textAlign = textAlign)
        Text(text, style = style, color = color, textAlign = textAlign)
    }
}

/** Speech bubble (`.bubble`) for the Main Character's tweet on the Xitter stage. */
@Composable
fun XSpeechBubble(
    text: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = XType.BodyStrong,
) {
    Box(modifier.sticker(shadowOffset = DpOffset(0.dp, 5.dp)).padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(text, style = textStyle, color = XColors.Ink)
    }
}

/** Paper card (`.card`): tweets, the Xitter stage. */
@Composable
fun XCard(
    modifier: Modifier = Modifier,
    fill: Color = XColors.Paper,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier.sticker(fill = fill, shape = StickerShape), content = content)
}

/**
 * Dark panel (`.dark`): bot panels, your panel, the stay/log-off box. [accent] recolours the
 * outline, as the design does for your leader and the Main Character.
 */
@Composable
fun XPanel(
    modifier: Modifier = Modifier,
    fill: Color = XColors.Panel,
    accent: Color = XColors.Ink,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier.sticker(fill = fill, borderColor = accent, shadowColor = XColors.PanelShadow),
        content = content,
    )
}
