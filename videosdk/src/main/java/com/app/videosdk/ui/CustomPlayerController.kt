package com.app.videosdk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.exoplayer.ExoPlayer
import com.app.videosdk.listener.PipListener
import com.app.videosdk.model.PlayerModel
import com.app.videosdk.utils.CastUtils
import kotlinx.coroutines.delay

@Composable
fun CustomPlayerController(
    playerModelList: List<PlayerModel>? = null,
    index: Int,
    pipListener: PipListener? = null,
    isFullScreen: (Boolean) -> Unit,
    isCurrentlyFullScreen: Boolean,
    exoPlayer: ExoPlayer,
    modifier: Modifier,
    onShowControls: (Boolean) -> Unit,
    isPipEnabled: (Boolean) -> Unit = {},
    onSettingsButtonClick: (Boolean) -> Unit = {},
    isLoading: Boolean,
    onBackPressed: () -> Unit = {},
    cuePoints: List<CuePoint>,
    playContent: (Int) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val showControlsState = rememberUpdatedState(onShowControls)
    val fullScreenState = rememberUpdatedState(isFullScreen)

    val castUtils = remember(context, exoPlayer) {
        CastUtils(context, exoPlayer)
    }
    val isCasting by remember { derivedStateOf { castUtils.isCasting() } }

    var isZoomed by remember { mutableStateOf(false) }
    var showForwardIcon by remember { mutableStateOf(false) }
    var showRewindIcon by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }

    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }

    /* ---------- PLAYBACK OBSERVER ---------- */

    LaunchedEffect(exoPlayer, isCasting) {
        while (true) {
            currentPosition =
                if (isCasting) castUtils.getCastPosition()
                else exoPlayer.currentPosition

            duration =
                if (isCasting) castUtils.getCastDuration()
                else exoPlayer.duration.takeIf { it > 0 } ?: 0L

            isPlaying = exoPlayer.isPlaying
            delay(1000)
        }
    }

    LaunchedEffect(isPlaying) {
        delay(3000)
        showControlsState.value(!isPlaying)
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
            isFullScreen = isCurrentlyFullScreen,
            context = context,
            castUtils = castUtils,
            pipListener = pipListener,
            isPipEnabled = isPipEnabled,
            onBackPressed = onBackPressed,
            onSettingsClick = {
                exoPlayer.pause()
                showControlsState.value(true)
                onSettingsButtonClick(true)
            },
            onFullScreenToggle = {
                fullScreenState.value(!isCurrentlyFullScreen)
            }
        )

        /* ---------- CENTER AREA ---------- */

        if (isCurrentlyFullScreen) {

            Row(modifier = Modifier.fillMaxSize()) {

                /* ---- BRIGHTNESS (LEFT) ---- */
                Box(
                    modifier = Modifier
                        .weight(0.1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    CustomBrightnessController(
                        onShowControls = showControlsState.value
                    )
                }

                /* ---- CENTER CONTROLS ---- */
                Box(modifier = Modifier.weight(0.8f)) {
                    CenterControls(
                        isLoading = isLoading,
                        exoPlayer = exoPlayer,
                        castUtils = castUtils,
                        isCasting = isCasting,
                        onShowControls = showControlsState.value,
                        showForwardIcon = showForwardIcon,
                        showRewindIcon = showRewindIcon,
                        onForward = { showForwardIcon = true },
                        onRewind = { showRewindIcon = true },
                        onForwardHide = { showForwardIcon = false },
                        onRewindHide = { showForwardIcon = false },
                        isZoomed = isZoomed,
                        onZoomChange = { isZoomed = it }
                    )
                }

                /* ---- VOLUME (RIGHT) ---- */
                Box(
                    modifier = Modifier
                        .weight(0.1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    CustomVolumeController(
                        exoPlayer = exoPlayer,
                        onShowControls = showControlsState.value
                    )
                }
            }

        } else {

            /* ---- NON FULLSCREEN CENTER ---- */
            CenterControls(
                isLoading = isLoading,
                exoPlayer = exoPlayer,
                castUtils = castUtils,
                isCasting = isCasting,
                onShowControls = showControlsState.value,
                showForwardIcon = showForwardIcon,
                showRewindIcon = showRewindIcon,
                onForward = { showForwardIcon = true },
                onRewind = { showRewindIcon = true },
                onForwardHide = { showForwardIcon = false },
                onRewindHide = { showForwardIcon = false },
                isZoomed = isZoomed,
                onZoomChange = { isZoomed = it }
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
                if (isCasting) castUtils.seekOnCast(it)
                else exoPlayer.seekTo(it)
            },
            onNext = playContent,
            cuePoints = cuePoints
        )

    }
}