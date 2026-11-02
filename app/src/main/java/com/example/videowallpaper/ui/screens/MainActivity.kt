package com.example.videowallpaper.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.videowallpaper.ui.theme.VideoLiveWallpaperTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VideoLiveWallpaperTheme {
                AppNavGraph()
            }
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
