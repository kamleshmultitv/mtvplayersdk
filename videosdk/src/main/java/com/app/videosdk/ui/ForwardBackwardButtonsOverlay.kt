package com.app.videosdk.ui
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.app.videosdk.model.PlayerCustomControls
import com.app.videosdk.model.PlayerControlsConfig
import com.app.videosdk.model.PlayerModel
import com.app.videosdk.utils.CastUtils
import kotlinx.coroutines.delay

@Composable
fun ForwardBackwardButtonsOverlay(
    playerModel: PlayerModel? = null,
    exoPlayer: ExoPlayer,
    castUtils: CastUtils?,
    isCasting: Boolean,
    onRewindIconHide: () -> Unit,
    onForwardIconHide: () -> Unit,
    onSeekStarted: (Long) -> Unit = {},
    onSeekCompleted: (Long) -> Unit = {},
    isControllerVisible: Boolean,
    isFullScreen: Boolean,
    isLoading: Boolean = false,
    controlsConfig: PlayerControlsConfig = PlayerControlsConfig()
) {
    val customControls = playerModel?.customControls

    /* ▶️ Player state */
    var isPlaying by remember { mutableStateOf(exoPlayer.isPlaying) }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    /* 🔁 Animation triggers (FIX) */
    var rewindAnimTrigger by remember { mutableIntStateOf(0) }
    var forwardAnimTrigger by remember { mutableIntStateOf(0) }

    /* 🔄 Animations */
    val rewindRotation by animateFloatAsState(
        targetValue = if (rewindAnimTrigger > 0) -90f else 0f,
        animationSpec = tween(300),
        label = "rewindRotation"
    )

    val forwardRotation by animateFloatAsState(
        targetValue = if (forwardAnimTrigger > 0) 90f else 0f,
        animationSpec = tween(300),
        label = "forwardRotation"
    )

    /* 🎮 UI */
    ForwardBackwardButtonsOverlayUi(
        isPlaying = isPlaying,
        isCasting = isCasting,
        customControls = customControls,
        controlsConfig = controlsConfig,
        isLoading = isLoading,
        onRewind = {
            if (controlsConfig.seekBack.enabled) {
                rewindAnimTrigger++

                val current =
                    if (isCasting && castUtils != null) castUtils.getCastPosition()
                    else exoPlayer.currentPosition
                val newPosition = maxOf(
                    current - (controlsConfig.seekBack.safeSeconds * 1000L),
                    0
                )
                onSeekStarted(current)
                if (isCasting && castUtils != null) castUtils.seekOnCast(newPosition)
                else exoPlayer.seekTo(newPosition)
                onSeekCompleted(newPosition)
            }
        },
        onForward = {
            if (controlsConfig.seekForward.enabled) {
                forwardAnimTrigger++

                val duration =
                    if (isCasting && castUtils != null) castUtils.getCastDuration()
                    else exoPlayer.duration

                val current =
                    if (isCasting && castUtils != null) castUtils.getCastPosition()
                    else exoPlayer.currentPosition
                val newPosition = minOf(
                    current + (controlsConfig.seekForward.safeSeconds * 1000L),
                    duration
                )
                onSeekStarted(current)
                if (isCasting && castUtils != null) castUtils.seekOnCast(newPosition)
                else exoPlayer.seekTo(newPosition)
                onSeekCompleted(newPosition)
            }
        },
        onPlayPause = {
            if (isPlaying) {
                if (isCasting && castUtils != null) castUtils.pauseCasting()
                else exoPlayer.pause()
            } else {
                if (isCasting && castUtils != null) castUtils.playCasting()
                else exoPlayer.play()
            }
        },
        onRewindIconHide = onRewindIconHide,
        onForwardIconHide = onForwardIconHide,
        isControllerVisible = isControllerVisible,
        playPauseIconSize = if (isFullScreen) 48.dp else 34.dp,
        seekIconSize = if (isFullScreen) 48.dp else 36.dp,
        rewindRotation = rewindRotation,
        forwardRotation = forwardRotation
    )


    /* 🔁 Auto reset animations */
    LaunchedEffect(rewindAnimTrigger) {
        if (rewindAnimTrigger > 0) {
            delay(300)
            rewindAnimTrigger = 0
            onRewindIconHide()
        }
    }

    LaunchedEffect(forwardAnimTrigger) {
        if (forwardAnimTrigger > 0) {
            delay(300)
            forwardAnimTrigger = 0
            onForwardIconHide()
        }
    }
}

