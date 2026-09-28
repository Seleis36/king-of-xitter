package com.kingofxitter.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.kingofxitter.app.ui.theme.XColors
import com.kingofxitter.app.ui.theme.XType
import com.kingofxitter.app.ui.theme.sticker

/**
 * Pill-shaped chip (`.chip`): turn counter, policy names, "if you stop now" previews. Becomes a
 * button when [onClick] is set.
 */
@Composable
fun XChip(
    text: String,
    modifier: Modifier = Modifier,
    icon: XIcon? = null,
    fill: Color = XColors.Paper,
    textStyle: TextStyle = XType.Chip,
    minHeight: Dp = 30.dp,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier =
            modifier
                .heightIn(min = minHeight)
                .sticker(fill = fill, shape = CircleShape, borderWidth = 2.5.dp, shadowOffset = DpOffset.Zero)
                .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
                .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) XIconImage(icon, contentDescription = null, size = 18.dp, tint = XColors.Ink)
        Text(text, style = textStyle, color = XColors.Ink, maxLines = 1)
    }
}

/** Small upper-case tag (`.tag`): "Your turn", "on xitter", "−3M". */
@Composable
fun XTag(
    text: String,
    modifier: Modifier = Modifier,
    fill: Color = XColors.Mandates,
    icon: XIcon? = null,
    textStyle: TextStyle = XType.Tag,
) {
    Row(
        modifier =
            modifier
                .sticker(fill = fill, shape = RoundedCornerShape(8.dp), borderWidth = 2.dp, shadowOffset = DpOffset.Zero)
                .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) XIconImage(icon, contentDescription = null, size = 14.dp, tint = XColors.Ink)
        Text(text.uppercase(), style = textStyle, color = XColors.Ink, maxLines = 1)
    }
}
