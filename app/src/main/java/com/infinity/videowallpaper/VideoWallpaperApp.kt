package com.infinity.videowallpaper

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.infinity.videowallpaper.data.WallpaperRepository

/**
 * Application entry point providing:
 * 1. Singleton [WallpaperRepository] shared across Activities, ViewModels, and Services.
 * 2. Coil [ImageLoader] with [VideoFrameDecoder.Factory] explicitly registered so video
 *    thumbnails decode reliably and downsample to target view dimensions.
 */
class VideoWallpaperApp : Application(), ImageLoaderFactory {

    val repository: WallpaperRepository by lazy { WallpaperRepository(this) }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }
}
