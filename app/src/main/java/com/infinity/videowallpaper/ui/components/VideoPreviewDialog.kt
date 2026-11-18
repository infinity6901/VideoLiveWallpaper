package com.infinity.videowallpaper.ui.components

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import com.infinity.videowallpaper.R
import com.infinity.videowallpaper.data.WallpaperConfig
import com.infinity.videowallpaper.engine.GlVideoRenderer
import com.infinity.videowallpaper.engine.ShaderParams

/**
 * Full-screen in-app live preview powered by the exact same [GlVideoRenderer] + Media3
 * [ExoPlayer] pipeline as [VideoWallpaperService], so users can preview dimming, blur,
 * vignette, saturation, fit mode, speed, and clip range before applying.
 */
@Composable
fun VideoPreviewDialog(
    config: WallpaperConfig,
    onDismiss: () -> Unit,
) {
    val uriString = config.videoUri ?: return
    val context = LocalContext.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    val player = remember(uriString, config.clipStartMs, config.clipEndMs) {
        ExoPlayer.Builder(context).build().apply {
            val itemBuilder = MediaItem.Builder().setUri(Uri.parse(uriString))
            if (config.clipRange.isTrimmed) {
                val clipBuilder = MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(config.clipStartMs)
                if (config.clipEndMs > config.clipStartMs) {
                    clipBuilder.setEndPositionMs(config.clipEndMs)
                }
                itemBuilder.setClippingConfiguration(clipBuilder.build())
            }
            setMediaItem(itemBuilder.build())
            repeatMode = if (config.loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            volume = if (config.mute) 0f else 1f
            runCatching { playbackParameters = PlaybackParameters(config.speedMultiplier()) }
            prepare()
            playWhenReady = true
        }
    }

    val rendererRef = remember { arrayOfNulls<GlVideoRenderer>(1) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    rendererRef[0]?.updateVideoSize(videoSize.width, videoSize.height)
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.clearVideoSurface()
            player.release()
            rendererRef[0]?.release()
            rendererRef[0] = null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    SurfaceView(ctx).apply {
                        holder.addCallback(object : SurfaceHolder.Callback {
                            override fun surfaceCreated(holder: SurfaceHolder) {
                                val frame = holder.surfaceFrame
                                val gl = GlVideoRenderer(
                                    surfaceHolder = holder,
                                    onVideoSurfaceReady = { surface ->
                                        mainHandler.post {
                                            player.setVideoSurface(surface)
                                        }
                                    },
                                )
                                rendererRef[0] = gl
                                gl.start(
                                    width = frame.width().coerceAtLeast(1),
                                    height = frame.height().coerceAtLeast(1),
                                    initialParams = ShaderParams(
                                        fitMode = config.fitMode,
                                        dimAmount = config.dimAmount,
                                        blurAmount = config.blurAmount,
                                        vignetteAmount = config.vignetteAmount,
                                        saturation = config.saturation,
                                        parallaxEnabled = false,
                                        fpsCap = config.fpsCap,
                                    ),
                                )
                            }

                            override fun surfaceChanged(
                                holder: SurfaceHolder,
                                format: Int,
                                width: Int,
                                height: Int,
                            ) {
                                rendererRef[0]?.updateSurfaceSize(width, height)
                            }

                            override fun surfaceDestroyed(holder: SurfaceHolder) {
                                player.clearVideoSurface()
                                rendererRef[0]?.release()
                                rendererRef[0] = null
                            }
                        })
                    }
                },
            )

            Surface(
                color = Color.Black.copy(alpha = 0.55f),
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp),
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.btn_close),
                        tint = Color.White,
                    )
                }
            }
        }
    }
}
