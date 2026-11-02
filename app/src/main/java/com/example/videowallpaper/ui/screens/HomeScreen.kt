package com.example.videowallpaper.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.videowallpaper.ui.components.VideoThumbnailCard
import com.example.videowallpaper.ui.theme.Coral
import com.example.videowallpaper.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // ── Launchers ──────────────────────────────────────────────────────────────

    // Video picker
    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val displayName = context.contentResolver.query(
                uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst())
                    cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                else null
            } ?: "video"
            viewModel.onVideoPicked(uri, displayName)
        }
    }

    // Permission launcher (Android 13+)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.onPermissionGranted()
            videoPicker.launch(arrayOf("video/*"))
        } else {
            viewModel.onPermissionDenied()
        }
    }

    // Wallpaper picker
    val wallpaperLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { /* result handled by system */ }

    // ── UI ─────────────────────────────────────────────────────────────────────

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Video Live Wallpaper",
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement   = Arrangement.spacedBy(16.dp),
        ) {

            // ── Hero banner ────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            )
                        ),
                        shape = RoundedCornerShape(20.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector        = Icons.Filled.Wallpaper,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.primary,
                        modifier           = Modifier.size(36.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text      = "Set any video as your live wallpaper",
                        style     = MaterialTheme.typography.bodyMedium,
                        color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // ── Video thumbnail ────────────────────────────────────────────────
            Text(
                text  = "Selected Video",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight    = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )

            VideoThumbnailCard(
                uri         = uiState.selectedVideoUri,
                displayName = uiState.videoDisplayName,
            )

            // ── Permission warning ─────────────────────────────────────────────
            if (uiState.permissionNeeded) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(
                        modifier          = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector        = Icons.Filled.Warning,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text  = "Storage permission is required to pick a video.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }

            // ── Action buttons ─────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Pick video button
                OutlinedButton(
                    onClick  = {
                        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                            Manifest.permission.READ_MEDIA_VIDEO
                        else
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        permissionLauncher.launch(permission)
                    },
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Filled.VideoLibrary, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Pick Video")
                }

                // Clear button (only when a video is selected)
                if (uiState.selectedVideoUri != null) {
                    IconButton(
                        onClick = viewModel::clearSelectedVideo,
                    ) {
                        Icon(
                            imageVector        = Icons.Filled.DeleteOutline,
                            contentDescription = "Clear video",
                            tint               = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            // Set wallpaper button
            Button(
                onClick  = {
                    val intent = viewModel.buildSetWallpaperIntent()
                    wallpaperLauncher.launch(intent)
                },
                enabled  = uiState.selectedVideoUri != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape    = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Filled.Wallpaper, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Set as Live Wallpaper",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                )
            }

            // ── Info card ──────────────────────────────────────────────────────
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.primary,
                            modifier           = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "How to use",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    listOf(
                        "1. Tap Pick Video and select any MP4/MOV from your gallery",
                        "2. Adjust loop, mute, and fit settings via the ⚙ button",
                        "3. Tap Set as Live Wallpaper to open the system picker",
                        "4. Confirm the wallpaper in the system dialog",
                    ).forEach { step ->
                        Text(
                            text     = step,
                            style    = MaterialTheme.typography.bodySmall,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
