package com.app.videosdk.ui

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
            sliderPosition =
                if (duration > 0) currentPosition.toFloat() / duration else 0f
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {

        /* -------- CURRENT POSITION (LEFT) -------- */

        Text(
            text = formatTime(currentPosition),
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier.padding(end = 8.dp)
        )

        /* -------- SEEK BAR (CENTER) -------- */

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
                thumbColor = Color.Transparent,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent,
            ),
            modifier = Modifier
                .weight(1f)               // ⭐ CENTER FLEX
                .height(4.dp)
                .drawBehind {
                    val trackHeight = 4.dp.toPx()
                    val redTrackHeight = 4.dp.toPx()
                    val trackY = size.height / 2 - trackHeight / 2

                    // Gray background track
                    drawRoundRect(
                        color = Color.Gray.copy(alpha = 0.5f),
                        topLeft = Offset(0f, trackY),
                        size = Size(size.width, trackHeight),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )

                    // Red progress track
                    drawRoundRect(
                        color = Color.Red,
                        topLeft = Offset(
                            0f,
                            trackY + (trackHeight - redTrackHeight) / 2
                        ),
                        size = Size(
                            size.width * sliderPosition,
                            redTrackHeight
                        ),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                }
        )

        /* -------- TOTAL DURATION (RIGHT) -------- */

        Text(
            text = formatTime(duration),
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
