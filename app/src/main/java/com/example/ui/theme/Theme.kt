package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val FuwaNimeDarkColorScheme = darkColorScheme(
    primary = FuwaPrimary,
    onPrimary = FuwaOnPrimary,
    primaryContainer = FuwaPrimaryContainer,
    onPrimaryContainer = FuwaOnPrimaryContainer,
    secondary = FuwaSecondary,
    onSecondary = FuwaOnSecondary,
    secondaryContainer = FuwaSecondaryContainer,
    onSecondaryContainer = FuwaOnSecondaryContainer,
    tertiary = FuwaTertiary,
    onTertiary = FuwaOnTertiary,
    tertiaryContainer = FuwaTertiaryContainer,
    onTertiaryContainer = FuwaOnTertiaryContainer,
    background = FuwaDarkBg,
    onBackground = TextPrimary,
    surface = FuwaDarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = FuwaDarkSurfaceContainer,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = FuwaDarkSurfaceContainer,
    surfaceContainerHigh = FuwaDarkSurfaceContainerHigh,
    surfaceContainerHighest = FuwaDarkSurfaceContainerHighest,
    outline = FuwaOutline,
    outlineVariant = FuwaOutlineVariant,
    error = DangerRed,
    onError = TextPrimary
)

@Composable
fun FuwaNimeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FuwaNimeDarkColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun AnimeIndoTheme(
    content: @Composable () -> Unit
) {
    FuwaNimeTheme(content = content)
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    FuwaNimeTheme(content = content)
}
