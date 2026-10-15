package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SprixDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF041E28),
    primaryContainer = Color(0xFF0C2B38),
    onPrimaryContainer = NeonCyan,

    secondary = GoldGradientStart,
    onSecondary = Color(0xFF291800),
    secondaryContainer = Color(0xFF452B00),
    onSecondaryContainer = Color(0xFFFFDDB3),

    tertiary = NeonPurple,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF3B1D54),
    onTertiaryContainer = Color(0xFFE9D5FF),

    background = CyberDarkBg,
    onBackground = TextPrimary,
    surface = CyberSurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = CyberCardBg,
    onSurfaceVariant = TextSecondary,
    outline = CyberCardBorder,
    error = TechError,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to deep cyberpunk tech theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SprixDarkColorScheme,
        typography = Typography,
        content = content
    )
}
