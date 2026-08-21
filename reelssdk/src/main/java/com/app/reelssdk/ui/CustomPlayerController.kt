package com.app.reelssdk.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.app.reelssdk.model.CuePoint
import com.app.reelssdk.model.ReelsPlayerControlsConfig
import com.app.reelssdk.model.ReelsPlayerModel
import com.app.reelssdk.ui.reels.ReelsFooter
import com.app.reelssdk.ui.reels.ReelsShareSheet
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun CustomPlayerController(
    playerModelList: List<ReelsPlayerModel>? = null,
    index: Int,
    totalDuration: Long,
    isFullScreen: (Boolean) -> Unit,
    isCurrentlyFullScreen: Boolean,
    exoPlayer: ExoPlayer,
    modifier: Modifier,
    isControlsVisible: Boolean,
    onShowControls: (Boolean) -> Unit,
    isLoading: Boolean,
    onBackPressed: () -> Unit = {},
    cuePoints: List<CuePoint> = emptyList(),
    controlsConfig: ReelsPlayerControlsConfig = ReelsPlayerControlsConfig(),
    onSeekbarDraggingChanged: (Boolean) -> Unit = {},
    onSettingsClick: (Boolean) -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val showControlsState = rememberUpdatedState(onShowControls)
    val fullScreenState = rememberUpdatedState(isFullScreen)
    val seekbarDraggingState = rememberUpdatedState(onSeekbarDraggingChanged)

    var isPlaying by remember(exoPlayer) { mutableStateOf(exoPlayer.isPlaying) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    val duration by rememberUpdatedState(if (totalDuration > 0) totalDuration else 0L)
    var showReelsShareSheet by remember(index) { mutableStateOf(false) }
    var isDraggingSeekbar by remember { mutableStateOf(false) }
    var onSeek by remember { mutableStateOf(false) }
    var playResumeHideRequest by remember { mutableIntStateOf(0) }
    val playerModel = playerModelList?.getOrNull(index)

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isPlaying = exoPlayer.isPlaying
            }
        }

        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    DisposableEffect(index) {
        onDispose {
            seekbarDraggingState.value(false)
        }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPosition = exoPlayer.currentPosition
            delay(1000.milliseconds)
        }
    }

    LaunchedEffect(
        isControlsVisible,
        isPlaying,
        showReelsShareSheet,
        isDraggingSeekbar,
        onSeek,
        playResumeHideRequest,
        index
    ) {
        if (!isControlsVisible) {
            return@LaunchedEffect
        }

        if (showReelsShareSheet || isDraggingSeekbar || onSeek) {
            showControlsState.value(true)
            return@LaunchedEffect
        }

        if (!isPlaying) {
            showControlsState.value(true)
            return@LaunchedEffect
        }

        delay(3000.milliseconds)

        if (exoPlayer.isPlaying && !isDraggingSeekbar && !showReelsShareSheet) {
            showControlsState.value(false)
        }
    }

    LaunchedEffect(onSeek) {
        if (onSeek) {
            delay(500.milliseconds)
            onSeek = false
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

    Box(modifier = modifier.fillMaxSize()) {
        if (isControlsVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to Color.Black.copy(alpha = 0.86f),
                                0.18f to Color.Black.copy(alpha = 0.52f),
                                0.45f to Color.Black.copy(alpha = 0.34f),
                                0.62f to Color.Black.copy(alpha = 0.34f),
                                0.84f to Color.Black.copy(alpha = 0.62f),
                                1.00f to Color.Black.copy(alpha = 0.90f)
                            )
                        )
                    )
            )
        }

        AnimatedVisibility(
            visible = isControlsVisible,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(2f),
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(
                    durationMillis = 700,
                    easing = LinearOutSlowInEasing
                )
            ) + fadeIn(
                animationSpec = tween(
                    durationMillis = 600,
                    delayMillis = 100,
                    easing = LinearEasing
                )
            ),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(
                    durationMillis = 600,
                    easing = FastOutLinearInEasing
                )
            ) + fadeOut(
                animationSpec = tween(
                    durationMillis = 400,
                    easing = LinearEasing
                )
            )
        ) {
            TopBar(
                playerModel = playerModel,
                isFullScreen = isCurrentlyFullScreen,
                showContentTitle = true,
                onBackPressed = onBackPressed
            )
        }

        CenterControls(
            playerModel = playerModel,
            isLoading = isLoading,
            exoPlayer = exoPlayer,
            onPlayPauseClick = { willPlay ->
                if (willPlay) {
                    isPlaying = true
                    playResumeHideRequest++
                } else {
                    isPlaying = false
                    showControlsState.value(true)
                }
            },
            controlsConfig = controlsConfig
        )

        AnimatedVisibility(
            visible = isControlsVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(
                    durationMillis = 700,
                    easing = LinearOutSlowInEasing
                )
            ) + fadeIn(
                animationSpec = tween(
                    durationMillis = 600,
                    delayMillis = 100,
                    easing = LinearEasing
                )
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(
                    durationMillis = 600,
                    easing = FastOutLinearInEasing
                )
            ) + fadeOut(
                animationSpec = tween(
                    durationMillis = 400,
                    easing = LinearEasing
                )
            )
        ) {
            BottomControls(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding(),
                playerModelList = playerModelList,
                index = index,
                currentPosition = currentPosition,
                duration = duration,
                exoPlayer = exoPlayer,
                onSeek = {
                    onSeek = true
                    showControlsState.value(true)
                    exoPlayer.seekTo(it)
                },
                cuePoints = cuePoints,
                onDragStateChange = { dragging ->
                    isDraggingSeekbar = dragging
                    seekbarDraggingState.value(dragging)
                    if (dragging) {
                        showControlsState.value(true)
                    }
                },
                controlsConfig = controlsConfig
            )
        }

        if (isControlsVisible) {
            ReelsFooter(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(3f),
                customControls = playerModel?.customControls,
                controlsConfig = controlsConfig,
                onLikeClick = {},
                onShareClick = {
                    showReelsShareSheet = true
                },
                onSettingsClick = {
                    onSettingsClick(true)
                },
                isFullScreen = isCurrentlyFullScreen,
                onFullScreenClick = {
                    fullScreenState.value(!isCurrentlyFullScreen)
                }
            )
        }

        if (showReelsShareSheet) {
            ReelsShareSheet(
                playerModel = playerModel,
                onDismiss = {
                    showReelsShareSheet = false
                }
            )
        }
    }
}
