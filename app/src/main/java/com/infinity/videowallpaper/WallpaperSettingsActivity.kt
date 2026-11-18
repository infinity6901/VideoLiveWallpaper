package com.infinity.videowallpaper

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.infinity.videowallpaper.ui.screens.WallpaperSettingsScreen
import com.infinity.videowallpaper.ui.theme.VideoLiveWallpaperTheme
import com.infinity.videowallpaper.ui.theme.resolveIsDarkTheme

/**
 * Launched by the system wallpaper picker when the user taps "Settings".
 * Hosts the same [WallpaperSettingsScreen] Composable with synced theme & edge-to-edge bars.
 */
class WallpaperSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        val repository = (application as VideoWallpaperApp).repository
        setContent {
            val config by repository.config.collectAsStateWithLifecycle()
            val isDark = resolveIsDarkTheme(config.themeMode)

            SideEffect {
                val style = if (isDark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }

            VideoLiveWallpaperTheme(
                themeMode = config.themeMode,
                dynamicColor = config.dynamicColor,
            ) {
                WallpaperSettingsScreen(onNavigateBack = { finish() })
            }
        }
    }
}
