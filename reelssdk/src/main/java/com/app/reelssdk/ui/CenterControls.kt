package com.app.reelssdk.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import com.app.reelssdk.model.ReelsPlayerControlsConfig
import com.app.reelssdk.model.ReelsPlayerModel

@Composable
fun CenterControls(
    playerModel: ReelsPlayerModel? = null,
    isLoading: Boolean,
    exoPlayer: ExoPlayer,
    verticalOffset: Dp = 0.dp,
    onPlayPauseClick: (Boolean) -> Unit = {},
    controlsConfig: ReelsPlayerControlsConfig = ReelsPlayerControlsConfig()
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset(y = verticalOffset),
        contentAlignment = Alignment.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp)
        ) {
            ForwardBackwardButtonsOverlay(
                playerModel = playerModel,
                exoPlayer = exoPlayer,
                isControllerVisible = true,
                controlsConfig = controlsConfig,
                onPlayPauseClick = onPlayPauseClick
            )

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(55.dp),
                    color = Color.White
                )
            }
        }
    }
}
