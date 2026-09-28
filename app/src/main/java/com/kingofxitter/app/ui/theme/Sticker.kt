package com.kingofxitter.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Default corner shape of cards, buttons and panels. */
val StickerShape = RoundedCornerShape(18.dp)

/**
 * The kit's sticker-bomb look: a 3dp ink outline, an optional paper halo, and a hard drop
 * shadow with no blur. Draws behind the content and doesn't change layout, so callers add their
 * own padding (at least [borderWidth]) and leave room for the shadow and halo.
 *
 * `.card` is the default. `.stk` is `halo = 4.dp, shadowOffset = DpOffset(4.dp, 7.dp)`.
 * `.dark` is `fill = XColors.Panel, shadowColor = XColors.PanelShadow`.
 */
fun Modifier.sticker(
    fill: Color = XColors.Paper,
    shape: Shape = StickerShape,
    borderWidth: Dp = 3.dp,
    borderColor: Color = XColors.Ink,
    halo: Dp = 0.dp,
    haloColor: Color = XColors.Paper,
    shadowOffset: DpOffset = DpOffset(0.dp, 6.dp),
    shadowColor: Color = XColors.Ink,
): Modifier =
    drawBehind {
        val haloPx = halo.toPx()
        val shadowX = shadowOffset.x.toPx()
        val shadowY = shadowOffset.y.toPx()
        if (shadowX != 0f || shadowY != 0f) {
            drawShape(shape, shadowColor, grow = haloPx, offset = Offset(shadowX, shadowY))
        }
        if (haloPx > 0f) drawShape(shape, haloColor, grow = haloPx)
        // Fill and border share one path inset by half the border, so no fill shows past the outline.
        val borderPx = borderWidth.toPx()
        val inset = borderPx / 2
        val outline = shape.createOutline(Size(size.width - borderPx, size.height - borderPx), layoutDirection, this)
        translate(inset, inset) {
            drawOutline(outline, fill)
            if (borderPx > 0f) drawOutline(outline, borderColor, style = Stroke(borderPx))
        }
    }

private fun DrawScope.drawShape(
    shape: Shape,
    color: Color,
    grow: Float = 0f,
    offset: Offset = Offset.Zero,
) {
    val outline = shape.createOutline(Size(size.width + grow * 2, size.height + grow * 2), layoutDirection, this)
    translate(offset.x - grow, offset.y - grow) { drawOutline(outline, color) }
}

/** Night background with the kit's 16dp dot grid. Use it on every full screen. */
fun Modifier.nightBackground(): Modifier =
    drawWithCache {
        val cell =
            16
                .dp
                .toPx()
                .roundToInt()
                .coerceAtLeast(1)
        val tile = ImageBitmap(cell, cell)
        Canvas(tile).apply {
            drawRect(0f, 0f, cell.toFloat(), cell.toFloat(), Paint().apply { color = XColors.Night })
            drawCircle(Offset(cell / 2f, cell / 2f), 1.6.dp.toPx(), Paint().apply { color = XColors.NightDot })
        }
        val brush = ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated))
        onDrawBehind { drawRect(brush) }
    }
