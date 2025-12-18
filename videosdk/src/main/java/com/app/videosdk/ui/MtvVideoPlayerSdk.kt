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
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
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
import com.app.videosdk.listener.PipListener
import com.app.videosdk.model.PlayerModel
import com.app.videosdk.utils.CastUtils
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

    /* ---------------- SAFE INDEX HANDLING ---------------- */

    val safeInitialIndex = remember(index, contentList) {
        val size = contentList?.size ?: 0
        when {
            size == 0 -> 0
            index == null -> 0
            index < 0 -> 0
            index >= size -> size - 1
            else -> index
        }
    }

    val selectedIndex = remember { mutableIntStateOf(safeInitialIndex) }

    LaunchedEffect(index, contentList) {
        val size = contentList?.size ?: return@LaunchedEffect
        selectedIndex.intValue = when {
            index == null -> 0
            index < 0 -> 0
            index >= size -> size - 1
            else -> index
        }
    }

    val playerModel by remember(contentList, selectedIndex.intValue) {
        derivedStateOf {
            contentList?.getOrNull(selectedIndex.intValue)
        }
    }

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

    val subtitleUri by remember(playerModel) {
        derivedStateOf {
            if (getMimeTypeFromExtension(playerModel?.hlsUrl.toString())) ""
            else playerModel?.srt
        }
    }

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

    /* ---------------- PLAYER LISTENER ---------------- */

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {

            override fun onPlaybackStateChanged(state: Int) {
                isLoading = state == Player.STATE_BUFFERING

                when (state) {
                    Player.STATE_READY -> {
                        if (!isCasting && exoPlayer.playWhenReady) {
                            exoPlayer.play()
                        }
                    }

                    Player.STATE_ENDED -> {
                        keepScreenOn = false
                        val lastIndex = contentList?.lastIndex ?: 0
                        if (selectedIndex.intValue < lastIndex) {
                            selectedIndex.intValue += 1
                        }
                    }

                    Player.STATE_IDLE -> keepScreenOn = false
                    Player.STATE_BUFFERING -> {
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("ExoPlayer", "Error: ${error.message}")
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

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                exoPlayer.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    /* ---------------- UI ---------------- */

    keepScreenOn =
        exoPlayer.playWhenReady && exoPlayer.playbackState == Player.STATE_READY

    ScreenRotationExample {
        isFullScreen = it
        setFullScreen(it)
    }
    FullScreenHandler(isFullScreen)

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val aspectRatioHeight = screenWidth * 9 / 16

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isFullScreen) Modifier.fillMaxSize() else Modifier.height(aspectRatioHeight))
            .background(Color.Black)
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
                            val seekBy = if (isLeft) -10_000 else 10_000
                            val position =
                                if (isCasting) castUtils.getCastPosition()
                                else exoPlayer.currentPosition

                            val newPosition = (position + seekBy).coerceAtLeast(0)
                            if (isCasting) castUtils.seekOnCast(newPosition)
                            else exoPlayer.seekTo(newPosition)

                            if (isLeft) showRewindIcon = true else showForwardIcon = true
                        }
                    )
                },
            update = {
                it.player = exoPlayer
                it.keepScreenOn = keepScreenOn
                it.resizeMode =
                    if (isZoomed) AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    else AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
        )

        if (!pipEnabled || isControllerVisible) {
            AnimatedVisibility(
                visible = isControllerVisible,
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
        }

        ForwardBackwardButtonsOverlay(
            exoPlayer,
            context,
            showRewindIcon,
            showForwardIcon,
            { showRewindIcon = false },
            { showForwardIcon = false },
            false
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
