package com.app.videosdk.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.rememberAsyncImagePainter
import com.app.videosdk.listener.PipListener
import com.app.videosdk.model.CuePoint
import com.app.videosdk.model.PlayerModel
import com.app.videosdk.utils.CastUtils
import com.app.videosdk.utils.PlayerUtils.timeToMillis
import kotlinx.coroutines.delay

@Composable
fun CustomPlayerController(
    playerModelList: List<PlayerModel>? = null,
    index: Int,
    totalDuration: Long,
    pipListener: PipListener? = null,
    isFullScreen: (Boolean) -> Unit,
    isLockScreen: (Boolean) -> Unit,
    isCurrentlyFullScreen: Boolean,
    isCurrentlyLockScreen: Boolean,
    exoPlayer: ExoPlayer,
    modifier: Modifier,
    isControlsVisible: Boolean,
    onShowControls: (Boolean) -> Unit,
    isPipEnabled: (Boolean) -> Unit = {},
    onSettingsButtonClick: (Boolean) -> Unit = {},
    isLoading: Boolean,
    onBackPressed: () -> Unit = {},
    cuePoints: List<CuePoint> = emptyList(),
    playContent: (Int) -> Unit,
    isSkipIntroClicked: Boolean,
    onSkipIntroClicked: (Boolean) -> Unit,
    onNextEpisodeClick: (Int) -> Unit,
    onChapterClick: () -> Unit = {},
    onCutClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val showControlsState = rememberUpdatedState(onShowControls)
    val fullScreenState = rememberUpdatedState(isFullScreen)
    val lockScreenState = rememberUpdatedState(isLockScreen)

    val castUtils = remember(context, exoPlayer) {
        CastUtils(context, exoPlayer)
    }
    val isCasting by remember { derivedStateOf { castUtils.isCasting() } }

    var isZoomed by remember { mutableStateOf(false) }
    var showForwardIcon by remember { mutableStateOf(false) }
    var showRewindIcon by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }

    var currentPosition by remember { mutableLongStateOf(0L) }
    val duration by rememberUpdatedState(
        if (totalDuration > 0) totalDuration else 0L
    )
    var nextEpisodeClicked by remember(index) { mutableStateOf(false) }
    var isDraggingSeekbar by remember { mutableStateOf(false) }
    var onSeek by remember { mutableStateOf(false) }
    val currentPlayerModel = playerModelList?.getOrNull(index)
    var expandSheet by remember { mutableStateOf(false) }


    /* ---------------- SKIP INTRO ---------------- */

    val showSkipIntro by remember(currentPosition, currentPlayerModel, isSkipIntroClicked) {
        derivedStateOf {
            currentPlayerModel?.skipIntro?.let { intro ->
                !currentPlayerModel.isLive &&
                        intro.enableSkipIntro &&
                        !isSkipIntroClicked &&
                        currentPosition in (intro.startTime ?: 0L)..(intro.endTime ?: 0L)
            } ?: false
        }
    }

    /* ---------------- NEXT EPISODE WINDOW ---------------- */

    val showNextEpisode by remember(
        currentPosition,
        currentPlayerModel,
        nextEpisodeClicked,
        duration
    ) {
        derivedStateOf {
            val next = currentPlayerModel?.nextEpisode ?: return@derivedStateOf false

            if (
                exoPlayer.isPlayingAd ||          // 🔥 KEY FIX
                duration <= 0L ||
                currentPosition <= 0L ||
                currentPosition >= duration ||
                currentPlayerModel.isLive ||
                !next.enableNextEpisode ||
                nextEpisodeClicked
            ) return@derivedStateOf false

            val startTime = timeToMillis(duration.toString(), next.showBeforeEndMs)

            currentPosition in startTime..startTime.plus(10_000L)
        }
    }


    /* ⭐ FIX 1: PLAYBACK-TIME BASED WINDOW (NOT animation based) */
    val isInNextEpisodeWindow by remember(
        currentPosition,
        currentPlayerModel,
        nextEpisodeClicked,
        duration
    ) {
        derivedStateOf {
            val next = currentPlayerModel?.nextEpisode ?: return@derivedStateOf false

            if (
                exoPlayer.isPlayingAd ||          // 🔥 KEY FIX
                duration <= 0L ||
                currentPosition <= 0L ||
                currentPosition >= duration ||
                currentPlayerModel.isLive ||
                !next.enableNextEpisode ||
                nextEpisodeClicked
            ) return@derivedStateOf false

            val startTime = timeToMillis(duration.toString(), next.showBeforeEndMs)

            currentPosition in startTime..startTime.plus(10_000L)
        }
    }


    // ⭐ FIX 4: FORCE SHOW CONTROLS EXACTLY AT startTime
    LaunchedEffect(isInNextEpisodeWindow) {
        if (isInNextEpisodeWindow) {
            showControlsState.value(true)
        }
    }


    /* ---------------- PROGRESS ANIMATION (UNCHANGED) ---------------- */
    val animationDurationMs by remember(currentPosition, currentPlayerModel) {
        derivedStateOf {
            currentPlayerModel?.nextEpisode?.let { next ->
                ((timeToMillis(
                    duration.toString(),
                    next.showBeforeEndMs
                ).plus(100_00L)) - currentPosition).coerceIn(0L, 10_000L)
            } ?: 0L
        }
    }

    val progress by animateFloatAsState(
        targetValue = if (showNextEpisode) 1f else 0f,
        animationSpec = tween(
            durationMillis = animationDurationMs.toInt().coerceAtLeast(1),
            easing = LinearEasing
        ),
        label = "NextEpisodeProgress"
    )

    /* ⭐ FIX 2: SINGLE SOURCE OF TRUTH FOR VISIBILITY */
    val shouldForceShowControls by remember(
        isInNextEpisodeWindow,
        showSkipIntro
    ) {
        derivedStateOf {
            isInNextEpisodeWindow || showSkipIntro
        }
    }

    /* ---------------- PLAYBACK OBSERVER ---------------- */

    LaunchedEffect(exoPlayer, isCasting) {
        while (true) {
            currentPosition =
                if (isCasting) castUtils.getCastPosition()
                else exoPlayer.currentPosition

            isPlaying = exoPlayer.isPlaying
            delay(1000)
        }
    }


    /* ⭐ FIX 3: AUTO-SHOW / AUTO-HIDE CONTROLS (FINAL LOGIC) */
    LaunchedEffect(
        isPlaying,
        shouldForceShowControls,
        isDraggingSeekbar,
        onSeek
    ) {
        // ✅ While skip intro OR next episode window → NEVER auto hide
        if (shouldForceShowControls || isDraggingSeekbar || onSeek) {
            showControlsState.value(true)
            return@LaunchedEffect
        }

        // ⏱ Normal behavior
        delay(3000)
        showControlsState.value(!isPlaying)
    }

    LaunchedEffect(onSeek) {
        if (onSeek) {
            delay(500)
            onSeek = false
        }
    }


    DisposableEffect(lifecycleOwner) {
        val observer = object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                exoPlayer.pause()
                showControlsState.value(false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var hasShownNextEpisodeControls by remember(index) { mutableStateOf(false) }

    LaunchedEffect(currentPosition, currentPlayerModel) {
        val next = currentPlayerModel?.nextEpisode ?: return@LaunchedEffect

        if (
            !hasShownNextEpisodeControls &&
            !currentPlayerModel.isLive &&
            next.enableNextEpisode &&
            currentPosition >= timeToMillis(duration.toString(), next.showBeforeEndMs)
        ) {
            hasShownNextEpisodeControls = true

            // 🔥 FORCE SHOW CONTROLS EXACTLY AT startTime
            showControlsState.value(true)
        }
    }

    val playerModel = playerModelList?.getOrNull(index)


    /* ---------------- UI (UNCHANGED) ---------------- */

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(16.dp)
    ) {

        /* ---- TOP BAR ---- */
        AnimatedVisibility(
            visible = isControlsVisible,
            modifier = Modifier.align(Alignment.TopCenter),

            enter = slideInVertically(
                initialOffsetY = { -it },   // 🔥 start ABOVE the screen
                animationSpec = tween(
                    durationMillis = 700,
                    easing = LinearOutSlowInEasing
                )
            ) + fadeIn(
                animationSpec = tween(
                    durationMillis = 600,
                    delayMillis = 100,
                    easing = LinearEasing
                )
            ),

            exit = slideOutVertically(
                targetOffsetY = { -it },    // 🔥 slide back UP
                animationSpec = tween(
                    durationMillis = 600,
                    easing = FastOutLinearInEasing
                )
            ) + fadeOut(
                animationSpec = tween(
                    durationMillis = 400,
                    easing = LinearEasing
                )
            )
        ) {
            TopBar(
                playerModel = playerModel,
                isFullScreen = isCurrentlyFullScreen,
                context = context,
                castUtils = castUtils,
                pipListener = pipListener,
                isPipEnabled = isPipEnabled,
                onBackPressed = onBackPressed,
                onSettingsClick = {
                    exoPlayer.pause()
                    showControlsState.value(true)
                    onSettingsButtonClick(true)
                },
                onFullScreenToggle = {
                    fullScreenState.value(!isCurrentlyFullScreen)
                },
                onLockScreenToggle = {
                    lockScreenState.value(!isCurrentlyLockScreen)
                },
                onChapterClick = {
                    onChapterClick()
                },
                onCutClick = {
                    onCutClick()

                }
            )
        }

        /* ---------- CENTER AREA ---------- */

        if (isCurrentlyFullScreen) {

            Row(modifier = Modifier.fillMaxSize()) {

                /* ---- BRIGHTNESS (LEFT) ---- */
                AnimatedVisibility(
                    visible = isControlsVisible,
                    modifier = Modifier
                        .weight(0.1f)
                        .fillMaxHeight(),

                    enter = slideInHorizontally(
                        initialOffsetX = { it },   // 🔥 Start from RIGHT outside screen
                        animationSpec = tween(
                            durationMillis = 600,
                            easing = LinearOutSlowInEasing
                        )
                    ) + fadeIn(
                        animationSpec = tween(
                            durationMillis = 500,
                            easing = LinearEasing
                        )
                    ),

                    exit = slideOutHorizontally(
                        targetOffsetX = { -it },   // 🔥 Move to LEFT when hiding
                        animationSpec = tween(
                            durationMillis = 500,
                            easing = FastOutLinearInEasing
                        )
                    ) + fadeOut(
                        animationSpec = tween(
                            durationMillis = 300,
                            easing = LinearEasing
                        )
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.1f),
                        contentAlignment = Alignment.Center
                    ) {

                        CustomBrightnessController(
                            playerModel = playerModel,
                            onShowControls = showControlsState.value
                        )
                    }
                }


                /* Box(
                     modifier = Modifier
                         .weight(0.1f)
                         .fillMaxHeight(),
                     contentAlignment = Alignment.Center
                 ) {
                     CustomBrightnessController(
                         playerModel = playerModel,
                         onShowControls = showControlsState.value
                     )
                 }*/

                /* ---- CENTER CONTROLS ---- */
                Box(modifier = Modifier.weight(0.8f)) {
                    CenterControls(
                        playerModel = playerModel,
                        isLoading = isLoading,
                        exoPlayer = exoPlayer,
                        castUtils = castUtils,
                        isCasting = isCasting,
                        onShowControls = showControlsState.value,
                        onForward = { showForwardIcon = true },
                        onRewind = { showRewindIcon = true },
                        onForwardHide = { showForwardIcon = false },
                        onRewindHide = { showForwardIcon = false },
                        isZoomed = isZoomed,
                        onZoomChange = { isZoomed = it }
                    )
                }

                /* ---- VOLUME (RIGHT) ---- */
                AnimatedVisibility(
                    visible = isControlsVisible,
                    modifier = Modifier
                        .weight(0.1f)
                        .fillMaxHeight(),

                    enter = slideInHorizontally(
                        initialOffsetX = { -it },   // 🔥 Start from LEFT outside screen
                        animationSpec = tween(
                            durationMillis = 600,
                            easing = LinearOutSlowInEasing
                        )
                    ) + fadeIn(
                        animationSpec = tween(
                            durationMillis = 500,
                            easing = LinearEasing
                        )
                    ),

                    exit = slideOutHorizontally(
                        targetOffsetX = { it },     // 🔥 Move to RIGHT when hiding
                        animationSpec = tween(
                            durationMillis = 500,
                            easing = FastOutLinearInEasing
                        )
                    ) + fadeOut(
                        animationSpec = tween(
                            durationMillis = 300,
                            easing = LinearEasing
                        )
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(0.1f),
                        contentAlignment = Alignment.Center
                    ) {

                        CustomVolumeController(
                            playerModel = playerModel,
                            exoPlayer = exoPlayer,
                            onShowControls = showControlsState.value
                        )
                    }
                }

            }

        } else {

            /* ---- NON FULLSCREEN CENTER ---- */
            CenterControls(
                playerModel = playerModel,
                isLoading = isLoading,
                exoPlayer = exoPlayer,
                castUtils = castUtils,
                isCasting = isCasting,
                onShowControls = showControlsState.value,
                onForward = { showForwardIcon = true },
                onRewind = { showRewindIcon = true },
                onForwardHide = { showForwardIcon = false },
                onRewindHide = { showForwardIcon = false },
                isZoomed = isZoomed,
                onZoomChange = { isZoomed = it }
            )
        }

        /* ---------- SKIP INTRO BUTTON ---------- */

        AnimatedVisibility(
            visible = showSkipIntro,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = if (isCurrentlyFullScreen) 75.dp else 45.dp, start = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                    .clickable {
                        onSkipIntroClicked(true)
                        currentPlayerModel?.skipIntro?.endTime?.let { endTime ->
                            if (isCasting) castUtils.seekOnCast(endTime)
                            else exoPlayer.seekTo(endTime)
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Skip Intro",
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        /* ---------- Next Episode BUTTON ---------- */

        AnimatedVisibility(
            visible = showNextEpisode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = if (isCurrentlyFullScreen) 75.dp else 45.dp)
        ) {
            if (playerModelList != null && playerModelList.size > 1) {
                val isLastItem = index >= playerModelList.lastIndex

                Box(
                    modifier = Modifier
                        .background(Color.Gray, RoundedCornerShape(4.dp))
                        .clickable(enabled = !isLastItem) {
                            if (!isLastItem) {
                                nextEpisodeClicked = true
                                hasShownNextEpisodeControls = false // 👈 RESET
                                onNextEpisodeClick(index + 1)
                            }
                        }
                ) {

                    // 🔥 Animated progress overlay (LEFT → RIGHT)
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(4.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress) // 👈 animation happens here
                                .background(Color.White)
                        )
                    }

                    // Text on top
                    Text(
                        text = "Next Episode",
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .align(Alignment.Center),
                        color = Color.Black,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }


        /* ---------- BOTTOM CONTROLS ---------- */

        AnimatedVisibility(
            visible = isControlsVisible,
            modifier = Modifier.align(Alignment.BottomCenter),

            enter = slideInVertically(
                initialOffsetY = { it },   // from bottom
                animationSpec = tween(
                    durationMillis = 700,
                    easing = LinearOutSlowInEasing   // smooth start, slow end
                )
            ) + fadeIn(
                animationSpec = tween(
                    durationMillis = 600,
                    delayMillis = 100,
                    easing = LinearEasing
                )
            ),

            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(
                    durationMillis = 600,
                    easing = FastOutLinearInEasing   // smooth exit
                )
            ) + fadeOut(
                animationSpec = tween(
                    durationMillis = 400,
                    easing = LinearEasing
                )
            )
        ) {
            BottomControls(
                modifier = Modifier.align(Alignment.BottomCenter),
                playerModelList = playerModelList,
                index = index,
                isFullScreen = isCurrentlyFullScreen,
                currentPosition = currentPosition,
                duration = duration,
                exoPlayer = exoPlayer,
                onSeek = {
                    onSeek = true
                    showControlsState.value(true)
                    if (isCasting) castUtils.seekOnCast(it)
                    else exoPlayer.seekTo(it)
                },
                onNext = playContent,
                cuePoints = cuePoints,
                onDragStateChange = { dragging ->
                    isDraggingSeekbar = dragging
                    if (dragging) {
                        showControlsState.value(true)
                    }
                },
                expandSheet = {
                    expandSheet = it
                }
            )
        }

        if (expandSheet) {
            EpisodeSelectionSheet(
                expandSheet = true,
                playerModelList = playerModelList,
                isCasting = isCasting,
                exoPlayer = exoPlayer,
                onDismiss = { expandSheet = false },
                onShowControls = onShowControls,
                playContent = playContent
            )

        }
    }
}