package com.app.videosdk.ui

import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
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
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import com.app.videosdk.R
import com.app.videosdk.listener.AdsListener
import com.app.videosdk.listener.PipListener
import com.app.videosdk.listener.PlayerController
import com.app.videosdk.listener.PlayerStateListener
import com.app.videosdk.model.Chapter
import com.app.videosdk.model.CuePoint
import com.app.videosdk.model.CueType
import com.app.videosdk.model.EpisodeNowPlayingStyle
import com.app.videosdk.model.AgeRatingResolver
import com.app.videosdk.model.FreePreviewConfig
import com.app.videosdk.model.FreePreviewEndConfig
import com.app.videosdk.model.PlayerAdsConfig
import com.app.videosdk.model.PlayerConfig
import com.app.videosdk.model.PlayerModel
import com.app.videosdk.model.WatermarkConfig
import com.app.videosdk.model.WatermarkPosition
import com.app.videosdk.ui.ads.LShapeAdContainer
import com.app.videosdk.ui.chapter.ChapterDrawer
import com.app.videosdk.ui.cut.CutBottomSheet
import com.app.videosdk.utils.PlayerMode
import com.app.videosdk.utils.PlayerUtils
import com.app.videosdk.utils.PlayerUtils.parseDurationToMillis
import com.google.android.gms.cast.framework.CastContext
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

