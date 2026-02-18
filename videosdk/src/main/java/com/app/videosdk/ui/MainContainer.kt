package com.app.videosdk.ui

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toColorInt
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import com.app.videosdk.listener.AdsListener
import com.app.videosdk.listener.PipListener
import com.app.videosdk.listener.PlayerStateListener
import com.app.videosdk.model.Chapter
import com.app.videosdk.model.CuePoint
import com.app.videosdk.model.CueType
import com.app.videosdk.model.PlayerModel
import com.app.videosdk.ui.ads.LShapeAdContainer
import com.app.videosdk.ui.chapter.ChapterDrawer
import com.app.videosdk.utils.PlayerMode
import com.app.videosdk.utils.PlayerUtils
import com.app.videosdk.utils.PlayerUtils.parseDurationToMillis
import com.google.android.gms.cast.framework.CastContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

@OptIn(UnstableApi::class)
@Composable
fun MainContainer(
    contentList: List<PlayerModel>?,
    index: Int? = 0,
    pipListener: PipListener?,
    isInPipMode: Boolean,
    playerStateListener: PlayerStateListener?,
    onPlayerBack: (Boolean) -> Unit,
    setFullScreen: (Boolean) -> Unit,
    startInFullScreen: Boolean = false,
    playerMode: PlayerMode = PlayerMode.OTT, // ✅ add this
) {

    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    var contentDuration by remember { mutableLongStateOf(0L) }


    val playerView = remember {
        PlayerView(context).apply {
            useController = false
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        }
    }

    CastContext.getSharedInstance(context)

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

    var isFullScreen by remember(selectedIndex.intValue) {
        mutableStateOf(
            if (playerMode == PlayerMode.REELS) true
            else startInFullScreen
        )
    }

    LaunchedEffect(startInFullScreen) {
        if (startInFullScreen) {
            setFullScreen(true)
        }
    }

    if (playerMode == PlayerMode.OTT) {
        FullScreenHandler(isFullScreen)
    }
    var isControllerVisible by remember { mutableStateOf(false) }
    var isLockScreen by remember { mutableStateOf(false) }
    var showUnlockConfirm by remember { mutableStateOf(false) }
    var isLockOverlayVisible by remember { mutableStateOf(true) }
    var pipEnabled = isInPipMode
    var isLoading by remember { mutableStateOf(false) }
    var isSettingsClick by remember { mutableStateOf(false) }
    var isAdsShowing by remember { mutableStateOf(false) }
    var showLShapeAd by remember { mutableStateOf(false) }
    val triggeredLBands = remember { mutableSetOf<String>() }
    val coroutineScope = rememberCoroutineScope()
    var lastVideoSize by remember { mutableStateOf<VideoSize?>(null) }
    var currentChapter by remember { mutableStateOf<Chapter?>(null) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var hasActivatedFill by remember { mutableStateOf(false) }
    var preloadPlayer by remember { mutableStateOf<ExoPlayer?>(null) }

    var isSkipIntroClicked by remember(selectedIndex.intValue) { mutableStateOf(false) }

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
            "MtvVideoPlayerSdk",
            "Selected playbackUrl: $url (DRM=${playerModel?.drm}, isLive=$isLive)"
        )

        url
    }

    val subtitleUri = if (isLive) "" else playerModel?.srt.orEmpty()

    var containerSize by remember { mutableStateOf(Size.Zero) }
    var fillScale by remember { mutableFloatStateOf(1f) }
    var isFilled by remember { mutableStateOf(false) }
    var zoomAccumulator by remember { mutableFloatStateOf(1f) }

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
            "MtvVideoPlayerSdk",
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
            adsListener = adsListener
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

    // 🔥 Position Tracker for Auto-Show Controls (Intro / Next Episode)
    var hasShownNextEpisodeControls by remember(selectedIndex.intValue) { mutableStateOf(false) }
    var hasShownSkipIntroControls by remember(selectedIndex.intValue) { mutableStateOf(false) }

    DisposableEffect(exoPlayer) {
        val player = exoPlayer ?: return@DisposableEffect onDispose {}

        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                hasActivatedFill = false
                lastVideoSize = videoSize
                if (videoSize.width == 0 || containerSize == Size.Zero) return
                fillScale = max(
                    containerSize.width / videoSize.width,
                    containerSize.height / videoSize.height
                )
                isFilled = false
                zoomAccumulator = 1f
            }

            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
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

                        if (playerMode == PlayerMode.OTT) {
                            val total = contentList?.size ?: 0
                            val nextIndex = selectedIndex.intValue + 1
                            if (nextIndex < total) {
                                selectedIndex.intValue = nextIndex
                            }
                        }

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
                playerStateListener?.onPlayStateChanged(isPlaying)
            }

            override fun onPlayerError(error: PlaybackException) {
                android.util.Log.e("MtvVideoPlayerSdk", "❌ PLAYER ERROR", error)
                android.util.Log.e("MtvVideoPlayerSdk", "Error code: ${error.errorCode}")
                android.util.Log.e("MtvVideoPlayerSdk", "Error message: ${error.message}")
                android.util.Log.e("MtvVideoPlayerSdk", "Cause: ${error.cause?.message}")
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

            // 🔥 Release preload player too
            preloadPlayer?.release()
            preloadPlayer = null
        }
    }

    LaunchedEffect(containerSize, showLShapeAd) {

        val videoSize = lastVideoSize ?: return@LaunchedEffect

        if (videoSize.width == 0 || containerSize == Size.Zero) return@LaunchedEffect

        fillScale = max(
            containerSize.width / videoSize.width,
            containerSize.height / videoSize.height
        )

        isFilled = false
        zoomAccumulator = 1f
    }

    LaunchedEffect(selectedIndex.intValue) {
        triggeredLBands.clear()
        imaCuePoints.clear()
    }

    LaunchedEffect(index, selectedIndex.intValue) {

        if (playerMode != PlayerMode.REELS) return@LaunchedEffect

        val shouldPlay = index == selectedIndex.intValue

        exoPlayer?.playWhenReady = shouldPlay
    }




    LaunchedEffect(exoPlayer, lBandCuePoints) {

        val player = exoPlayer ?: return@LaunchedEffect

        while (true) {

            val currentPos = player.currentPosition
            val model = playerModel

            // 🔥 L-BAND CUE HANDLER
            lBandCuePoints.forEach { cue ->

                if (!triggeredLBands.contains(cue.id)
                    && currentPos >= cue.positionMs
                    && !isAdsShowing
                    && !pipEnabled
                    && isFullScreen
                ) {

                    triggeredLBands.add(cue.id)

                    showLShapeAd = true

                    coroutineScope.launch {
                        delay(15_000L)

                        if (!isAdsShowing && !pipEnabled) {
                            showLShapeAd = false
                        }
                    }
                }
            }

            if (model != null && !model.isLive) {

                model.skipIntro?.let { intro ->
                    if (!isSkipIntroClicked &&
                        !hasShownSkipIntroControls &&
                        intro.enableSkipIntro
                    ) {

                        val startTime = intro.startTime ?: 0L

                        if (currentPos >= startTime &&
                            currentPos < startTime + 2000 &&
                            !isAdsShowing
                        ) {
                            hasShownSkipIntroControls = true
                            isControllerVisible = true
                        }
                    }
                }

                model.nextEpisode?.let { next ->

                    if (!hasShownNextEpisodeControls &&
                        next.enableNextEpisode &&
                        contentDuration > 0
                    ) {

                        val showBeforeEndMs =
                            parseDurationToMillis(next.showBeforeEndMs)

                        val triggerTime =
                            (contentDuration - showBeforeEndMs)
                                .coerceAtLeast(0L)

                        if (currentPos >= triggerTime &&
                            triggerTime != 0L &&
                            !isAdsShowing
                        ) {
                            hasShownNextEpisodeControls = true
                            isControllerVisible = true
                        }
                    }
                }
            }

            delay(1000L)
        }
    }

    val chapters = remember(playerModel) {
        if (playerModel?.isChapterEnabled == true)
            playerModel.chapters?.sortedBy { it.startMs }
        else
            emptyList()
    }

    LaunchedEffect(exoPlayer, chapters) {

        val player = exoPlayer ?: return@LaunchedEffect

        if (chapters?.isEmpty() == true) {
            currentChapter = null
            return@LaunchedEffect
        }

        while (true) {

            val position = player.currentPosition

            currentChapter =
                chapters?.lastOrNull { position >= it.startMs }

            delay(500)
        }
    }

    LaunchedEffect(isLockScreen, isLockOverlayVisible) {
        if (isLockScreen && isLockOverlayVisible) {
            delay(3000)
            isLockOverlayVisible = false
        }
    }
    Box(
        modifier = when (playerMode) {
            PlayerMode.REELS -> Modifier.fillMaxSize()
            PlayerMode.OTT -> Modifier
                .fillMaxWidth()
                .then(
                    if (isFullScreen)
                        Modifier.fillMaxSize()
                    else
                        Modifier.height(configuration.screenWidthDp.dp * 9 / 16)
                )
        }
    ) {

        // 🔥 Drawer RTL only
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {

            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = true,
                drawerContent = {
                    if (playerModel?.isChapterEnabled == true &&
                        playerModel.chapters?.isNotEmpty() == true
                    ) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            ChapterDrawer(
                                chapters = playerModel.chapters,
                                currentChapter = currentChapter,
                                onChapterClick = { chapter ->
                                    exoPlayer?.seekTo(chapter.startMs)
                                    coroutineScope.launch { drawerState.close() }
                                }
                            )
                        }
                    }
                }

            ) {

                // 🔥 Reset back to LTR for player
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {

                    LShapeAdContainer(
                        playerModel = playerModel,
                        isFullScreen = isFullScreen,
                        isVisible = showLShapeAd && !isAdsShowing && !pipEnabled,
                        modifier = when (playerMode) {

                            PlayerMode.REELS -> Modifier
                                .fillMaxSize() // 🔥 ALWAYS FULL SCREEN

                            PlayerMode.OTT -> Modifier
                                .fillMaxWidth()
                                .then(
                                    if (isFullScreen)
                                        Modifier.fillMaxSize()
                                    else
                                        Modifier.height(configuration.screenWidthDp.dp * 9 / 16)
                                )
                        }.background(Color.Black)

                    ) {

                        AndroidView(
                            factory = { playerView },
                            update = { view ->

                                if (view.player !== exoPlayer) {
                                    view.player = exoPlayer
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
                                .fillMaxSize()
                                .graphicsLayer {

                                    val finalScale = when {
                                        zoomAccumulator > fillScale -> zoomAccumulator
                                        isFilled -> fillScale
                                        else -> 1f
                                    }

                                    scaleX = finalScale
                                    scaleY = finalScale
                                }

                                // 🔥 PINCH ZOOM
                                .then(
                                    if (playerMode == PlayerMode.OTT)
                                        Modifier.pointerInput(
                                            isFullScreen,
                                            pipEnabled,
                                            isAdsShowing
                                        ) {
                                            detectTransformGestures { _, _, zoom, _ ->

                                                if (!pipEnabled && !isAdsShowing && !isLockScreen) {

                                                    if (!isFullScreen) {
                                                        isFullScreen = true
                                                        setFullScreen(true)
                                                        playerStateListener?.onFullScreenChanged(
                                                            true
                                                        )
                                                    }

                                                    if (!hasActivatedFill) {
                                                        isFilled = true
                                                        hasActivatedFill = true
                                                        zoomAccumulator = fillScale
                                                    } else {
                                                        zoomAccumulator *= zoom
                                                        zoomAccumulator =
                                                            zoomAccumulator.coerceIn(fillScale, 3f)
                                                    }
                                                }
                                            }
                                        }
                                    else Modifier
                                )


                                // 🔥 SINGLE TAP
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onDoubleTap = {
                                            hasActivatedFill = false
                                            isFilled = false
                                            zoomAccumulator = 1f
                                            // Reset zoom on double tap
                                            if (zoomAccumulator > 1f) {
                                                zoomAccumulator = 1f
                                            }
                                        },
                                        onTap = {
                                            when {
                                                isLockScreen -> {
                                                    isLockOverlayVisible = true
                                                }

                                                !pipEnabled && !isSettingsClick -> {
                                                    isControllerVisible = !isControllerVisible
                                                }
                                            }
                                        }
                                    )
                                }

                        )

                        // 🔄 Loading
                        if (!isControllerVisible && isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                                color = Color.White
                            )
                        }

                        // 🎛 Controller
                        AnimatedVisibility(
                            visible = isControllerVisible && !pipEnabled && !isLockScreen,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            exoPlayer?.let { player ->
                                CustomPlayerController(
                                    playerModelList = contentList,
                                    index = selectedIndex.intValue,
                                    totalDuration = contentDuration,
                                    pipListener = pipListener,
                                    isFullScreen = { full ->
                                        isFullScreen = full
                                        setFullScreen(full)
                                        playerStateListener?.onFullScreenChanged(full)
                                    },
                                    isLockScreen = { isLockScreen = it },
                                    isCurrentlyFullScreen = isFullScreen,
                                    isCurrentlyLockScreen = isLockScreen,
                                    exoPlayer = player,
                                    modifier = Modifier.fillMaxSize(),
                                    isControllerVisible,
                                    onShowControls = { isControllerVisible = it },
                                    isPipEnabled = { pipEnabled = it },
                                    onSettingsButtonClick = { isSettingsClick = it },
                                    isLoading = isLoading,
                                    onBackPressed = {
                                        if (playerMode == PlayerMode.OTT && isFullScreen) {
                                            isFullScreen = false
                                            setFullScreen(false)
                                            playerStateListener?.onFullScreenChanged(false)
                                        } else {
                                            onPlayerBack(true)
                                        }
                                    },
                                    cuePoints = imaCuePoints/* +
                                            if (playerModel?.gamAdsConfig?.isAdsEnabled == true)
                                                lBandCuePoints
                                            else emptyList()*/,
                                    playContent = { selectedIndex.intValue = it },
                                    isSkipIntroClicked = isSkipIntroClicked,
                                    onSkipIntroClicked = { isSkipIntroClicked = it },
                                    onNextEpisodeClick = { selectedIndex.intValue = it },
                                    onChapterClick = {
                                        if (playerModel?.isChapterEnabled == true) {
                                            coroutineScope.launch { drawerState.open() }
                                        }
                                    },
                                    mode = playerMode,
                                    onSettingsClick = {
                                        isSettingsClick = true
                                    }

                                )
                            }
                        }

                        // 🔒 Lock Overlay
                        if (isLockScreen) {
                            AnimatedVisibility(
                                visible = isLockOverlayVisible && !pipEnabled,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                exoPlayer?.let {
                                    LockScreenOverlay(
                                        playerModel = playerModel,
                                        isLocked = isLockScreen,
                                        showUnlockConfirm = showUnlockConfirm,
                                        onUnlockRequest = { showUnlockConfirm = true },
                                        onConfirmUnlock = {
                                            isLockScreen = false
                                            showUnlockConfirm = false
                                        }
                                    )
                                }
                            }
                        }

                        // ⚙ Settings
                        if (isSettingsClick) {
                            SelectorHeader(
                                playerModel = playerModel,
                                exoPlayer = exoPlayer
                            ) { isSettingsClick = it }
                        }
                    }
                }
            }
        }
    }
}
