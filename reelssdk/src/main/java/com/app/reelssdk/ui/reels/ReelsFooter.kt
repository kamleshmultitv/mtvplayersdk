package com.app.reelssdk.ui.reels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.reelssdk.model.ReelsPlayerControlsConfig
import com.app.reelssdk.model.ReelsPlayerCustomControls

@Composable
fun ReelsFooter(
    modifier: Modifier = Modifier,
    customControls: ReelsPlayerCustomControls? = null,
    controlsConfig: ReelsPlayerControlsConfig = ReelsPlayerControlsConfig(),
    onLikeClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    isFullScreen: Boolean = true,
    onFullScreenClick: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(start = 18.dp, bottom = 18.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.End
    ) {
        FooterUserActions(
            customControls = customControls,
            controlsConfig = controlsConfig,
            onLikeClick = onLikeClick,
            onShareClick = onShareClick,
            onSettingsClick = onSettingsClick,
            isFullScreen = isFullScreen,
            onFullScreenClick = onFullScreenClick
        )
    }
}
