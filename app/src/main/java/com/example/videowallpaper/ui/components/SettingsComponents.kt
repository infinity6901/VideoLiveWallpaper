package com.example.videowallpaper.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.videowallpaper.data.FitMode
import com.example.videowallpaper.data.WallpaperPreferences

// ── Section Header ─────────────────────────────────────────────────────────────

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text     = title.uppercase(),
        style    = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 2.sp,
            fontWeight    = FontWeight.Bold,
        ),
        color    = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(top = 24.dp, bottom = 8.dp),
    )
}

// ── Toggle Setting Row ─────────────────────────────────────────────────────────

@Composable
fun ToggleSettingRow(
    title:    String,
    subtitle: String,
    checked:  Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier          = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text  = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(16.dp))
        Switch(
            checked         = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

// ── Speed Chip Selector ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedSelector(
    selectedIndex: Int,
    onSelect:      (Int) -> Unit,
    modifier:      Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Text(
            text  = "Playback Speed",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            WallpaperPreferences.SPEED_LABELS.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                FilterChip(
                    selected = selected,
                    onClick  = { onSelect(index) },
                    label    = { Text(label) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// ── Fit Mode Selector ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitModeSelector(
    selected: FitMode,
    onSelect: (FitMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(
        FitMode.CROP     to "Crop",
        FitMode.CONTAIN  to "Contain",
        FitMode.STRETCH  to "Stretch",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Text(
            text  = "Fit Mode",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text  = "How the video is scaled to fill the screen",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (mode, label) ->
                SegmentedButton(
                    selected = selected == mode,
                    onClick  = { onSelect(mode) },
                    shape    = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    label    = { Text(label) },
                )
            }
        }
    }
}

// ── Dim Slider Row ─────────────────────────────────────────────────────────────

@Composable
fun DimSliderRow(
    value:    Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text  = "Dim Brightness",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text  = "${(value * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text  = "Darkens the wallpaper to save battery",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value         = value,
            onValueChange = onValueChange,
            valueRange    = 0f..0.8f,
            steps         = 7,
            modifier      = Modifier.fillMaxWidth(),
        )
    }
}
