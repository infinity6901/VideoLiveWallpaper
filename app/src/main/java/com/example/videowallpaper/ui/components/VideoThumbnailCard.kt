package com.example.videowallpaper.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size

@Composable
fun VideoThumbnailCard(
    uri:         String?,
    displayName: String,
    modifier:    Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(shape)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = shape,
            )
            .background(MaterialTheme.colorScheme.surface),
    ) {
        if (uri != null) {
            // Video thumbnail via Coil's video decoder
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(Uri.parse(uri))
                    .size(Size.ORIGINAL)
                    .crossfade(true)
                    .build(),
                contentDescription = "Video thumbnail",
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.fillMaxSize(),
            )

            // Gradient overlay at the bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                        )
                    )
            )

            // Play icon
            Icon(
                imageVector        = Icons.Filled.PlayCircle,
                contentDescription = null,
                tint               = Color.White.copy(alpha = 0.9f),
                modifier           = Modifier
                    .size(56.dp)
                    .align(Alignment.Center),
            )

            // File name
            if (displayName.isNotBlank()) {
                Text(
                    text     = displayName,
                    style    = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color    = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp),
                )
            }
        } else {
            // Empty state
            Column(
                modifier              = Modifier.fillMaxSize(),
                verticalArrangement   = Arrangement.Center,
                horizontalAlignment   = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector        = Icons.Filled.VideoFile,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier           = Modifier.size(48.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text  = "No video selected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }
    }
}
