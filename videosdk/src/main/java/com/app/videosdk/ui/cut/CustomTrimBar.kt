package com.app.videosdk.ui.cut

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

@Composable
fun CustomTrimBar(
    duration: Long,
    minClip: Long,
    maxClip: Long,
    modifier: Modifier = Modifier,
    onRangeChanged: (Long, Long) -> Unit
) {

    val density = LocalDensity.current
    var containerWidth by remember { mutableStateOf(1f) }

    var startPx by remember { mutableStateOf(0f) }
    var endPx by remember { mutableStateOf(0f) }

    val handleWidthPx = with(density) { 24.dp.toPx() }

    Box(
        modifier = modifier
            .height(90.dp)
            .fillMaxWidth()
            .onSizeChanged {
                containerWidth = it.width.toFloat()

                // default 60s selection
                val defaultDuration = 60_000L
                endPx = (defaultDuration.toFloat() / duration) * containerWidth
            }
    ) {

        // Background timeline
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.DarkGray.copy(alpha = 0.3f))
        )

        // Selected Area (CENTER DRAG AREA)
        Box(
            Modifier
                .offset { IntOffset(startPx.toInt(), 0) }
                .width(with(density) { (endPx - startPx).toDp() })
                .fillMaxHeight()
                .background(Color(0xFF2196F3).copy(alpha = 0.5f))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()

                        val width = endPx - startPx

                        var newStart = startPx + dragAmount.x
                        var newEnd = endPx + dragAmount.x

                        if (newStart < 0f) {
                            newStart = 0f
                            newEnd = width
                        }

                        if (newEnd > containerWidth) {
                            newEnd = containerWidth
                            newStart = containerWidth - width
                        }

                        startPx = newStart
                        endPx = newEnd

                        val startMs =
                            (startPx / containerWidth * duration).toLong()
                        val endMs =
                            (endPx / containerWidth * duration).toLong()

                        onRangeChanged(startMs, endMs)
                    }
                }
        )

        // LEFT HANDLE
        Box(
            Modifier
                .offset { IntOffset(startPx.toInt(), 0) }
                .width(24.dp)
                .fillMaxHeight()
                .background(Color.Red)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()

                        val minWidthPx =
                            (minClip.toFloat() / duration) * containerWidth

                        var newStart = (startPx + dragAmount.x)
                            .coerceIn(0f, endPx - minWidthPx)

                        startPx = newStart

                        val startMs =
                            (startPx / containerWidth * duration).toLong()
                        val endMs =
                            (endPx / containerWidth * duration).toLong()

                        onRangeChanged(startMs, endMs)
                    }
                }
        )

        // RIGHT HANDLE
        Box(
            Modifier
                .offset { IntOffset(endPx.toInt() - handleWidthPx.toInt(), 0) }
                .width(24.dp)
                .fillMaxHeight()
                .background(Color.Red)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()

                        val minWidthPx =
                            (minClip.toFloat() / duration) * containerWidth
                        val maxWidthPx =
                            (maxClip.toFloat() / duration) * containerWidth

                        var newEnd = endPx + dragAmount.x

                        val currentWidth = newEnd - startPx

                        if (currentWidth < minWidthPx) {
                            newEnd = startPx + minWidthPx
                        }

                        if (currentWidth > maxWidthPx) {
                            newEnd = startPx + maxWidthPx
                        }

                        endPx = newEnd.coerceAtMost(containerWidth)

                        val startMs =
                            (startPx / containerWidth * duration).toLong()
                        val endMs =
                            (endPx / containerWidth * duration).toLong()

                        onRangeChanged(startMs, endMs)
                    }
                }
        )
    }
}