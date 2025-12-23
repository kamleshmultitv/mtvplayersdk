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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
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
import com.app.videosdk.listener.PipListener
import com.app.videosdk.model.PlayerModel
import com.app.videosdk.utils.PlayerUtils.createExoPlayer
import com.app.videosdk.utils.PlayerUtils.getMimeTypeFromExtension

@OptIn(UnstableApi::class)
@Composable
fun MtvVideoPlayerSdk(
    contentList: List<PlayerModel>? = null,
    index: Int? = 0,
    pipListener: PipListener? = null,
    onPlayerBack: (Boolean) -> Unit,
    setFullScreen: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    /* ---------------- SAFE INDEX ---------------- */

    val safeIndex = remember(contentList, index) {
        val size = contentList?.size ?: 0
        when {
            size == 0 -> 0
            index == null -> 0
            index < 0 -> 0
            index >= size -> size - 1
            else -> index
        }
    }

    val selectedIndex = remember { mutableIntStateOf(safeIndex) }

    LaunchedEffect(index, contentList) {
        val size = contentList?.size ?: return@LaunchedEffect
        selectedIndex.intValue = when {
            index == null -> 0
            index < 0 -> 0
            index >= size -> size - 1
            else -> index
        }
    }

    val playerModel = contentList?.getOrNull(selectedIndex.intValue)

    /* ---------------- PLAYER STATE ---------------- */

    var isFullScreen by remember { mutableStateOf(false) }
    var isControllerVisible by remember { mutableStateOf(false) }
    var pipEnabled by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var keepScreenOn by remember { mutableStateOf(false) }
    var isSettingsClick by remember { mutableStateOf(false) }
    var showForwardIcon by remember { mutableStateOf(false) }
    var showRewindIcon by remember { mutableStateOf(false) }
    var isZoomed by remember { mutableStateOf(false) }

    /* ---------------- SUBTITLE ---------------- */

    val subtitleUri = remember(playerModel) {
        if (getMimeTypeFromExtension(playerModel?.hlsUrl.toString())) ""
        else playerModel?.srt.orEmpty()
    }

    /* ---------------- EXOPLAYER ---------------- */

    val exoPlayer = remember(playerModel?.mpdUrl, subtitleUri) {
        createExoPlayer(
            context = context,
            videoUrl = playerModel?.mpdUrl.orEmpty(),
            drmToken = playerModel?.drmToken,
            srt = subtitleUri
        )
    }

    /* ---------------- PLAYER LISTENER ---------------- */

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {

            override fun onPlaybackStateChanged(state: Int) {
                isLoading = state == Player.STATE_BUFFERING

                when (state) {
                    Player.STATE_READY -> {
                        if (exoPlayer.playWhenReady) exoPlayer.play()
                    }

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
        exoPlayer.repeatMode = Player.REPEAT_MODE_OFF

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

    keepScreenOn =
        exoPlayer.playWhenReady && exoPlayer.playbackState == Player.STATE_READY

    ScreenRotation {
        isFullScreen = it
        setFullScreen(it)
    }
    FullScreenHandler(isFullScreen)

    val configuration = LocalConfiguration.current
    val aspectRatioHeight = configuration.screenWidthDp.dp * 9 / 16

    /* ---------------- UI ---------------- */

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isFullScreen) Modifier.fillMaxSize() else Modifier.height(aspectRatioHeight))
            .background(Color.Black)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

                val seekBy = 10_000L

                when (event.key) {
                    Key.DirectionCenter,
                    Key.Enter,
                    Key.NumPadEnter,
                    Key.MediaPlayPause -> {
                        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                        isControllerVisible = true
                        true
                    }

                    Key.DirectionLeft -> {
                        exoPlayer.seekTo(
                            (exoPlayer.currentPosition - seekBy).coerceAtLeast(0)
                        )
                        isControllerVisible = true
                        true
                    }

                    Key.DirectionRight -> {
                        exoPlayer.seekTo(exoPlayer.currentPosition + seekBy)
                        isControllerVisible = true
                        true
                    }

                    else -> false
                }
            }
            .focusable()
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
                        onTap = { isControllerVisible = !isControllerVisible },
                        onDoubleTap = { offset ->
                            val isLeft = offset.x < size.width / 2
                            val seek = if (isLeft) -10_000 else 10_000
                            exoPlayer.seekTo(
                                (exoPlayer.currentPosition + seek).coerceAtLeast(0)
                            )
                            if (isLeft) showRewindIcon = true else showForwardIcon = true
                        }
                    )
                },
            update = {
                it.player = exoPlayer
                it.keepScreenOn = keepScreenOn
                it.resizeMode =
                    if (isZoomed)
                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    else
                        AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
        )

        AnimatedVisibility(
            visible = !pipEnabled && isControllerVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            CustomPlayerController(
                playerModelList = contentList,
                index = selectedIndex.intValue,
                pipListener = pipListener,
                isFullScreen = {
                    isFullScreen = it
                    setFullScreen(it)
                },
                isCurrentlyFullScreen = isFullScreen,
                exoPlayer = exoPlayer,
                modifier = Modifier.fillMaxSize(),
                onShowControls = { isControllerVisible = it },
                isPipEnabled = { pipEnabled = it },
                onSettingsButtonClick = { isSettingsClick = it },
                isLoading = isLoading,
                onBackPressed = {
                    if (isFullScreen) {
                        isFullScreen = false
                        setFullScreen(false)
                    } else {
                        onPlayerBack(true)
                    }
                },
                playContent = { newIndex ->
                    val size = contentList?.size ?: return@CustomPlayerController
                    if (newIndex in 0 until size) {
                        selectedIndex.intValue = newIndex
                    }
                }
            )
        }

        ForwardBackwardButtonsOverlay(
            exoPlayer = exoPlayer,
            showRewindIcon = showRewindIcon,
            showForwardIcon = showForwardIcon,
            onRewindIconHide = { showRewindIcon = false },
            onForwardIconHide = { showForwardIcon = false },
            isControllerVisible = false
        )

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
