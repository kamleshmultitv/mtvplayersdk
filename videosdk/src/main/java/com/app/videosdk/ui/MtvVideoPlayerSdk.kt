package com.app.videosdk.ui

import android.util.Log
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.app.videosdk.model.PlayerModel
import com.app.videosdk.listener.PipListener
import com.app.videosdk.utils.CastUtils
import com.app.videosdk.utils.PlayerUtils.createExoPlayer
import com.app.videosdk.utils.PlayerUtils.getMimeTypeFromExtension

@OptIn(UnstableApi::class)
@Composable
fun MtvVideoPlayerSdk(
    contentList: List<PlayerModel>? = null,
    index: Int,
    pipListener: PipListener,
    onPlayerBack: (Boolean) -> Unit,
    setFullScreen: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val selectedIndex = remember { mutableIntStateOf(index) }
    var isFullScreen by remember { mutableStateOf(false) }
    var isControllerVisible by remember { mutableStateOf(false) }
    var pipEnabled by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var keepScreenOn by remember { mutableStateOf(false) }
    var isSettingsClick by remember { mutableStateOf(false) }
    var showForwardIcon by remember { mutableStateOf(false) }
    var showRewindIcon by remember { mutableStateOf(false) }

    // Update selectedIndex when index changes
    LaunchedEffect(index) {
        isLoading = true
        selectedIndex.intValue = index
    }

    val playerModel by remember(contentList, selectedIndex.intValue) {
        derivedStateOf { contentList?.getOrNull(selectedIndex.intValue) }
    }

    val subtitleUri by remember(playerModel) {
        derivedStateOf {
            if (getMimeTypeFromExtension(playerModel?.hlsUrl.toString())) "" else playerModel?.srt
        }
    }

    // Initialize ExoPlayer and remember it only when hlsUrl changes
    val exoPlayer = remember(playerModel?.mpdUrl) {
        createExoPlayer(
            context,
            playerModel?.mpdUrl.orEmpty(),
            playerModel?.drmToken,
            subtitleUri.orEmpty()
        )
    }

    val castUtils = remember { CastUtils(context, exoPlayer) }
    val isCasting by remember { derivedStateOf { castUtils.isCasting() } }

    DisposableEffect(exoPlayer) {
        val playerListener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                Log.d("ExoPlayer", "Playback State Changed: $state")
                isLoading = state == Player.STATE_BUFFERING

                when (state) {
                    Player.STATE_READY -> {
                        if (!isCasting && exoPlayer.playWhenReady) {
                            isLoading = false
                            exoPlayer.play()
                        }
                    }

                    Player.STATE_IDLE -> {
                        isLoading = false
                        keepScreenOn = false
                    }

                    Player.STATE_ENDED -> {
                        Log.d("ExoPlayer", "Playback has ended.")
                        isLoading = false
                        keepScreenOn = false
                        val isLastItem = selectedIndex.intValue >= contentList?.size!! - 1
                        if (!isLastItem) {
                            selectedIndex.intValue += 1
                        }
                    }

                    Player.STATE_BUFFERING -> {
                        Log.d("ExoPlayer", "Buffering...")
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("ExoPlayer", "Error: ${error.message}")
            }

            override fun onEvents(player: Player, events: Player.Events) {
                if (events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED) && player.playbackState == Player.STATE_ENDED) {
                    Log.d("ExoPlayer", "Playback Ended (onEvents).")
                    isLoading = false
                    keepScreenOn = false
                }
            }
        }

        exoPlayer.addListener(playerListener)
        exoPlayer.repeatMode = Player.REPEAT_MODE_OFF // Ensure playback stops at the end

        onDispose {
            exoPlayer.removeListener(playerListener)
            if (exoPlayer.playbackState != Player.STATE_ENDED) {
                exoPlayer.release()
            }
        }
    }


    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                super.onStop(owner)
                exoPlayer.pause() // Pause player when app goes to background

            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Handle Cast Session Setup
    LaunchedEffect(playerModel?.hlsUrl) {
        playerModel?.let { castUtils.setupCastSession(it) }
    }

    // Ensure screen stays on while playing
    keepScreenOn = exoPlayer.playWhenReady && exoPlayer.playbackState == Player.STATE_READY

    ScreenRotationExample(setOrientation = {
        isFullScreen = it
        setFullScreen(it)
    })
    FullScreenHandler(isFullScreen)
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val aspectRatioHeight = screenWidth * 9 / 16

    var isZoomed by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isFullScreen) Modifier.fillMaxSize() else Modifier.height(aspectRatioHeight))
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    // Detect pinch zoom gesture by tracking zoom factor
                    if (zoom > 1f && !isZoomed) {
                        isZoomed = true // Zoom in
                    } else if (zoom < 1f && isZoomed) {
                        isZoomed = false // Zoom out
                    }
                }
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT // Start in normal mode
                    player = exoPlayer
                    useController = false // Disable default controller
                    layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            isControllerVisible = !isControllerVisible // Toggle UI visibility
                        },
                        onDoubleTap = { offset ->
                            val isLeftSide = offset.x < size.width / 2
                            val currentPosition =
                                if (isCasting) castUtils.getCastPosition() else exoPlayer.currentPosition
                            val newPosition =
                                maxOf(currentPosition + if (isLeftSide) -10_000 else 10_000, 0)

                            // Apply seek for both ExoPlayer and Casting
                            if (isCasting) {
                                castUtils.seekOnCast(newPosition)
                            } else {
                                exoPlayer.seekTo(newPosition)
                            }

                            // Show corresponding feedback icon
                            if (isLeftSide) {
                                showRewindIcon = true
                            } else {
                                showForwardIcon = true
                            }
                        }
                    )
                },
            update = { playerView ->
                playerView.player = exoPlayer
                playerView.keepScreenOn = keepScreenOn
                playerView.resizeMode = if (isZoomed) {
                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM // Zoom in
                } else {
                    AspectRatioFrameLayout.RESIZE_MODE_FIT // Normal fit
                }
            }
        )

        // custom controller
        if (!pipEnabled || isControllerVisible) {
            AnimatedVisibility(
                visible = isControllerVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                CustomPlayerController(
                    playerModelList = contentList,
                    index = selectedIndex.intValue,
                    pipListener,
                    isFullScreen = {
                        isFullScreen = it
                        setFullScreen(it)
                    },
                    isCurrentlyFullScreen = isFullScreen,
                    exoPlayer = exoPlayer,
                    modifier = Modifier.fillMaxSize(),
                    onShowControls = {
                        isControllerVisible = it
                    },
                    isPipEnabled = { isPipEnabled ->
                        if (isPipEnabled) {
                            pipEnabled = true
                        }
                    },
                    onSettingsButtonClick = {
                        isSettingsClick = it
                    },
                    isLoading,
                    onBackPressed = {
                        if (isFullScreen) {
                            isFullScreen = false
                            setFullScreen(false)
                        } else {
                            setFullScreen(true)
                            onPlayerBack(true)
                        }
                    },
                    playContent = {
                        selectedIndex.intValue = it
                    }
                )
            }
        }

        ForwardBackwardButtonsOverlay(
            exoPlayer = exoPlayer,
            context = context,
            showRewindIcon = showRewindIcon,
            showForwardIcon = showForwardIcon,
            onRewindIconHide = { showRewindIcon = false },
            onForwardIconHide = { showForwardIcon = false },
            false
        )

        if (!isControllerVisible && isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(55.dp),
                color = Color.White
            )
        }

        if (isSettingsClick) {
            SelectorHeader(exoPlayer = exoPlayer, closeOptionCard = {
                isSettingsClick = it
            })
        }
    }
}