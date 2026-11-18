package com.infinity.videowallpaper.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.infinity.videowallpaper.R
import com.infinity.videowallpaper.data.PlaybackScope
import com.infinity.videowallpaper.data.RotationInterval
import com.infinity.videowallpaper.data.ThemeMode
import com.infinity.videowallpaper.data.WallpaperConfig
import com.infinity.videowallpaper.ui.components.ChipSelectorRow
import com.infinity.videowallpaper.ui.components.FitModeSelector
import com.infinity.videowallpaper.ui.components.SectionHeader
import com.infinity.videowallpaper.ui.components.SpeedSelector
import com.infinity.videowallpaper.ui.components.ToggleSettingRow
import com.infinity.videowallpaper.ui.components.ValueSliderRow
import com.infinity.videowallpaper.viewmodel.SettingsViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_auto_saved),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // ── Playback ───────────────────────────────────────────────────────
            SectionHeader(stringResource(R.string.section_playback))

            ToggleSettingRow(
                title = stringResource(R.string.setting_loop),
                subtitle = stringResource(R.string.setting_loop_desc),
                checked = config.loop,
                onCheckedChange = viewModel::setLoop,
            )

            ToggleSettingRow(
                title = stringResource(R.string.setting_mute),
                subtitle = stringResource(R.string.setting_mute_desc),
                checked = config.mute,
                onCheckedChange = viewModel::setMute,
            )

            SpeedSelector(
                selectedIndex = config.speedIndex,
                onSelect = viewModel::setSpeedIndex,
            )

            // ── Visual & Shader Effects ────────────────────────────────────────
            SectionHeader(stringResource(R.string.section_visual))

            FitModeSelector(
                selected = config.fitMode,
                onSelect = viewModel::setFitMode,
            )

            ValueSliderRow(
                title = stringResource(R.string.setting_dim),
                subtitle = stringResource(R.string.setting_dim_desc),
                value = config.dimAmount,
                valueLabel = "${(config.dimAmount * 100f).roundToInt()}%",
                valueRange = 0f..0.8f,
                steps = 7,
                onValueChange = viewModel::setDimAmount,
            )

            ValueSliderRow(
                title = stringResource(R.string.setting_blur),
                subtitle = stringResource(R.string.setting_blur_desc),
                value = config.blurAmount,
                valueLabel = "${(config.blurAmount * 100f).roundToInt()}%",
                valueRange = 0f..1f,
                steps = 9,
                onValueChange = viewModel::setBlurAmount,
            )

            ValueSliderRow(
                title = stringResource(R.string.setting_vignette),
                subtitle = stringResource(R.string.setting_vignette_desc),
                value = config.vignetteAmount,
                valueLabel = "${(config.vignetteAmount * 100f).roundToInt()}%",
                valueRange = 0f..1f,
                steps = 9,
                onValueChange = viewModel::setVignetteAmount,
            )

            ValueSliderRow(
                title = stringResource(R.string.setting_saturation),
                subtitle = stringResource(R.string.setting_saturation_desc),
                value = config.saturation,
                valueLabel = "${(config.saturation * 100f).roundToInt()}%",
                valueRange = 0f..1.5f,
                steps = 14,
                onValueChange = viewModel::setSaturation,
            )

            ToggleSettingRow(
                title = stringResource(R.string.setting_parallax),
                subtitle = stringResource(R.string.setting_parallax_desc),
                checked = config.parallaxEnabled,
                onCheckedChange = viewModel::setParallaxEnabled,
            )

            // ── Battery & Gestures ─────────────────────────────────────────────
            SectionHeader(stringResource(R.string.section_power))

            ToggleSettingRow(
                title = stringResource(R.string.setting_pause_battery_saver),
                subtitle = stringResource(R.string.setting_pause_battery_saver_desc),
                checked = config.pauseOnBatterySaver,
                onCheckedChange = viewModel::setPauseOnBatterySaver,
            )

            ValueSliderRow(
                title = stringResource(R.string.setting_low_battery),
                subtitle = stringResource(R.string.setting_low_battery_desc),
                value = config.lowBatteryThreshold.toFloat(),
                valueLabel = if (config.lowBatteryThreshold == 0) "Off" else "${config.lowBatteryThreshold}%",
                valueRange = 0f..50f,
                steps = 9,
                onValueChange = { viewModel.setLowBatteryThreshold(it.roundToInt()) },
            )

            ChipSelectorRow(
                title = stringResource(R.string.setting_fps_cap),
                subtitle = stringResource(R.string.setting_fps_cap_desc),
                options = WallpaperConfig.FPS_OPTIONS.map { it to "$it fps" },
                selected = config.fpsCap,
                onSelect = viewModel::setFpsCap,
            )

            ChipSelectorRow(
                title = stringResource(R.string.setting_playback_scope),
                subtitle = stringResource(R.string.setting_playback_scope_desc),
                options = listOf(
                    PlaybackScope.BOTH to stringResource(R.string.scope_both),
                    PlaybackScope.HOME_ONLY to stringResource(R.string.scope_home_only),
                    PlaybackScope.LOCK_ONLY to stringResource(R.string.scope_lock_only),
                ),
                selected = config.playbackScope,
                onSelect = viewModel::setPlaybackScope,
            )

            ToggleSettingRow(
                title = stringResource(R.string.setting_double_tap),
                subtitle = stringResource(R.string.setting_double_tap_desc),
                checked = config.doubleTapToPause,
                onCheckedChange = viewModel::setDoubleTapToPause,
            )

            // ── Playlist & Storage ─────────────────────────────────────────────
            SectionHeader(stringResource(R.string.section_playlist))

            ChipSelectorRow(
                title = stringResource(R.string.setting_rotation),
                subtitle = stringResource(R.string.setting_rotation_desc),
                options = listOf(
                    RotationInterval.OFF to stringResource(R.string.rotation_off),
                    RotationInterval.ON_UNLOCK to stringResource(R.string.rotation_unlock),
                    RotationInterval.HOURLY to stringResource(R.string.rotation_hourly),
                    RotationInterval.DAILY to stringResource(R.string.rotation_daily),
                ),
                selected = config.rotationInterval,
                onSelect = viewModel::setRotationInterval,
            )

            ToggleSettingRow(
                title = stringResource(R.string.setting_copy_local),
                subtitle = stringResource(R.string.setting_copy_local_desc),
                checked = config.copyToLocalStorage,
                onCheckedChange = viewModel::setCopyToLocalStorage,
            )

            // ── Appearance ─────────────────────────────────────────────────────
            SectionHeader(stringResource(R.string.section_appearance))

            ChipSelectorRow(
                title = stringResource(R.string.setting_theme),
                options = listOf(
                    ThemeMode.SYSTEM to stringResource(R.string.theme_system),
                    ThemeMode.LIGHT to stringResource(R.string.theme_light),
                    ThemeMode.DARK to stringResource(R.string.theme_dark),
                    ThemeMode.AMOLED to stringResource(R.string.theme_amoled),
                ),
                selected = config.themeMode,
                onSelect = viewModel::setThemeMode,
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ToggleSettingRow(
                    title = stringResource(R.string.setting_dynamic_color),
                    subtitle = stringResource(R.string.setting_dynamic_color_desc),
                    checked = config.dynamicColor,
                    onCheckedChange = viewModel::setDynamicColor,
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
