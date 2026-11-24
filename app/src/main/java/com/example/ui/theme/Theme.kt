package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val AetherDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF0E3D33),
    onPrimaryContainer = Color(0xFF8FF5D8),
    secondary = CyanAccent,
    onSecondary = Color(0xFF062535),
    secondaryContainer = Color(0xFF103247),
    onSecondaryContainer = Color(0xFFBBE8FF),
    tertiary = ModeStorytellerColor,
    onTertiary = Color(0xFF1C1033),
    background = ObsidianBackground,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianBackground,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceVariant,
    surfaceContainerHigh = ObsidianElevated,
    outline = ObsidianBorder,
    outlineVariant = ObsidianBorderBright,
    error = ErrorRose,
    onError = Color.White,
    errorContainer = ErrorContainerDark,
    onErrorContainer = Color(0xFFFFD9DF)
)

val AetherShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AetherAiTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AetherDarkColorScheme,
        typography = AppTypography,
        shapes = AetherShapes,
        content = content
    )
}
