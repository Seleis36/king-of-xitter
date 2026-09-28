package com.kingofxitter.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val XitterColorScheme =
    darkColorScheme(
        primary = XColors.Mandates,
        onPrimary = XColors.Ink,
        secondary = XColors.Followers,
        onSecondary = XColors.Ink,
        tertiary = XColors.Characters,
        onTertiary = XColors.Ink,
        background = XColors.Night,
        onBackground = XColors.Paper,
        surface = XColors.Panel,
        onSurface = XColors.Paper,
        surfaceVariant = XColors.Disabled,
        onSurfaceVariant = XColors.Muted,
        outline = XColors.Ink,
        outlineVariant = XColors.GhostBorder,
        error = XColors.SendIt,
        onError = XColors.Paper,
    )

private val XitterTypography =
    Typography(
        displayLarge = XType.DisplayHero,
        displayMedium = XType.MemeHero,
        headlineLarge = XType.DisplayLarge,
        headlineMedium = XType.DisplayMedium,
        headlineSmall = XType.DisplaySmall,
        titleLarge = XType.DisplayMedium,
        titleMedium = XType.DisplaySmall,
        titleSmall = XType.BodyStrong,
        bodyLarge = XType.TweetLarge,
        bodyMedium = XType.Body,
        bodySmall = XType.Caption,
        labelLarge = XType.DisplayButton,
        labelMedium = XType.Chip,
        labelSmall = XType.Label,
    )

/**
 * App theme. Material components pick up the kit's colours and fonts through it; custom
 * components read [XColors] and [XType] directly.
 */
@Composable
fun XitterTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = XitterColorScheme, typography = XitterTypography, content = content)
}
