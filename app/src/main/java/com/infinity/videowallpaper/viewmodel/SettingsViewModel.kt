package com.infinity.videowallpaper.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.infinity.videowallpaper.VideoWallpaperApp
import com.infinity.videowallpaper.data.FitMode
import com.infinity.videowallpaper.data.PlaybackScope
import com.infinity.videowallpaper.data.RotationInterval
import com.infinity.videowallpaper.data.ThemeMode
import com.infinity.videowallpaper.data.WallpaperConfig
import kotlinx.coroutines.flow.StateFlow

/**
 * Auto-saving Settings ViewModel directly backed by [WallpaperRepository].
 * Every change persists atomically and applies immediately to the live wallpaper.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as VideoWallpaperApp).repository
    val config: StateFlow<WallpaperConfig> = repository.config

    fun setLoop(value: Boolean) = repository.update { it.copy(loop = value) }
    fun setMute(value: Boolean) = repository.update { it.copy(mute = value) }
    fun setSpeedIndex(value: Int) = repository.update { it.copy(speedIndex = value) }
    fun setFitMode(value: FitMode) = repository.update { it.copy(fitMode = value) }
    fun setDimAmount(value: Float) = repository.update { it.copy(dimAmount = value) }
    fun setBlurAmount(value: Float) = repository.update { it.copy(blurAmount = value) }
    fun setVignetteAmount(value: Float) = repository.update { it.copy(vignetteAmount = value) }
    fun setSaturation(value: Float) = repository.update { it.copy(saturation = value) }
    fun setParallaxEnabled(value: Boolean) = repository.update { it.copy(parallaxEnabled = value) }
    fun setPauseOnBatterySaver(value: Boolean) = repository.update { it.copy(pauseOnBatterySaver = value) }
    fun setLowBatteryThreshold(value: Int) = repository.update { it.copy(lowBatteryThreshold = value) }
    fun setFpsCap(value: Int) = repository.update { it.copy(fpsCap = value) }
    fun setPlaybackScope(value: PlaybackScope) = repository.update { it.copy(playbackScope = value) }
    fun setDoubleTapToPause(value: Boolean) = repository.update { it.copy(doubleTapToPause = value) }
    fun setRotationInterval(value: RotationInterval) = repository.update { it.copy(rotationInterval = value) }
    fun setCopyToLocalStorage(value: Boolean) = repository.update { it.copy(copyToLocalStorage = value) }
    fun setThemeMode(value: ThemeMode) = repository.update { it.copy(themeMode = value) }
    fun setDynamicColor(value: Boolean) = repository.update { it.copy(dynamicColor = value) }
}
