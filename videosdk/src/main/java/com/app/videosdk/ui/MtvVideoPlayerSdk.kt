package com.app.videosdk.ui

import android.util.Log
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.app.videosdk.listener.PipListener
import com.app.videosdk.model.PlayerModel
import com.app.videosdk.utils.PlayerUtils.createExoPlayer
import com.app.videosdk.utils.PlayerUtils.getMimeTypeFromExtension
import com.google.android.gms.cast.framework.CastContext
import kotlin.math.max

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
    val configuration = LocalConfiguration.current

    CastContext.getSharedInstance(context)

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

    /* ---------------- FIT ↔ FILL ZOOM STATE ---------------- */

    var containerSize by remember { mutableStateOf(Size.Zero) }
    var fillScale by remember { mutableStateOf(1f) }
    var isFilled by remember { mutableStateOf(false) }
    var zoomAccumulator by remember { mutableStateOf(1f) }

    val targetScale = if (isFilled) fillScale else 1f

    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(220),
        label = "video-scale"
    )

    /* ---------------- SUBTITLE ---------------- */

    val subtitleUri = remember(playerModel) {
        if (getMimeTypeFromExtension(playerModel?.hlsUrl.toString())) ""
        else playerModel?.srt.orEmpty()
    }

    /* ---------------- EXOPLAYER ---------------- */

    val exoPlayer = remember(playerModel?.mpdUrl, subtitleUri) {
        createExoPlayer(
            context,
            playerModel?.mpdUrl.orEmpty(),
            playerModel?.drmToken,
            subtitleUri
        )
    }

    /* ---------------- VIDEO SIZE → CALCULATE FILL SCALE ---------------- */

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (
                    videoSize.width == 0 ||
                    videoSize.height == 0 ||
                    containerSize == Size.Zero
                ) return

                val scaleX = containerSize.width / videoSize.width
                val scaleY = containerSize.height / videoSize.height
                fillScale = max(scaleX, scaleY)

                isFilled = false
                zoomAccumulator = 1f
            }

            override fun onPlaybackStateChanged(state: Int) {
                isLoading = state == Player.STATE_BUFFERING

                if (state == Player.STATE_ENDED) {
                    val size = contentList?.size ?: return
                    val nextIndex = selectedIndex.intValue + 1

                    if (nextIndex < size) {
                        selectedIndex.intValue = nextIndex
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("MtvPlayer", error.message ?: "Playback error")
            }
        }

        exoPlayer.addListener(listener)
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

    ScreenRotation {
        isFullScreen = it
        setFullScreen(it)
    }
    FullScreenHandler(isFullScreen)

    val aspectRatioHeight = remember(configuration) {
        configuration.screenWidthDp.dp * 9 / 16
    }

    /* ---------------- UI ---------------- */

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isFullScreen) Modifier.fillMaxSize() else Modifier.height(aspectRatioHeight))
            .background(Color.Black)
            .onSizeChanged {
                containerSize = Size(it.width.toFloat(), it.height.toFloat())
            }

            /* 🔥 SMOOTH PINCH → FIT / FILL */
            .pointerInput(Unit) {
                awaitEachGesture {
                    zoomAccumulator = 1f

                    while (true) {
                        val event = awaitPointerEvent()
                        val zoomChange = event.calculateZoom()

                        if (zoomChange != 1f) {
                            zoomAccumulator *= zoomChange

                            when {
                                zoomAccumulator > 1.15f && !isFilled -> {
                                    isFilled = true
                                }
                                zoomAccumulator < 0.85f && isFilled -> {
                                    isFilled = false
                                }
                            }
                        }

                        if (event.changes.all { !it.pressed }) break
                    }
                }
            }
    ) {

        key(selectedIndex.intValue) {

            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        layoutParams = FrameLayout.LayoutParams(
                            MATCH_PARENT,
                            MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = animatedScale
                        scaleY = animatedScale
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                if (!isSettingsClick && !pipEnabled) {
                                    isControllerVisible = !isControllerVisible
                                }
                            }
                        )
                    },
                update = { playerView ->
                    playerView.player = exoPlayer

                    // CC padding fix
                    playerView.subtitleView?.setBottomPaddingFraction(
                        if (isFilled) 0.15f else 0.08f
                    )
                }
            )
        }



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
