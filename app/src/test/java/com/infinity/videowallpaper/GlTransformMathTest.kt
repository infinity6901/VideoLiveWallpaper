package com.infinity.videowallpaper

import com.infinity.videowallpaper.data.FitMode
import com.infinity.videowallpaper.engine.GlTransformMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlTransformMathTest {

    @Test
    fun stretchAlwaysReturnsUnitScaleAndZeroOffset() {
        val t = GlTransformMath.compute(
            videoWidth = 1920,
            videoHeight = 1080,
            surfaceWidth = 1080,
            surfaceHeight = 2400,
            fitMode = FitMode.STRETCH,
            parallaxEnabled = true,
            xOffset = 0.0f,
        )
        assertEquals(1f, t.scaleX, 0.001f)
        assertEquals(1f, t.scaleY, 0.001f)
        assertEquals(0f, t.offsetX, 0.001f)
    }

    @Test
    fun cropExpandsHorizontalAxisForLandscapeVideoOnPortraitScreen() {
        val t = GlTransformMath.compute(
            videoWidth = 1920,
            videoHeight = 1080,
            surfaceWidth = 1080,
            surfaceHeight = 2160,
            fitMode = FitMode.CROP,
        )
        // videoAspect = 16/9 (~1.7778), surfaceAspect = 0.5 -> scaleX ~= 3.5556
        assertEquals(3.5556f, t.scaleX, 0.01f)
        assertEquals(1f, t.scaleY, 0.001f)
        assertEquals(0f, t.offsetX, 0.001f)
    }

    @Test
    fun containLetterboxesLandscapeVideoOnPortraitScreen() {
        val t = GlTransformMath.compute(
            videoWidth = 1920,
            videoHeight = 1080,
            surfaceWidth = 1080,
            surfaceHeight = 2160,
            fitMode = FitMode.CONTAIN,
        )
        assertEquals(1f, t.scaleX, 0.001f)
        assertEquals(0.5f / (16f / 9f), t.scaleY, 0.01f)
    }

    @Test
    fun parallaxPansWithinSafeBoundsWithoutExposingBlackBars() {
        val left = GlTransformMath.compute(
            videoWidth = 1920,
            videoHeight = 1080,
            surfaceWidth = 1080,
            surfaceHeight = 1920,
            fitMode = FitMode.CROP,
            parallaxEnabled = true,
            xOffset = 0f,
        )
        val center = GlTransformMath.compute(
            videoWidth = 1920,
            videoHeight = 1080,
            surfaceWidth = 1080,
            surfaceHeight = 1920,
            fitMode = FitMode.CROP,
            parallaxEnabled = true,
            xOffset = 0.5f,
        )
        val right = GlTransformMath.compute(
            videoWidth = 1920,
            videoHeight = 1080,
            surfaceWidth = 1080,
            surfaceHeight = 1920,
            fitMode = FitMode.CROP,
            parallaxEnabled = true,
            xOffset = 1f,
        )

        assertTrue(left.offsetX > 0f)
        assertEquals(0f, center.offsetX, 0.001f)
        assertEquals(-left.offsetX, right.offsetX, 0.001f)
        // Verify edges never pull inside [-1, 1]
        assertEquals(left.scaleX - 1f, left.offsetX, 0.001f)
    }

    @Test
    fun invalidDimensionsReturnIdentityTransform() {
        val t = GlTransformMath.compute(0, 1080, 1080, 2400, FitMode.CROP)
        assertEquals(1f, t.scaleX, 0.001f)
        assertEquals(1f, t.scaleY, 0.001f)
    }
}
