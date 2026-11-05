package com.app.videosdk.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach

@Composable
internal fun PlayerGestureLayer(
    pipEnabled: Boolean,
    isAdsShowing: Boolean,
    isLockScreen: Boolean,
    zoomAccumulator: Float,
    onZoomAccumulatorChanged: (Float) -> Unit
) {
    val currentZoomAccumulator by rememberUpdatedState(zoomAccumulator)
    val currentPipEnabled by rememberUpdatedState(pipEnabled)
    val currentIsAdsShowing by rememberUpdatedState(isAdsShowing)
    val currentIsLockScreen by rememberUpdatedState(isLockScreen)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val activeCount = event.changes.count { it.pressed }
                        if (activeCount > 1) {
                            if (!currentPipEnabled && !currentIsLockScreen && !currentIsAdsShowing) {
                                while (true) {
                                    val zoomEvent = awaitPointerEvent(PointerEventPass.Initial)
                                    val currentActive = zoomEvent.changes.count { it.pressed }
                                    if (currentActive < 2) break
                                    val zoomChange = zoomEvent.calculateZoom()
                                    if (zoomChange != 1f) {
                                        val newZoom = (currentZoomAccumulator * zoomChange).coerceIn(1f, 4f)
                                        onZoomAccumulatorChanged(newZoom)
                                    }
                                    zoomEvent.changes.fastForEach {
                                        if (it.positionChanged()) it.consume()
                                    }
                                }
                                do {
                                    val upEvent = awaitPointerEvent(PointerEventPass.Initial)
                                    upEvent.changes.fastForEach { it.consume() }
                                } while (upEvent.changes.fastAny { it.pressed })
                            }
                            break
                        }
                        if (activeCount == 0) break
                    }
                }
            }
    )
}
