package com.app.videosdk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.exoplayer.ExoPlayer
import com.app.videosdk.model.PlayerModel
import kotlinx.coroutines.delay

@Composable
fun CustomPlayerController(
    playerModelList: List<PlayerModel>? = null,
    index: Int,
    isFullScreen: (Boolean) -> Unit,
    isCurrentlyFullScreen: Boolean,
    exoPlayer: ExoPlayer,
    modifier: Modifier,
    onShowControls: (Boolean) -> Unit,
    onSettingsButtonClick: (Boolean) -> Unit = {},
    isLoading: Boolean,
    onBackPressed: () -> Unit = {},
    playContent: (Int) -> Unit,
    showIntroOverlay: Boolean,
    onPlayClicked: () -> Unit,
    // Lifted focus requesters
    backButtonFocusRequester: FocusRequester,
    playFocusRequester: FocusRequester,
    sliderFocusRequester: FocusRequester
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val showControlsState = rememberUpdatedState(onShowControls)

    val introPlayFocusRequester = remember { FocusRequester() }

    var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }

    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPosition = exoPlayer.currentPosition
            duration = exoPlayer.duration.takeIf { it > 0 } ?: 0L
            isPlaying = exoPlayer.isPlaying
            delay(1000)
        }
    }

    // Auto-hide controls only when playing AND intro is gone
    LaunchedEffect(isPlaying, showIntroOverlay) {
        if (isPlaying && !showIntroOverlay) {
            delay(3000)
            showControlsState.value(false)
        } else if (showIntroOverlay) {
            showControlsState.value(true)
        }
    }

    LaunchedEffect(showIntroOverlay) {
        if (showIntroOverlay) {
            introPlayFocusRequester.requestFocus()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                exoPlayer.pause()
                showControlsState.value(false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(16.dp)
    ) {
        if (showIntroOverlay) {
            /* -------------------- INTRO SCREEN -------------------- */
            val model = playerModelList?.getOrNull(index)
            
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.6f)
                    .align(Alignment.CenterStart)
                    .padding(start = 32.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = model?.season_title.orEmpty(),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = model?.season_des.orEmpty(),
                    color = Color.LightGray,
                    fontSize = 16.sp,
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(24.dp))

                var isPlayBtnFocused by remember { mutableStateOf(false) }
                Button(
                    onClick = onPlayClicked,
                    modifier = Modifier
                        .focusRequester(introPlayFocusRequester)
                        .onFocusChanged { isPlayBtnFocused = it.isFocused }
                        .border(
                            width = if (isPlayBtnFocused) 2.dp else 0.dp,
                            color = if (isPlayBtnFocused) Color.White else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .height(56.dp)
                        .wrapContentWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlayBtnFocused) Color.White.copy(alpha = 0.3f) else Color.Red
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PLAY", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            /* -------------------- ACTUAL PLAYER CONTROLS -------------------- */
            
            TopBar(
                title = playerModelList?.getOrNull(index)?.title.orEmpty(),
                onBackPressed = onBackPressed,
                backButtonFocusRequester = backButtonFocusRequester,
                playFocusRequester = playFocusRequester,
                exoPlayer = exoPlayer
            )

            Box(modifier = Modifier.fillMaxSize()) {
                CenterControls(
                    isLoading = isLoading,
                    exoPlayer = exoPlayer,
                    onShowControls = showControlsState.value,
                    showForwardIcon = false,
                    showRewindIcon = false,
                    onForward = { },
                    onRewind = { },
                    onForwardHide = { },
                    onRewindHide = { },
                    isZoomed = false,
                    onZoomChange = { },
                    backButtonFocusRequester = backButtonFocusRequester,
                    playFocusRequester = playFocusRequester,
                    sliderFocusRequester = sliderFocusRequester
                )
            }

            BottomControls(
                modifier = Modifier.align(Alignment.BottomCenter),
                playerModelList = playerModelList,
                index = index,
                isFullScreen = isCurrentlyFullScreen,
                currentPosition = currentPosition,
                duration = duration,
                exoPlayer = exoPlayer,
                onSeek = {
                    showControlsState.value(true)
                    exoPlayer.seekTo(it)
                },
                onNext = playContent,
                onSettingsClick = {
                    exoPlayer.pause()
                    showControlsState.value(true)
                    onSettingsButtonClick(true)
                },
                sliderFocusRequester = sliderFocusRequester,
                playFocusRequester = playFocusRequester
            )
        }
    }
}
