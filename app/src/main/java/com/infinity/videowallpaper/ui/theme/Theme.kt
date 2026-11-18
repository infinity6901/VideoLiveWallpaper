package com.infinity.videowallpaper.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.infinity.videowallpaper.data.ThemeMode

val DeepNavy     = Color(0xFF0D1B2A)
val MidnightBlue = Color(0xFF1B263B)
val SlateBlue    = Color(0xFF415A77)
val SteelBlue    = Color(0xFF778DA9)
val MutedSteel   = Color(0xFFA9B8CC)
val Ivory        = Color(0xFFE0E1DD)
val Coral        = Color(0xFFE94560)
val CoralDark    = Color(0xFFB5253C)

private val DarkColors = darkColorScheme(
    primary          = Coral,
    onPrimary        = Color.White,
    primaryContainer = CoralDark,
    onPrimaryContainer = Color.White,
    secondary        = SteelBlue,
    onSecondary      = DeepNavy,
    background       = DeepNavy,
    onBackground     = Ivory,
    surface          = MidnightBlue,
    onSurface        = Ivory,
    surfaceVariant   = SlateBlue,
    onSurfaceVariant = MutedSteel,
    outline          = SlateBlue,
)

private val AmoledColors = DarkColors.copy(
    background     = Color.Black,
    surface        = Color(0xFF10141A),
    surfaceVariant = Color(0xFF1D2430),
)

private val LightColors = lightColorScheme(
    primary          = CoralDark,
    onPrimary        = Color.White,
    primaryContainer = Color(0xFFFFD9DF),
    onPrimaryContainer = Color(0xFF3F0011),
    secondary        = SlateBlue,
    onSecondary      = Color.White,
    background       = Color(0xFFF5F7FA),
    onBackground     = Color(0xFF141B29),
    surface          = Color.White,
    onSurface        = Color(0xFF141B29),
    surfaceVariant   = Color(0xFFE4E9F1),
    onSurfaceVariant = Color(0xFF475569),
    outline          = Color(0xFF94A3B8),
)

@Composable
fun resolveIsDarkTheme(themeMode: ThemeMode): Boolean {
    return when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
    }
}

@Composable
fun VideoLiveWallpaperTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val isDark = resolveIsDarkTheme(themeMode)
    val context = LocalContext.current

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val base = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (themeMode == ThemeMode.AMOLED) base.copy(background = Color.Black) else base
        }
        themeMode == ThemeMode.AMOLED -> AmoledColors
        isDark -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
