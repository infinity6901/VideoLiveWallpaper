package com.example.videowallpaper.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.videowallpaper.data.FitMode
import com.example.videowallpaper.data.WallpaperPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val loop:        Boolean  = true,
    val mute:        Boolean  = true,
    val speedIndex:  Int      = 2,
    val fitMode:     FitMode  = FitMode.CROP,
    val dimAmount:   Float    = 0f,
    val savedToast:  Boolean  = false,
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = WallpaperPreferences(application)

    private val _uiState = MutableStateFlow(loadFromPrefs())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // ── Load ───────────────────────────────────────────────────────────────────

    private fun loadFromPrefs() = SettingsUiState(
        loop       = prefs.loop,
        mute       = prefs.mute,
        speedIndex = prefs.speedIndex,
        fitMode    = prefs.fitMode,
        dimAmount  = prefs.dimAmount,
    )

    // ── Mutations ──────────────────────────────────────────────────────────────

    fun setLoop(value: Boolean)       = _uiState.update { it.copy(loop = value) }
    fun setMute(value: Boolean)       = _uiState.update { it.copy(mute = value) }
    fun setSpeedIndex(value: Int)     = _uiState.update { it.copy(speedIndex = value) }
    fun setFitMode(value: FitMode)    = _uiState.update { it.copy(fitMode = value) }
    fun setDimAmount(value: Float)    = _uiState.update { it.copy(dimAmount = value) }

    // ── Persist ────────────────────────────────────────────────────────────────

    fun save() {
        viewModelScope.launch {
            val s = _uiState.value
            prefs.loop       = s.loop
            prefs.mute       = s.mute
            prefs.speedIndex = s.speedIndex
            prefs.fitMode    = s.fitMode
            prefs.dimAmount  = s.dimAmount
            _uiState.update { it.copy(savedToast = true) }
        }
    }

    fun clearToast() = _uiState.update { it.copy(savedToast = false) }
}
