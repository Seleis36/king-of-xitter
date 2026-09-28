package com.kingofxitter.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingofxitter.app.ui.theme.Rubik
import com.kingofxitter.app.ui.theme.XColors
import com.kingofxitter.app.ui.theme.XType
import com.kingofxitter.app.ui.theme.sticker

/** Button variants from the kit (`.btn`, `.alt`, `.hot`, `.ghost`). Disabled is automatic. */
enum class XButtonStyle(
    internal val fill: Color,
    internal val content: Color,
    internal val border: Color,
    internal val shadow: Color?,
) {
    Primary(XColors.Mandates, XColors.Ink, XColors.Ink, XColors.Ink),
    Secondary(XColors.Paper, XColors.Ink, XColors.Ink, XColors.Ink),
    Hot(XColors.SendIt, XColors.Paper, XColors.Ink, XColors.Ink),
    Ghost(Color.Transparent, XColors.Paper, XColors.GhostBorder, null),
}

private const val PRESS_DEPTH = 4

/** Smallest a button label shrinks to, relative to its style. */
private const val MIN_LABEL_SCALE = 0.6f

/**
 * The kit's chunky button. Pressing pushes it into its shadow. [subtitle] is the small Rubik line
 * under the label ("+2 mandates next turn"), and [icon] sits before the label.
 */
@Composable
fun XButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: XButtonStyle = XButtonStyle.Primary,
    enabled: Boolean = true,
    subtitle: String? = null,
    icon: XIcon? = null,
    minHeight: Dp = 58.dp,
    textStyle: TextStyle = XType.DisplayButton,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val fill = if (enabled) style.fill else XColors.Disabled
    val content = if (enabled) style.content else XColors.Muted
    val border = if (enabled) style.border else XColors.Ink
    val shadow = if (enabled) style.shadow else XColors.PanelShadow
    val depth = if (pressed && shadow != null) PRESS_DEPTH.dp else 0.dp
    Box(
        modifier =
            modifier
                .heightIn(min = minHeight)
                .offset(y = depth)
                .sticker(
                    fill = fill,
                    borderColor = border,
                    shadowOffset = DpOffset(0.dp, if (shadow == null) 0.dp else 6.dp - depth),
                    shadowColor = shadow ?: Color.Transparent,
                ).clickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                ).padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (icon != null) XIconImage(icon, contentDescription = null, size = 24.dp, tint = content)
                // Labels stay on one line: at large font scales or on narrow phones they shrink
                // instead of breaking mid-word ("RERO / LL").
                BasicText(
                    text,
                    style = textStyle.copy(color = content, textAlign = TextAlign.Center),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    autoSize = TextAutoSize.StepBased(minFontSize = textStyle.fontSize * MIN_LABEL_SCALE, maxFontSize = textStyle.fontSize),
                    modifier = Modifier.weight(1f, fill = false).padding(top = 3.dp),
                )
            }
            if (subtitle != null) {
                Text(
                    subtitle,
                    fontFamily = Rubik,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = content,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Square 48dp icon button (`.ibtn`) for the board's top bar and screen headers. */
@Composable
fun XIconButton(
    icon: XIcon,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val depth = if (pressed) 3.dp else 0.dp
    Box(
        modifier =
            modifier
                .size(48.dp)
                .offset(y = depth)
                .sticker(shape = RoundedCornerShape(14.dp), shadowOffset = DpOffset(0.dp, 4.dp - depth))
                .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        XIconImage(icon, contentDescription, size = 26.dp, tint = XColors.Ink)
    }
}

/**
 * A choice row (the zinger options on the compose screen): radio dot, text, and a yellow fill
 * with a slight tilt when [selected].
 */
@Composable
fun XOptionCard(
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier =
            modifier
                .rotate(if (selected) -1f else 0f)
                .sticker(
                    fill = if (selected) XColors.Mandates else XColors.Paper,
                    shape = RoundedCornerShape(16.dp),
                    shadowOffset = DpOffset(0.dp, 5.dp),
                ).selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .padding(top = 1.dp)
                    .size(22.dp)
                    .sticker(
                        fill = if (selected) XColors.Ink else XColors.Paper,
                        shape = CircleShape,
                        shadowOffset = DpOffset.Zero,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(
                    Modifier
                        .size(8.dp)
                        .sticker(
                            fill = XColors.Mandates,
                            shape = CircleShape,
                            borderWidth = 0.dp,
                            shadowOffset = DpOffset.Zero,
                        ),
                )
            }
        }
        content()
    }
}
