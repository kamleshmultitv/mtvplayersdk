package com.app.videosdk.ui

import android.net.Uri
import android.util.Log
import android.view.SurfaceView
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
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
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.SubtitleView
import com.app.videosdk.listener.PipListener
import com.app.videosdk.model.PlayerModel
import com.app.videosdk.utils.CastUtils
import com.app.videosdk.utils.PlayerUtils.createExoPlayer
import com.app.videosdk.utils.PlayerUtils.getMimeTypeFromExtension
import com.google.android.gms.cast.framework.CastContext
import androidx.core.util.Consumer

@OptIn(UnstableApi::class)
@Composable
public fun MtvVideoPlayerSdk(
    contentList: List<PlayerModel>? = null,
    index: Int? = 0,
    pipListener: PipListener? = null,
    onPlayerBack: (Boolean) -> Unit,
    setFullScreen: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current

    remember {
        try {
            CastContext.getSharedInstance(context)
        } catch (e: Exception) {
            null
        }
    }

    /* ---------------- SAFE INDEX ---------------- */

    val selectedIndex = remember { mutableIntStateOf(index ?: 0) }

    LaunchedEffect(index) {
        if (index != null) {
            selectedIndex.intValue = index
        }
    }

    val playerModel = contentList?.getOrNull(selectedIndex.intValue)

    /* ---------------- PLAYER STATE ---------------- */

    var isFullScreen by remember { mutableStateOf(false) }
    var isControllerVisible by remember { mutableStateOf(true) } 
    var pipEnabled by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var keepScreenOn by remember { mutableStateOf(false) }
    var isSettingsClick by remember { mutableStateOf(false) }

    // Default to Normal (not zoomed)
    var isZoomed by remember { mutableStateOf(false) }

    /* ---------------- PIP LISTENER ---------------- */

    DisposableEffect(context) {
        val activity = context as? ComponentActivity
        val pipModeListener = Consumer<PictureInPictureModeChangedInfo> { info ->
            pipEnabled = info.isInPictureInPictureMode
            if (!info.isInPictureInPictureMode) {
                isControllerVisible = true
            }
        }

        activity?.addOnPictureInPictureModeChangedListener(pipModeListener)
        onDispose {
            activity?.removeOnPictureInPictureModeChangedListener(pipModeListener)
        }
    }

    /* ---------------- EXOPLAYER ---------------- */

    val exoPlayer = remember {
        createExoPlayer(context, "", null, "")
    }

    LaunchedEffect(selectedIndex.intValue, contentList) {
        val model = contentList?.getOrNull(selectedIndex.intValue) ?: return@LaunchedEffect
        val videoUrl = if (model.isLive) model.liveUrl.orEmpty() else model.mpdUrl.orEmpty()
        val cleanUrl = videoUrl.substringBefore("?")

        val mediaItemBuilder = MediaItem.Builder()
            .setUri(videoUrl)
            .setMimeType(
                when {
                    cleanUrl.endsWith(".mpd", ignoreCase = true) -> MimeTypes.APPLICATION_MPD
                    cleanUrl.endsWith(".m3u8", ignoreCase = true) -> MimeTypes.APPLICATION_M3U8
                    else -> MimeTypes.VIDEO_MP4
                }
            )

        if (model.isLive) {
            mediaItemBuilder.setLiveConfiguration(
                MediaItem.LiveConfiguration.Builder().setTargetOffsetMs(5000).build()
            )
        }

        val srtUrl = if (getMimeTypeFromExtension(model.hlsUrl.toString())) "" else model.srt.orEmpty()
        if (srtUrl.isNotBlank()) {
            val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(Uri.parse(srtUrl))
                .setMimeType(MimeTypes.APPLICATION_SUBRIP)
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()
            mediaItemBuilder.setSubtitleConfigurations(listOf(subtitleConfig))
        }

        if (!model.isLive) {
            model.drmToken?.let {
                mediaItemBuilder.setDrmConfiguration(
                    MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
                        .setLicenseUri(it)
                        .build()
                )
            }
        }

        exoPlayer.setMediaItem(mediaItemBuilder.build())
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        exoPlayer.repeatMode = Player.REPEAT_MODE_OFF

        // FORCED: Reset scaling mode to Normal (Fit) whenever new content starts
        exoPlayer.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
    }

    val castUtils = remember(context, exoPlayer) {
        CastUtils(context, exoPlayer)
    }
    val isCasting by remember { derivedStateOf { castUtils.isCasting() } }

    /* ---------------- PLAYER LISTENER ---------------- */

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isLoading = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY && !isCasting && exoPlayer.playWhenReady) {
                    exoPlayer.play()
                }
                if (state == Player.STATE_ENDED) {
                    val lastIndex = contentList?.lastIndex ?: 0
                    if (selectedIndex.intValue < lastIndex) {
                        selectedIndex.intValue++
                    }
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                Log.e("MtvPlayer", "Playback error: ${error.message}")
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
                if (!pipEnabled) {
                    isControllerVisible = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(isZoomed) {
        exoPlayer.videoScalingMode = if (isZoomed) {
            C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
        } else {
            C.VIDEO_SCALING_MODE_SCALE_TO_FIT
        }
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
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    isZoomed = zoom > 1f
                }
            }
    ) {
        AndroidView(
            factory = { ctx ->
                val root = FrameLayout(ctx)
                val surfaceView = SurfaceView(ctx)
                val subtitleView = SubtitleView(ctx).apply {
                    setUserDefaultStyle()
                    setUserDefaultTextSize()
                }

                root.addView(surfaceView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
                root.addView(subtitleView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

                exoPlayer.setVideoSurfaceView(surfaceView)
                exoPlayer.addListener(object : Player.Listener {
                    override fun onCues(cueGroup: CueGroup) {
                        subtitleView.setCues(cueGroup.cues)
                    }
                })

                root
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            if (!isSettingsClick && !pipEnabled) {
                                isControllerVisible = !isControllerVisible
                            }
                        },
                        onDoubleTap = { offset ->
                            val model = contentList?.getOrNull(selectedIndex.intValue)
                            if (!pipEnabled && model?.isLive == false) {
                                val isLeft = offset.x < size.width / 2
                                val seekBy = if (isLeft) -10_000 else 10_000
                                val position = if (isCasting) castUtils.getCastPosition() else exoPlayer.currentPosition
                                val newPosition = (position + seekBy).coerceAtLeast(0)

                                if (isCasting) castUtils.seekOnCast(newPosition)
                                else exoPlayer.seekTo(newPosition)
                            }
                        }
                    )
                },
            update = { root ->
                val surfaceView = root.getChildAt(0) as SurfaceView
                surfaceView.keepScreenOn = keepScreenOn
                // FORCED: Reset scaling mode to Normal Fit on every update if not explicitly zoomed
                if (!isZoomed) {
                    exoPlayer.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                }
            }
        )

        AnimatedVisibility(
            visible = !pipEnabled && isControllerVisible && !isSettingsClick,
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
                onShowControls = {
                    if (!pipEnabled) isControllerVisible = it
                },
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

        if (!isControllerVisible && isLoading && !pipEnabled) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center).size(55.dp),
                color = Color.White
            )
        }

        if (isSettingsClick && !pipEnabled) {
            SelectorHeader(exoPlayer) { isSettingsClick = it }
        }
    }
}
