package com.infinity.videowallpaper.data

enum class FitMode { CROP, CONTAIN, STRETCH }

enum class PlaybackScope { BOTH, HOME_ONLY, LOCK_ONLY }

enum class RotationInterval(val intervalMs: Long) {
    OFF(0L),
    ON_UNLOCK(1L),
    HOURLY(3_600_000L),
    DAILY(86_400_000L),
}

enum class ThemeMode { SYSTEM, LIGHT, DARK, AMOLED }

data class ClipRange(
    val startMs: Long = 0L,
    val endMs: Long = 0L, // 0L means play to end of video
) {
    val isTrimmed: Boolean
        get() = startMs > 0L || endMs > 0L

    fun normalized(durationMs: Long): ClipRange {
        if (durationMs <= 0L) return ClipRange(0L, 0L)
        val safeStart = startMs.coerceIn(0L, (durationMs - MIN_CLIP_MS).coerceAtLeast(0L))
        val safeEnd = if (endMs <= 0L || endMs >= durationMs) {
            0L
        } else {
            endMs.coerceIn(safeStart + MIN_CLIP_MS, durationMs)
        }
        return ClipRange(safeStart, safeEnd)
    }

    companion object {
        const val MIN_CLIP_MS = 500L
    }
}

data class VideoItem(
    val uri: String,
    val displayName: String,
    val isLocalCopy: Boolean = false,
    val durationMs: Long = 0L,
    val clipStartMs: Long = 0L,
    val clipEndMs: Long = 0L,
) {
    val clipRange: ClipRange
        get() = ClipRange(clipStartMs, clipEndMs)
}

data class WallpaperConfig(
    val videoUri: String? = null,
    val videoDisplayName: String = "",
    val isLocalCopy: Boolean = false,
    val clipStartMs: Long = 0L,
    val clipEndMs: Long = 0L,
    val durationMs: Long = 0L,
    val playlist: List<VideoItem> = emptyList(),
    val playlistIndex: Int = 0,
    val loop: Boolean = true,
    val mute: Boolean = true,
    val speedIndex: Int = DEFAULT_SPEED_INDEX,
    val fitMode: FitMode = FitMode.CROP,
    val dimAmount: Float = 0f,
    val blurAmount: Float = 0f,
    val vignetteAmount: Float = 0f,
    val saturation: Float = 1f,
    val parallaxEnabled: Boolean = false,
    val pauseOnBatterySaver: Boolean = true,
    val lowBatteryThreshold: Int = 15,
    val fpsCap: Int = 30,
    val playbackScope: PlaybackScope = PlaybackScope.BOTH,
    val doubleTapToPause: Boolean = true,
    val manualPaused: Boolean = false,
    val rotationInterval: RotationInterval = RotationInterval.OFF,
    val lastRotatedAtMs: Long = 0L,
    val copyToLocalStorage: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
) {
    fun speedMultiplier(): Float =
        SPEED_VALUES[speedIndex.coerceIn(0, SPEED_VALUES.lastIndex)]

    val clipRange: ClipRange
        get() = ClipRange(clipStartMs, clipEndMs)

    companion object {
        const val DEFAULT_SPEED_INDEX = 2
        val SPEED_VALUES = floatArrayOf(0.25f, 0.5f, 1.0f, 1.5f, 2.0f)
        val SPEED_LABELS = listOf("0.25×", "0.5×", "1×", "1.5×", "2×")
        val FPS_OPTIONS = listOf(24, 30, 60)
    }
}
