package com.app.videosdk.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.videosdk.utils.PlayerUtils.formatTime

@Composable
fun CustomSlider(
    spriteUrl: String? = null,
    currentPosition: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    showControls: (Boolean) -> Unit
) {
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isSeeking by remember { mutableStateOf(false) }

    LaunchedEffect(currentPosition, duration) {
        if (!isSeeking) {
            sliderPosition = if (duration > 0) currentPosition.toFloat() / duration else 0f
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
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
                thumbColor = Color.Transparent,  // Custom thumb color
                activeTrackColor = Color.Transparent,  // Hide default red track
                inactiveTrackColor = Color.Transparent,  // Hide default inactive track
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp) // Adjust thumb area (does not affect track thickness)
                .drawBehind {
                    val trackHeight = 4.dp.toPx()  // Change track thickness here
                    val redTrackHeight = 4.dp.toPx()  // Custom Red progress height
                    val trackY = size.height / 2 - trackHeight / 2

                    // Draw Gray Background Track
                    drawRoundRect(
                        color = Color.Gray.copy(alpha = 0.5f),
                        topLeft = Offset(0f, trackY),
                        size = Size(size.width, trackHeight),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )

                    // Draw Custom Red Progress Track (only up to current position)
                    drawRoundRect(
                        color = Color.Red,
                        topLeft = Offset(
                            0f,
                            trackY + (trackHeight - redTrackHeight) / 2
                        ), // Align center
                        size = Size(
                            size.width * sliderPosition,
                            redTrackHeight
                        ),  // Red progress width
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(currentPosition),
                color = Color.White,
                fontSize = 12.sp
            )
            Text(
                text = formatTime(duration),
                color = Color.White,
                fontSize = 12.sp
            )
        }
    }
}