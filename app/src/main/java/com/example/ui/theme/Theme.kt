package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val NoirColorScheme = darkColorScheme(
    primary = NoirAccent,
    onPrimary = NoirOnAccent,
    primaryContainer = NoirSurfaceElevated,
    onPrimaryContainer = NoirTextPrimary,
    secondary = NoirTextSecondary,
    onSecondary = NoirBackgroundPrimary,
    secondaryContainer = NoirBackgroundSecondary,
    onSecondaryContainer = NoirTextPrimary,
    tertiary = NoirSuccess,
    onTertiary = NoirTextPrimary,
    background = NoirBackgroundPrimary,
    onBackground = NoirTextPrimary,
    surface = NoirBackgroundSecondary,
    onSurface = NoirTextPrimary,
    surfaceVariant = NoirSurfaceElevated,
    onSurfaceVariant = NoirTextSecondary,
    outline = NoirBorder,
    outlineVariant = NoirBorderSubtle,
    error = NoirDanger,
    onError = NoirTextPrimary
)

// Architectural, sharp-edged shapes adhering to Noir minimalism (no overly rounded bubbles)
val NoirShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(3.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(6.dp),
    extraLarge = RoundedCornerShape(8.dp)
)

@Composable
fun NoirStrideTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NoirColorScheme,
        typography = NoirTypography,
        shapes = NoirShapes,
        content = content
    )
}