@OptIn(UnstableApi::class)
@Composable
fun MtvVideoPlayerSdk(
    contentList: List<PlayerModel>? = null,
    index: Int? = 0,
    pipListener: PipListener? = null,
    isInPipMode: Boolean = false,
    isDeepLink: Boolean? = false,
    playerMode: PlayerMode = PlayerMode.MINI,
    playerStateListener: PlayerStateListener? = null,
    controller: PlayerController? = null,
    isMutedInitially: Boolean = true,
    onPlayerBack: (Boolean) -> Unit = {},
    setFullScreen: (Boolean) -> Unit = {},
    onIndexChanged: (Int) -> Unit = {},
    episodeNowPlayingStyle: EpisodeNowPlayingStyle = EpisodeNowPlayingStyle(),
    showControls: Boolean = true,
    playerConfig: PlayerConfig = PlayerConfig(),
    onCurrentIndexChanged: (Int) -> Unit = {},
    onPreviewPrimaryAction: () -> Unit = {},
    onPreviewSecondaryAction: () -> Unit = {}
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    var contentDuration by remember { mutableLongStateOf(0L) }


    val playerView = remember {
        (LayoutInflater.from(context)
            .inflate(R.layout.mtv_video_player_view, null, false) as PlayerView).apply {
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

    fun notifyIndexChanged(newIndex: Int) {
        onIndexChanged(newIndex)
        onCurrentIndexChanged(newIndex)
    }

    LaunchedEffect(index, contentList) {
        val size = contentList?.size ?: 0
        if (size == 0) {
            selectedIndex.intValue = 0
            return@LaunchedEffect
        }

        val safeExternalIndex = index?.coerceIn(0, size - 1) ?: 0
        selectedIndex.intValue = safeExternalIndex

        if (index != null && index != safeExternalIndex) {
            notifyIndexChanged(safeExternalIndex)
        }
    }

    fun changeSelectedIndex(newIndex: Int) {
        val size = contentList?.size ?: 0
        if (size == 0) return

        val safeIndex = newIndex.coerceIn(0, size - 1)
        if (selectedIndex.intValue == safeIndex) return

        selectedIndex.intValue = safeIndex
        notifyIndexChanged(safeIndex)
    }

    var requestedVideo by remember { mutableStateOf<PlayerModel?>(null) }
    var playRequestId by remember { mutableLongStateOf(0L) }

    DisposableEffect(controller) {
        val listener: (PlayerModel, Long) -> Unit = { video, requestId ->
            requestedVideo = video
            playRequestId = requestId
        }
        controller?.attachPlayRequestListener(listener)
        onDispose { controller?.detachPlayRequestListener(listener) }
    }

    val playerModel = requestedVideo ?: contentList?.getOrNull(selectedIndex.intValue)
    val ageRating = remember(playerModel) { AgeRatingResolver.resolve(playerModel) }
    val activeContentList = remember(requestedVideo, contentList, playerModel) {
        if (requestedVideo != null && playerModel != null) listOf(playerModel) else contentList
    }
    val activeIndex = if (requestedVideo != null) 0 else selectedIndex.intValue

    var currentMode by remember { mutableStateOf(playerMode) }

    LaunchedEffect(playerMode) {
        currentMode = playerMode
    }

    val initialDisplayMode = remember {
        when (playerMode) {
            PlayerMode.FULL_SCREEN ->
                if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    PlayerMode.FULL_SCREEN
                } else {
                    PlayerMode.MINI
                }

            else -> playerMode
        }
    }

    var displayMode by remember { mutableStateOf(initialDisplayMode) }

    LaunchedEffect(currentMode, configuration.orientation) {
        displayMode = when (currentMode) {
            PlayerMode.FULL_SCREEN ->
                if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    PlayerMode.FULL_SCREEN
                } else {
                    displayMode
                }

            PlayerMode.MINI ->
                if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    PlayerMode.MINI
                } else {
                    displayMode
                }

            PlayerMode.REELS -> PlayerMode.REELS
        }
    }

    // Fullscreen controls follow the requested player mode immediately. displayMode
    // remains orientation-aware and is used only to animate the player dimensions.
    val isFullScreen = currentMode == PlayerMode.FULL_SCREEN

    FullScreenHandler(isFullScreen)
    var isControllerVisible by remember { mutableStateOf(false) }
    LaunchedEffect(showControls) {
        if (!showControls) {
            isControllerVisible = false
        }
    }

    var isLockScreen by remember { mutableStateOf(false) }
    var showUnlockConfirm by remember { mutableStateOf(false) }
    var isLockOverlayVisible by remember { mutableStateOf(true) }
    var pipEnabled by remember { mutableStateOf(isInPipMode) }
    var isLoading by remember { mutableStateOf(false) }
    var isSettingsClick by remember { mutableStateOf(false) }
    var isAdsShowing by remember { mutableStateOf(false) }
    var showLShapeAd by remember { mutableStateOf(false) }
    val triggeredLBands = remember { mutableSetOf<String>() }
    val coroutineScope = rememberCoroutineScope()
    var lastVideoSize by remember { mutableStateOf<VideoSize?>(null) }
    var hasRenderedFirstFrame by remember(playerModel, activeIndex, playRequestId) {
        mutableStateOf(false)
    }
    var currentChapter by remember { mutableStateOf<Chapter?>(null) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var hasActivatedFill by remember { mutableStateOf(false) }
    var showCutSheet by remember { mutableStateOf(false) }

    var isSkipIntroClicked by remember(selectedIndex.intValue) { mutableStateOf(false) }
    var hasPreviewLimitTriggered by remember(playerModel, activeIndex, playRequestId) {
        mutableStateOf(false)
    }
    var isPreviewDialogVisible by remember(playerModel, activeIndex, playRequestId) {
        mutableStateOf(false)
    }

    val imaCuePoints = remember {
        mutableStateListOf<CuePoint>()
    }

    val isLive = playerModel?.isLive == true
    val defaultPlayerConfig = remember { PlayerConfig() }
    val defaultAdsConfig = remember { PlayerAdsConfig() }
    val isDefaultPlayerConfig = playerConfig == defaultPlayerConfig
    val hasExplicitAdsConfig = playerConfig.ads != defaultAdsConfig
    val hasExplicitControlsConfig = playerConfig.controls != defaultPlayerConfig.controls

    val effectiveVmapAdsEnabled =
        if (hasExplicitAdsConfig) {
            playerConfig.ads.googleAdsEnabled && playerConfig.ads.vmapAdsEnabled
        } else {
            playerModel?.adsConfig?.enableAds == true
        }

    val effectiveBannerAdsEnabled =
        if (hasExplicitAdsConfig) {
            playerConfig.ads.googleAdsEnabled && playerConfig.ads.bannerAdsEnabled
        } else {
            playerModel?.gamAdsConfig?.isAdsEnabled == true
        }

    val effectiveAdsConfig = remember(playerModel?.adsConfig, effectiveVmapAdsEnabled) {
        playerModel?.adsConfig?.copy(enableAds = effectiveVmapAdsEnabled)
    }

    LaunchedEffect(isInPipMode) {
        pipEnabled = isInPipMode
    }

    val playbackUrl = remember(playerModel) {
        val url = when {
            // Live content always uses liveUrl
            isLive && !playerModel.liveUrl.isNullOrEmpty() -> playerModel.liveUrl

            // DRM content must use MPD
            playerModel?.drm == "1" && !playerModel.mpdUrl.isNullOrEmpty() -> playerModel.mpdUrl

            // Non‑DRM: prefer HLS, then MPD as fallback
            !playerModel?.hlsUrl.isNullOrEmpty() -> playerModel.hlsUrl
            !playerModel?.mpdUrl.isNullOrEmpty() -> playerModel.mpdUrl
            !playerModel?.videoUrl.isNullOrEmpty() -> playerModel.videoUrl

            else -> ""
        }

        // ✅ DEBUG: Log URL selection
        android.util.Log.d(
            "MtvVideoPlayerSdk",
            "Selected playbackUrl: $url (DRM=${playerModel?.drm}, isLive=$isLive)"
        )

        url
    }

    val effectiveSubtitleEnabled = isDefaultPlayerConfig || playerConfig.subtitleEnabled
    val subtitleUri = if (isLive || !effectiveSubtitleEnabled) "" else playerModel?.srt.orEmpty()
    val shouldAutoPlay = remember(playbackUrl, currentMode, isDefaultPlayerConfig, playerConfig) {
        if (playbackUrl.isNullOrBlank()) {
            false
        } else if (isDefaultPlayerConfig) {
            true
        } else {
            when (currentMode) {
                PlayerMode.REELS -> playerConfig.autoPlayAssets
                PlayerMode.MINI -> playerConfig.autoPlayFeature || playerConfig.autoPlayDetail
                PlayerMode.FULL_SCREEN -> playerConfig.autoPlayDetail || playerConfig.autoPlayFeature
            }
        }
    }

    var containerSize by remember { mutableStateOf(Size.Zero) }
    var fillScale by remember { mutableFloatStateOf(1f) }
    var isFilled by remember { mutableStateOf(false) }
    var zoomAccumulator by remember { mutableFloatStateOf(1f) }

    val animatedScale by animateFloatAsState(
        targetValue = if (isFilled) fillScale else 1f,
        animationSpec = tween(220),
        label = "scale"
    )

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


    val playerWithAds = remember(
        playerModel,
        activeIndex,
        playRequestId,
        playbackUrl,
        subtitleUri,
        effectiveAdsConfig,
        shouldAutoPlay
    ) {
        val model = playerModel ?: return@remember null
        val urlString = playbackUrl

        android.util.Log.d(
            "MtvVideoPlayerSdk",
            "Creating player with url: $urlString, drmToken: ${if (model.drmToken.isNullOrBlank()) "null" else "present"}"
        )

        PlayerUtils.createPlayer(
            context = context,
            activeContentList,
            activeIndex,
            videoUrl = urlString,
            drmToken = model.drmToken,
            srt = subtitleUri,
            playerView = playerView,
            adsConfig = effectiveAdsConfig,
            adsListener = adsListener,
            playWhenReady = shouldAutoPlay
        )
    }

    val exoPlayer = playerWithAds?.first
    val adsLoader = playerWithAds?.second
    val freePreview = playerConfig.freePreview
    val freePreviewEnd = playerConfig.freePreviewEnd

    LaunchedEffect(isMutedInitially, exoPlayer) {
        exoPlayer?.volume = if (isMutedInitially) 0f else 1f
    }

    LaunchedEffect(exoPlayer, freePreview, freePreviewEnd, activeIndex, playRequestId) {
        val player = exoPlayer ?: return@LaunchedEffect
        val preview = freePreview ?: return@LaunchedEffect

        if (!preview.enabled) return@LaunchedEffect

        val limitMs =
            when {
                preview.durationMs > 0L -> preview.durationMs
                freePreviewEnd?.durationMs != null && freePreviewEnd.durationMs > 0L ->
                    freePreviewEnd.durationMs
                else -> 0L
            }

        if (limitMs <= 0L) return@LaunchedEffect

        while (!hasPreviewLimitTriggered) {
            if (player.currentPosition >= limitMs) {
                hasPreviewLimitTriggered = true
                player.pause()
                if (freePreviewEnd?.enabled == true || preview.popupAllowed) {
                    isPreviewDialogVisible = true
                }
                break
            }
            delay(250L)
        }
    }

    LaunchedEffect(exoPlayer) {
        controller?.exoPlayer = exoPlayer
    }

    LaunchedEffect(exoPlayer, currentMode) {
        exoPlayer?.repeatMode =
            if (currentMode == PlayerMode.REELS) {
                Player.REPEAT_MODE_ONE   // 🔥 replay same video
            } else {
                Player.REPEAT_MODE_OFF   // normal behavior
            }
    }

    val lBandCuePoints = remember(
        contentDuration,
        selectedIndex.intValue,
        effectiveBannerAdsEnabled,
        hasExplicitAdsConfig,
        playerConfig.ads,
        playerModel?.gamAdsConfig?.timeIntervalInMilliseconds
    ) {
        val interval =
            if (hasExplicitAdsConfig) {
                playerConfig.ads.gapDurationMs
            } else {
                playerModel?.gamAdsConfig?.timeIntervalInMilliseconds
            }

        if (!effectiveBannerAdsEnabled || contentDuration <= 0L || interval == null || interval <= 0L) {
            emptyList()
        } else {
            val cueList = mutableListOf<CuePoint>()
            val startMs =
                if (hasExplicitAdsConfig && playerConfig.ads.startTimeSec > 0) {
                    playerConfig.ads.startTimeSec.toLong() * 1000L
                } else {
                    interval
                }
            val configuredEndMs =
                if (hasExplicitAdsConfig && playerConfig.ads.endTimeSec > 0) {
                    playerConfig.ads.endTimeSec.toLong() * 1000L
                } else {
                    contentDuration
                }
            val endMs = configuredEndMs.coerceAtMost(contentDuration)
            var position = startMs
            var count = 1
            while (position in 1L..endMs) {
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
    var ageRatingPresentationKey by remember(playerModel, activeIndex, playRequestId) {
        mutableLongStateOf(0L)
    }
    var isAgeRatingPresentationActive by remember(playerModel, activeIndex, playRequestId) {
        mutableStateOf(false)
    }

    DisposableEffect(exoPlayer, playerModel) {
        val player = exoPlayer ?: return@DisposableEffect onDispose {}
        var hasPresentedForThisPlayback = false
        var restartArmed = false

        fun presentAgeRatingIfNeeded() {
            if (ageRating == null || !player.isPlaying) return
            if (!hasPresentedForThisPlayback || restartArmed) {
                ageRatingPresentationKey++
                hasPresentedForThisPlayback = true
                restartArmed = false
            }
        }

        val listener = object : Player.Listener {
            override fun onVolumeChanged(volume: Float) {
                playerStateListener?.onMuteStateChanged(volume == 0f)
            }
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

            override fun onRenderedFirstFrame() {
                hasRenderedFirstFrame = true
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
                        restartArmed = true
                        val total = contentList?.size ?: 0
                        val nextIndex = selectedIndex.intValue + 1
                        if (nextIndex < total) {
                            changeSelectedIndex(nextIndex)
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
                if (isPlaying) presentAgeRatingIfNeeded()
            }

            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) {
                    restartArmed = true
                    presentAgeRatingIfNeeded()
                }
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                if (reason == Player.DISCONTINUITY_REASON_SEEK &&
                    oldPosition.positionMs > 1_000L && newPosition.positionMs <= 1_000L
                ) {
                    restartArmed = true
                    presentAgeRatingIfNeeded()
                }
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
        presentAgeRatingIfNeeded()

        onDispose {
            player.removeListener(listener)
            adsLoader?.setPlayer(null)
            player.release()
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


    LaunchedEffect(exoPlayer, lBandCuePoints) {

        val player = exoPlayer ?: return@LaunchedEffect

        while (true) {

            val currentPos = player.currentPosition
            val model = playerModel

            // 🔥 L-BAND CUE HANDLER
            lBandCuePoints.forEach { cue ->

                if (!triggeredLBands.contains(cue.id)
                    && currentPos >= cue.positionMs
                    && effectiveBannerAdsEnabled
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

    LaunchedEffect(isFullScreen) {
        if (!isFullScreen) {
            zoomAccumulator = 1f
            isFilled = false
            hasActivatedFill = false
        }
    }

    val playerSizeModifier = when (displayMode) {
        PlayerMode.FULL_SCREEN -> Modifier.fillMaxSize()
        PlayerMode.REELS -> Modifier.fillMaxSize()
        PlayerMode.MINI -> Modifier.aspectRatio(16f / 9f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(durationMillis = 260))
            .then(playerSizeModifier)
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
                        bannerAdsEnabled = effectiveBannerAdsEnabled,
                        closeButtonEnabled = hasExplicitAdsConfig && playerConfig.ads.closeButtonEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(animationSpec = tween(durationMillis = 260))
                            .then(playerSizeModifier)
                            .background(Color.Black),
                        onCloseClick = { showLShapeAd = false }
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

                                    val finalScale =
                                        if (isFullScreen) zoomAccumulator else 1f

                                    scaleX = finalScale
                                    scaleY = finalScale
                                }
                        )

                        if (isLive && !hasRenderedFirstFrame) {
                            LiveImagePlaceholder(playerConfig.liveImageUrl)
                        }

                        PlayerWatermarkOverlay(playerConfig.watermark)

                        // Keep gesture handling on a Compose layer above PlayerView.
                        // AndroidView can otherwise consume the first multi-touch gesture
                        // after the controller overlay has disappeared.
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                // 🔥 PINCH ZOOM
                                .pointerInput(
                                    isFullScreen,
                                    pipEnabled,
                                    isAdsShowing,
                                    isLockScreen
                                ) {
                                    detectTransformGestures { _, _, zoom, _ ->
                                        if (!pipEnabled && !isAdsShowing && !isLockScreen && isFullScreen) {
                                            if (!hasActivatedFill) {
                                                isFilled = true
                                                hasActivatedFill = true
                                                zoomAccumulator = fillScale
                                            }

                                            zoomAccumulator *= zoom

                                            // Allow zoom from normal (1x) to 3x
                                            zoomAccumulator = zoomAccumulator.coerceIn(1f, 3f)
                                        }
                                    }
                                }

                                // 🔥 SINGLE TAP
                                .pointerInput(pipEnabled, isSettingsClick, isLockScreen, showControls) {
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

                                                showControls && !pipEnabled && !isSettingsClick -> {
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
                            visible = showControls && isControllerVisible && !pipEnabled && !isLockScreen,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            exoPlayer?.let { player ->
                                CustomPlayerController(
                                    playerModelList = activeContentList,
                                    index = activeIndex,
                                    totalDuration = contentDuration,
                                    pipListener = pipListener,
                                    isFullScreen = { full ->
                                        val newMode = if (full) PlayerMode.FULL_SCREEN else PlayerMode.MINI
                                        currentMode = newMode
                                        setFullScreen(full) // ✅ notify parent
                                        playerStateListener?.onFullScreenChanged(full)
                                    },
                                    isLockScreen = { isLockScreen = it },
                                    isCurrentlyFullScreen = isFullScreen,
                                    isCurrentlyLockScreen = isLockScreen,
                                    exoPlayer = player,
                                    episodeNowPlayingStyle = episodeNowPlayingStyle,
                                    modifier = Modifier.fillMaxSize(),
                                    isControlsVisible = isControllerVisible,
                                    onShowControls = { isControllerVisible = it },
                                    isPipEnabled = {
                                        pipEnabled = it
                                        if (it) {
                                            isControllerVisible = false
                                            isSettingsClick = false
                                        }
                                    },
                                    onSettingsButtonClick = { isSettingsClick = it },
                                    isLoading = isLoading,
                                    onBackPressed = {
                                        if (currentMode == PlayerMode.FULL_SCREEN) {
                                            currentMode = PlayerMode.MINI
                                            setFullScreen(false) // ✅ sync parent
                                        } else {
                                            onPlayerBack(true)
                                        }
                                    },
                                    cuePoints = imaCuePoints/* +
                                            if (playerModel?.gamAdsConfig?.isAdsEnabled == true)
                                                lBandCuePoints
                                            else emptyList()*/,
                                    playContent = { changeSelectedIndex(it) },
                                    isSkipIntroClicked = isSkipIntroClicked,
                                    onSkipIntroClicked = { isSkipIntroClicked = it },
                                    onNextEpisodeClick = { changeSelectedIndex(it) },
                                    showContentTitle = !isAgeRatingPresentationActive,
                                    onChapterClick = {
                                        if (playerModel?.isChapterEnabled == true) {
                                            coroutineScope.launch { drawerState.open() }
                                        }
                                    },
                                    onCutClick = {
                                        showCutSheet = true
                                    },
                                    controlsConfig = playerConfig.controls,
                                    showPreviousControl = hasExplicitControlsConfig &&
                                            playerConfig.controls.previous

                                )
                            }
                        }

                        // Rating and title share one visual slot. The title is hidden
                        // until this badge completes its own collapse animation.
                        ageRating?.let { rating ->
                            PlayerAgeRatingOverlay(
                                ageRating = rating,
                                presentationKey = ageRatingPresentationKey,
                                isInPictureInPicture = pipEnabled || isInPipMode,
                                modifier = Modifier.align(Alignment.TopStart),
                                titleSlotTopPadding = if (isFullScreen) 19.dp else 23.dp,
                                applyStatusBarPadding = isFullScreen,
                                onPresentationActiveChanged = {
                                    isAgeRatingPresentationActive = it
                                }
                            )
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

                        if (showCutSheet && exoPlayer != null) {

                            val duration = exoPlayer.duration

                            if (duration > 0) {
                                CutBottomSheet(
                                    contentId = playerModel?.id,
                                    url = playbackUrl,
                                    duration = duration,
                                    onDismiss = { showCutSheet = false }
                                )
                            }
                        }

                    }
                }
            }
        }
    }

    if (isPreviewDialogVisible) {
        PreviewLimitDialog(
            freePreview = freePreview,
            freePreviewEnd = freePreviewEnd,
            onPrimary = {
                isPreviewDialogVisible = false
                onPreviewPrimaryAction()
            },
            onSecondary = {
                isPreviewDialogVisible = false
                onPreviewSecondaryAction()
            },
            onDismiss = {
                isPreviewDialogVisible = false
            }
        )
    }

}

@Composable
private fun PreviewLimitDialog(
    freePreview: FreePreviewConfig?,
    freePreviewEnd: FreePreviewEndConfig?,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
    onDismiss: () -> Unit
) {
    val endConfig = freePreviewEnd?.takeIf { it.enabled }
    val message =
        if (endConfig != null) {
            endConfig.popupText
        } else {
            freePreview?.popupText
        }.orEmpty().ifBlank { "Preview ended" }

    val primaryLabel =
        if (endConfig != null) {
            endConfig.primaryButtonLabel
        } else {
            freePreview?.buttonLabel
        }.orEmpty().ifBlank { "Continue" }

    val secondaryLabel =
        if (endConfig != null) {
            endConfig.secondaryButtonLabel
        } else {
            null
        }?.takeIf { it.isNotBlank() }

    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Text(text = message)
        },
        confirmButton = {
            Button(onClick = onPrimary) {
                Text(text = primaryLabel)
            }
        },
        dismissButton = {
            secondaryLabel?.let { label ->
                Button(onClick = onSecondary) {
                    Text(text = label)
                }
            }
        }
    )
}

@Composable
private fun BoxScope.LiveImagePlaceholder(liveImageUrl: String?) {
    val imageUrl = liveImageUrl?.takeIf { it.isNotBlank() } ?: return

    Image(
        painter = rememberAsyncImagePainter(imageUrl),
        contentDescription = "Live Placeholder",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun BoxScope.PlayerWatermarkOverlay(watermark: WatermarkConfig?) {
    val config = watermark?.takeIf { it.enabled } ?: return
    val imageUrl = config.imageUrl?.takeIf { it.isNotBlank() } ?: return

    Image(
        painter = rememberAsyncImagePainter(imageUrl),
        contentDescription = "Watermark",
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .align(config.position.toAlignment())
            .padding(16.dp)
            .sizeIn(
                minWidth = 72.dp,
                minHeight = 32.dp,
                maxWidth = 140.dp,
                maxHeight = 72.dp
            )
    )
}

private fun WatermarkPosition.toAlignment(): Alignment =
    when (this) {
        WatermarkPosition.TOP_LEFT -> Alignment.TopStart
        WatermarkPosition.TOP_RIGHT -> Alignment.TopEnd
        WatermarkPosition.BOTTOM_LEFT -> Alignment.BottomStart
        WatermarkPosition.BOTTOM_RIGHT -> Alignment.BottomEnd
        WatermarkPosition.CENTER -> Alignment.Center
    }
