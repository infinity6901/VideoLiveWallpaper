package com.example.videowallpaper.service

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import com.example.videowallpaper.data.FitMode
import com.example.videowallpaper.data.WallpaperPreferences

/**
 * Live wallpaper service that plays a user-selected video using [MediaPlayer].
 *
 * Lifecycle summary:
 *   onCreateEngine() → Engine.onCreate() → onSurfaceCreated() → startVideo()
 *   onVisibilityChanged(false) → pause; onVisibilityChanged(true) → resume
 *   onSurfaceDestroyed() / onDestroy() → release player
 */
class VideoWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = VideoEngine()

    // ── Engine ─────────────────────────────────────────────────────────────────

    inner class VideoEngine : Engine() {

        private var mediaPlayer: MediaPlayer? = null
        private val prefs by lazy { WallpaperPreferences(this@VideoWallpaperService) }

        /** Reused Paint for the dim overlay */
        private val dimPaint = Paint().apply { color = Color.BLACK }

        private var surfaceWidth  = 0
        private var surfaceHeight = 0
        private var isVisible     = false
        private var surfaceReady  = false

        private val handler = Handler(Looper.getMainLooper())

        // ── WallpaperService.Engine callbacks ──────────────────────────────────

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            surfaceReady = true
            tryStartVideo()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            surfaceWidth  = width
            surfaceHeight = height
            // Re-apply video display area whenever the surface size changes
            applyVideoLayout()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            surfaceReady = false
            releasePlayer()
            super.onSurfaceDestroyed(holder)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            isVisible = visible
            if (visible) {
                if (mediaPlayer == null) tryStartVideo() else resumePlayer()
            } else {
                pausePlayer()
            }
        }

        override fun onDestroy() {
            releasePlayer()
            super.onDestroy()
        }

        // ── Player management ──────────────────────────────────────────────────

        private fun tryStartVideo() {
            if (!surfaceReady) return
            val uriString = prefs.videoUri ?: run {
                drawFallback()
                return
            }
            startVideo(Uri.parse(uriString))
        }

        private fun startVideo(uri: Uri) {
            releasePlayer()
            try {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(this@VideoWallpaperService, uri)

                    // Render directly on the WallpaperService surface
                    setSurface(surfaceHolder.surface)

                    // Respect user settings
                    isLooping = prefs.loop
                    val vol = if (prefs.mute) 0f else 1f
                    setVolume(vol, vol)

                    setOnPreparedListener { mp ->
                        mp.playbackParams = mp.playbackParams.setSpeed(prefs.speedMultiplier())
                        applyVideoLayout()
                        if (isVisible) mp.start()
                        drawDimOverlay()
                    }

                    setOnErrorListener { _, what, extra ->
                        Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                        drawFallback()
                        true
                    }

                    setOnCompletionListener { mp ->
                        // If not looping, pause on the last frame
                        if (!prefs.loop) mp.seekTo(0)
                    }

                    prepareAsync()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start video", e)
                drawFallback()
            }
        }

        private fun pausePlayer() {
            mediaPlayer?.takeIf { it.isPlaying }?.pause()
        }

        private fun resumePlayer() {
            mediaPlayer?.takeIf { !it.isPlaying }?.start()
        }

        private fun releasePlayer() {
            mediaPlayer?.apply {
                try { if (isPlaying) stop() } catch (_: Exception) {}
                reset()
                release()
            }
            mediaPlayer = null
        }

        // ── Video layout (crop / contain / stretch) ────────────────────────────

        private fun applyVideoLayout() {
            val mp = mediaPlayer ?: return
            val vw = mp.videoWidth.takeIf  { it > 0 } ?: return
            val vh = mp.videoHeight.takeIf { it > 0 } ?: return
            if (surfaceWidth == 0 || surfaceHeight == 0) return

            when (prefs.fitMode) {
                // Fill the screen, cropping edges to maintain aspect ratio
                FitMode.CROP -> {
                    surfaceHolder.setFixedSize(surfaceWidth, surfaceHeight)
                    mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                }
                // Fit the entire frame inside the screen (letterbox / pillarbox bars)
                FitMode.CONTAIN -> {
                    surfaceHolder.setFixedSize(surfaceWidth, surfaceHeight)
                    mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT)
                }
                // Force the video into the exact screen dimensions regardless of aspect ratio
                FitMode.STRETCH -> {
                    // Resize the surface to the video's native size then let the
                    // SurfaceHolder stretch it to fill the screen.
                    val scale = maxOf(
                        surfaceWidth.toFloat()  / vw,
                        surfaceHeight.toFloat() / vh
                    )
                    surfaceHolder.setFixedSize(
                        (vw * scale).toInt(),
                        (vh * scale).toInt()
                    )
                    mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                }
            }
        }

        // ── Drawing helpers ────────────────────────────────────────────────────

        /**
         * Draw a semi-transparent black overlay with the requested [dimAmount].
         * Called after video is prepared so it sits on top of the first frame.
         */
        private fun drawDimOverlay() {
            val dim = prefs.dimAmount
            if (dim <= 0f) return
            var canvas: Canvas? = null
            try {
                canvas = surfaceHolder.lockCanvas()
                canvas?.let {
                    dimPaint.alpha = (dim * 255).toInt().coerceIn(0, 255)
                    it.drawRect(0f, 0f, surfaceWidth.toFloat(), surfaceHeight.toFloat(), dimPaint)
                }
            } finally {
                canvas?.let { surfaceHolder.unlockCanvasAndPost(it) }
            }
        }

        /** Draws a simple dark placeholder when no video URI is set or playback fails. */
        private fun drawFallback() {
            var canvas: Canvas? = null
            try {
                canvas = surfaceHolder.lockCanvas()
                canvas?.drawColor(Color.BLACK, PorterDuff.Mode.CLEAR)
            } finally {
                canvas?.let { surfaceHolder.unlockCanvasAndPost(it) }
            }
        }
    }

    companion object {
        private const val TAG = "VideoWallpaperService"
    }
}
