package com.app.videosdk.ui.reels

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.app.videosdk.listener.PipListener
import com.app.videosdk.listener.PlayerStateListener
import com.app.videosdk.model.PlayerModel
import com.app.videosdk.ui.MainContainer
import com.app.videosdk.utils.PlayerMode
import com.app.videosdk.utils.PlayerUtils

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReelsContainer(
    contentList: List<PlayerModel>?,
    pipListener: PipListener?,
    isInPipMode: Boolean,
    playerStateListener: PlayerStateListener?,
    setFullScreen: (Boolean) -> Unit,
    startInFullScreen: Boolean = false,
    playerMode: PlayerMode = PlayerMode.OTT
) {

    val context = LocalContext.current

    // ✅ SINGLE PLAYER
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .build()
    }

    val pagerState = rememberPagerState {
        contentList?.size ?: 0
    }

    // 🔥 Change media when page changes
    LaunchedEffect(pagerState.currentPage) {
        val nextIndex = pagerState.currentPage + 1
        if (nextIndex < contentList!!.size) {
            val nextUri = PlayerUtils.resolveToPlayableUri(contentList, nextIndex)
            exoPlayer.addMediaItem(MediaItem.fromUri(nextUri))
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    VerticalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->

        MainContainer(
            contentList = contentList,
            index = page,
            pipListener = pipListener,
            isInPipMode = isInPipMode,
            playerStateListener = playerStateListener,
            onPlayerBack = {},
            setFullScreen = setFullScreen,
            playerMode = playerMode,
            startInFullScreen = startInFullScreen
        )
    }
}
