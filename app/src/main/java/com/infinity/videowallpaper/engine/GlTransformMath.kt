package com.infinity.videowallpaper.engine

import com.infinity.videowallpaper.data.FitMode

/**
 * Pure vertex-transform parameters (NDC scale and translation) for rendering a video
 * onto a viewport with exact aspect-ratio handling, rotation, and optional home-screen parallax.
 * Extracted as pure math so it can be thoroughly unit-tested on the JVM.
 */
data class VideoTransform(
    val scaleX: Float,
    val scaleY: Float,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
)

object GlTransformMath {

    /**
     * Computes NDC scale and translation for the quad vertices in [-1, 1].
     *
     * When [rotationDegrees] is 90 or 270, the video's effective width and height are swapped
     * before computing aspect ratio so a 16:9 landscape video rotated sideways fills a portrait
     * screen cleanly.
     */
    fun compute(
        videoWidth: Int,
        videoHeight: Int,
        surfaceWidth: Int,
        surfaceHeight: Int,
        fitMode: FitMode,
        parallaxEnabled: Boolean = false,
        xOffset: Float = 0.5f,
        rotationDegrees: Int = 0,
    ): VideoTransform {
        if (videoWidth <= 0 || videoHeight <= 0 || surfaceWidth <= 0 || surfaceHeight <= 0) {
            return VideoTransform(1f, 1f, 0f, 0f)
        }

        val normRot = ((rotationDegrees % 360) + 360) % 360
        val swapped = normRot == 90 || normRot == 270
        val effectiveW = if (swapped) videoHeight else videoWidth
        val effectiveH = if (swapped) videoWidth else videoHeight

        val videoAspect = effectiveW.toFloat() / effectiveH.toFloat()
        val surfaceAspect = surfaceWidth.toFloat() / surfaceHeight.toFloat()

        var scaleX = 1f
        var scaleY = 1f

        when (fitMode) {
            FitMode.STRETCH -> {
                scaleX = 1f
                scaleY = 1f
            }
            FitMode.CROP -> {
                if (videoAspect > surfaceAspect) {
                    scaleX = videoAspect / surfaceAspect
                    scaleY = 1f
                } else {
                    scaleX = 1f
                    scaleY = surfaceAspect / videoAspect
                }
            }
            FitMode.CONTAIN -> {
                if (videoAspect > surfaceAspect) {
                    scaleX = 1f
                    scaleY = surfaceAspect / videoAspect
                } else {
                    scaleX = videoAspect / surfaceAspect
                    scaleY = 1f
                }
            }
        }

        val clampedOffset = xOffset.coerceIn(0f, 1f)
        val offsetX = if (parallaxEnabled && fitMode == FitMode.CROP && scaleX > 1.01f) {
            val maxShift = scaleX - 1f
            (0.5f - clampedOffset) * 2f * maxShift
        } else {
            0f
        }

        return VideoTransform(
            scaleX = scaleX,
            scaleY = scaleY,
            offsetX = offsetX,
            offsetY = 0f,
        )
    }
}
