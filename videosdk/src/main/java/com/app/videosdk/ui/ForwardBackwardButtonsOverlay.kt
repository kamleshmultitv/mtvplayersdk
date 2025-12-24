package com.app.videosdk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay

@Composable
fun ForwardBackwardButtonsOverlay(
    exoPlayer: ExoPlayer,
    showRewindIcon: Boolean,
    showForwardIcon: Boolean,
    onRewindIconHide: () -> Unit,
    onForwardIconHide: () -> Unit,
    isControllerVisible: Boolean,
    backButtonFocusRequester: FocusRequester,
    playFocusRequester: FocusRequester,
    sliderFocusRequester: FocusRequester
) {
    LaunchedEffect(isControllerVisible) {
        if (isControllerVisible) {
            playFocusRequester.requestFocus()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }
        var isPlayFocused by remember { mutableStateOf(false) }

        LaunchedEffect(exoPlayer) {
            while (true) {
                if (isPlaying != exoPlayer.isPlaying) {
                    isPlaying = exoPlayer.isPlaying
                }
                delay(500)
            }
        }

        IconButton(
            modifier = Modifier
                .size(70.dp)
                .focusRequester(playFocusRequester)
                .onFocusChanged { isPlayFocused = it.isFocused }
                .background(
                    if (isPlayFocused) Color.White.copy(alpha = 0.2f) else Color.Transparent,
                    CircleShape
                )
                .border(
                    width = if (isPlayFocused) 2.dp else 0.dp,
                    color = if (isPlayFocused) Color.White else Color.Transparent,
                    shape = CircleShape
                )
                .focusable()
                .graphicsLayer {
                    alpha = if (isControllerVisible) 1f else 0.5f
                }
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        when (event.key) {
                            Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                isPlaying = exoPlayer.isPlaying
                                true
                            }
                            Key.DirectionUp -> {
                                backButtonFocusRequester.requestFocus()
                                true
                            }
                            Key.DirectionDown -> {
                                sliderFocusRequester.requestFocus()
                                true
                            }
                            // Seek if no buttons on the side
                            Key.DirectionLeft -> {
                                exoPlayer.seekTo((exoPlayer.currentPosition - 10_000).coerceAtLeast(0))
                                true
                            }
                            Key.DirectionRight -> {
                                exoPlayer.seekTo((exoPlayer.currentPosition + 10_000).coerceAtMost(exoPlayer.duration))
                                true
                            }
                            else -> false
                        }
                    } else false
                },
            onClick = {
                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                isPlaying = exoPlayer.isPlaying
            }
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Play Pause",
                tint = Color.White,
                modifier = Modifier.size(50.dp)
            )
        }
    }
}
