package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TacticalDarkColorScheme = darkColorScheme(
    primary = RadioCyan,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFF97F0FF),
    secondary = RadioGreen,
    onSecondary = Color(0xFF003915),
    secondaryContainer = Color(0xFF005322),
    onSecondaryContainer = Color(0xFF70FCA0),
    tertiary = RadioAmber,
    onTertiary = Color(0xFF452B00),
    tertiaryContainer = Color(0xFF653E00),
    onTertiaryContainer = Color(0xFFFFDDB8),
    error = RadioRed,
    onError = Color.White,
    background = RadioBackground,
    onBackground = RadioTextPrimary,
    surface = RadioSurface,
    onSurface = RadioTextPrimary,
    surfaceVariant = RadioSurfaceVariant,
    onSurfaceVariant = RadioTextSecondary,
    outline = RadioBorder,
    outlineVariant = RadioSurfaceElevated
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Walkie talkie interface is optimized for dark tactical theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TacticalDarkColorScheme,
        typography = Typography,
        content = content
    )
}
