package com.app.videosdk.ui

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.app.videosdk.model.PlayerModel
import com.app.videosdk.utils.CastUtils
import kotlinx.coroutines.delay

@Composable
fun ForwardBackwardButtonsOverlay(
    playerModel: PlayerModel? = null,
    exoPlayer: ExoPlayer,
    context: Context,
    onRewindIconHide: () -> Unit,
    onForwardIconHide: () -> Unit,
    isControllerVisible: Boolean
) {
    val castUtils = remember { CastUtils(context, exoPlayer) }
    val isCasting = castUtils.isCasting()
    val customControls = playerModel?.customControls

    /* ▶️ Player state */
    var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    /* 🔁 Animation triggers (FIX) */
    var rewindAnimTrigger by remember { mutableStateOf(0) }
    var forwardAnimTrigger by remember { mutableStateOf(0) }

    /* 🔄 Animations */
    val rewindRotation by animateFloatAsState(
        targetValue = if (rewindAnimTrigger > 0) -90f else 0f,
        animationSpec = tween(300),
        label = "rewindRotation"
    )

    val forwardRotation by animateFloatAsState(
        targetValue = if (forwardAnimTrigger > 0) 90f else 0f,
        animationSpec = tween(300),
        label = "forwardRotation"
    )

    /* 🎮 UI */
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            /* ⏪ Rewind */
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    modifier = Modifier
                        .fillMaxWidth(),
                    onClick = {
                        rewindAnimTrigger++

                        val newPosition = maxOf(
                            (if (isCasting) castUtils.getCastPosition()
                            else exoPlayer.currentPosition) - 10_000,
                            0
                        )
                        if (isCasting) castUtils.seekOnCast(newPosition)
                        else exoPlayer.seekTo(newPosition)
                    }
                ) {

                    CustomIcon(
                        resId = customControls?.rewindIconRes,
                        defaultIcon = Icons.Default.Replay10,
                        contentDescription = "Rewind 10s",
                        modifier = Modifier
                            .size(48.dp)
                            .graphicsLayer(rotationZ = rewindRotation),
                        tint = customControls?.iconTintRes
                    )
                }
            }

            /* ▶️ Play / Pause */
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (isControllerVisible) {
                    IconButton(
                        onClick = {
                            if (isPlaying) {
                                if (isCasting) castUtils.pauseCasting()
                                else exoPlayer.pause()
                            } else {
                                if (isCasting) castUtils.playCasting()
                                else exoPlayer.play()
                            }
                        }
                    ) {

                        CustomIcon(
                            resId = if (isPlaying) customControls?.pauseIconRes else customControls?.playIconRes,
                            defaultIcon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            modifier = Modifier.size(72.dp),
                            tint = customControls?.iconTintRes
                        )
                    }
                }
            }

            /* ⏩ Forward */
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    modifier = Modifier
                        .fillMaxWidth(),
                    onClick = {
                        forwardAnimTrigger++

                        val duration =
                            if (isCasting) castUtils.getCastDuration()
                            else exoPlayer.duration

                        val newPosition = minOf(
                            (if (isCasting) castUtils.getCastPosition()
                            else exoPlayer.currentPosition) + 10_000,
                            duration
                        )
                        if (isCasting) castUtils.seekOnCast(newPosition)
                        else exoPlayer.seekTo(newPosition)
                    }
                ) {

                    CustomIcon(
                        resId = customControls?.forwardIconRes,
                        defaultIcon = Icons.Default.Forward10,
                        contentDescription = "Forward 10s",
                        modifier = Modifier
                            .size(48.dp)
                            .graphicsLayer(rotationZ = forwardRotation),
                        tint = customControls?.iconTintRes
                    )
                }
            }
        }
    }

    /* 🔁 Auto reset animations */
    LaunchedEffect(rewindAnimTrigger) {
        if (rewindAnimTrigger > 0) {
            delay(300)
            rewindAnimTrigger = 0
            onRewindIconHide()
        }
    }

    LaunchedEffect(forwardAnimTrigger) {
        if (forwardAnimTrigger > 0) {
            delay(300)
            forwardAnimTrigger = 0
            onForwardIconHide()
        }
    }
}