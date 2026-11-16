package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val XavionDarkColorScheme = darkColorScheme(
    primary = XavionCyan,
    onPrimary = XavionBlack,
    primaryContainer = XavionDeepBlue,
    onPrimaryContainer = XavionCyanSoft,
    secondary = XavionCobalt,
    onSecondary = XavionTextPrimary,
    secondaryContainer = XavionPanelElevated,
    onSecondaryContainer = XavionTextPrimary,
    tertiary = XavionViolet,
    onTertiary = XavionTextPrimary,
    background = XavionBlack,
    onBackground = XavionTextPrimary,
    surface = XavionObsidian,
    onSurface = XavionTextPrimary,
    surfaceVariant = XavionPanelDark,
    onSurfaceVariant = XavionTextSecondary,
    outline = XavionBorderSubtle,
    outlineVariant = XavionBorderBright,
    error = XavionCrimson,
    onError = XavionTextPrimary
)

val XavionShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun XavionTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = XavionDarkColorScheme,
        typography = XavionTypography,
        shapes = XavionShapes,
        content = content
    )
}
