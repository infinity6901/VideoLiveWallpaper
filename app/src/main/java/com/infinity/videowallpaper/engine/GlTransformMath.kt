package com.infinity.videowallpaper.engine

import com.infinity.videowallpaper.data.FitMode

/**
 * Pure vertex-transform parameters (NDC scale and translation) for rendering a video
 * onto a viewport with exact aspect-ratio handling and optional home-screen parallax.
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
     * @param videoWidth Width of the decoded video in pixels.
     * @param videoHeight Height of the decoded video in pixels.
     * @param surfaceWidth Width of the wallpaper surface in pixels.
     * @param surfaceHeight Height of the wallpaper surface in pixels.
     * @param fitMode Desired scaling mode (CROP, CONTAIN, STRETCH).
     * @param parallaxEnabled Whether horizontal home-screen parallax panning is enabled.
     * @param xOffset Launcher horizontal scroll fraction in [0f, 1f] (0.5f = centered).
     */
    fun compute(
        videoWidth: Int,
        videoHeight: Int,
        surfaceWidth: Int,
        surfaceHeight: Int,
        fitMode: FitMode,
        parallaxEnabled: Boolean = false,
        xOffset: Float = 0.5f,
    ): VideoTransform {
        if (videoWidth <= 0 || videoHeight <= 0 || surfaceWidth <= 0 || surfaceHeight <= 0) {
            return VideoTransform(1f, 1f, 0f, 0f)
        }

        val videoAspect = videoWidth.toFloat() / videoHeight.toFloat()
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
                    // Video is wider than screen: expand X beyond [-1, 1]
                    scaleX = videoAspect / surfaceAspect
                    scaleY = 1f
                } else {
                    // Video is taller than screen: expand Y beyond [-1, 1]
                    scaleX = 1f
                    scaleY = surfaceAspect / videoAspect
                }
            }
            FitMode.CONTAIN -> {
                if (videoAspect > surfaceAspect) {
                    // Video is wider than screen: letterbox top/bottom
                    scaleX = 1f
                    scaleY = surfaceAspect / videoAspect
                } else {
                    // Video is taller than screen: pillarbox left/right
                    scaleX = videoAspect / surfaceAspect
                    scaleY = 1f
                }
            }
        }

        val clampedOffset = xOffset.coerceIn(0f, 1f)
        val offsetX = if (parallaxEnabled && fitMode == FitMode.CROP && scaleX > 1.01f) {
            // Maximum horizontal travel in NDC so edges never pull away from viewport
            val maxShift = scaleX - 1f
            // xOffset=0 -> left edge (+maxShift), xOffset=0.5 -> center (0), xOffset=1 -> right edge (-maxShift)
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
