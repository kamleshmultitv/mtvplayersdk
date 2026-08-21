package com.app.reelssdk.ui

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.media3.common.util.UnstableApi
import com.app.reelssdk.listener.ReelsPlayerStateListener
import com.app.reelssdk.model.ReelsPlayerConfig
import com.app.reelssdk.model.ReelsPlayerModel
import com.app.reelssdk.ui.reels.ReelsContainer

@OptIn(UnstableApi::class)
@Composable
fun MtvReelsPlayerSdk(
    contentList: List<ReelsPlayerModel>? = null,
    index: Int? = 0,
    startInFullScreen: Boolean = false,
    playerStateListener: ReelsPlayerStateListener? = null,
    onPlayerBack: (Boolean) -> Unit = {},
    setFullScreen: (Boolean) -> Unit = {},
    playerConfig: ReelsPlayerConfig = ReelsPlayerConfig()
) {
    ReelsContainer(
        contentList = contentList,
        initialIndex = index,
        playerStateListener = playerStateListener,
        onPlayerBack = onPlayerBack,
        setFullScreen = setFullScreen,
        startInFullScreen = startInFullScreen,
        controlsConfig = playerConfig.controls
    )
}
