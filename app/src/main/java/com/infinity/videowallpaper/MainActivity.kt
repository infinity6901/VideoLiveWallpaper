package com.infinity.videowallpaper

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.infinity.videowallpaper.ui.screens.HomeScreen
import com.infinity.videowallpaper.ui.screens.WallpaperSettingsScreen
import com.infinity.videowallpaper.ui.theme.VideoLiveWallpaperTheme
import com.infinity.videowallpaper.ui.theme.resolveIsDarkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableDisplayCutout()
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
                AppNavGraph()
            }
        }
    }

    private fun enableDisplayCutout() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(onNavigateToSettings = { navController.navigate("settings") })
        }
        composable("settings") {
            WallpaperSettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
