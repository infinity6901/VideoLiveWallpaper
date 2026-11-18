package com.infinity.videowallpaper

import com.infinity.videowallpaper.data.ClipRange
import com.infinity.videowallpaper.data.WallpaperConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperModelsTest {

    @Test
    fun clipRangeNormalizesOutOfBoundsValues() {
        val normalized = ClipRange(startMs = 9_800L, endMs = 12_000L).normalized(durationMs = 10_000L)
        assertEquals(9_500L, normalized.startMs)
        assertEquals(0L, normalized.endMs)
        assertTrue(normalized.isTrimmed)
    }

    @Test
    fun fullVideoClipRangeIsNotTrimmed() {
        val full = ClipRange(0L, 0L)
        assertFalse(full.isTrimmed)
    }

    @Test
    fun speedMultiplierClampsOutOfRangeIndices() {
        assertEquals(0.25f, WallpaperConfig(speedIndex = -5).speedMultiplier(), 0.001f)
        assertEquals(1.0f, WallpaperConfig(speedIndex = 2).speedMultiplier(), 0.001f)
        assertEquals(2.0f, WallpaperConfig(speedIndex = 99).speedMultiplier(), 0.001f)
    }
}
