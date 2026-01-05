package com.app.videosdk.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer
import com.app.videosdk.utils.PlayerUtils.formatTime

@Composable
fun CustomSlider(
    modifier: Modifier = Modifier,
    currentPosition: Long,
    duration: Long,
    cuePoints: List<CuePoint> = emptyList(),
    onSeek: (Long) -> Unit,
    showControls: (Boolean) -> Unit,
    isLive: Boolean = false,
    exoPlayer: ExoPlayer? = null
) {
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isSeeking by remember { mutableStateOf(false) }

    // Check if the user is at the live edge (within 10 seconds)
    val isAtLiveEdge = remember(currentPosition, duration, isLive) {
        isLive && (duration - currentPosition) < 10_000L
    }

    // Sync slider with playback
    LaunchedEffect(currentPosition, duration) {
        if (!isSeeking && duration > 0) {
            sliderPosition = currentPosition.toFloat() / duration
        }
    }

    // Blinking animation for Live indicator
    val infiniteTransition = rememberInfiniteTransition(label = "liveIndicator")
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blinkAlpha"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // Current time (hidden for Live)
        if (!isLive) {
            Text(
                text = formatTime(currentPosition),
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        Slider(
            value = sliderPosition,
            onValueChange = {
                sliderPosition = it
                isSeeking = true
                showControls(true)
            },
            onValueChangeFinished = {
                onSeek((sliderPosition * duration).toLong())
                isSeeking = false
                showControls(false)
            },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = Color.Red,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent,
            ),
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .drawBehind {
                    val trackHeight = 4.dp.toPx()
                    val trackY = size.height / 2 - trackHeight / 2

                    // Background track
                    drawRoundRect(
                        color = Color.Gray.copy(alpha = 0.5f),
                        topLeft = Offset(0f, trackY),
                        size = Size(size.width, trackHeight),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )

                    // Progress track
                    drawRoundRect(
                        color = if (isLive && isAtLiveEdge) Color.Red else Color.Red.copy(alpha = 0.7f),
                        topLeft = Offset(0f, trackY),
                        size = Size(size.width * sliderPosition, trackHeight),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )

                    // Cue markers (VISUAL ONLY)
                    if (duration > 0) {
                        cuePoints.forEach { cue ->

                            val x = (cue.positionMs.toFloat() / duration) * size.width

                            val markerWidth = 4.dp.toPx()
                            val markerHeight = 4.dp.toPx()
                            val cornerRadius = 1.dp.toPx()

                            drawRoundRect(
                                color = Color.Yellow,
                                topLeft = Offset(
                                    x = x - markerWidth / 2,
                                    y = size.height / 2 - markerHeight / 2
                                ),
                                size = Size(markerWidth, markerHeight),
                                cornerRadius = CornerRadius(cornerRadius)
                            )
                        }
                    }
                }
        )

        // Duration or Live button
        if (!isLive) {
            Text(
                text = formatTime(duration),
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isAtLiveEdge) Color.Transparent else Color.Gray.copy(alpha = 0.3f))
                    .clickable {
                        onSeek(duration)
                        exoPlayer?.play()
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .alpha(if (isAtLiveEdge) blinkAlpha else 1f)
                        .clip(CircleShape)
                        .background(if (isAtLiveEdge) Color.Red else Color.Gray)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isAtLiveEdge) "LIVE" else "Go Live",
                    color = if (isAtLiveEdge) Color.Red else Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

