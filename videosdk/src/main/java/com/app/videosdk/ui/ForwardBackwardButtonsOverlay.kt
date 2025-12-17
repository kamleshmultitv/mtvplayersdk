package com.app.videosdk.ui

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import com.app.videosdk.utils.CastUtils
import kotlinx.coroutines.delay

@Composable
fun ForwardBackwardButtonsOverlay(
    exoPlayer: ExoPlayer,
    context: Context,
    showRewindIcon: Boolean,
    showForwardIcon: Boolean,
    onRewindIconHide: () -> Unit,
    onForwardIconHide: () -> Unit,
    isControllerVisible: Boolean
) {
    val castUtils = remember { CastUtils(context, exoPlayer) }
    val isCasting = castUtils.isCasting()

    val rewindRotation by animateFloatAsState(
        targetValue = if (showRewindIcon) -90f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "Rewind Rotation"
    )

    val forwardRotation by animateFloatAsState(
        targetValue = if (showForwardIcon) 90f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "Forward Rotation"
    )

    val rewindAlpha by animateFloatAsState(
        targetValue = if (showRewindIcon || isControllerVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "Rewind Alpha"
    )

    val forwardAlpha by animateFloatAsState(
        targetValue = if (showForwardIcon || isControllerVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "Forward Alpha"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Rewind Icon
            IconButton(
                modifier = Modifier.size(60.dp),
                onClick = {
                    val newPosition = maxOf(
                        (if (isCasting) castUtils.getCastPosition() else exoPlayer.currentPosition) - 10_000,
                        0
                    )
                    if (isCasting) castUtils.seekOnCast(newPosition) else exoPlayer.seekTo(
                        newPosition
                    )
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Replay10,
                    contentDescription = "Rewind 10s",
                    tint = Color.White.copy(alpha = rewindAlpha),
                    modifier = Modifier
                        .size(48.dp)
                        .graphicsLayer(rotationZ = rewindRotation)
                )
            }

            // Play/Pause Button
            if (isControllerVisible) {
                var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }
                IconButton(
                    modifier = Modifier.size(70.dp),
                    onClick = {
                        if (isPlaying) {
                            if (isCasting) {
                                castUtils.pauseCasting() // Pause casting
                            } else {
                                exoPlayer.pause() // Pause local playback
                            }
                        } else {
                            if (isCasting) {
                                castUtils.playCasting() // Play casting
                            } else {
                                exoPlayer.play() // Play local playback
                            }
                        }
                        isPlaying = !isPlaying
                    }
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(50.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(70.dp))
            }

            // Forward Icon
            IconButton(
                modifier = Modifier.size(60.dp),
                onClick = {
                    val duration =
                        if (isCasting) castUtils.getCastDuration() else exoPlayer.duration
                    val newPosition = minOf(
                        (if (isCasting) castUtils.getCastPosition() else exoPlayer.currentPosition) + 10_000,
                        duration
                    )
                    if (isCasting) castUtils.seekOnCast(newPosition) else exoPlayer.seekTo(
                        newPosition
                    )
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Forward10,
                    contentDescription = "Forward 10s",
                    tint = Color.White.copy(alpha = forwardAlpha),
                    modifier = Modifier
                        .size(48.dp)
                        .graphicsLayer(rotationZ = forwardRotation)
                )
            }
        }
    }

    // Hide icons after a delay
    LaunchedEffect(showRewindIcon, showForwardIcon) {
        if (showRewindIcon || showForwardIcon) {
            delay(500)
            if (showRewindIcon) onRewindIconHide()
            if (showForwardIcon) onForwardIconHide()
        }
    }
}