package com.app.videosdk.ui

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.media3.common.util.UnstableApi
import com.app.videosdk.listener.PipListener
import com.app.videosdk.listener.PlayerController
import com.app.videosdk.listener.PlayerStateListener
import com.app.videosdk.model.EpisodeNowPlayingStyle
import com.app.videosdk.model.PlayerConfig
import com.app.videosdk.model.PlayerModel
import com.app.videosdk.ui.reels.ReelsContainer
import com.app.videosdk.utils.PlayerMode

@OptIn(UnstableApi::class)
@Suppress("UNUSED_PARAMETER")
@Composable
fun MtvVideoPlayerSdk(
    contentList: List<PlayerModel>? = null,
    index: Int? = 0,
    mode: PlayerMode? = PlayerMode.REELS,
    pipListener: PipListener? = null,
    isInPipMode: Boolean = false,
    startInFullScreen: Boolean = false,
    isDeepLink: Boolean? = false,
    playerMode: PlayerMode = PlayerMode.REELS,
    playerStateListener: PlayerStateListener? = null,
    controller: PlayerController? = null,
    isMutedInitially: Boolean = true,
    onVideoEnded: (() -> Unit)? = null,
    onPlayerBack: (Boolean) -> Unit = {},
    setFullScreen: (Boolean) -> Unit = {},
    onIndexChanged: (Int) -> Unit = {},
    episodeNowPlayingStyle: EpisodeNowPlayingStyle = EpisodeNowPlayingStyle(),
    showControls: Boolean = true,
    playerConfig: PlayerConfig = PlayerConfig(),
    onCurrentIndexChanged: (Int) -> Unit = {},
    onPreviewPrimaryAction: () -> Unit = {},
    onPreviewSecondaryAction: () -> Unit = {}
) {
    ReelsContainer(
        contentList = contentList,
        playerStateListener = playerStateListener,
        onPlayerBack = onPlayerBack,
        setFullScreen = setFullScreen,
        startInFullScreen = startInFullScreen,
        controlsConfig = playerConfig.controls
    )
}
