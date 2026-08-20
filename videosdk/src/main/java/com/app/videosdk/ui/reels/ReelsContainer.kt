package com.app.videosdk.ui.reels

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.app.videosdk.listener.PlayerStateListener
import com.app.videosdk.model.PlayerControlsConfig
import com.app.videosdk.model.PlayerModel
import com.app.videosdk.ui.MainContainer

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReelsContainer(
    contentList: List<PlayerModel>?,
    playerStateListener: PlayerStateListener?,
    onPlayerBack: (Boolean) -> Unit,
    setFullScreen: (Boolean) -> Unit,
    startInFullScreen: Boolean = false,
    controlsConfig: PlayerControlsConfig = PlayerControlsConfig()
) {
    val pageCount = contentList?.size ?: 0

    val pagerState = rememberPagerState {
        pageCount
    }

    LaunchedEffect(pagerState.currentPage, pageCount) {
        if (pageCount == 0) return@LaunchedEffect

        val currentPage = pagerState.currentPage.coerceIn(0, pageCount - 1)
        playerStateListener?.onReelChanged(currentPage)

        val nextIndex = currentPage + 1
        if (nextIndex < pageCount) {
            playerStateListener?.onPreloadNext(nextIndex)
        }
    }

    VerticalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = REELS_PREFETCH_PAGE_COUNT
    ) { page ->
        MainContainer(
            contentList = contentList,
            index = page,
            playerStateListener = playerStateListener,
            onPlayerBack = onPlayerBack,
            setFullScreen = setFullScreen,
            startInFullScreen = startInFullScreen,
            isReelPageActive = page == pagerState.currentPage,
            controlsConfig = controlsConfig
        )
    }
}

private const val REELS_PREFETCH_PAGE_COUNT = 1
