package com.app.videosdk.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer
import com.app.videosdk.utils.PlayerUtils.formatTime

@Composable
fun CustomSlider(
    modifier: Modifier = Modifier,
    spriteUrl: String? = null,
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    showControls: (Boolean) -> Unit,
    onDownPressed: () -> Unit = {},
    onUpPressed: () -> Unit = {},
    isLive: Boolean = false,
    exoPlayer: ExoPlayer? = null,
    playFocusRequester: FocusRequester? = null // Added to recover focus
) {
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isSeeking by remember { mutableStateOf(false) }
    var isRowFocused by remember { mutableStateOf(false) }
    
    val liveButtonFocusRequester = remember { FocusRequester() }
    val sliderFocusRequester = remember { FocusRequester() }

    val isAtLiveEdge = remember(currentPosition, duration, isLive) {
        if (!isLive) false else (duration - currentPosition) < 10_000L
    }

    LaunchedEffect(currentPosition, duration) {
        if (!isSeeking) {
            sliderPosition = if (duration > 0) currentPosition.toFloat() / duration else 0f
        }
    }

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

    LaunchedEffect(isAtLiveEdge, isRowFocused) {
        if (isLive && !isAtLiveEdge && isRowFocused) {
            liveButtonFocusRequester.requestFocus()
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { isRowFocused = it.isFocused }
            .background(
                if (isRowFocused) Color.White.copy(alpha = 0.1f) else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .border(
                if (isRowFocused) 1.dp else 0.dp,
                if (isRowFocused) Color.White.copy(alpha = 0.5f) else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
                val newSeekPosition = (sliderPosition * duration).toLong()
                onSeek(newSeekPosition)
                isSeeking = false
                showControls(false)
            },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = if (isRowFocused) Color.Red else Color.Transparent,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent,
            ),
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .focusRequester(sliderFocusRequester)
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        when (event.key) {
                            Key.DirectionLeft -> {
                                if (duration > 0) {
                                    val step = 10_000f / duration
                                    sliderPosition = (sliderPosition - step).coerceIn(0f, 1f)
                                    onSeek((sliderPosition * duration).toLong())
                                    showControls(true)
                                }
                                true
                            }
                            Key.DirectionRight -> {
                                if (isLive && !isAtLiveEdge) {
                                    liveButtonFocusRequester.requestFocus()
                                } else if (duration > 0) {
                                    val step = 10_000f / duration
                                    sliderPosition = (sliderPosition + step).coerceIn(0f, 1f)
                                    onSeek((sliderPosition * duration).toLong())
                                    showControls(true)
                                }
                                true
                            }
                            Key.DirectionDown -> {
                                onDownPressed()
                                true
                            }
                            Key.DirectionUp -> {
                                onUpPressed()
                                true
                            }
                            // FIXED: Handle Center/OK to prevent focus loss
                            Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                if (exoPlayer?.isPlaying == true) {
                                    exoPlayer.pause()
                                } else {
                                    exoPlayer?.play()
                                }
                                // Force focus back to the Play/Pause button
                                playFocusRequester?.requestFocus()
                                true
                            }
                            else -> false
                        }
                    } else false
                }
                .focusable()
                .drawBehind {
                    val trackHeight = 4.dp.toPx()
                    val redTrackHeight = if (isRowFocused) 6.dp.toPx() else 4.dp.toPx()
                    val trackY = size.height / 2 - trackHeight / 2
                    drawRoundRect(
                        color = Color.Gray.copy(alpha = 0.5f),
                        topLeft = Offset(0f, trackY),
                        size = Size(size.width, trackHeight),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                    drawRoundRect(
                        color = if (isLive && isAtLiveEdge) Color.Red else Color.Red.copy(alpha = 0.7f),
                        topLeft = Offset(0f, trackY + (trackHeight - redTrackHeight) / 2),
                        size = Size(size.width * sliderPosition, redTrackHeight),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                }
        )

        if (!isLive) {
            Text(
                text = formatTime(duration),
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
        } else {
            var isLiveButtonFocused by remember { mutableStateOf(false) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .focusRequester(liveButtonFocusRequester)
                    .onFocusChanged { isLiveButtonFocused = it.isFocused }
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isLiveButtonFocused) Color.White.copy(alpha = 0.2f) else Color.Transparent)
                    .border(
                        width = if (isLiveButtonFocused) 1.dp else 0.dp,
                        color = if (isLiveButtonFocused) Color.White else Color.Transparent,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .clickable {
                        onSeek(duration)
                        exoPlayer?.play()
                    }
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            when (event.key) {
                                Key.DirectionLeft -> {
                                    sliderFocusRequester.requestFocus()
                                    true
                                }
                                Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                    onSeek(duration)
                                    exoPlayer?.play()
                                    true
                                }
                                Key.DirectionDown -> {
                                    onDownPressed()
                                    true
                                }
                                Key.DirectionUp -> {
                                    onUpPressed()
                                    true
                                }
                                else -> false
                            }
                        } else false
                    }
                    .focusable()
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