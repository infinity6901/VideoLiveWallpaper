package com.infinity.videowallpaper.viewmodel

import android.app.Application
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.infinity.videowallpaper.R
import com.infinity.videowallpaper.VideoWallpaperApp
import com.infinity.videowallpaper.data.VideoItem
import com.infinity.videowallpaper.data.VideoStorageHelper
import com.infinity.videowallpaper.data.WallpaperConfig
import com.infinity.videowallpaper.service.VideoWallpaperService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeUiState(
    val config: WallpaperConfig = WallpaperConfig(),
    val isWallpaperActive: Boolean = false,
    val isCopyingVideo: Boolean = false,
    val statusMessage: String? = null,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application
    private val repository = (application as VideoWallpaperApp).repository

    private val _ephemeral = MutableStateFlow(EphemeralState())

    val uiState: StateFlow<HomeUiState> = combine(
        repository.config,
        _ephemeral,
    ) { config, eph ->
        HomeUiState(
            config = config,
            isWallpaperActive = eph.isWallpaperActive,
            isCopyingVideo = eph.isCopyingVideo,
            statusMessage = eph.statusMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(
            config = repository.current(),
            isWallpaperActive = queryIsWallpaperActive(),
        ),
    )

    init {
        refreshActiveStatus()
    }

    fun refreshActiveStatus() {
        val active = queryIsWallpaperActive()
        _ephemeral.update { it.copy(isWallpaperActive = active) }
    }

    fun onVideoPicked(uri: Uri) {
        viewModelScope.launch {
            _ephemeral.update { it.copy(isCopyingVideo = true, statusMessage = null) }
            val result = withContext(Dispatchers.IO) {
                val displayName = VideoStorageHelper.queryDisplayName(appContext, uri)
                val durationMs = VideoStorageHelper.queryDurationMs(appContext, uri)
                val persisted = VideoStorageHelper.takePersistableReadPermission(appContext, uri)
                val shouldCopy = repository.current().copyToLocalStorage || !persisted

                var finalUri = uri.toString()
                var isLocalCopy = false
                var warning: String? = null

                if (shouldCopy) {
                    val copied = VideoStorageHelper.copyToLocalStorage(appContext, uri, displayName)
                    if (copied != null) {
                        finalUri = copied
                        isLocalCopy = true
                    } else if (!persisted) {
                        warning = appContext.getString(R.string.error_copy_failed)
                    }
                }

                PickOutcome(
                    item = VideoItem(
                        uri = finalUri,
                        displayName = displayName,
                        isLocalCopy = isLocalCopy,
                        durationMs = durationMs,
                    ),
                    warning = warning,
                )
            }

            repository.selectVideo(result.item, addToPlaylist = true)
            _ephemeral.update {
                it.copy(
                    isCopyingVideo = false,
                    statusMessage = result.warning,
                )
            }
        }
    }

    fun selectFromPlaylist(item: VideoItem) {
        repository.selectVideo(item, addToPlaylist = true)
    }

    fun removeVideo(item: VideoItem) {
        repository.removeFromPlaylist(item.uri)
        viewModelScope.launch(Dispatchers.IO) {
            VideoStorageHelper.releaseVideoResource(appContext, item.uri, item.isLocalCopy)
        }
    }

    fun clearSelectedVideo() {
        val cur = repository.current()
        val uri = cur.videoUri ?: return
        val inPlaylist = cur.playlist.any { it.uri == uri }
        repository.update {
            it.copy(
                videoUri = null,
                videoDisplayName = "",
                isLocalCopy = false,
                durationMs = 0L,
                clipStartMs = 0L,
                clipEndMs = 0L,
            )
        }
        if (!inPlaylist) {
            viewModelScope.launch(Dispatchers.IO) {
                VideoStorageHelper.releaseVideoResource(appContext, uri, cur.isLocalCopy)
            }
        }
    }

    fun updateClipRange(startMs: Long, endMs: Long) {
        repository.updateClipRange(startMs, endMs)
    }

    fun rotateClockwise() {
        repository.rotateClockwise()
    }

    fun toggleManualPause() {
        repository.toggleManualPause()
    }

    fun advancePlaylist() {
        repository.advancePlaylist()
    }

    fun showMessage(message: String) {
        _ephemeral.update { it.copy(statusMessage = message) }
    }

    fun clearMessage() {
        _ephemeral.update { it.copy(statusMessage = null) }
    }

    fun buildSetWallpaperIntent(): Intent {
        return Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(appContext, VideoWallpaperService::class.java),
            )
        }
    }

    fun buildFallbackWallpaperChooserIntent(): Intent {
        return Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)
    }

    private fun queryIsWallpaperActive(): Boolean {
        return runCatching {
            val wm = WallpaperManager.getInstance(appContext)
            wm.wallpaperInfo?.component == ComponentName(appContext, VideoWallpaperService::class.java)
        }.getOrDefault(false)
    }

    private data class EphemeralState(
        val isWallpaperActive: Boolean = false,
        val isCopyingVideo: Boolean = false,
        val statusMessage: String? = null,
    )

    private data class PickOutcome(
        val item: VideoItem,
        val warning: String?,
    )
}
