package com.infinity.videowallpaper.engine

import android.app.KeyguardManager
import android.content.Context
import android.media.AudioManager
import android.os.BatteryManager
import android.os.PowerManager
import com.infinity.videowallpaper.data.PlaybackScope
import com.infinity.videowallpaper.data.RotationInterval
import com.infinity.videowallpaper.data.WallpaperConfig

/**
 * Pure playback-gating and playlist-rotation logic, separated from Android system services
 * so every battery, lock-screen, pause, and rotation edge case can be unit-tested on the JVM.
 */
object PowerPolicyHelper {

    fun shouldPlay(
        config: WallpaperConfig,
        isVisible: Boolean,
        isPreview: Boolean,
        hasCompletedNonLoop: Boolean,
        isPowerSaveMode: Boolean,
        batteryPercent: Int,
        isKeyguardLocked: Boolean,
    ): Boolean {
        if (!isVisible) return false
        if (config.videoUri.isNullOrBlank()) return false
        if (hasCompletedNonLoop && !config.loop) return false

        // Always allow preview in system wallpaper picker regardless of pause/power guards
        if (isPreview) return true

        if (config.manualPaused) return false
        if (config.pauseOnBatterySaver && isPowerSaveMode) return false
        if (config.lowBatteryThreshold > 0 && batteryPercent in 1..config.lowBatteryThreshold) {
            return false
        }

        return when (config.playbackScope) {
            PlaybackScope.BOTH -> true
            PlaybackScope.HOME_ONLY -> !isKeyguardLocked
            PlaybackScope.LOCK_ONLY -> isKeyguardLocked
        }
    }

    fun shouldRotatePlaylist(
        config: WallpaperConfig,
        nowMs: Long,
        wasJustUnlocked: Boolean,
    ): Boolean {
        if (config.playlist.size < 2) return false
        return when (config.rotationInterval) {
            RotationInterval.OFF -> false
            RotationInterval.ON_UNLOCK -> wasJustUnlocked
            RotationInterval.HOURLY, RotationInterval.DAILY -> {
                config.lastRotatedAtMs > 0L &&
                    (nowMs - config.lastRotatedAtMs) >= config.rotationInterval.intervalMs
            }
        }
    }

    fun queryPowerSaveMode(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return pm?.isPowerSaveMode ?: false
    }

    fun queryBatteryPercent(context: Context): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        return if (level in 0..100) level else 100
    }

    fun queryKeyguardLocked(context: Context): Boolean {
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return km?.isKeyguardLocked ?: false
    }

    fun queryOtherAudioPlaying(context: Context): Boolean {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        return am?.isMusicActive ?: false
    }
}
