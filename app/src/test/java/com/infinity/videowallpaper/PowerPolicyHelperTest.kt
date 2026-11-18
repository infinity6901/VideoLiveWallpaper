package com.infinity.videowallpaper

import com.infinity.videowallpaper.data.PlaybackScope
import com.infinity.videowallpaper.data.RotationInterval
import com.infinity.videowallpaper.data.VideoItem
import com.infinity.videowallpaper.data.WallpaperConfig
import com.infinity.videowallpaper.engine.PowerPolicyHelper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PowerPolicyHelperTest {

    private val baseConfig = WallpaperConfig(
        videoUri = "content://media/video/1",
        pauseOnBatterySaver = true,
        lowBatteryThreshold = 20,
        playbackScope = PlaybackScope.BOTH,
    )

    @Test
    fun pausesWhenBatterySaverIsActive() {
        assertFalse(
            PowerPolicyHelper.shouldPlay(
                config = baseConfig,
                isVisible = true,
                isPreview = false,
                hasCompletedNonLoop = false,
                isPowerSaveMode = true,
                batteryPercent = 80,
                isKeyguardLocked = false,
            ),
        )
    }

    @Test
    fun pausesWhenBatteryBelowThreshold() {
        assertFalse(
            PowerPolicyHelper.shouldPlay(
                config = baseConfig,
                isVisible = true,
                isPreview = false,
                hasCompletedNonLoop = false,
                isPowerSaveMode = false,
                batteryPercent = 15,
                isKeyguardLocked = false,
            ),
        )
    }

    @Test
    fun previewIgnoresManualPauseAndBatterySaver() {
        assertTrue(
            PowerPolicyHelper.shouldPlay(
                config = baseConfig.copy(manualPaused = true),
                isVisible = true,
                isPreview = true,
                hasCompletedNonLoop = false,
                isPowerSaveMode = true,
                batteryPercent = 5,
                isKeyguardLocked = false,
            ),
        )
    }

    @Test
    fun respectsHomeOnlyAndLockOnlyScopes() {
        assertFalse(
            PowerPolicyHelper.shouldPlay(
                config = baseConfig.copy(playbackScope = PlaybackScope.HOME_ONLY),
                isVisible = true,
                isPreview = false,
                hasCompletedNonLoop = false,
                isPowerSaveMode = false,
                batteryPercent = 90,
                isKeyguardLocked = true,
            ),
        )
        assertTrue(
            PowerPolicyHelper.shouldPlay(
                config = baseConfig.copy(playbackScope = PlaybackScope.LOCK_ONLY),
                isVisible = true,
                isPreview = false,
                hasCompletedNonLoop = false,
                isPowerSaveMode = false,
                batteryPercent = 90,
                isKeyguardLocked = true,
            ),
        )
    }

    @Test
    fun holdsLastFrameWhenNonLoopingCompleted() {
        assertFalse(
            PowerPolicyHelper.shouldPlay(
                config = baseConfig.copy(loop = false),
                isVisible = true,
                isPreview = false,
                hasCompletedNonLoop = true,
                isPowerSaveMode = false,
                batteryPercent = 90,
                isKeyguardLocked = false,
            ),
        )
    }

    @Test
    fun playlistRotatesOnUnlockAndHourly() {
        val twoItems = listOf(
            VideoItem("uri://1", "one.mp4"),
            VideoItem("uri://2", "two.mp4"),
        )
        assertTrue(
            PowerPolicyHelper.shouldRotatePlaylist(
                config = baseConfig.copy(
                    playlist = twoItems,
                    rotationInterval = RotationInterval.ON_UNLOCK,
                ),
                nowMs = 10_000L,
                wasJustUnlocked = true,
            ),
        )
        assertTrue(
            PowerPolicyHelper.shouldRotatePlaylist(
                config = baseConfig.copy(
                    playlist = twoItems,
                    rotationInterval = RotationInterval.HOURLY,
                    lastRotatedAtMs = 1_000L,
                ),
                nowMs = 1_000L + 3_600_000L,
                wasJustUnlocked = false,
            ),
        )
    }
}
