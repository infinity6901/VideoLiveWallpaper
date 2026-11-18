package com.infinity.videowallpaper.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Single reactive source of truth for all wallpaper settings and playlist state.
 * Backed by [SharedPreferences] with a live [SharedPreferences.OnSharedPreferenceChangeListener]
 * so changes made in the UI propagate immediately to [VideoWallpaperService] and [WallpaperQuickTileService].
 */
class WallpaperRepository(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _config = MutableStateFlow(readConfig())
    val config: StateFlow<WallpaperConfig> = _config.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        _config.value = readConfig()
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun current(): WallpaperConfig = _config.value

    private fun readConfig(): WallpaperConfig {
        val fitIdx = prefs.getInt(KEY_FIT_MODE, FitMode.CROP.ordinal)
        val scopeIdx = prefs.getInt(KEY_PLAYBACK_SCOPE, PlaybackScope.BOTH.ordinal)
        val rotIdx = prefs.getInt(KEY_ROTATION_INTERVAL, RotationInterval.OFF.ordinal)
        val themeIdx = prefs.getInt(KEY_THEME_MODE, ThemeMode.SYSTEM.ordinal)

        return WallpaperConfig(
            videoUri = prefs.getString(KEY_VIDEO_URI, null),
            videoDisplayName = prefs.getString(KEY_VIDEO_NAME, "") ?: "",
            isLocalCopy = prefs.getBoolean(KEY_IS_LOCAL_COPY, false),
            clipStartMs = prefs.getLong(KEY_CLIP_START, 0L).coerceAtLeast(0L),
            clipEndMs = prefs.getLong(KEY_CLIP_END, 0L).coerceAtLeast(0L),
            durationMs = prefs.getLong(KEY_DURATION_MS, 0L).coerceAtLeast(0L),
            playlist = decodePlaylist(prefs.getString(KEY_PLAYLIST_JSON, null)),
            playlistIndex = prefs.getInt(KEY_PLAYLIST_INDEX, 0).coerceAtLeast(0),
            loop = prefs.getBoolean(KEY_LOOP, true),
            mute = prefs.getBoolean(KEY_MUTE, true),
            speedIndex = prefs.getInt(KEY_SPEED_INDEX, WallpaperConfig.DEFAULT_SPEED_INDEX)
                .coerceIn(0, WallpaperConfig.SPEED_VALUES.lastIndex),
            fitMode = FitMode.entries.getOrElse(fitIdx) { FitMode.CROP },
            dimAmount = prefs.getFloat(KEY_DIM, 0f).coerceIn(0f, 0.8f),
            blurAmount = prefs.getFloat(KEY_BLUR, 0f).coerceIn(0f, 1f),
            vignetteAmount = prefs.getFloat(KEY_VIGNETTE, 0f).coerceIn(0f, 1f),
            saturation = prefs.getFloat(KEY_SATURATION, 1f).coerceIn(0f, 1.5f),
            parallaxEnabled = prefs.getBoolean(KEY_PARALLAX, false),
            pauseOnBatterySaver = prefs.getBoolean(KEY_PAUSE_BATTERY_SAVER, true),
            lowBatteryThreshold = prefs.getInt(KEY_LOW_BATTERY, 15).coerceIn(0, 50),
            fpsCap = prefs.getInt(KEY_FPS_CAP, 30).let {
                if (it in WallpaperConfig.FPS_OPTIONS) it else 30
            },
            playbackScope = PlaybackScope.entries.getOrElse(scopeIdx) { PlaybackScope.BOTH },
            doubleTapToPause = prefs.getBoolean(KEY_DOUBLE_TAP, true),
            manualPaused = prefs.getBoolean(KEY_MANUAL_PAUSED, false),
            rotationInterval = RotationInterval.entries.getOrElse(rotIdx) { RotationInterval.OFF },
            lastRotatedAtMs = prefs.getLong(KEY_LAST_ROTATED, 0L),
            copyToLocalStorage = prefs.getBoolean(KEY_COPY_LOCAL, false),
            themeMode = ThemeMode.entries.getOrElse(themeIdx) { ThemeMode.SYSTEM },
            dynamicColor = prefs.getBoolean(KEY_DYNAMIC_COLOR, false),
        )
    }

    /** Batched atomic update to SharedPreferences. */
    fun update(transform: (WallpaperConfig) -> WallpaperConfig) {
        val next = transform(_config.value)
        prefs.edit {
            putString(KEY_VIDEO_URI, next.videoUri)
            putString(KEY_VIDEO_NAME, next.videoDisplayName)
            putBoolean(KEY_IS_LOCAL_COPY, next.isLocalCopy)
            putLong(KEY_CLIP_START, next.clipStartMs)
            putLong(KEY_CLIP_END, next.clipEndMs)
            putLong(KEY_DURATION_MS, next.durationMs)
            putString(KEY_PLAYLIST_JSON, encodePlaylist(next.playlist))
            putInt(KEY_PLAYLIST_INDEX, next.playlistIndex)
            putBoolean(KEY_LOOP, next.loop)
            putBoolean(KEY_MUTE, next.mute)
            putInt(KEY_SPEED_INDEX, next.speedIndex.coerceIn(0, WallpaperConfig.SPEED_VALUES.lastIndex))
            putInt(KEY_FIT_MODE, next.fitMode.ordinal)
            putFloat(KEY_DIM, next.dimAmount.coerceIn(0f, 0.8f))
            putFloat(KEY_BLUR, next.blurAmount.coerceIn(0f, 1f))
            putFloat(KEY_VIGNETTE, next.vignetteAmount.coerceIn(0f, 1f))
            putFloat(KEY_SATURATION, next.saturation.coerceIn(0f, 1.5f))
            putBoolean(KEY_PARALLAX, next.parallaxEnabled)
            putBoolean(KEY_PAUSE_BATTERY_SAVER, next.pauseOnBatterySaver)
            putInt(KEY_LOW_BATTERY, next.lowBatteryThreshold.coerceIn(0, 50))
            putInt(KEY_FPS_CAP, next.fpsCap)
            putInt(KEY_PLAYBACK_SCOPE, next.playbackScope.ordinal)
            putBoolean(KEY_DOUBLE_TAP, next.doubleTapToPause)
            putBoolean(KEY_MANUAL_PAUSED, next.manualPaused)
            putInt(KEY_ROTATION_INTERVAL, next.rotationInterval.ordinal)
            putLong(KEY_LAST_ROTATED, next.lastRotatedAtMs)
            putBoolean(KEY_COPY_LOCAL, next.copyToLocalStorage)
            putInt(KEY_THEME_MODE, next.themeMode.ordinal)
            putBoolean(KEY_DYNAMIC_COLOR, next.dynamicColor)
        }
        _config.value = next
    }

    fun selectVideo(item: VideoItem, addToPlaylist: Boolean = true) {
        update { cur ->
            val updatedList = if (addToPlaylist) {
                val existingIdx = cur.playlist.indexOfFirst { it.uri == item.uri }
                if (existingIdx >= 0) {
                    cur.playlist.toMutableList().apply { set(existingIdx, item) }
                } else {
                    (listOf(item) + cur.playlist).take(MAX_PLAYLIST_ITEMS)
                }
            } else {
                cur.playlist
            }
            val newIndex = updatedList.indexOfFirst { it.uri == item.uri }.coerceAtLeast(0)
            cur.copy(
                videoUri = item.uri,
                videoDisplayName = item.displayName,
                isLocalCopy = item.isLocalCopy,
                durationMs = item.durationMs,
                clipStartMs = item.clipStartMs,
                clipEndMs = item.clipEndMs,
                playlist = updatedList,
                playlistIndex = newIndex,
                manualPaused = false,
                lastRotatedAtMs = System.currentTimeMillis(),
            )
        }
    }

    fun updateClipRange(startMs: Long, endMs: Long) {
        update { cur ->
            val uri = cur.videoUri ?: return@update cur
            val normalized = ClipRange(startMs, endMs).normalized(cur.durationMs)
            val updatedList = cur.playlist.map {
                if (it.uri == uri) {
                    it.copy(clipStartMs = normalized.startMs, clipEndMs = normalized.endMs)
                } else {
                    it
                }
            }
            cur.copy(
                clipStartMs = normalized.startMs,
                clipEndMs = normalized.endMs,
                playlist = updatedList,
            )
        }
    }

    fun removeFromPlaylist(uri: String) {
        update { cur ->
            val nextList = cur.playlist.filterNot { it.uri == uri }
            if (cur.videoUri == uri) {
                val fallback = nextList.firstOrNull()
                cur.copy(
                    videoUri = fallback?.uri,
                    videoDisplayName = fallback?.displayName ?: "",
                    isLocalCopy = fallback?.isLocalCopy ?: false,
                    durationMs = fallback?.durationMs ?: 0L,
                    clipStartMs = fallback?.clipStartMs ?: 0L,
                    clipEndMs = fallback?.clipEndMs ?: 0L,
                    playlist = nextList,
                    playlistIndex = 0,
                )
            } else {
                val newIdx = nextList.indexOfFirst { it.uri == cur.videoUri }.coerceAtLeast(0)
                cur.copy(playlist = nextList, playlistIndex = newIdx)
            }
        }
    }

    fun advancePlaylist(nowMs: Long = System.currentTimeMillis()): Boolean {
        val cur = _config.value
        if (cur.playlist.size < 2) return false
        val currentIdx = cur.playlist.indexOfFirst { it.uri == cur.videoUri }.let {
            if (it >= 0) it else cur.playlistIndex
        }
        val nextIdx = (currentIdx + 1) % cur.playlist.size
        val nextItem = cur.playlist[nextIdx]
        update {
            it.copy(
                videoUri = nextItem.uri,
                videoDisplayName = nextItem.displayName,
                isLocalCopy = nextItem.isLocalCopy,
                durationMs = nextItem.durationMs,
                clipStartMs = nextItem.clipStartMs,
                clipEndMs = nextItem.clipEndMs,
                playlistIndex = nextIdx,
                lastRotatedAtMs = nowMs,
            )
        }
        return true
    }

    fun toggleManualPause(): Boolean {
        var newState = false
        update { cur ->
            newState = !cur.manualPaused
            cur.copy(manualPaused = newState)
        }
        return newState
    }

    companion object {
        const val PREFS_NAME = "video_wallpaper_prefs"
        private const val MAX_PLAYLIST_ITEMS = 12

        private const val KEY_VIDEO_URI = "video_uri"
        private const val KEY_VIDEO_NAME = "video_display_name"
        private const val KEY_IS_LOCAL_COPY = "is_local_copy"
        private const val KEY_CLIP_START = "clip_start_ms"
        private const val KEY_CLIP_END = "clip_end_ms"
        private const val KEY_DURATION_MS = "duration_ms"
        private const val KEY_PLAYLIST_JSON = "playlist_json"
        private const val KEY_PLAYLIST_INDEX = "playlist_index"
        private const val KEY_LOOP = "loop"
        private const val KEY_MUTE = "mute"
        private const val KEY_SPEED_INDEX = "speed_index"
        private const val KEY_FIT_MODE = "fit_mode"
        private const val KEY_DIM = "dim_amount"
        private const val KEY_BLUR = "blur_amount"
        private const val KEY_VIGNETTE = "vignette_amount"
        private const val KEY_SATURATION = "saturation"
        private const val KEY_PARALLAX = "parallax_enabled"
        private const val KEY_PAUSE_BATTERY_SAVER = "pause_battery_saver"
        private const val KEY_LOW_BATTERY = "low_battery_threshold"
        private const val KEY_FPS_CAP = "fps_cap"
        private const val KEY_PLAYBACK_SCOPE = "playback_scope"
        private const val KEY_DOUBLE_TAP = "double_tap_pause"
        private const val KEY_MANUAL_PAUSED = "manual_paused"
        private const val KEY_ROTATION_INTERVAL = "rotation_interval"
        private const val KEY_LAST_ROTATED = "last_rotated_ms"
        private const val KEY_COPY_LOCAL = "copy_to_local"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"

        internal fun encodePlaylist(items: List<VideoItem>): String {
            val arr = JSONArray()
            items.forEach { item ->
                arr.put(
                    JSONObject().apply {
                        put("uri", item.uri)
                        put("name", item.displayName)
                        put("local", item.isLocalCopy)
                        put("dur", item.durationMs)
                        put("start", item.clipStartMs)
                        put("end", item.clipEndMs)
                    }
                )
            }
            return arr.toString()
        }

        internal fun decodePlaylist(json: String?): List<VideoItem> {
            if (json.isNullOrBlank()) return emptyList()
            return runCatching {
                val arr = JSONArray(json)
                buildList(arr.length()) {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val uri = obj.optString("uri", "")
                        if (uri.isNotBlank()) {
                            add(
                                VideoItem(
                                    uri = uri,
                                    displayName = obj.optString("name", "video"),
                                    isLocalCopy = obj.optBoolean("local", false),
                                    durationMs = obj.optLong("dur", 0L).coerceAtLeast(0L),
                                    clipStartMs = obj.optLong("start", 0L).coerceAtLeast(0L),
                                    clipEndMs = obj.optLong("end", 0L).coerceAtLeast(0L),
                                )
                            )
                        }
                    }
                }
            }.getOrDefault(emptyList())
        }
    }
}
