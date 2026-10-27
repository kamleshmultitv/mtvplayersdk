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
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.hypot

@Composable
internal fun PlayerGestureLayer(
    isFullScreen: Boolean,
    pipEnabled: Boolean,
    isAdsShowing: Boolean,
    isLockScreen: Boolean,
    isSettingsClick: Boolean,
    showControls: Boolean,
    fillScale: Float,
    zoomAccumulator: Float,
    hasActivatedFill: Boolean,
    onActivatedFillChanged: (Boolean) -> Unit,
    onFilledChanged: (Boolean) -> Unit,
    onZoomAccumulatorChanged: (Float) -> Unit,
    onLockOverlayVisibleChanged: (Boolean) -> Unit,
    onToggleControllerVisibility: () -> Unit,
    onLongPress2xSpeedStart: () -> Unit = {},
    onLongPress2xSpeedEnd: () -> Unit = {}
) {
    val currentFillScale by rememberUpdatedState(fillScale)
    val currentZoomAccumulator by rememberUpdatedState(zoomAccumulator)
    val currentHasActivatedFill by rememberUpdatedState(hasActivatedFill)
    val currentOnActivatedFillChanged by rememberUpdatedState(onActivatedFillChanged)
    val currentOnFilledChanged by rememberUpdatedState(onFilledChanged)
    val currentOnZoomAccumulatorChanged by rememberUpdatedState(onZoomAccumulatorChanged)
    val currentOnLockOverlayVisibleChanged by rememberUpdatedState(onLockOverlayVisibleChanged)
    val currentOnToggleControllerVisibility by rememberUpdatedState(onToggleControllerVisibility)
    val currentOnLongPress2xSpeedStart by rememberUpdatedState(onLongPress2xSpeedStart)
    val currentOnLongPress2xSpeedEnd by rememberUpdatedState(onLongPress2xSpeedEnd)

    val currentIsFullScreen by rememberUpdatedState(isFullScreen)
    val currentPipEnabled by rememberUpdatedState(pipEnabled)
    val currentIsAdsShowing by rememberUpdatedState(isAdsShowing)
    val currentIsLockScreen by rememberUpdatedState(isLockScreen)
    val currentIsSettingsClick by rememberUpdatedState(isSettingsClick)
    val currentShowControls by rememberUpdatedState(showControls)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectPlayerGestures(
                    onTap = {
                        when {
                            currentIsLockScreen -> {
                                currentOnLockOverlayVisibleChanged(true)
                            }
                            currentShowControls && !currentPipEnabled && !currentIsSettingsClick -> {
                                currentOnToggleControllerVisibility()
                            }
                        }
                    },
                    onDoubleTap = {
                        currentOnActivatedFillChanged(false)
                        currentOnFilledChanged(false)
                        currentOnZoomAccumulatorChanged(1f)
                    },
                    onLongPressStart = {
                        if (!currentIsLockScreen && !currentPipEnabled && !currentIsAdsShowing) {
                            currentOnLongPress2xSpeedStart()
                        }
                    },
                    onLongPressEnd = {
                        currentOnLongPress2xSpeedEnd()
                    },
                    onZoom = { zoomChange ->
                        if (!currentPipEnabled && !currentIsAdsShowing && !currentIsLockScreen && currentIsFullScreen) {
                            val baseZoom = if (!currentHasActivatedFill) {
                                currentOnFilledChanged(true)
                                currentOnActivatedFillChanged(true)
                                maxOf(1f, currentFillScale, currentZoomAccumulator)
                            } else {
                                currentZoomAccumulator
                            }
                            currentOnZoomAccumulatorChanged((baseZoom * zoomChange).coerceIn(1f, 3f))
                        }
                    },
                    isZoomEnabled = currentIsFullScreen
                )
            }
    )
}

internal suspend fun PointerInputScope.detectPlayerGestures(
    onTap: () -> Unit,
    onDoubleTap: () -> Unit,
    onLongPressStart: () -> Unit,
    onLongPressEnd: () -> Unit,
    onZoom: (Float) -> Unit,
    isZoomEnabled: Boolean = true
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val downPosition = down.position
        val touchSlop = viewConfiguration.touchSlop
        val longPressTimeout = viewConfiguration.longPressTimeoutMillis

        var isMultiTouch = false
        var isLongPressTriggered = false
        var isMovedPastSlop = false
        var firstUp: PointerInputChange? = null

        try {
            withTimeout(longPressTimeout) {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    if (event.changes.size > 1) {
                        isMultiTouch = true
                        break
                    }
                    val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes[0]
                    if (change.changedToUp()) {
                        firstUp = change
                        break
                    }
                    val panDistance = hypot(change.position.x - downPosition.x, change.position.y - downPosition.y)
                    if (panDistance > touchSlop) {
                        isMovedPastSlop = true
                        break
                    }
                }
            }
        } catch (e: PointerEventTimeoutCancellationException) {
            isLongPressTriggered = true
        }

        // Multi-touch: Pinch to Zoom
        if (isMultiTouch) {
            if (isZoomEnabled) {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    val activeCount = event.changes.count { it.pressed }
                    if (activeCount < 2) {
                        break
                    }
                    val zoomChange = event.calculateZoom()
                    if (zoomChange != 1f) {
                        onZoom(zoomChange)
                    }
                    event.changes.fastForEach {
                        if (it.positionChanged()) it.consume()
                    }
                }
            }
            return@awaitEachGesture
        }

        // Long Press: 2X Speed
        if (isLongPressTriggered) {
            onLongPressStart()
            try {
                do {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    event.changes.fastForEach { it.consume() }
                } while (event.changes.fastAny { it.pressed })
            } finally {
                onLongPressEnd()
            }
            return@awaitEachGesture
        }

        if (isMovedPastSlop) {
            return@awaitEachGesture
        }

        val up = firstUp ?: return@awaitEachGesture
        up.consume()

        // Double tap detection
        val secondDown = withTimeoutOrNull(viewConfiguration.doubleTapTimeoutMillis) {
            val minUptime = up.uptimeMillis + viewConfiguration.doubleTapMinTimeMillis
            var change: PointerInputChange
            do {
                change = awaitFirstDown(requireUnconsumed = false)
            } while (change.uptimeMillis < minUptime)
            change
        }

        if (secondDown == null) {
            onTap()
        } else {
            var secondUp: PointerInputChange? = null
            try {
                withTimeout(longPressTimeout) {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        if (event.changes.size > 1) break
                        val change = event.changes.firstOrNull { it.id == secondDown.id } ?: event.changes[0]
                        if (change.changedToUp()) {
                            secondUp = change
                            break
                        }
                        val panDistance = hypot(change.position.x - secondDown.position.x, change.position.y - secondDown.position.y)
                        if (panDistance > touchSlop) {
                            break
                        }
                    }
                }
            } catch (e: PointerEventTimeoutCancellationException) {
                // Second tap held
            }

            val finalSecondUp = secondUp
            if (finalSecondUp != null) {
                finalSecondUp.consume()
                onDoubleTap()
            } else {
                onTap()
            }
        }
    }
}
