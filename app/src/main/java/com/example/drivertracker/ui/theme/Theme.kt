package com.example.drivertracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF91CC8A),
    onPrimary = Color(0xFF102510),
    primaryContainer = Color(0xFF315437),
    onPrimaryContainer = Color(0xFFD0ECCC),
    secondary = Color(0xFFC4C6CE),
    onSecondary = Color(0xFF292A30),
    secondaryContainer = Color(0xFF44464E),
    onSecondaryContainer = Color(0xFFE1E2E9),
    tertiary = Color(0xFFC2C6C9),
    background = Color(0xFF242528),
    onBackground = Color(0xFFF0F0F2),
    surface = Color(0xFF2D2E32),
    onSurface = Color(0xFFF0F0F2),
    surfaceVariant = Color(0xFF3A3B40),
    onSurfaceVariant = Color(0xFFD0D1D6),
    outline = Color(0xFF999BA3),
    outlineVariant = Color(0xFF585A61),
    error = Color(0xFFFFB4AB)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF16803C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8F3DC),
    onPrimaryContainer = Color(0xFF062D14),
    secondary = Color(0xFF5F6368),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE4E6E8),
    onSecondaryContainer = Color(0xFF25272A),
    tertiary = Color(0xFF646A70),
    background = Color(0xFFF0F2F3),
    onBackground = Color(0xFF1F2328),
    surface = Color.White,
    onSurface = Color(0xFF1F2328),
    surfaceDim = Color(0xFFD9DBDE),
    surfaceBright = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF8F9FA),
    surfaceContainer = Color(0xFFE6E9EC),
    surfaceContainerHigh = Color(0xFFDDE1E5),
    surfaceContainerHighest = Color(0xFFD4D8DD),
    surfaceVariant = Color(0xFFD9DDE1),
    onSurfaceVariant = Color(0xFF4C5259),
    outline = Color(0xFF777D84),
    outlineVariant = Color(0xFFC9CDD2),
    error = Color(0xFFBA1A1A)
)

@Composable
fun DriverTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),

    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
