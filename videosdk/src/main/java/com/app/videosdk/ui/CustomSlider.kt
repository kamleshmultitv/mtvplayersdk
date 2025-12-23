package com.app.videosdk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onUpPressed: () -> Unit = {}
) {
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isSeeking by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(currentPosition, duration) {
        if (!isSeeking) {
            sliderPosition =
                if (duration > 0) currentPosition.toFloat() / duration else 0f
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                if (isFocused) Color.White.copy(alpha = 0.1f) else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .border(
                if (isFocused) 1.dp else 0.dp,
                if (isFocused) Color.White.copy(alpha = 0.5f) else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .onKeyEvent { event ->
                // ONLY handle KeyDown to prevent "double jumping" focus
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
                            if (duration > 0) {
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
                        else -> false
                    }
                } else false
            }
            .focusable()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(
            text = formatTime(currentPosition),
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier.padding(end = 8.dp)
        )

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
                thumbColor = if (isFocused) Color.Red else Color.Transparent,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent,
            ),
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .drawBehind {
                    val trackHeight = 4.dp.toPx()
                    val redTrackHeight = if (isFocused) 6.dp.toPx() else 4.dp.toPx()
                    val trackY = size.height / 2 - trackHeight / 2
                    drawRoundRect(
                        color = Color.Gray.copy(alpha = 0.5f),
                        topLeft = Offset(0f, trackY),
                        size = Size(size.width, trackHeight),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                    drawRoundRect(
                        color = Color.Red,
                        topLeft = Offset(0f, trackY + (trackHeight - redTrackHeight) / 2),
                        size = Size(size.width * sliderPosition, redTrackHeight),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                }
        )

        Text(
            text = formatTime(duration),
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}