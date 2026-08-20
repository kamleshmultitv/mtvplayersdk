package com.app.videosdk.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import com.app.videosdk.model.CuePoint
import com.app.videosdk.model.PlayerControlsConfig
import com.app.videosdk.model.PlayerModel
import com.app.videosdk.ui.sprite.SpriteThumbnail
import com.app.videosdk.ui.sprite.SpriteUtils

@Composable
fun BottomControls(
    modifier: Modifier = Modifier,
    playerModelList: List<PlayerModel>?,
    index: Int,
    currentPosition: Long,
    duration: Long,
    exoPlayer: ExoPlayer,
    onSeek: (Long) -> Unit,
    cuePoints: List<CuePoint> = emptyList(),
    onDragStateChange: (Boolean) -> Unit = {},
    controlsConfig: PlayerControlsConfig = PlayerControlsConfig()
) {
    val model = playerModelList?.getOrNull(index)
    val isLive = model?.isLive ?: false

    var isDragging by remember { mutableStateOf(false) }
    var previewMs by remember { mutableLongStateOf(0L) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (controlsConfig.seekbar && !isLive && isDragging && duration > 0) {
            val posToShow =
                if (previewMs > 0) previewMs else exoPlayer.currentPosition.coerceAtLeast(0L)

            val width = 120.dp
            val height = width * (16f / 9f)

            val spriteUrl =
                model?.spriteUrl?.takeIf { it.isNotBlank() }
                    ?: SpriteUtils.spriteUrlForVideo(model?.spriteUrl.orEmpty())

            SpriteThumbnail(
                spriteUrl = spriteUrl,
                positionMs = posToShow,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .size(width, height)
            )
        }

        if (controlsConfig.seekbar) {
            CustomSlider(
                playerModel = model,
                currentPosition = currentPosition,
                duration = duration,
                cuePoints = cuePoints,
                isLive = isLive,
                exoPlayer = exoPlayer,
                showControls = {},
                onDragStateChange = { dragging ->
                    isDragging = dragging
                    onDragStateChange(dragging)

                    if (!dragging && previewMs > 0) {
                        onSeek(previewMs)
                    }
                },
                onPreviewChange = { targetMs ->
                    previewMs = targetMs
                },
                onSeek = onSeek
            )
        }
    }
}