@Composable
private fun ForwardBackwardButtonsOverlayUi(
    isPlaying: Boolean,
    isCasting: Boolean,
    customControls: PlayerCustomControls?,
    controlsConfig: PlayerControlsConfig,
    isLoading: Boolean,
    onRewind: () -> Unit,
    onForward: () -> Unit,
    onPlayPause: () -> Unit,
    onRewindIconHide: () -> Unit,
    onForwardIconHide: () -> Unit,
    isControllerVisible: Boolean,
    playPauseIconSize: Dp,
    seekIconSize: Dp,
    rewindRotation: Float,
    forwardRotation: Float
) {
    val rewindInteractionSource = remember { MutableInteractionSource() }
    val playPauseInteractionSource = remember { MutableInteractionSource() }
    val forwardInteractionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            /* ⏪ Rewind */
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (controlsConfig.seekBack.enabled) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clickable(
                                interactionSource = rewindInteractionSource,
                                indication = null,
                                onClick = onRewind
                            ),
                        contentAlignment = Alignment.Center
                    ) {

                        CustomIcon(
                            resId = customControls?.rewindIconRes,
                            iconUrl = customControls?.rewindIconUrl,
                            defaultIcon = Icons.Default.Replay10,
                            contentDescription = "Rewind ${controlsConfig.seekBack.safeSeconds}s",
                            modifier = Modifier
                                .size(seekIconSize)
                                .graphicsLayer(rotationZ = rewindRotation),
                            tint = customControls?.iconTintRes,
                            tintColorInt = customControls?.iconTintColorInt,
                            tintHex = customControls?.iconTintHex
                        )
                    }
                }
            }

            /* ▶️ Play / Pause */
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (isControllerVisible && ((isPlaying && controlsConfig.pause) || (!isPlaying && controlsConfig.play))) {
                    val containerSize = playPauseIconSize + 16.dp
                    Box(
                        modifier = Modifier
                            .size(containerSize)
                            .clickable(
                                interactionSource = playPauseInteractionSource,
                                indication = null,
                                onClick = onPlayPause
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularLoader(
                                modifier = Modifier.size(playPauseIconSize + 10.dp),
                                color = Color.White,
                                strokeWidth = 3.dp
                            )
                        }

                        CustomIcon(
                            resId = if (isPlaying) customControls?.pauseIconRes else customControls?.playIconRes,
                            iconUrl = if (isPlaying) customControls?.pauseIconUrl else customControls?.playIconUrl,
                            defaultIcon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            modifier = Modifier.size(playPauseIconSize),
                            tint = customControls?.iconTintRes,
                            tintColorInt = customControls?.iconTintColorInt,
                            tintHex = customControls?.iconTintHex
                        )
                    }
                }
            }

            /* ⏩ Forward */
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (controlsConfig.seekForward.enabled) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clickable(
                                interactionSource = forwardInteractionSource,
                                indication = null,
                                onClick = onForward
                            ),
                        contentAlignment = Alignment.Center
                    ) {

                        CustomIcon(
                            resId = customControls?.forwardIconRes,
                            iconUrl = customControls?.forwardIconUrl,
                            defaultIcon = Icons.Default.Forward10,
                            contentDescription = "Forward ${controlsConfig.seekForward.safeSeconds}s",
                            modifier = Modifier
                                .size(seekIconSize)
                                .graphicsLayer(rotationZ = forwardRotation),
                            tint = customControls?.iconTintRes,
                            tintColorInt = customControls?.iconTintColorInt,
                            tintHex = customControls?.iconTintHex
                        )
                    }
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun ForwardBackwardButtonsOverlayPreview() {
    // Mock states for preview
    var isPlaying by remember { mutableStateOf(true) }
    val rewindRotation by animateFloatAsState(targetValue = 0f, animationSpec = tween(300), label = "rewindRotation")
    val forwardRotation by animateFloatAsState(targetValue = 0f, animationSpec = tween(300), label = "forwardRotation")
    val customControls = PlayerCustomControls(iconTintRes = null) // Minimal controls

    ForwardBackwardButtonsOverlayUi(
        isPlaying = isPlaying,
        isCasting = false, // Not casting for preview
        customControls = customControls,
        controlsConfig = PlayerControlsConfig(),
        isLoading = false,
        onRewind = { /* mock seek back */ },
        onForward = { /* mock seek forward */ },
        onPlayPause = { isPlaying = !isPlaying },
        onRewindIconHide = { /* mock hide */ },
        onForwardIconHide = { /* mock hide */ },
        isControllerVisible = true,
        playPauseIconSize = 48.dp,
        seekIconSize = 48.dp,
        rewindRotation = rewindRotation,
        forwardRotation = forwardRotation
    )
}

@Composable
private fun CircularLoader(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    strokeWidth: Dp = 3.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "circularLoader")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loaderRotation"
    )

    val strokeWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) { strokeWidth.toPx() }

    Box(
        modifier = modifier.graphicsLayer(rotationZ = rotation),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val diameter = size.minDimension - strokeWidthPx
            val topLeft = Offset(
                (size.width - diameter) / 2f,
                (size.height - diameter) / 2f
            )
            val arcSize = Size(diameter, diameter)

            // Draw 75% arc (270 degrees sweep angle out of 360 = 75% cover)
            drawArc(
                color = color,
                startAngle = 0f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(
                    width = strokeWidthPx,
                    cap = StrokeCap.Round
                )
            )
        }
    }
}
