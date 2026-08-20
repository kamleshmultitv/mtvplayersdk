package com.app.sample.composable

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.app.sample.extra.ApiConstant.TOKEN
import com.app.sample.model.DeepLinkResponse
import com.app.sample.model.OverrideContent
import com.app.sample.viewModel.ContentViewModel
import com.app.videosdk.listener.PipListener

@Composable
fun ContentScreen(
    viewModel: ContentViewModel,
    pipListener: PipListener,
    isInPipMode: Boolean,
    isDeepLink: Boolean,
    deepLinkContentId: String?,
    deepLinkUrl: String?,
    deepLinkStart: Long,
    deepLinkEnd: Long,
    deepLinkTotalClipDuration: Long
) {
    val context = LocalContext.current
    val pagingItems = viewModel.contentListData.collectAsLazyPagingItems()

    val selectedIndex = remember { mutableIntStateOf(0) }
    var isFullScreen by remember { mutableStateOf(false) }

    var overrideContent by remember { mutableStateOf<OverrideContent?>(null) }


    var deepLinkContent by remember { mutableStateOf<DeepLinkResponse?>(null) }

    if (isDeepLink) {
        deepLinkContent = DeepLinkResponse(
            url = deepLinkUrl,
            contentId = deepLinkContentId,
            seekTo = deepLinkStart,
            clipEndTime = deepLinkEnd,
            totalClipDuration = deepLinkTotalClipDuration
        )
    }
    LaunchedEffect(isDeepLink) {
        if (!isDeepLink) {
            viewModel.getSeason()
        }
    }

    when (pagingItems.loadState.refresh) {
        LoadState.Loading -> LoadingView()
        //  is LoadState.Error -> ErrorView()
        else -> ContentBody(
            context = context,
            contentItem = null,
            pagingItems = pagingItems,
            selectedIndex = selectedIndex,
            overrideContent = overrideContent,
            deepLinkContent = deepLinkContent,
            isDeepLink = isDeepLink,
            pipListener = pipListener,
            isInPipMode = isInPipMode,
            isFullScreen = isFullScreen,
            onFullScreenChange = { isFullScreen = it },
            onOverrideContent = { overrideContent = it }
        )
    }
}
