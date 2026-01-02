package com.app.videosdk.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import com.app.videosdk.model.PlayerModel

@Composable
fun BottomControls(
    modifier: Modifier = Modifier,
    playerModelList: List<PlayerModel>?,
    index: Int,
    isFullScreen: Boolean,
    currentPosition: Long,
    duration: Long,
    exoPlayer: ExoPlayer,
    onSeek: (Long) -> Unit,
    onNext: (Int) -> Unit,
    cuePoints: List<CuePoint> = emptyList() // ✅ keep ONLY this
) {
    val model = playerModelList?.getOrNull(index)
    val isLive = model?.isLive ?: false

    Column(modifier = modifier.fillMaxWidth()) {

        /* ---------- SEEK BAR ---------- */

        CustomSlider(
            currentPosition = currentPosition,
            duration = duration,
            cuePoints = cuePoints,   // ✅ markers only
            onSeek = onSeek,
            showControls = {},
            isLive = isLive,
            exoPlayer = exoPlayer
        )

        /* ---------- BOTTOM ACTION BAR ---------- */

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            if (!isLive) {

                /* ---------- START : NEXT ---------- */

                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (isFullScreen && playerModelList != null && playerModelList.size > 1) {
                        val isLastItem = index >= playerModelList.lastIndex

                        Row(
                            modifier = Modifier
                                .clickable(enabled = !isLastItem) {
                                    if (!isLastItem) onNext(index + 1)
                                }
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next Episode",
                                tint = if (isLastItem) Color.Gray else Color.White
                            )
                            Text(
                                text = "Next Ep.",
                                color = if (isLastItem) Color.Gray else Color.White
                            )
                        }
                    }
                }

                /* ---------- CENTER : EPISODES ---------- */

                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (isFullScreen && playerModelList != null && playerModelList.size > 1) {
                        SeasonSelector(
                            playerModelList = playerModelList,
                            exoPlayer = exoPlayer,
                            onShowControls = {},
                            pausePlayer = {},
                            playContent = onNext
                        )
                    }
                }

            } else {
                Spacer(modifier = Modifier.weight(2f))
            }

            Box(modifier = Modifier.weight(1f))
        }
    }
}
