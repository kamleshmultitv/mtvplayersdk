package com.app.reelssdk.ui

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toColorInt
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.Tracks
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import com.app.reelssdk.listener.AdsListener
import com.app.reelssdk.listener.ReelsPlayerStateListener
import com.app.reelssdk.model.CuePoint
import com.app.reelssdk.model.CueType
import com.app.reelssdk.model.ReelsPlayerControlsConfig
import com.app.reelssdk.model.ReelsPlayerModel
import com.app.reelssdk.ui.ads.LShapeAdContainer
import com.app.reelssdk.ui.reels.ReelsSettingsSheet
import com.app.reelssdk.utils.PlayerUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun MainContainer(
    contentList: List<ReelsPlayerModel>?,
    index: Int? = 0,
    playerStateListener: ReelsPlayerStateListener?,
    onPlayerBack: (Boolean) -> Unit,
    setFullScreen: (Boolean) -> Unit,
    startInFullScreen: Boolean = false,
    isFullScreen: Boolean = startInFullScreen,
    onFullScreenModeChanged: (Boolean) -> Unit = {},
    isReelPageActive: Boolean = true,
    controlsConfig: ReelsPlayerControlsConfig = ReelsPlayerControlsConfig()
) {
    if (LocalInspectionMode.current) {
        MainContainerPreviewSurface(
            playerModel = contentList?.getOrNull(index ?: 0),
            isReelPageActive = isReelPageActive
        )
        return
    }

    val context = LocalContext.current
    var contentDuration by remember { mutableLongStateOf(0L) }


    val playerView = remember {
        PlayerView(context).apply {
            useController = false
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
    }

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
    val modelControlsConfig = playerModel?.controlsConfig ?: controlsConfig

    var isControllerVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isPlayerPlaying by remember { mutableStateOf(false) }
    var isSeekbarDragging by remember { mutableStateOf(false) }
    var isSettingsClick by remember { mutableStateOf(false) }
    val reelsSettingsSelectedItems = remember(selectedIndex.intValue) {
        mutableStateMapOf<Int, Int>()
    }
    var reelsSettingsSelectedOption by remember(selectedIndex.intValue) {
        mutableStateOf<Int?>(null)
    }
    var isAdsShowing by remember { mutableStateOf(false) }
    var showLShapeAd by remember { mutableStateOf(false) }
    val triggeredLBands = remember { mutableSetOf<String>() }
    val coroutineScope = rememberCoroutineScope()

    val imaCuePoints = remember {
        mutableStateListOf<CuePoint>()
    }

    val isLive = playerModel?.isLive == true

    val playbackUrl = remember(playerModel) {
        val url = when {
            // Live content always uses liveUrl
            isLive -> playerModel.liveUrl

            // DRM content must use MPD
            playerModel?.drm == "1" && !playerModel.mpdUrl.isNullOrEmpty() -> playerModel.mpdUrl

            // Non‑DRM: prefer HLS, then MPD as fallback
            !playerModel?.hlsUrl.isNullOrEmpty() -> playerModel.hlsUrl
            !playerModel?.mpdUrl.isNullOrEmpty() -> playerModel.mpdUrl

            else -> ""
        }

        // ✅ DEBUG: Log URL selection
        android.util.Log.d(
            "MtvReelsPlayerSdk",
            "Selected playbackUrl: $url (DRM=${playerModel?.drm}, isLive=$isLive)"
        )

        url
    }

    val subtitleUri = if (isLive) "" else playerModel?.srt.orEmpty()

    var hasReleasedStartupBitrate by remember(selectedIndex.intValue, playbackUrl) { mutableStateOf(false) }

    val adsListener = remember {
        object : AdsListener {

            override fun onAdsLoaded() {
                isControllerVisible = false
                isAdsShowing = true
            }

            override fun onAdStarted() {
                isControllerVisible = false
                isAdsShowing = true
                showLShapeAd = false
                playerStateListener?.onAdStateChanged(true)
            }

            override fun onAdCompleted() {
                isControllerVisible = true
                isAdsShowing = false
                playerStateListener?.onAdStateChanged(false)

                showLShapeAd = true

                coroutineScope.launch {
                    delay(200000)
                    showLShapeAd = false
                }
            }

            override fun onAllAdsCompleted() {
                isControllerVisible = true
                isAdsShowing = false
            }

            override fun onAdError(message: String) {
                isControllerVisible = true
                isAdsShowing = false
            }
        }
    }

    val playerWithAds = remember(selectedIndex.intValue, playbackUrl) {
        val model = playerModel ?: return@remember null
        val urlString = playbackUrl ?: ""

        android.util.Log.d(
            "MtvReelsPlayerSdk",
            "Creating player with url: $urlString, drmToken: ${if (model.drmToken.isNullOrBlank()) "null" else "present"}"
        )

        PlayerUtils.createPlayer(
            context = context,
            contentList,
            selectedIndex.intValue,
            videoUrl = urlString,
            drmToken = model.drmToken,
            srt = subtitleUri,
            playerView = playerView,
            adsConfig = model.adsConfig,
            adsListener = adsListener,
            playWhenReady = isReelPageActive,
            fastStart = true
        )
    }

    val exoPlayer = playerWithAds?.first
    val adsLoader = playerWithAds?.second

    val lBandCuePoints = remember(contentDuration, selectedIndex.intValue) {
        val interval = playerModel?.gamAdsConfig?.timeIntervalInMilliseconds
        if (contentDuration <= 0L || interval == null || interval <= 0L) {
            emptyList()
        } else {
            val cueList = mutableListOf<CuePoint>()
            var position = interval
            var count = 1
            while (position < contentDuration) {
                cueList.add(
                    CuePoint(
                        positionMs = position,
                        id = "lband_$count",
                        type = CueType.L_BAND
                    )
                )
                position += interval   // now safe (Long, not nullable)
                count++
            }
            cueList
        }
    }

    DisposableEffect(exoPlayer) {
        val player = exoPlayer ?: return@DisposableEffect onDispose {}

        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                if (!hasReleasedStartupBitrate) {
                    hasReleasedStartupBitrate = true
                    player.trackSelectionParameters =
                        player.trackSelectionParameters
                            .buildUpon()
                            .setMaxVideoBitrate(Int.MAX_VALUE)
                            .setForceHighestSupportedBitrate(false)
                            .build()
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_IDLE -> {
                        isLoading = false
                    }

                    Player.STATE_BUFFERING -> {
                        isLoading = true
                        playerStateListener?.onBuffering(true)
                    }

                    Player.STATE_READY -> {

                        isLoading = false

                        val d = player.duration

                        if (d != C.TIME_UNSET && d > 0) {
                            contentDuration = d
                        }

                        playerStateListener?.onBuffering(false)

                        val duration =
                            if (player.duration != C.TIME_UNSET && player.duration > 0)
                                player.duration
                            else
                                0L

                        playerStateListener?.onPlayerReady(duration)

                        exoPlayer.trackSelectionParameters =
                            exoPlayer.trackSelectionParameters
                                .buildUpon()
                                .setForceHighestSupportedBitrate(false)
                                .build()
                    }


                    Player.STATE_ENDED -> {
                        isLoading = false
                        playerStateListener?.onPlaybackCompleted()
                    }

                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                tracks.groups.forEach { group ->
                    Log.d("TRACK", "TrackGroup type=${group.type}")
                    for (i in 0 until group.mediaTrackGroup.length) {
                        val format = group.mediaTrackGroup.getFormat(i)
                        Log.d(
                            "TRACK",
                            "  mime=${format.sampleMimeType}, codecs=${format.codecs}"
                        )
                    }
                }
            }


            override fun onIsPlayingChanged(isPlaying: Boolean) {
                isPlayerPlaying = isPlaying
                playerStateListener?.onPlayStateChanged(isPlaying)
            }

            override fun onPlayerError(error: PlaybackException) {
                android.util.Log.e("MtvReelsPlayerSdk", "❌ PLAYER ERROR", error)
                android.util.Log.e("MtvReelsPlayerSdk", "Error code: ${error.errorCode}")
                android.util.Log.e("MtvReelsPlayerSdk", "Error message: ${error.message}")
                android.util.Log.e("MtvReelsPlayerSdk", "Cause: ${error.cause?.message}")
                playerStateListener?.onPlayerError(error)
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                if (!timeline.isEmpty) {
                    val d = player.duration
                    if (d > 0) {
                        contentDuration = d
                    }
                }

                if (timeline.isEmpty) return
                val period = Timeline.Period()
                timeline.getPeriod(0, period)
                imaCuePoints.clear()
                for (adGroupIndex in 0 until period.adGroupCount) {
                    val timeUs = period.getAdGroupTimeUs(adGroupIndex)
                    val positionMs =
                        if (timeUs == C.TIME_END_OF_SOURCE) exoPlayer.duration else timeUs / 1000
                    imaCuePoints.add(
                        CuePoint(
                            id = "ima_$adGroupIndex",
                            positionMs = positionMs,
                            type = CueType.AD
                        )
                    )
                }
            }
        }

        player.addListener(listener)

        onDispose {
            player.removeListener(listener)
            adsLoader?.setPlayer(null)
            player.release()
        }
    }

    LaunchedEffect(selectedIndex.intValue) {
        triggeredLBands.clear()
        imaCuePoints.clear()
    }

    LaunchedEffect(exoPlayer, isReelPageActive) {
        if (isReelPageActive) {
            exoPlayer?.playWhenReady = true
            exoPlayer?.play()
        } else {
            exoPlayer?.playWhenReady = false
            exoPlayer?.pause()
        }
    }

    LaunchedEffect(
        isReelPageActive,
        isControllerVisible,
        isPlayerPlaying,
        isSeekbarDragging,
        isAdsShowing,
        isSettingsClick,
        selectedIndex.intValue
    ) {
        if (
            !isReelPageActive ||
            !isControllerVisible ||
            !isPlayerPlaying ||
            isSeekbarDragging ||
            isAdsShowing ||
            isSettingsClick
        ) {
            return@LaunchedEffect
        }

        delay(3000L)

        if (
            exoPlayer?.isPlaying == true &&
            isControllerVisible &&
            !isSeekbarDragging &&
            !isAdsShowing &&
            !isSettingsClick
        ) {
            isControllerVisible = false
        }
    }




    LaunchedEffect(exoPlayer, lBandCuePoints) {

        val player = exoPlayer ?: return@LaunchedEffect

        while (true) {

            val currentPos = player.currentPosition

            // 🔥 L-BAND CUE HANDLER
            lBandCuePoints.forEach { cue ->

                if (!triggeredLBands.contains(cue.id)
                    && currentPos >= cue.positionMs
                    && !isAdsShowing
                    && isFullScreen
                ) {

                    triggeredLBands.add(cue.id)

                    showLShapeAd = true

                    coroutineScope.launch {
                        delay(15_000L)

                        if (!isAdsShowing) {
                            showLShapeAd = false
                        }
                    }
                }
            }

            delay(1000L)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val reelsCollapsedHeight = maxWidth * 16 / 9

        LShapeAdContainer(
            playerModel = playerModel,
            isFullScreen = isFullScreen,
            isVisible = showLShapeAd && !isAdsShowing,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val videoSurfaceModifier = Modifier.then(
                    if (isFullScreen) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .height(reelsCollapsedHeight)
                    }
                )

                AndroidView(
                    factory = { playerView },
                    update = { view ->
                        if (view.player !== exoPlayer) {
                            view.player = exoPlayer
                        }

                        val targetResizeMode =
                            if (isFullScreen) {
                                AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            } else {
                                AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                        if (view.resizeMode != targetResizeMode) {
                            view.resizeMode = targetResizeMode
                        }

                        view.subtitleView?.apply {
                            setApplyEmbeddedStyles(false)
                            setApplyEmbeddedFontSizes(false)
                            setStyle(
                                CaptionStyleCompat(
                                    android.graphics.Color.WHITE,
                                    "#80000000".toColorInt(),
                                    android.graphics.Color.TRANSPARENT,
                                    CaptionStyleCompat.EDGE_TYPE_NONE,
                                    android.graphics.Color.TRANSPARENT,
                                    android.graphics.Typeface.DEFAULT
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .then(videoSurfaceModifier)
                        .background(Color.Black)
                        .pointerInput(isSettingsClick) {
                            detectTapGestures(
                                onTap = {
                                    if (!isSettingsClick) {
                                        isControllerVisible = !isControllerVisible
                                    }
                                }
                            )
                        }
                )

                if (!isControllerVisible && !isSettingsClick) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = {
                                        isControllerVisible = true
                                    }
                                )
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
                    visible = isControllerVisible,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    exoPlayer?.let { player ->
                        CustomPlayerController(
                            playerModelList = contentList,
                            index = selectedIndex.intValue,
                            totalDuration = contentDuration,
                            isFullScreen = { full ->
                                onFullScreenModeChanged(full)
                                setFullScreen(full)
                                playerStateListener?.onFullScreenChanged(full)
                            },
                            isCurrentlyFullScreen = isFullScreen,
                            exoPlayer = player,
                            modifier = Modifier.fillMaxSize(),
                            isControlsVisible = isControllerVisible,
                            onShowControls = { isControllerVisible = it },
                            isLoading = isLoading,
                            onBackPressed = { onPlayerBack(true) },
                            cuePoints = imaCuePoints,
                            controlsConfig = modelControlsConfig,
                            onSeekbarDraggingChanged = { dragging ->
                                isSeekbarDragging = dragging
                            },
                            onSettingsClick = {
                                isSettingsClick = true
                            }
                        )
                    }
                }

                if (isSettingsClick) {
                    ReelsSettingsSheet(
                        playerModel = playerModel,
                        exoPlayer = exoPlayer,
                        selectedItemsState = reelsSettingsSelectedItems,
                        selectedOptionId = reelsSettingsSelectedOption,
                        onSelectedOptionChange = { optionId ->
                            reelsSettingsSelectedOption = optionId
                        },
                        onDismiss = {
                            isSettingsClick = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MainContainerPreviewSurface(
    playerModel: ReelsPlayerModel?,
    isReelPageActive: Boolean
) {
    val title = playerModel?.episodeTitle
        ?: playerModel?.title
        ?: "MainContainer preview"
    val subtitle = if (isReelPageActive) "Reels active page" else "Reels prefetched page"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF171717))
        )

        Text(
            text = title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        )

        Text(
            text = subtitle,
            color = Color.White.copy(alpha = 0.72f),
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.Center)
        )

        Text(
            text = "Static Compose preview",
            color = Color.White.copy(alpha = 0.56f),
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        )
    }
}

@Preview(name = "MainContainer Reels", widthDp = 360, heightDp = 720, showBackground = true)
@Composable
private fun MainContainerReelsPreview() {
    var isFullScreen by remember { mutableStateOf(true) }

    MainContainer(
        contentList = mainContainerPreviewItems(),
        index = 0,
        playerStateListener = null,
        onPlayerBack = {},
        setFullScreen = {},
        startInFullScreen = true,
        isFullScreen = isFullScreen,
        onFullScreenModeChanged = { isFullScreen = it },
        isReelPageActive = true
    )
}

private fun mainContainerPreviewItems(): List<ReelsPlayerModel> =
    listOf(
        ReelsPlayerModel(
            title = "Preview Reel",
            episodeTitle = "Morning Flow",
            hlsUrl = "https://example.com/reel.m3u8"
        )
    )
