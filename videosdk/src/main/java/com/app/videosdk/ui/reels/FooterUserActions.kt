package com.app.videosdk.ui.reels

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.videosdk.model.PlayerControlsConfig
import com.app.videosdk.model.PlayerCustomControls


@Composable
fun FooterUserActions(
    customControls: PlayerCustomControls? = null,
    controlsConfig: PlayerControlsConfig = PlayerControlsConfig(),
    onLikeClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    isFullScreen: Boolean = true,
    onFullScreenClick: () -> Unit = {}
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(end = 12.dp)
    ) {

        if (controlsConfig.share) {
            UserActionWithText(
                resId = customControls?.shareIconRes,
                defaultIcon = Icons.Default.Share,
                text = "Share",
                iconTintRes = customControls?.iconTintRes,
                onClick = onShareClick
            )

            Spacer(modifier = Modifier.height(20.dp))
        }

        if (controlsConfig.settings) {
            UserActionWithText(
                resId = customControls?.settingsIconRes,
                defaultIcon = Icons.Default.Settings,
                text = "Settings",
                iconTintRes = customControls?.iconTintRes,
                onClick = onSettingsClick
            )

            Spacer(modifier = Modifier.height(20.dp))
        }

        val showFullScreenButton =
            if (isFullScreen) controlsConfig.exitFullscreen else controlsConfig.fullscreen

        if (showFullScreenButton) {
            UserActionWithText(
                resId = if (isFullScreen) {
                    customControls?.exitFullScreenIconRes
                } else {
                    customControls?.fullScreenIconRes
                },
                defaultIcon = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                text = if (isFullScreen) "Collapse" else "Expand",
                iconTintRes = customControls?.iconTintRes,
                onClick = onFullScreenClick
            )

            Spacer(modifier = Modifier.height(20.dp))
        }

        if (controlsConfig.bookmark) {
            UserActionWithoutText(
                resId = customControls?.bookmarkIconRes,
                defaultIcon = Icons.Default.BookmarkBorder,
                iconTintRes = customControls?.iconTintRes,
                contentDescription = "Bookmark",
                onClick = onLikeClick
            )
        }

        Spacer(modifier = Modifier.height(72.dp))
    }
}
