package com.example.videowallpaper.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Palette ────────────────────────────────────────────────────────────────────

val DeepNavy     = Color(0xFF0D1B2A)
val MidnightBlue = Color(0xFF1B263B)
val SlateBlue    = Color(0xFF415A77)
val SteelBlue    = Color(0xFF778DA9)
val Ivory        = Color(0xFFE0E1DD)
val Coral        = Color(0xFFE94560)
val CoralDark    = Color(0xFFB5253C)

// ── Dark scheme (primary UI) ───────────────────────────────────────────────────

private val DarkColors = darkColorScheme(
    primary          = Coral,
    onPrimary        = Color.White,
    primaryContainer = CoralDark,
    secondary        = SteelBlue,
    onSecondary      = DeepNavy,
    background       = DeepNavy,
    onBackground     = Ivory,
    surface          = MidnightBlue,
    onSurface        = Ivory,
    surfaceVariant   = SlateBlue,
    onSurfaceVariant = Ivory,
    outline          = SlateBlue,
)

// ── Light scheme ───────────────────────────────────────────────────────────────

private val LightColors = lightColorScheme(
    primary          = CoralDark,
    onPrimary        = Color.White,
    secondary        = SlateBlue,
    onSecondary      = Color.White,
    background       = Color(0xFFF5F5F5),
    onBackground     = Color(0xFF1A1A2E),
    surface          = Color.White,
    onSurface        = Color(0xFF1A1A2E),
)

@Composable
fun VideoLiveWallpaperTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content     = content
    )
}
