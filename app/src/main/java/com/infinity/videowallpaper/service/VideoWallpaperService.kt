package com.infinity.videowallpaper.service

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceHolder
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import com.infinity.videowallpaper.VideoWallpaperApp
import com.infinity.videowallpaper.data.WallpaperConfig
import com.infinity.videowallpaper.data.WallpaperRepository
import com.infinity.videowallpaper.engine.GlVideoRenderer
import com.infinity.videowallpaper.engine.PowerPolicyHelper
import com.infinity.videowallpaper.engine.ShaderParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Hardware-accelerated Live Wallpaper Service powered by Jetpack Media3 (ExoPlayer)
 * and an OpenGL ES 2.0 [GlVideoRenderer].
 *
 * Fixes all legacy MediaPlayer + Canvas issues:
 * - Zero lockCanvas() crashes (single EGL producer).
 * - Real-time hot updates via [WallpaperRepository.config] StateFlow.
 * - Gapless looping and non-destructive video trimming via Media3 ClippingConfiguration.
 * - Exact Crop / Contain / Stretch + Dim / Blur / Vignette / Saturation / Parallax in GPU shader.
 * - Battery Saver, low-battery threshold, lock/home screen scope, FPS cap, audio-focus safety,
 *   double-tap pause gesture, and playlist auto-rotation.
 */
class VideoWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = VideoEngine()

    inner class VideoEngine : Engine() {

        private val repository: WallpaperRepository by lazy {
            (application as VideoWallpaperApp).repository
        }

        private val mainHandler = Handler(Looper.getMainLooper())
        private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        private var configCollectorJob: Job? = null

        private var renderer: GlVideoRenderer? = null
        private var videoSurface: Surface? = null
        private var player: ExoPlayer? = null

        private var currentConfig: WallpaperConfig = WallpaperConfig()
        private var loadedUri: String? = null
        private var loadedClipStart: Long = 0L
        private var loadedClipEnd: Long = 0L

        private var isVisible = false
        private var wasLocked = false
        private var hasCompletedNonLoop = false
        private var xOffset = 0.5f

        private val gestureDetector by lazy {
            GestureDetector(
                this@VideoWallpaperService,
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onDoubleTap(e: MotionEvent): Boolean {
                        if (!isPreview && currentConfig.doubleTapToPause) {
                            repository.toggleManualPause()
                            return true
                        }
                        return false
                    }
                },
            )
        }

        private val playerListener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    renderer?.updateVideoSize(videoSize.width, videoSize.height)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED && !currentConfig.loop) {
                    hasCompletedNonLoop = true
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "ExoPlayer error: ${error.errorCodeName}", error)
                renderer?.clearToBlack()
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(true)
            setOffsetNotificationsEnabled(true)
            currentConfig = repository.current()

            configCollectorJob = engineScope.launch {
                repository.config.collect { updated ->
                    onConfigChanged(updated)
                }
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            val rect = holder.surfaceFrame
            val width = rect.width().coerceAtLeast(1)
            val height = rect.height().coerceAtLeast(1)

            renderer?.release()
            renderer = GlVideoRenderer(
                surfaceHolder = holder,
                onVideoSurfaceReady = { surface ->
                    mainHandler.post {
                        videoSurface = surface
                        initOrUpdatePlayer(forceReload = true)
                    }
                },
            ).also {
                it.start(width, height, buildShaderParams(currentConfig))
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            renderer?.updateSurfaceSize(width, height)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            releasePlayer()
            videoSurface = null
            renderer?.release()
            renderer = null
            super.onSurfaceDestroyed(holder)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            isVisible = visible
            val nowLocked = PowerPolicyHelper.queryKeyguardLocked(this@VideoWallpaperService)
            val justUnlocked = visible && wasLocked && !nowLocked
            wasLocked = nowLocked

            if (visible && !isPreview) {
                if (PowerPolicyHelper.shouldRotatePlaylist(
                        config = currentConfig,
                        nowMs = System.currentTimeMillis(),
                        wasJustUnlocked = justUnlocked,
                    )
                ) {
                    repository.advancePlaylist()
                    return
                }
            }
            syncPlaybackState()
        }

        override fun onOffsetsChanged(
            xOffset: Float,
            yOffset: Float,
            xOffsetStep: Float,
            yOffsetStep: Float,
            xPixelOffset: Int,
            yPixelOffset: Int,
        ) {
            this.xOffset = xOffset
            if (currentConfig.parallaxEnabled) {
                renderer?.updateParams(buildShaderParams(currentConfig))
            }
        }

        override fun onTouchEvent(event: MotionEvent) {
            gestureDetector.onTouchEvent(event)
            super.onTouchEvent(event)
        }

        override fun onDestroy() {
            configCollectorJob?.cancel()
            engineScope.cancel()
            releasePlayer()
            renderer?.release()
            renderer = null
            super.onDestroy()
        }

        // ── Config & Player Synchronization ────────────────────────────────────

        private fun onConfigChanged(updated: WallpaperConfig) {
            val prev = currentConfig
            currentConfig = updated

            renderer?.updateParams(buildShaderParams(updated))

            val mediaChanged = updated.videoUri != loadedUri ||
                updated.clipStartMs != loadedClipStart ||
                updated.clipEndMs != loadedClipEnd

            if (updated.loop && !prev.loop) {
                hasCompletedNonLoop = false
            }

            if (mediaChanged) {
                hasCompletedNonLoop = false
                initOrUpdatePlayer(forceReload = true)
            } else {
                applyLivePlayerSettings(updated)
                syncPlaybackState()
            }
        }

        private fun initOrUpdatePlayer(forceReload: Boolean) {
            val surface = videoSurface ?: return
            val uriString = currentConfig.videoUri
            if (uriString.isNullOrBlank()) {
                releasePlayer()
                loadedUri = null
                renderer?.clearToBlack()
                return
            }

            val exo = player ?: ExoPlayer.Builder(this@VideoWallpaperService)
                .build()
                .also { created ->
                    created.addListener(playerListener)
                    created.setVideoSurface(surface)
                    created.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(C.USAGE_MEDIA)
                            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                            .build(),
                        false,
                    )
                    player = created
                }

            if (forceReload || uriString != loadedUri ||
                currentConfig.clipStartMs != loadedClipStart ||
                currentConfig.clipEndMs != loadedClipEnd
            ) {
                val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return
                val itemBuilder = MediaItem.Builder().setUri(uri)
                if (currentConfig.clipRange.isTrimmed) {
                    val clipBuilder = MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(currentConfig.clipStartMs)
                    if (currentConfig.clipEndMs > currentConfig.clipStartMs) {
                        clipBuilder.setEndPositionMs(currentConfig.clipEndMs)
                    }
                    itemBuilder.setClippingConfiguration(clipBuilder.build())
                }

                loadedUri = uriString
                loadedClipStart = currentConfig.clipStartMs
                loadedClipEnd = currentConfig.clipEndMs

                exo.setMediaItem(itemBuilder.build())
                applyLivePlayerSettings(currentConfig)
                exo.prepare()
            } else {
                applyLivePlayerSettings(currentConfig)
            }

            syncPlaybackState()
        }

        private fun applyLivePlayerSettings(config: WallpaperConfig) {
            val exo = player ?: return
            exo.repeatMode = if (config.loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF

            val otherAudioPlaying = PowerPolicyHelper.queryOtherAudioPlaying(this@VideoWallpaperService)
            exo.volume = if (config.mute || otherAudioPlaying) 0f else 1f

            runCatching {
                exo.playbackParameters = PlaybackParameters(config.speedMultiplier())
            }.onFailure {
                Log.w(TAG, "Device rejected speed=${config.speedMultiplier()}", it)
            }
        }

        private fun syncPlaybackState() {
            val exo = player ?: return
            val shouldPlay = PowerPolicyHelper.shouldPlay(
                config = currentConfig,
                isVisible = isVisible,
                isPreview = isPreview,
                hasCompletedNonLoop = hasCompletedNonLoop,
                isPowerSaveMode = PowerPolicyHelper.queryPowerSaveMode(this@VideoWallpaperService),
                batteryPercent = PowerPolicyHelper.queryBatteryPercent(this@VideoWallpaperService),
                isKeyguardLocked = PowerPolicyHelper.queryKeyguardLocked(this@VideoWallpaperService),
            )
            // Re-check music active when resuming so unmuted wallpaper doesn't talk over Spotify/calls
            if (shouldPlay && !currentConfig.mute) {
                val otherAudio = PowerPolicyHelper.queryOtherAudioPlaying(this@VideoWallpaperService)
                exo.volume = if (otherAudio) 0f else 1f
            }
            exo.playWhenReady = shouldPlay
        }

        private fun releasePlayer() {
            player?.let { exo ->
                runCatching {
                    exo.removeListener(playerListener)
                    exo.clearVideoSurface()
                    exo.release()
                }
            }
            player = null
            loadedUri = null
        }

        private fun buildShaderParams(config: WallpaperConfig): ShaderParams = ShaderParams(
            fitMode = config.fitMode,
            rotationDegrees = config.rotationDegrees,
            dimAmount = config.dimAmount,
            blurAmount = config.blurAmount,
            vignetteAmount = config.vignetteAmount,
            saturation = config.saturation,
            parallaxEnabled = config.parallaxEnabled,
            xOffset = xOffset,
            fpsCap = config.fpsCap,
        )
    }

    companion object {
        private const val TAG = "VideoWallpaperService"
    }
}
