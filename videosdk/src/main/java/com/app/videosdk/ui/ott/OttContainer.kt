package com.app.videosdk.ui.ott

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.media3.common.util.UnstableApi
import com.app.videosdk.listener.PipListener
import com.app.videosdk.listener.PlayerStateListener
import com.app.videosdk.model.PlayerModel
import com.app.videosdk.ui.MainContainer
import com.app.videosdk.utils.PlayerMode

@OptIn(UnstableApi::class)
@Composable
fun OttContainer(
    contentList: List<PlayerModel>?,
    index: Int? = 0,
    pipListener: PipListener?,
    isInPipMode: Boolean,
    playerStateListener: PlayerStateListener?,
    onPlayerBack: (Boolean) -> Unit,
    setFullScreen: (Boolean) -> Unit,
    startInFullScreen: Boolean = false,
    playerMode: PlayerMode = PlayerMode.OTT // ✅ add this
) {

    MainContainer(
        contentList = contentList,
        index = index,
        pipListener = pipListener,
        isInPipMode = isInPipMode,
        playerStateListener = playerStateListener,
        onPlayerBack = {},
        setFullScreen = setFullScreen,
        startInFullScreen = startInFullScreen,
        playerMode = playerMode
    )
}
