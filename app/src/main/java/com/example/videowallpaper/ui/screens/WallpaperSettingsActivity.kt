package com.example.videowallpaper.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.videowallpaper.ui.theme.VideoLiveWallpaperTheme

/**
 * Launched by the system wallpaper picker when the user taps "Settings".
 * Hosts the same WallpaperSettingsScreen Composable.
 */
class WallpaperSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VideoLiveWallpaperTheme {
                WallpaperSettingsScreen(onNavigateBack = { finish() })
            }
        }
    }
}
