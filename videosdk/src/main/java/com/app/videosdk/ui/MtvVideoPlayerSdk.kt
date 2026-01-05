package com.app.videosdk.ui

import android.app.Activity
import android.content.pm.ActivityInfo
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.app.videosdk.listener.AdsListener
import com.app.videosdk.listener.PipListener
import com.app.videosdk.model.PlayerModel
import com.app.videosdk.utils.PlayerUtils
import com.google.android.gms.cast.framework.CastContext
import kotlinx.coroutines.delay
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
    val activity = remember(context) { context as Activity }
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current

    // ✅ SINGLE PlayerView (VERY IMPORTANT FOR ADS)
    val playerView = remember {
        PlayerView(context).apply {
            useController = false
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        }
    }

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

    // ✅ ADD THIS
    val firedCuePoints = remember {
        mutableSetOf<String>()
    }

    val imaCuePoints = remember {
        mutableStateListOf<CuePoint>()
    }


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

            override fun onAdsLoaded() {
                Log.d("IMA ADS", "Ad Loaded")
            }

            override fun onAdStarted() {
                Log.d("IMA ADS", "Ad Started")
            }

            override fun onAdCompleted() {
                Log.d("IMA ADS", "Ad Completed")
            }

            override fun onAllAdsCompleted() {
                Log.d("IMA ADS", "All Ads Completed")
            }

            override fun onAdError(message: String) {
                Log.e("IMA ADS", message)
            }
        }
    }


    /* ---------------- PLAYER + ADS ---------------- */

    val playerWithAds = remember(selectedIndex.intValue, playbackUrl) {
        val model = playerModel ?: return@remember null

        PlayerUtils.createPlayer(
            context = context,
            videoUrl = playbackUrl.toString(),
            drmToken = model.drmToken,
            srt = subtitleUri,
            playerView = playerView,          // ✅ SAME PlayerView
            adsConfig = model.adsConfig,
            adsListener = adsListener
        )
    }

    val exoPlayer = playerWithAds?.first
    val adsLoader = playerWithAds?.second

    LaunchedEffect(selectedIndex.intValue) {
        firedCuePoints.clear()
    }

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

                if (state == Player.STATE_ENDED) {
                    val total = contentList?.size ?: 0
                    val nextIndex = selectedIndex.intValue + 1

                    if (nextIndex < total) {
                        Log.d("MtvPlayer", "Auto-playing next index: $nextIndex")
                        selectedIndex.intValue = nextIndex
                    } else {
                        Log.d("MtvPlayer", "Playlist completed")
                    }
                }
            }


            override fun onPlayerError(error: PlaybackException) {
                Log.e("MtvPlayer", error.message ?: "Playback error")
            }

            override fun onTimelineChanged(
                timeline: Timeline,
                reason: Int
            ) {
                if (timeline.isEmpty) return

                val period = Timeline.Period()
                timeline.getPeriod(0, period)

                imaCuePoints.clear()

                for (adGroupIndex in 0 until period.adGroupCount) {

                    val timeUs = period.getAdGroupTimeUs(adGroupIndex)

                    val positionMs =
                        if (timeUs == C.TIME_END_OF_SOURCE) {
                            exoPlayer.duration   // POST-ROLL
                        } else {
                            timeUs / 1000        // µs → ms
                        }

                    imaCuePoints.add(
                        CuePoint(
                            id = "ima_$adGroupIndex",
                            positionMs = positionMs,
                            type = CueType.AD
                        )
                    )
                }

                Log.d("IMA", "Updated seekbar cue points → $imaCuePoints")
            }



        }

        player.addListener(listener)

        onDispose {
            player.removeListener(listener)
            loader?.setPlayer(null)
            player.release()
        }
    }

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

        // ✅ REUSE SAME PlayerView
        AndroidView(
            factory = { playerView },
            update = {
                if (it.player !== exoPlayer) it.player = exoPlayer
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
                    isFullScreen = { full ->
                        isFullScreen = full
                        setFullScreen(full)

                        activity.requestedOrientation =
                            if (full) {
                                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            } else {
                                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            }
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
                            activity.requestedOrientation =
                                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        } else {
                            onPlayerBack(true)
                        }
                    },
                    cuePoints = imaCuePoints,
                    playContent = { selectedIndex.intValue = it }
                )
            }
        }

        if (isSettingsClick) {
            SelectorHeader(exoPlayer) { isSettingsClick = it }
        }
    }
}
