package com.app.videosdk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
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
    onSettingsClick: () -> Unit,
    sliderFocusRequester: FocusRequester,
    playFocusRequester: FocusRequester
) {
    val spriteUrl = remember(playerModelList, index) {
        playerModelList?.getOrNull(index)?.spriteUrl
    }

    val nextEpFocusRequester = remember { FocusRequester() }
    val seasonSelectorFocusRequester = remember { FocusRequester() }
    val settingsFocusRequester = remember { FocusRequester() }

    Column(modifier = modifier.fillMaxWidth()) {

        /* ---------- SEEK BAR ---------- */

        CustomSlider(
            modifier = Modifier.focusRequester(sliderFocusRequester),
            spriteUrl = spriteUrl,
            currentPosition = currentPosition,
            duration = duration,
            onSeek = onSeek,
            showControls = {},
            onDownPressed = {
                nextEpFocusRequester.requestFocus()
            },
            onUpPressed = {
                playFocusRequester.requestFocus()
            }
        )

        /* ---------- BOTTOM ACTION BAR ---------- */

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            /* ---------- START : NEXT ---------- */

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (isFullScreen && playerModelList != null && playerModelList.size > 1) {
                    val isLastItem = index >= playerModelList.lastIndex
                    var isNextFocused by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier
                            .focusRequester(nextEpFocusRequester)
                            .onFocusChanged { isNextFocused = it.isFocused }
                            .background(
                                if (isNextFocused) Color.White.copy(alpha = 0.2f) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                if (isNextFocused) 2.dp else 0.dp,
                                if (isNextFocused) Color.White else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable(enabled = !isLastItem) {
                                if (!isLastItem) onNext(index + 1)
                            }
                            .onKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown) {
                                    when (event.key) {
                                        Key.DirectionRight -> {
                                            seasonSelectorFocusRequester.requestFocus()
                                            true
                                        }
                                        Key.DirectionLeft -> {
                                            exoPlayer.seekTo((exoPlayer.currentPosition - 10_000).coerceAtLeast(0))
                                            true
                                        }
                                        Key.DirectionUp -> {
                                            sliderFocusRequester.requestFocus()
                                            true
                                        }
                                        else -> false
                                    }
                                } else false
                            }
                            .focusable()
                            .padding(8.dp),
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
                        playContent = onNext,
                        focusRequester = seasonSelectorFocusRequester,
                        modifier = Modifier.onKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown) {
                                when (event.key) {
                                    Key.DirectionRight -> {
                                        settingsFocusRequester.requestFocus()
                                        true
                                    }
                                    Key.DirectionLeft -> {
                                        nextEpFocusRequester.requestFocus()
                                        true
                                    }
                                    Key.DirectionUp -> {
                                        sliderFocusRequester.requestFocus()
                                        true
                                    }
                                    else -> false
                                }
                            } else false
                        }
                    )
                }
            }

            /* ---------- END : SETTINGS ---------- */

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                var isSettingsFocused by remember { mutableStateOf(false) }
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .focusRequester(settingsFocusRequester)
                        .onFocusChanged { isSettingsFocused = it.isFocused }
                        .background(
                            if (isSettingsFocused) Color.White.copy(alpha = 0.2f) else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            if (isSettingsFocused) 2.dp else 0.dp,
                            if (isSettingsFocused) Color.White else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .onKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown) {
                                when (event.key) {
                                    Key.DirectionRight -> {
                                        exoPlayer.seekTo((exoPlayer.currentPosition + 10_000).coerceAtMost(exoPlayer.duration))
                                        true
                                    }
                                    Key.DirectionLeft -> {
                                        seasonSelectorFocusRequester.requestFocus()
                                        true
                                    }
                                    Key.DirectionUp -> {
                                        sliderFocusRequester.requestFocus()
                                        true
                                    }
                                    else -> false
                                }
                            } else false
                        }
                        .focusable()
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
