package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SkylineDarkColorScheme = darkColorScheme(
    primary = SkyBluePrimary,
    onPrimary = Color(0xFF082F49),
    primaryContainer = Color(0xFF0C4A6E),
    onPrimaryContainer = SkyBlueLight,
    secondary = EmeraldAccent,
    onSecondary = Color(0xFF022C22),
    tertiary = AmberGold,
    onTertiary = Color(0xFF451A03),
    background = Color(0xFF0B1120),
    onBackground = TextPrimaryLight,
    surface = SlateCardDark,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = TextSecondaryLight,
    error = CoralAlert,
    onError = Color.White,
    outline = SlateBorderDark
)

@Composable
fun SkylineMayorTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SkylineDarkColorScheme,
        typography = AppTypography,
        content = content
    )
}
