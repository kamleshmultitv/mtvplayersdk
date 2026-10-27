package com.app.videosdk.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer
import com.app.videosdk.model.PlayerControlsConfig
import com.app.videosdk.model.PlayerModel
import kotlin.math.roundToInt

@Composable
fun CustomVolumeController(
    playerModel: PlayerModel? = null,
    exoPlayer: ExoPlayer,
    modifier: Modifier = Modifier,
    onShowControls: (Boolean) -> Unit = {},
    controlsConfig: PlayerControlsConfig = PlayerControlsConfig()
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var systemVolume by remember { mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()) }
    var isMuted by rememberSaveable { mutableStateOf(exoPlayer.volume == 0f) }

    val volumePercentage = (systemVolume * 100 / maxVolume).toInt().coerceIn(0, 100)
    val isDragging = remember { mutableStateOf(false) }

    // Ensure ExoPlayer respects the mute state
    LaunchedEffect(systemVolume, isMuted) {
        exoPlayer.volume = if (isMuted) 0f else systemVolume / maxVolume
    }

    // BroadcastReceiver to detect system volume changes
    DisposableEffect(context) {
        val volumeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                    val newVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
                    systemVolume = newVolume
                    if (newVolume > 0) isMuted = false // Unmute when volume is increased
                }
            }
        }

        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        context.registerReceiver(volumeReceiver, filter)

        onDispose {
            context.unregisterReceiver(volumeReceiver)
        }
    }

    // Gesture detection for vertical drag (volume control)
    val dragGesture = Modifier.pointerInput(maxVolume) {
        detectVerticalDragGestures(
            onDragStart = {
                isDragging.value = true
                onShowControls(true)
            },
            onDragEnd = {
                isDragging.value = false
                onShowControls(true)
            },
            onDragCancel = {
                isDragging.value = false
                onShowControls(true)
            },
            onVerticalDrag = { _, dragAmount ->
                // Dragging up (negative dragAmount) increases volume.
                // Full vertical bar height (~150dp / ~300px) maps to full maxVolume range.
                val delta = (dragAmount / 150f) * maxVolume
                val newVolume = (systemVolume - delta).coerceIn(0f, maxVolume.toFloat())

                if (newVolume != systemVolume) {
                    systemVolume = newVolume
                    val targetStreamVolume = newVolume.roundToInt()
                    val currentStreamVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    if (targetStreamVolume != currentStreamVolume) {
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetStreamVolume, 0)
                    }
                    if (newVolume > 0f) isMuted = false
                }
                onShowControls(true)
            }
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .background(Color.Transparent)
            .padding(16.dp)
            .size(60.dp, 200.dp)
            .then(dragGesture)
    ) {
        val canToggleVolume = if (isMuted) controlsConfig.unmute else controlsConfig.mute
        val progressColor = customControlTintColor(
            tintRes = playerModel?.customControls?.iconTintRes,
            fallback = Color.Green
        )

        if (canToggleVolume) {
            IconButton(
                modifier = Modifier.wrapContentSize()
                    .size(24.dp),
                onClick = {
                    onShowControls(true)
                    isMuted = !isMuted
                    exoPlayer.volume = if (isMuted) 0f else systemVolume / maxVolume
                }
            ) {
                CustomIcon(
                    resId = if (isMuted) playerModel?.customControls?.muteIconRes else playerModel?.customControls?.unMuteIconRes,
                    iconUrl = if (isMuted) playerModel?.customControls?.muteIconUrl else playerModel?.customControls?.unMuteIconUrl,
                    defaultIcon = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = if (isMuted) "Unmute" else "Mute",
                    modifier = Modifier.size(24.dp),
                    tint = playerModel?.customControls?.iconTintRes,
                    tintColorInt = playerModel?.customControls?.iconTintColorInt,
                    tintHex = playerModel?.customControls?.iconTintHex
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Volume bar
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(120.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(progressColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(1f - (systemVolume / maxVolume))
                    .background(Color.Gray, shape = RoundedCornerShape(4.dp))
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        // Volume percentage
        Text(
            text = "$volumePercentage%",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}
