package com.example.videowallpaper.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.videowallpaper.ui.components.*
import com.example.videowallpaper.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val state       by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show save confirmation toast
    LaunchedEffect(state.savedToast) {
        if (state.savedToast) {
            snackbarHostState.showSnackbar("Settings saved ✓")
            viewModel.clearToast()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text("Wallpaper Settings", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    // Save FAB in top bar
                    TextButton(
                        onClick = viewModel::save,
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Save")
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

            // ── Playback ───────────────────────────────────────────────────────
            SectionHeader("Playback")

            ToggleSettingRow(
                title    = "Loop Video",
                subtitle = "Restart the video when it ends",
                checked  = state.loop,
                onCheckedChange = viewModel::setLoop,
            )

            ToggleSettingRow(
                title    = "Mute Audio",
                subtitle = "Play without sound (recommended)",
                checked  = state.mute,
                onCheckedChange = viewModel::setMute,
            )

            SpeedSelector(
                selectedIndex = state.speedIndex,
                onSelect      = viewModel::setSpeedIndex,
            )

            // ── Visual ─────────────────────────────────────────────────────────
            SectionHeader("Visual")

            FitModeSelector(
                selected = state.fitMode,
                onSelect = viewModel::setFitMode,
            )

            DimSliderRow(
                value         = state.dimAmount,
                onValueChange = viewModel::setDimAmount,
            )

            Spacer(Modifier.height(24.dp))

            // Save button at bottom
            Button(
                onClick  = viewModel::save,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape    = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Filled.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Save Settings",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
