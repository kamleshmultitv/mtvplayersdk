package com.app.videosdk.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.videosdk.listener.PipListener
import com.app.videosdk.model.PlayerControlsConfig
import com.app.videosdk.model.PlayerModel
import com.app.videosdk.utils.CastUtils

@Composable
fun TopBar(
    playerModel: PlayerModel? = null,
    isFullScreen: Boolean = false,
    context: Context,
    castUtils: CastUtils?,
    pipListener: PipListener?,
    controlsConfig: PlayerControlsConfig = PlayerControlsConfig(),
    showContentTitle: Boolean = true,
    isAudioOnly: Boolean = false,
    showLowBandwidthHint: Boolean = false,
    isPipEnabled: (Boolean) -> Unit,
    onBackPressed: () -> Unit,
    onSettingsClick: () -> Unit,
    onFullScreenToggle: () -> Unit,
    onLockScreenToggle: () -> Unit,
    onChapterClick: () -> Unit,
    onCutClick: () -> Unit,
    onAudioModeToggle: () -> Unit = {}
) {
    val episodeTitle = playerModel?.episodeTitle?.takeIf { it.isNotBlank() }
    val seasonTitle = playerModel?.seasonTitle?.takeIf { it.isNotBlank() }
    val heading = episodeTitle
        ?: playerModel?.title?.takeIf { it.isNotBlank() }
        ?: seasonTitle
    val subheading = seasonTitle?.takeUnless { it == heading }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isFullScreen) {
                    Modifier.background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.70f),
                                Color.Black.copy(alpha = 0.35f),
                                Color.Transparent
                            )
                        )
                    )
                } else {
                    Modifier
                }
            )
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        IconButton(onClick = onBackPressed) {
            CustomIcon(
                resId = playerModel?.customControls?.backIconRes,
                iconUrl = playerModel?.customControls?.backIconUrl,
                defaultIcon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = playerModel?.customControls?.iconTintRes,
                tintColorInt = playerModel?.customControls?.iconTintColorInt,
                tintHex = playerModel?.customControls?.iconTintHex
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(start = 2.dp, end = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (showContentTitle) {
                Column {
                    heading?.let {
                        Text(
                            text = it,
                            color = Color.White,
                            fontSize = if (isFullScreen) 16.sp else 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (isFullScreen) {
                        subheading?.let {
                            Text(
                                text = it,
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }


        if (isFullScreen && playerModel?.isClipEnabled == true) {
            IconButton(onClick = onCutClick) {
                CustomIcon(
                    resId = playerModel.customControls?.cutIconRes,
                    iconUrl = playerModel.customControls?.cutIconUrl,
                    defaultIcon = Icons.Default.ContentCut,
                    contentDescription = "Cut",
                    modifier = Modifier.size(24.dp),
                    tint = playerModel.customControls?.iconTintRes,
                    tintColorInt = playerModel.customControls?.iconTintColorInt,
                    tintHex = playerModel.customControls?.iconTintHex
                )
            }
        }

        if (isFullScreen && playerModel?.isChapterEnabled == true) {
            IconButton(onClick = onChapterClick) {
                CustomIcon(
                    resId = playerModel.customControls?.chapterIconRes,
                    iconUrl = playerModel.customControls?.chapterIconUrl,
                    defaultIcon = Icons.Default.AutoStories,
                    contentDescription = "Chapter",
                    modifier = Modifier.size(24.dp),
                    tint = playerModel.customControls?.iconTintRes,
                    tintColorInt = playerModel.customControls?.iconTintColorInt,
                    tintHex = playerModel.customControls?.iconTintHex
                )
            }
        }


        if (isFullScreen && controlsConfig.cast && castUtils != null) {
            CastButton(
                resId = playerModel?.customControls?.castIconRes,
                connectedResId = playerModel?.customControls?.castConnectedIconRes,
                tint = playerModel?.customControls?.iconTintRes
            )
        }

        val showAudioOnlyToggle = playerModel?.enableAudioOnlyToggle ?: controlsConfig.audioOnlyToggle
        if (isFullScreen && showAudioOnlyToggle) {
            Box(
                modifier = Modifier.wrapContentSize()
            ) {
                AudioVideoToggleSwitch(
                    isAudioOnly = isAudioOnly,
                    onToggle = onAudioModeToggle,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                if (showLowBandwidthHint) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(y = 52.dp, x = 16.dp)
                            .width(230.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.92f)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Text(
                                text = "Switch to audio mode (Low network)",
                                color = Color.Yellow,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "You can manually toggle between audio and video anytime using this switch.",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // PIP and Settings remain visible only in FullScreen (VOD context)
        if (isFullScreen) {
            if (controlsConfig.pip && !isAudioOnly) {
                PipButton(
                    playerModel,
                    pipListener = pipListener,
                    isPipEnabled = isPipEnabled
                )
            }

            if (controlsConfig.settings) {
                IconButton(onClick = onSettingsClick) {
                    CustomIcon(
                        resId = playerModel?.customControls?.settingsIconRes,
                        iconUrl = playerModel?.customControls?.settingsIconUrl,
                        defaultIcon = Icons.Default.Settings,
                        contentDescription = "Settings",
                        modifier = Modifier.size(24.dp),
                        tint = playerModel?.customControls?.iconTintRes,
                        tintColorInt = playerModel?.customControls?.iconTintColorInt,
                        tintHex = playerModel?.customControls?.iconTintHex
                    )
                }
            }
        }

        if (isFullScreen) {
            IconButton(onClick = onLockScreenToggle) {
                CustomIcon(
                    resId = playerModel?.customControls?.lockIconRes,
                    iconUrl = playerModel?.customControls?.lockIconUrl,
                    defaultIcon = Icons.Default.Lock,
                    contentDescription = "Toggle Lock Screen",
                    modifier = Modifier.size(24.dp),
                    tint = playerModel?.customControls?.iconTintRes,
                    tintColorInt = playerModel?.customControls?.iconTintColorInt,
                    tintHex = playerModel?.customControls?.iconTintHex
                )
            }
        }

        // FIXED: Moved Fullscreen button OUTSIDE the if(isFullScreen) block 
        // to make it visible in Portrait mode.
        val showFullScreenButton =
            if (isFullScreen) controlsConfig.exitFullscreen else controlsConfig.fullscreen

        if (showFullScreenButton) {
            IconButton(
                onClick = onFullScreenToggle,
                modifier = Modifier.size(56.dp)
            ) {
                CustomIcon(
                    resId = if (isFullScreen) playerModel?.customControls?.exitFullScreenIconRes else playerModel?.customControls?.fullScreenIconRes,
                    iconUrl = if (isFullScreen) playerModel?.customControls?.exitFullScreenIconUrl else playerModel?.customControls?.fullScreenIconUrl,
                    defaultIcon = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = "Toggle Fullscreen",
                    modifier = Modifier.size(24.dp),
                    tint = playerModel?.customControls?.iconTintRes,
                    tintColorInt = playerModel?.customControls?.iconTintColorInt,
                    tintHex = playerModel?.customControls?.iconTintHex
                )
            }
        }
    }
}
