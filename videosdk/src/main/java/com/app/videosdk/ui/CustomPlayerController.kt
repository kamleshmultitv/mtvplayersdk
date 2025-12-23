package com.app.videosdk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
    playContent: (Int) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val showControlsState = rememberUpdatedState(onShowControls)

    // Focus requesters for TV navigation
    val backButtonFocusRequester = remember { FocusRequester() }
    val playFocusRequester = remember { FocusRequester() }
    val sliderFocusRequester = remember { FocusRequester() }

    var isZoomed by remember { mutableStateOf(false) }
    var showForwardIcon by remember { mutableStateOf(false) }
    var showRewindIcon by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }

    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }

    /* ---------- PLAYBACK OBSERVER ---------- */

    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPosition =
                exoPlayer.currentPosition

            duration =
                exoPlayer.duration.takeIf { it > 0 } ?: 0L

            isPlaying = exoPlayer.isPlaying
            delay(1000)
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            delay(3000)
            showControlsState.value(false)
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

    /* -------------------- UI -------------------- */

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .onKeyEvent {
                // Show controls on any key press if they are hidden
                showControlsState.value(true)
                false
            }
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = if (isCurrentlyFullScreen) 8.dp else 4.dp
            )
    ) {

        /* ---------- TOP BAR ---------- */

        TopBar(
            title = playerModelList?.getOrNull(index)?.title.orEmpty(),
            onBackPressed = onBackPressed,
            backButtonFocusRequester = backButtonFocusRequester,
            playFocusRequester = playFocusRequester
        )

        /* ---------- CENTER AREA ---------- */

        Box(modifier = Modifier.fillMaxSize()) {
            CenterControls(
                isLoading = isLoading,
                exoPlayer = exoPlayer,
                onShowControls = showControlsState.value,
                showForwardIcon = showForwardIcon,
                showRewindIcon = showRewindIcon,
                onForward = { showForwardIcon = true },
                onRewind = { showRewindIcon = true },
                onForwardHide = { showForwardIcon = false },
                onRewindHide = { showForwardIcon = false },
                isZoomed = isZoomed,
                onZoomChange = { isZoomed = it },
                backButtonFocusRequester = backButtonFocusRequester,
                playFocusRequester = playFocusRequester,
                sliderFocusRequester = sliderFocusRequester
            )
        }

        /* ---------- BOTTOM CONTROLS ---------- */

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
