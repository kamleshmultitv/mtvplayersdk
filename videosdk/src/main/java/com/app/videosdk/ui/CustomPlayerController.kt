package com.app.videosdk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.exoplayer.ExoPlayer
import com.app.videosdk.model.PlayerModel
import com.app.videosdk.listener.PipListener
import com.app.videosdk.utils.CastUtils
import kotlinx.coroutines.delay

@Composable
fun CustomPlayerController(
    playerModelList: List<PlayerModel>? = null,
    index: Int,
    pipListener: PipListener,
    isFullScreen: (Boolean) -> Unit,
    isCurrentlyFullScreen: Boolean,
    exoPlayer: ExoPlayer,
    modifier: Modifier = Modifier,
    onShowControls: (Boolean) -> Unit,
    isPipEnabled: (Boolean) -> Unit = {},
    onSettingsButtonClick: (Boolean) -> Unit = {},
    isLoading: Boolean,
    onBackPressed: () -> Unit = {},
    playContent: (Int) -> Unit
) {
    var isZoomed by remember { mutableStateOf(false) }
    var showForwardIcon by remember { mutableStateOf(false) }
    var showRewindIcon by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    val castUtils = remember { CastUtils(context, exoPlayer) }
    val isCasting = castUtils.isCasting()

    LaunchedEffect(exoPlayer.isPlaying) {
        delay(3000L)
        if (exoPlayer.isPlaying) {
            onShowControls(false)
        } else {
            onShowControls(true)
        }
    }

    // Periodically update playback state
    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPosition =
                if (isCasting) castUtils.getCastPosition() else exoPlayer.currentPosition
            duration =
                if (isCasting) castUtils.getCastDuration() else exoPlayer.duration.takeIf { it > 0 }
                    ?: 0L
            isPlaying = exoPlayer.isPlaying
            delay(1000L) // Update every second
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                super.onStop(owner)
                exoPlayer.pause() // Pause player when app goes to background
                onShowControls(false)

            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = if (isCurrentlyFullScreen) 32.dp else 0.dp
            )
    )
    {

        // Control buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(top = 16.dp)
                .background(Color.Transparent)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically // Vertically align items
        ) {
            // Back Button
            IconButton(
                onClick = onBackPressed,
                modifier = Modifier
                    .padding(start = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Text(
                text = playerModelList?.getOrNull(index)?.title ?: "No Title",
                style = TextStyle(
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )


            if (isCurrentlyFullScreen) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (castUtils.isCastTVAvailable(context)) {
                        CastButton()
                    }

                    // Picture-in-Picture Button
                    PipButton(pipListener = pipListener, isPipEnabled = isPipEnabled)

                    // Settings Button
                    IconButton(
                        modifier = Modifier.wrapContentSize(),
                        onClick = {
                            exoPlayer.pause()
                            isPlaying = !isPlaying
                            onShowControls(true)
                            onSettingsButtonClick(true)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings Track",
                            tint = Color.White
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.TopEnd
                ) {

                    if (castUtils.isCastTVAvailable(context)) {
                        CastButton()
                    }
                }
            }
        }

        // brightness, volume, play/pause, rewind, forward buttons
        if (isCurrentlyFullScreen) {
            Row(
                modifier = Modifier
                    .wrapContentSize()
            ) {
                // Brightness Control (Left Edge)
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .weight(.1f)// Set specific width for interaction
                        .fillMaxHeight()
                ) {
                    CustomBrightnessController(
                        modifier = Modifier.align(Alignment.Center),
                        onShowControls = { onShowControls(it) }
                    )
                }

                // play pause and rewind button
                Box(
                    modifier = Modifier
                        .weight(.8f)
                        .padding(top = 80.dp, bottom = 80.dp)
                        .pointerInput(Unit) {
                            detectTransformGestures { _, _, zoom, _ ->
                                // Detect pinch zoom gesture by tracking zoom factor
                                if (zoom > 1f && !isZoomed) {
                                    isZoomed = true // Zoom in
                                } else if (zoom < 1f && isZoomed) {
                                    isZoomed = false // Zoom out
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    onShowControls(false)
                                },
                                onDoubleTap = { offset ->
                                    val isLeftSide = offset.x < size.width / 2
                                    val newCurrentPosition =
                                        if (isCasting) castUtils.getCastPosition() else exoPlayer.currentPosition
                                    val newPosition = maxOf(
                                        newCurrentPosition + if (isLeftSide) -10_000 else 10_000,
                                        0
                                    )

                                    // Apply seek for both ExoPlayer and Casting
                                    if (isCasting) {
                                        castUtils.seekOnCast(newPosition)
                                    } else {
                                        exoPlayer.seekTo(newPosition)
                                    }

                                    // Show corresponding feedback icon
                                    if (isLeftSide) {
                                        showRewindIcon = true
                                    } else {
                                        showForwardIcon = true
                                    }
                                }

                            )
                        }

                ) {
                    ForwardBackwardButtonsOverlay(
                        exoPlayer = exoPlayer,
                        context = context,
                        showRewindIcon = showRewindIcon,
                        showForwardIcon = showForwardIcon,
                        onRewindIconHide = { showRewindIcon = false },
                        onForwardIconHide = { showForwardIcon = false },
                        isControllerVisible = true
                    )

                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(55.dp),
                            color = Color.White
                        )
                    }
                }

                // Volume Control (Right Edge)
                Box(
                    contentAlignment = Alignment.CenterEnd,
                    modifier = Modifier
                        .weight(.1f) // Set specific width for interaction
                        .fillMaxHeight()
                ) {
                    CustomVolumeController(
                        exoPlayer,
                        modifier = Modifier.align(Alignment.Center),
                        onShowControls = { onShowControls(it) }
                    )
                }
            }
        } else {
            // play pause and rewind button
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 80.dp, bottom = 80.dp)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            // Detect pinch zoom gesture by tracking zoom factor
                            if (zoom > 1f && !isZoomed) {
                                isZoomed = true // Zoom in
                            } else if (zoom < 1f && isZoomed) {
                                isZoomed = false // Zoom out
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                onShowControls(false)
                            },
                            onDoubleTap = { offset ->
                                val isLeftSide = offset.x < size.width / 2
                                val newCurrentPosition =
                                    if (isCasting) castUtils.getCastPosition() else exoPlayer.currentPosition
                                val newPosition = maxOf(
                                    newCurrentPosition + if (isLeftSide) -10_000 else 10_000,
                                    0
                                )

                                // Apply seek for both ExoPlayer and Casting
                                if (isCasting) {
                                    castUtils.seekOnCast(newPosition)
                                } else {
                                    exoPlayer.seekTo(newPosition)
                                }

                                // Show corresponding feedback icon
                                if (isLeftSide) {
                                    showRewindIcon = true
                                } else {
                                    showForwardIcon = true
                                }
                            }

                        )
                    }

            ) {
                ForwardBackwardButtonsOverlay(
                    exoPlayer = exoPlayer,
                    context = context,
                    showRewindIcon = showRewindIcon,
                    showForwardIcon = showForwardIcon,
                    onRewindIconHide = { showRewindIcon = false },
                    onForwardIconHide = { showForwardIcon = false },
                    isControllerVisible = true
                )

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(55.dp),
                        color = Color.White
                    )
                }
            }
        }

        // Bottom Controls (Seek Bar)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            verticalArrangement = Arrangement.Bottom
        ) {
            // Seek Bar
            val spriteUrl = if (playerModelList != null && index in playerModelList.indices) {
                playerModelList[index].spriteUrl
            } else {
                null
            }
            CustomSlider(
                spriteUrl = spriteUrl,
                currentPosition = currentPosition,
                duration = duration,
                onSeek = { newPosition ->
                    onShowControls(true)
                    if (isCasting) {
                        castUtils.seekOnCast(newPosition)
                    } else {
                        exoPlayer.seekTo(newPosition)
                    }
                },
               showControls = { onShowControls(it) }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Season Selector - Center
                if (isCurrentlyFullScreen && playerModelList != null && playerModelList.size > 1) {
                    // Next
                    val isLastItem = index >= playerModelList.size - 1

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clickable(enabled = !isLastItem) { // Disable if last item
                                if (!isLastItem) {
                                    playContent(index + 1)
                                }
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Episode",
                            tint = if (isLastItem) Color.Gray else Color.White // Dim if last item
                        )
                        Text(
                            text = "Next Ep.",
                            color = if (isLastItem) Color.Gray else Color.White,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    SeasonSelector(
                        playerModelList = playerModelList,
                        exoPlayer,
                        onShowControls,
                        pausePlayer = { isPlaying = it },
                        playContent = playContent
                    )
                }

                // Spacer to push the third element to the end
                Spacer(modifier = Modifier.weight(1f))

                if (isCurrentlyFullScreen) {
                    // Full-Screen Toggle Button
                    IconButton(
                        modifier = Modifier.wrapContentSize(),
                        onClick = { isFullScreen(false) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FullscreenExit,
                            contentDescription = "Toggle Fullscreen",
                            tint = Color.White
                        )
                    }
                } else {
                    IconButton(
                        modifier = Modifier.wrapContentSize(),
                        onClick = { isFullScreen(true) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Toggle Fullscreen",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}