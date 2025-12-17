package com.example.quickcut.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = BrightOrange,
    secondary = BrightCyan,
    tertiary = BrightPink,
    background = LightBackground,
    surface = CardLight,
    onPrimary = TextLight,
    onSecondary = TextDark,
    onTertiary = TextLight,
    onBackground = TextDark,
    onSurface = TextDark,
)

@Composable
fun QuickcutTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
