package com.app.videosdk.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.app.videosdk.model.PlayerModel

@Composable
fun AudioVideoToggleSwitch(
    isAudioOnly: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val thumbOffset by animateDpAsState(
        targetValue = if (isAudioOnly) 0.dp else 36.dp,
        label = "thumbOffset"
    )

    Box(
        modifier = modifier
            .width(72.dp)
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable { onToggle() }
            .padding(2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // Sliding active thumb background
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.Gray.copy(alpha = 0.5f))
        )

        // Row of icons
        Row(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = Icons.Default.Headphones,
                contentDescription = "Audio Mode",
                tint = if (isAudioOnly) Color.White else Color.LightGray.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Video Mode",
                tint = if (!isAudioOnly) Color.White else Color.LightGray.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun AudioOnlyThumbnailOverlay(
    playerModel: PlayerModel?,
    isAudioOnly: Boolean,
    isFullScreen: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (!isAudioOnly) return

    val thumbUrl = if (isFullScreen) {
        playerModel?.seasonBanner?.takeIf { it.isNotBlank() }
            ?: playerModel?.imageUrl?.takeIf { it.isNotBlank() }
            ?: playerModel?.thumbnail
    } else {
        playerModel?.imageUrl?.takeIf { it.isNotBlank() }
            ?: playerModel?.thumbnail
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            if (!thumbUrl.isNullOrBlank()) {
                Image(
                    painter = rememberAsyncImagePainter(thumbUrl),
                    contentDescription = "Audio Thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .clip(RoundedCornerShape(if (isFullScreen) 12.dp else 6.dp))
                        .width(if (isFullScreen) 260.dp else 180.dp)
                        .height(if (isFullScreen) 146.dp else 100.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            playerModel?.episodeTitle?.takeIf { it.isNotBlank() }?.let { title ->
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Headphones,
                    contentDescription = "Audio Mode",
                    tint = Color.Green,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Audio Only Mode",
                    color = Color.Green,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
