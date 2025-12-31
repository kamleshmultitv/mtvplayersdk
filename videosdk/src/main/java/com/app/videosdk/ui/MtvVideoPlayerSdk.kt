package com.app.videosdk.ui

import android.util.Log
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.app.videosdk.listener.AdsListener
import com.app.videosdk.listener.PipListener
import com.app.videosdk.model.PlayerModel
import com.app.videosdk.utils.PlayerUtils
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

    var playerViewRef by remember { mutableStateOf<PlayerView?>(null) }

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
        selectedIndex.intValue = index?.coerceIn(0, size - 1) ?: 0
    }

    val playerModel = contentList?.getOrNull(selectedIndex.intValue)

    /* ---------------- STATE ---------------- */

    var isFullScreen by remember { mutableStateOf(false) }
    var isControllerVisible by remember { mutableStateOf(false) }
    var pipEnabled by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isSettingsClick by remember { mutableStateOf(false) }

    val isLive = playerModel?.isLive == true

    /* ---------------- URL ---------------- */

    val playbackUrl = remember(playerModel) {
        when {
            isLive -> playerModel?.liveUrl
            !playerModel?.hlsUrl.isNullOrEmpty() -> playerModel?.hlsUrl
            !playerModel?.mpdUrl.isNullOrEmpty() -> playerModel?.mpdUrl
            else -> ""
        }
    }

    val subtitleUri = if (isLive) "" else playerModel?.srt.orEmpty()

    /* ---------------- ZOOM ---------------- */

    var containerSize by remember { mutableStateOf(Size.Zero) }
    var fillScale by remember { mutableFloatStateOf(1f) }
    var isFilled by remember { mutableStateOf(false) }
    var zoomAccumulator by remember { mutableFloatStateOf(1f) }

    val animatedScale by animateFloatAsState(
        targetValue = if (isFilled) fillScale else 1f,
        animationSpec = tween(220),
        label = "scale"
    )

    /* ---------------- ADS LISTENER ---------------- */

    val adsListener = remember {
        object : AdsListener {
            override fun onAdsLoaded() {}
            override fun onAdStarted() {}
            override fun onAdCompleted() {}
            override fun onAllAdsCompleted() {}
            override fun onAdError(message: String) {
                Log.e("ADS", message)
            }
        }
    }

    /* ---------------- PLAYER + ADS (OWNED TOGETHER) ---------------- */

    val playerWithAds = key(selectedIndex.intValue) {
        remember(selectedIndex.intValue, playbackUrl, playerViewRef) {
            val model = playerModel ?: return@remember null
            val view = playerViewRef ?: return@remember null

            PlayerUtils.createPlayer(
                context = context,
                videoUrl = playbackUrl.toString(),
                drmToken = model.drmToken,
                srt = subtitleUri,
                playerView = view,
                adsConfig = model.adsConfig,
                adsListener = adsListener
            )
        }
    }

    val exoPlayer = playerWithAds?.first
    val adsLoader = playerWithAds?.second

    /* ---------------- PLAYER LISTENER ---------------- */

    DisposableEffect(exoPlayer) {
        val player = exoPlayer ?: return@DisposableEffect onDispose {}
        val loader = adsLoader

        val listener = object : Player.Listener {

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width == 0 || containerSize == Size.Zero) return
                fillScale = max(
                    containerSize.width / videoSize.width,
                    containerSize.height / videoSize.height
                )
                isFilled = false
                zoomAccumulator = 1f
            }

            override fun onPlaybackStateChanged(state: Int) {
                isLoading = state == Player.STATE_BUFFERING

                if (
                    state == Player.STATE_ENDED &&
                    !isLive &&
                    !player.isPlayingAd
                ) {
                    val size = contentList?.size ?: return
                    val next = selectedIndex.intValue + 1
                    if (next < size) {
                        selectedIndex.intValue = next
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("MtvPlayer", error.message ?: "Playback error")
            }
        }

        player.addListener(listener)

        onDispose {
            player.removeListener(listener)

            // ✅ SAFE: ads loader belongs ONLY to this player
            loader?.setPlayer(null)
            loader?.release()

            player.release()
        }
    }

    /* ---------------- LIFECYCLE ---------------- */

    DisposableEffect(lifecycleOwner) {
        val observer = object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                exoPlayer?.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    /* ---------------- FULLSCREEN ---------------- */

    ScreenRotation {
        isFullScreen = it
        setFullScreen(it)
    }
    FullScreenHandler(isFullScreen)

    /* ---------------- UI ---------------- */

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isFullScreen) Modifier.fillMaxSize()
                else Modifier.height(configuration.screenWidthDp.dp * 9 / 16)
            )
            .background(Color.Black)
            .onSizeChanged {
                containerSize = Size(it.width.toFloat(), it.height.toFloat())
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    zoomAccumulator = 1f
                    while (true) {
                        val event = awaitPointerEvent()
                        val zoomChange = event.calculateZoom()
                        if (zoomChange != 1f) {
                            zoomAccumulator *= zoomChange
                            if (zoomAccumulator > 1.15f && !isFilled) isFilled = true
                            if (zoomAccumulator < 0.85f && isFilled) isFilled = false
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
                        layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                        playerViewRef = this
                        player = exoPlayer
                    }
                },
                update = { view ->
                    if (view.player !== exoPlayer) {
                        view.player = exoPlayer
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = animatedScale
                        scaleY = animatedScale
                    }
                    .pointerInput(Unit) {
                        detectTapGestures {
                            if (!pipEnabled && !isSettingsClick) {
                                isControllerVisible = !isControllerVisible
                            }
                        }
                    }
            )
        }

        if (!isControllerVisible && isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )
        }

        AnimatedVisibility(
            visible = isControllerVisible && !pipEnabled,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            exoPlayer?.let {
                CustomPlayerController(
                    playerModelList = contentList,
                    index = selectedIndex.intValue,
                    pipListener = pipListener,
                    isFullScreen = {
                        isFullScreen = it
                        setFullScreen(it)
                    },
                    isCurrentlyFullScreen = isFullScreen,
                    exoPlayer = it,
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
                    playContent = { selectedIndex.intValue = it }
                )
            }
        }

        if (isSettingsClick) {
            SelectorHeader(exoPlayer) { isSettingsClick = it }
        }
    }
}

