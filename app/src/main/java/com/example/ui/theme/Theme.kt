package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GalaxyProDarkColorScheme = darkColorScheme(
    primary = GalaxyLaserAmber,
    onPrimary = Color(0xFF141000),
    primaryContainer = GalaxyAmberDim,
    onPrimaryContainer = Color(0xFFFFE082),
    secondary = OpticCyan,
    onSecondary = Color(0xFF001F24),
    secondaryContainer = OpticCyanDim,
    onSecondaryContainer = Color(0xFFB2EBF2),
    tertiary = GenerativeViolet,
    onTertiary = Color(0xFF1B093B),
    tertiaryContainer = GenerativeVioletDim,
    onTertiaryContainer = Color(0xFFEDE7F6),
    background = ObsidianBackground,
    onBackground = TextPrimary,
    surface = CharcoalSurface,
    onSurface = TextPrimary,
    surfaceVariant = ControlPodSurface,
    onSurfaceVariant = TextSecondary,
    outline = TitaniumBorder
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GalaxyProDarkColorScheme,
        typography = Typography,
        content = content
    )
}
