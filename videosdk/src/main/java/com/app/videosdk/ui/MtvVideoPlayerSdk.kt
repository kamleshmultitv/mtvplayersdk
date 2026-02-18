package com.app.videosdk.ui

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.media3.common.util.UnstableApi
import com.app.videosdk.listener.PipListener
import com.app.videosdk.listener.PlayerStateListener
import com.app.videosdk.model.PlayerModel
import com.app.videosdk.ui.ott.OttContainer
import com.app.videosdk.ui.reels.ReelsContainer
import com.app.videosdk.utils.PlayerMode

@OptIn(UnstableApi::class)
@Composable
fun MtvVideoPlayerSdk(
    contentList: List<PlayerModel>? = null,
    index: Int? = 0,
    mode: PlayerMode = PlayerMode.OTT,
    pipListener: PipListener? = null,
    isInPipMode: Boolean = false,
    startInFullScreen: Boolean = false,
    playerStateListener: PlayerStateListener? = null,
    onVideoEnded: (() -> Unit)? = null,
    onPlayerBack: (Boolean) -> Unit = {},     // ✅ default
    setFullScreen: (Boolean) -> Unit = {},     // ✅ default
) {
    if (mode == PlayerMode.REELS) {
        ReelsContainer(
            contentList = contentList,
            pipListener = pipListener,
            isInPipMode = isInPipMode,
            playerStateListener = playerStateListener,
            setFullScreen = setFullScreen,
            startInFullScreen = true,
            playerMode= mode
        )
    } else {
        OttContainer(
            contentList = contentList,
            index = index,
            pipListener = pipListener,
            isInPipMode = isInPipMode,
            playerStateListener = playerStateListener,
            onPlayerBack = onPlayerBack,
            setFullScreen = setFullScreen,
            startInFullScreen = startInFullScreen,
            playerMode= mode
        )
    }

}