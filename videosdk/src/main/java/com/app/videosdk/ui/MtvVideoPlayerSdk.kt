package com.app.videosdk.ui

import android.util.Log
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
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
import com.app.videosdk.utils.PlayerUtils.createExoPlayer
import com.app.videosdk.utils.PlayerUtils.getMimeTypeFromExtension

@OptIn(UnstableApi::class)
@Composable
fun MtvVideoPlayerSdk(
    contentList: List<PlayerModel>? = null,
    index: Int? = 0,
    onPlayerBack: (Boolean) -> Unit,
    setFullScreen: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val selectedIndex = remember { mutableIntStateOf(index ?: 0) }

    LaunchedEffect(index) {
        if (index != null) {
            selectedIndex.intValue = index
        }
    }

    val playerModel = remember(selectedIndex.intValue, contentList) {
        contentList?.getOrNull(selectedIndex.intValue)
    }

    /* ---------------- PLAYER STATE ---------------- */

    var isControllerVisible by remember { mutableStateOf(true) } 
    var isLoading by remember { mutableStateOf(false) }
    var keepScreenOn by remember { mutableStateOf(false) }
    var isSettingsClick by remember { mutableStateOf(false) }
    var isZoomed by remember { mutableStateOf(false) }
    var showIntroOverlay by remember { mutableStateOf(true) }

    val backButtonFocusRequester = remember { FocusRequester() }
    val playFocusRequester = remember { FocusRequester() }
    val sliderFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isSettingsClick) {
        if (!isSettingsClick && !showIntroOverlay) {
            playFocusRequester.requestFocus()
        }
    }

    /* ---------------- SUBTITLE ---------------- */

    val subtitleUri = remember(playerModel) {
        if (playerModel?.hlsUrl != null && getMimeTypeFromExtension(playerModel.hlsUrl.toString())) ""
        else playerModel?.srt.orEmpty()
    }

    /* ---------------- EXOPLAYER ---------------- */

    val exoPlayer = remember(playerModel?.liveUrl, playerModel?.mpdUrl, playerModel?.isLive, subtitleUri) {
        val targetVideoUrl = if (playerModel?.isLive == true) {
            playerModel.liveUrl.orEmpty()
        } else {
            playerModel?.mpdUrl.orEmpty()
        }

        createExoPlayer(
            context = context,
            videoUrl = targetVideoUrl,
            drmToken = playerModel?.drmToken,
            srt = subtitleUri,
            isLive = playerModel?.isLive ?: false
        )
    }

    /* ---------------- PLAYER LISTENER ---------------- */

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isLoading = state == Player.STATE_BUFFERING
                when (state) {
                    Player.STATE_READY -> if (exoPlayer.playWhenReady) exoPlayer.play()
                    Player.STATE_ENDED -> {
                        keepScreenOn = false
                        val lastIndex = contentList?.lastIndex ?: 0
                        if (selectedIndex.intValue < lastIndex) {
                            selectedIndex.intValue++
                        }
                    }
                    Player.STATE_IDLE -> keepScreenOn = false
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                Log.e("MtvPlayer", "Playback error: ${error.message}")
            }
        }
        exoPlayer.addListener(listener)
        exoPlayer.repeatMode = if (playerModel?.isLive == true) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ONE
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    /* ---------------- LIFECYCLE ---------------- */

    DisposableEffect(lifecycleOwner) {
        val observer = object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                exoPlayer.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    keepScreenOn = exoPlayer.playWhenReady && exoPlayer.playbackState == Player.STATE_READY

    /* ---------------- UI ---------------- */

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                if (showIntroOverlay || isSettingsClick) return@onPreviewKeyEvent false

                if (!isControllerVisible) {
                    when (event.key) {
                        Key.DirectionLeft -> {
                            if (playerModel?.isLive == false) {
                                exoPlayer.seekTo((exoPlayer.currentPosition - 10_000).coerceAtLeast(0))
                            }
                            isControllerVisible = true
                            return@onPreviewKeyEvent true
                        }
                        Key.DirectionRight -> {
                            if (playerModel?.isLive == false) {
                                exoPlayer.seekTo((exoPlayer.currentPosition + 10_000).coerceAtMost(exoPlayer.duration))
                            }
                            isControllerVisible = true
                            return@onPreviewKeyEvent true
                        }
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            isControllerVisible = true
                            return@onPreviewKeyEvent true
                        }
                        Key.DirectionUp, Key.DirectionDown -> {
                            isControllerVisible = true
                            return@onPreviewKeyEvent true
                        }
                    }
                }
                false
            }
            .focusable(!isControllerVisible) // Only focusable when controls are hidden
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    isZoomed = zoom > 1f
                }
            }
    ) {

        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    player = exoPlayer
                    useController = false
                    layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { 
                            if (!showIntroOverlay && !isSettingsClick) {
                                isControllerVisible = !isControllerVisible 
                            }
                        }
                    )
                },
            update = {
                it.player = exoPlayer
                it.keepScreenOn = keepScreenOn
                it.resizeMode = if (isZoomed) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
        )

        AnimatedVisibility(
            visible = isControllerVisible && !isSettingsClick,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            CustomPlayerController(
                playerModelList = contentList,
                index = selectedIndex.intValue,
                isFullScreen = setFullScreen,
                isCurrentlyFullScreen = true,
                exoPlayer = exoPlayer,
                modifier = Modifier.fillMaxSize(),
                onShowControls = { 
                    if (!showIntroOverlay) isControllerVisible = it 
                },
                onSettingsButtonClick = { isSettingsClick = it },
                isLoading = isLoading,
                onBackPressed = { onPlayerBack(true) },
                playContent = { newIndex ->
                    val size = contentList?.size ?: return@CustomPlayerController
                    if (newIndex in 0 until size) {
                        selectedIndex.intValue = newIndex
                    }
                },
                showIntroOverlay = showIntroOverlay,
                onPlayClicked = { 
                    showIntroOverlay = false
                },
                backButtonFocusRequester = backButtonFocusRequester,
                playFocusRequester = playFocusRequester,
                sliderFocusRequester = sliderFocusRequester
            )
        }

        if (!isControllerVisible && isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center).size(55.dp),
                color = Color.White
            )
        }

        if (isSettingsClick) {
            SelectorHeader(exoPlayer) { isSettingsClick = it }
        }
    }
}
