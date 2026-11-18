package com.infinity.videowallpaper.ui.screens

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.infinity.videowallpaper.R
import com.infinity.videowallpaper.data.VideoItem
import com.infinity.videowallpaper.ui.components.VideoPreviewDialog
import com.infinity.videowallpaper.ui.components.VideoThumbnailCard
import com.infinity.videowallpaper.ui.components.VideoTrimmerSheet
import com.infinity.videowallpaper.ui.components.formatDurationMs
import com.infinity.videowallpaper.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val config = uiState.config
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showPreview by remember { mutableStateOf(false) }
    var showTrimmer by remember { mutableStateOf(false) }

    val noPickerMsg = stringResource(R.string.error_no_wallpaper_picker)

    // Refresh active live wallpaper status whenever returning from system picker
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshActiveStatus()
    }

    LaunchedEffect(uiState.statusMessage) {
        val msg = uiState.statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    // SAF OpenDocument requires ZERO runtime storage permissions on any Android version
    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onVideoPicked(uri)
        }
    }

    val wallpaperLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        viewModel.refreshActiveStatus()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.btn_settings),
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Hero banner + live status badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            ),
                        ),
                        shape = RoundedCornerShape(20.dp),
                    )
                    .padding(horizontal = 16.dp, vertical = 18.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    StatusBadge(
                        isActive = uiState.isWallpaperActive,
                        isPaused = config.manualPaused,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.home_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // Selected Video Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.selected_video_header),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (uiState.isCopyingVideo) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }

            val clipSummary = if (config.clipRange.isTrimmed && config.durationMs > 0L) {
                val endLabel = if (config.clipEndMs > 0L) config.clipEndMs else config.durationMs
                "${formatDurationMs(config.clipStartMs)} – ${formatDurationMs(endLabel)}"
            } else {
                null
            }

            VideoThumbnailCard(
                uri = config.videoUri,
                displayName = config.videoDisplayName,
                isLocalCopy = config.isLocalCopy,
                clipSummary = clipSummary,
                rotationDegrees = config.rotationDegrees,
                onPreviewClick = { showPreview = true },
            )

            // Primary picker / trim / clear row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = { videoPicker.launch(arrayOf("video/*")) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Filled.VideoLibrary, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.btn_pick_video), maxLines = 1)
                }

                if (config.videoUri != null) {
                    OutlinedButton(
                        onClick = viewModel::rotateClockwise,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Filled.Rotate90DegreesCw, contentDescription = stringResource(R.string.btn_rotate))
                        Spacer(Modifier.width(4.dp))
                        Text("${config.rotationDegrees}°", maxLines = 1)
                    }
                }

                if (config.videoUri != null && config.durationMs >= 1_500L) {
                    IconButton(onClick = { showTrimmer = true }) {
                        Icon(
                            imageVector = Icons.Filled.ContentCut,
                            contentDescription = stringResource(R.string.btn_trim_video),
                        )
                    }
                }

                if (config.videoUri != null) {
                    IconButton(onClick = viewModel::clearSelectedVideo) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = stringResource(R.string.btn_clear_video),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            // Quick playback controls when wallpaper is active or playlist has multiple videos
            if (uiState.isWallpaperActive || config.playlist.size >= 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = viewModel::toggleManualPause,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(
                            imageVector = if (config.manualPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            contentDescription = null,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_toggle_pause), maxLines = 1)
                    }
                    if (config.playlist.size >= 2) {
                        OutlinedButton(
                            onClick = viewModel::advancePlaylist,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(Icons.Filled.SkipNext, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.btn_next_video), maxLines = 1)
                        }
                    }
                }
            }

            // Set as Live Wallpaper button with OEM fallback
            Button(
                onClick = {
                    try {
                        wallpaperLauncher.launch(viewModel.buildSetWallpaperIntent())
                    } catch (_: ActivityNotFoundException) {
                        try {
                            wallpaperLauncher.launch(viewModel.buildFallbackWallpaperChooserIntent())
                        } catch (_: ActivityNotFoundException) {
                            viewModel.showMessage(noPickerMsg)
                        }
                    }
                },
                enabled = config.videoUri != null && !uiState.isCopyingVideo,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Filled.Wallpaper, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.btn_set_wallpaper),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                )
            }

            // Playlist / Recent Videos strip
            if (config.playlist.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.recent_playlist_header),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 2.dp),
                ) {
                    items(config.playlist, key = { it.uri }) { item ->
                        PlaylistVideoChip(
                            item = item,
                            isSelected = item.uri == config.videoUri,
                            onSelect = { viewModel.selectFromPlaylist(item) },
                            onRemove = { viewModel.removeVideo(item) },
                        )
                    }
                }
            }

            // How to use card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.how_to_use_title),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    listOf(
                        stringResource(R.string.how_to_use_step_1),
                        stringResource(R.string.how_to_use_step_2),
                        stringResource(R.string.how_to_use_step_3),
                        stringResource(R.string.how_to_use_step_4),
                    ).forEach { step ->
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    if (showPreview && config.videoUri != null) {
        VideoPreviewDialog(
            config = config,
            onRotate = viewModel::rotateClockwise,
            onDismiss = { showPreview = false },
        )
    }

    if (showTrimmer && config.videoUri != null && config.durationMs > 0L) {
        VideoTrimmerSheet(
            durationMs = config.durationMs,
            initialStartMs = config.clipStartMs,
            initialEndMs = config.clipEndMs,
            onApply = { start, end -> viewModel.updateClipRange(start, end) },
            onDismiss = { showTrimmer = false },
        )
    }
}

@Composable
private fun StatusBadge(
    isActive: Boolean,
    isPaused: Boolean,
) {
    val label = when {
        isActive && isPaused -> stringResource(R.string.status_paused)
        isActive -> stringResource(R.string.status_active)
        else -> stringResource(R.string.status_inactive)
    }
    val containerColor = when {
        isActive && !isPaused -> MaterialTheme.colorScheme.primary
        isActive && isPaused -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when {
        isActive && !isPaused -> MaterialTheme.colorScheme.onPrimary
        isActive && isPaused -> MaterialTheme.colorScheme.onSecondary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(999.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = if (isActive) Icons.Filled.CheckCircle else Icons.Filled.Wallpaper,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = contentColor,
            )
        }
    }
}

@Composable
private fun PlaylistVideoChip(
    item: VideoItem,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onRemove: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .width(124.dp)
            .height(86.dp)
            .clip(shape)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                shape = shape,
            )
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onSelect),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(Uri.parse(item.uri))
                .videoFrameMillis(500L)
                .crossfade(true)
                .build(),
            contentDescription = item.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f)),
                    ),
                ),
        )
        Surface(
            color = Color.Black.copy(alpha = 0.6f),
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(22.dp)
                .clickable(onClick = onRemove),
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.btn_clear_video),
                tint = Color.White,
                modifier = Modifier.padding(3.dp),
            )
        }
        Text(
            text = item.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp),
        )
    }
}
