package com.example.videowallpaper.viewmodel

import android.app.Application
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import com.example.videowallpaper.data.WallpaperPreferences
import com.example.videowallpaper.service.VideoWallpaperService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class HomeUiState(
    val selectedVideoUri:  String?  = null,
    val videoDisplayName:  String   = "",
    val isWallpaperActive: Boolean  = false,
    val permissionNeeded:  Boolean  = false,
    val errorMessage:      String?  = null,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs   = WallpaperPreferences(application)
    private val context = application

    private val _uiState = MutableStateFlow(
        HomeUiState(selectedVideoUri = prefs.videoUri)
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // ── Video selection ────────────────────────────────────────────────────────

    fun onVideoPicked(uri: Uri, displayName: String) {
        // Persist a persistable URI permission so the service can read it later
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Some URIs don't support persistable permissions; we'll handle gracefully
        }

        prefs.videoUri = uri.toString()
        _uiState.update {
            it.copy(
                selectedVideoUri  = uri.toString(),
                videoDisplayName  = displayName,
                errorMessage      = null,
            )
        }
    }

    fun clearSelectedVideo() {
        prefs.videoUri = null
        _uiState.update { it.copy(selectedVideoUri = null, videoDisplayName = "") }
    }

    // ── Set Wallpaper ──────────────────────────────────────────────────────────

    /**
     * Launches the system wallpaper picker pre-selecting our service.
     * Returns the [Intent] to be started by the Activity (avoids ViewModel
     * holding Activity references).
     */
    fun buildSetWallpaperIntent(): Intent {
        return Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(context, VideoWallpaperService::class.java)
            )
        }
    }

    fun onPermissionDenied() {
        _uiState.update { it.copy(permissionNeeded = true) }
    }

    fun onPermissionGranted() {
        _uiState.update { it.copy(permissionNeeded = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
