package com.infinity.videowallpaper.service

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.infinity.videowallpaper.R
import com.infinity.videowallpaper.VideoWallpaperApp

/**
 * Quick Settings Tile letting users pause/resume their live wallpaper directly
 * from Android's Quick Settings shade.
 */
class WallpaperQuickTileService : TileService() {

    private val repository by lazy { (application as VideoWallpaperApp).repository }

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
    }

    override fun onClick() {
        super.onClick()
        val config = repository.current()
        if (config.videoUri.isNullOrBlank()) {
            refreshTile()
            return
        }
        repository.toggleManualPause()
        refreshTile()
    }

    private fun refreshTile() {
        val tile = qsTile ?: return
        val config = repository.current()
        when {
            config.videoUri.isNullOrBlank() -> {
                tile.state = Tile.STATE_UNAVAILABLE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = getString(R.string.no_video_selected)
                }
            }
            config.manualPaused -> {
                tile.state = Tile.STATE_INACTIVE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = getString(R.string.qs_tile_paused)
                }
            }
            else -> {
                tile.state = Tile.STATE_ACTIVE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = getString(R.string.qs_tile_playing)
                }
            }
        }
        tile.label = getString(R.string.qs_tile_label)
        tile.updateTile()
    }
}
