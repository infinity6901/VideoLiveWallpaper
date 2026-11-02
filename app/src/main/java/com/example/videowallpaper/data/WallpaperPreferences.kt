package com.example.videowallpaper.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Single source of truth for all wallpaper settings persisted via SharedPreferences.
 * Used by both the Settings UI and the VideoWallpaperService.
 */
class WallpaperPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Video URI ──────────────────────────────────────────────────────────────

    var videoUri: String?
        get() = prefs.getString(KEY_VIDEO_URI, null)
        set(value) = prefs.edit { putString(KEY_VIDEO_URI, value) }

    // ── Playback ───────────────────────────────────────────────────────────────

    /** Whether the video should loop when it reaches the end. */
    var loop: Boolean
        get() = prefs.getBoolean(KEY_LOOP, true)
        set(value) = prefs.edit { putBoolean(KEY_LOOP, value) }

    /** Whether audio should be muted. */
    var mute: Boolean
        get() = prefs.getBoolean(KEY_MUTE, true)
        set(value) = prefs.edit { putBoolean(KEY_MUTE, value) }

    /**
     * Playback speed multiplier. Stored as an Int index (0-4) mapping to:
     * 0 → 0.25x, 1 → 0.5x, 2 → 1.0x (default), 3 → 1.5x, 4 → 2.0x
     */
    var speedIndex: Int
        get() = prefs.getInt(KEY_SPEED_INDEX, 2)
        set(value) = prefs.edit { putInt(KEY_SPEED_INDEX, value) }

    // ── Visual ─────────────────────────────────────────────────────────────────

    /**
     * How the video is scaled to fill the screen.
     * Values: FitMode.CROP | FitMode.CONTAIN | FitMode.STRETCH
     */
    var fitMode: FitMode
        get() = FitMode.entries.getOrElse(prefs.getInt(KEY_FIT_MODE, FitMode.CROP.ordinal)) { FitMode.CROP }
        set(value) = prefs.edit { putInt(KEY_FIT_MODE, value.ordinal) }

    /**
     * Dim overlay alpha [0f – 0.8f]. 0f = fully bright, 0.8f = very dark.
     */
    var dimAmount: Float
        get() = prefs.getFloat(KEY_DIM, 0f)
        set(value) = prefs.edit { putFloat(KEY_DIM, value) }

    // ── Helpers ────────────────────────────────────────────────────────────────

    fun speedMultiplier(): Float = SPEED_VALUES[speedIndex.coerceIn(0, SPEED_VALUES.lastIndex)]

    companion object {
        private const val PREFS_NAME     = "video_wallpaper_prefs"
        private const val KEY_VIDEO_URI  = "video_uri"
        private const val KEY_LOOP       = "loop"
        private const val KEY_MUTE       = "mute"
        private const val KEY_SPEED_INDEX = "speed_index"
        private const val KEY_FIT_MODE   = "fit_mode"
        private const val KEY_DIM        = "dim_amount"

        val SPEED_VALUES = floatArrayOf(0.25f, 0.5f, 1.0f, 1.5f, 2.0f)
        val SPEED_LABELS = listOf("0.25×", "0.5×", "1×", "1.5×", "2×")
    }
}

enum class FitMode { CROP, CONTAIN, STRETCH }
