package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val KernixDarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = OnCyberCyan,
    primaryContainer = ObsidianSurfaceHighest,
    onPrimaryContainer = CyberCyan,
    secondary = MatrixMint,
    onSecondary = OnMatrixMint,
    secondaryContainer = ObsidianSurfaceHigh,
    onSecondaryContainer = MatrixMint,
    tertiary = SolarAmber,
    onTertiary = ObsidianBg,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceHigh,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceHigh,
    surfaceContainerHigh = ObsidianSurfaceHighest,
    outline = ObsidianOutline,
    outlineVariant = ObsidianOutline,
    error = CrimsonCoral,
    onError = ObsidianBg
)

private val KernixShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun KernixTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = KernixDarkColorScheme,
        typography = KernixTypography,
        shapes = KernixShapes,
        content = content
    )
}
