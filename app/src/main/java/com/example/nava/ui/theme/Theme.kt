package com.example.nava.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = NavaGold,
    onPrimary = NavaBlack,
    secondary = NavaGray,
    background = NavaBlack,
    onBackground = NavaWhite,
    surface = NavaNavy,
    onSurface = NavaWhite,
    surfaceVariant = NavaNavySurface,
    onSurfaceVariant = NavaGray,
)

private val LightColors = lightColorScheme(
    primary = NavaGold,
    onPrimary = NavaBlack,
    secondary = NavaNavy,
    background = NavaWhite,
    onBackground = NavaBlack,
    surface = NavaWhite,
    onSurface = NavaBlack,
    surfaceVariant = NavaGray,
    onSurfaceVariant = NavaMuted,
)

@Composable
fun NavaTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        shapes = NavaShapes,
        content = content,
    )
}
