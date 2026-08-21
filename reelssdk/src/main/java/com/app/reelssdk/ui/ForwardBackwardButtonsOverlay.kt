package com.app.reelssdk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.app.reelssdk.model.ReelsPlayerControlsConfig
import com.app.reelssdk.model.ReelsPlayerCustomControls
import com.app.reelssdk.model.ReelsPlayerModel

@Composable
fun ForwardBackwardButtonsOverlay(
    playerModel: ReelsPlayerModel? = null,
    exoPlayer: ExoPlayer,
    isControllerVisible: Boolean,
    controlsConfig: ReelsPlayerControlsConfig = ReelsPlayerControlsConfig(),
    onPlayPauseClick: (Boolean) -> Unit = {}
) {
    val customControls = playerModel?.customControls
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

    ForwardBackwardButtonsOverlayUi(
        isPlaying = isPlaying,
        customControls = customControls,
        controlsConfig = controlsConfig,
        isControllerVisible = isControllerVisible,
        onPlayPause = {
            val willPlay = !isPlaying

            if (isPlaying) {
                exoPlayer.pause()
            } else {
                exoPlayer.play()
            }

            onPlayPauseClick(willPlay)
        }
    )
}

@Composable
private fun ForwardBackwardButtonsOverlayUi(
    isPlaying: Boolean,
    customControls: ReelsPlayerCustomControls?,
    controlsConfig: ReelsPlayerControlsConfig,
    isControllerVisible: Boolean,
    onPlayPause: () -> Unit
) {
    val playPauseInteractionSource = remember { MutableInteractionSource() }
    val playPauseButtonSize = 72.dp
    val playPauseIconSize = 48.dp

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (isControllerVisible && ((isPlaying && controlsConfig.pause) || (!isPlaying && controlsConfig.play))) {
            Box(
                modifier = Modifier
                    .size(playPauseButtonSize)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.2f))
                    .clickable(
                        interactionSource = playPauseInteractionSource,
                        indication = null,
                        onClick = onPlayPause
                    ),
                contentAlignment = Alignment.Center
            ) {
                CustomIcon(
                    resId = if (isPlaying) {
                        customControls?.pauseIconRes
                    } else {
                        customControls?.playIconRes
                    },
                    defaultIcon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    modifier = Modifier.size(playPauseIconSize),
                    tint = customControls?.iconTintRes
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ForwardBackwardButtonsOverlayPreview() {
    var isPlaying by remember { mutableStateOf(true) }
    val customControls = ReelsPlayerCustomControls(iconTintRes = null)

    ForwardBackwardButtonsOverlayUi(
        isPlaying = isPlaying,
        customControls = customControls,
        controlsConfig = ReelsPlayerControlsConfig(),
        isControllerVisible = true,
        onPlayPause = { isPlaying = !isPlaying }
    )
}
