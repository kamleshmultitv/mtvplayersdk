package com.app.reelssdk.ui.reels

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.app.reelssdk.listener.ReelsPlayerStateListener
import com.app.reelssdk.model.ReelsPlayerControlsConfig
import com.app.reelssdk.model.ReelsPlayerModel
import com.app.reelssdk.ui.MainContainer

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReelsContainer(
    contentList: List<ReelsPlayerModel>?,
    initialIndex: Int? = 0,
    playerStateListener: ReelsPlayerStateListener?,
    onPlayerBack: (Boolean) -> Unit,
    setFullScreen: (Boolean) -> Unit,
    startInFullScreen: Boolean = false,
    controlsConfig: ReelsPlayerControlsConfig = ReelsPlayerControlsConfig()
) {
    val pageCount = contentList?.size ?: 0
    var isFullScreen by remember { mutableStateOf(startInFullScreen) }
    val initialPage = when {
        pageCount == 0 -> 0
        initialIndex == null -> 0
        initialIndex < 0 -> 0
        initialIndex >= pageCount -> pageCount - 1
        else -> initialIndex
    }

    val pagerState = rememberPagerState(initialPage = initialPage) {
        pageCount
    }

    LaunchedEffect(startInFullScreen) {
        isFullScreen = startInFullScreen
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
            isFullScreen = isFullScreen,
            onFullScreenModeChanged = { full ->
                isFullScreen = full
            },
            isReelPageActive = page == pagerState.currentPage,
            controlsConfig = controlsConfig
        )
    }
}

private const val REELS_PREFETCH_PAGE_COUNT = 1
