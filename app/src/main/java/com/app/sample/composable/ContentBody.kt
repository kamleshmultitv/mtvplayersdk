package com.app.sample.composable

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.paging.compose.LazyPagingItems
import com.app.sample.R
import com.app.sample.model.ContentItem
import com.app.sample.model.OverrideContent
import com.app.sample.utils.FileUtils.buildPlayerContentList
import com.app.videosdk.ui.MtvVideoPlayerSdk

@Composable
fun ContentBody(
    context: Context,
    pagingItems: LazyPagingItems<ContentItem>,
    selectedIndex: MutableIntState,
    overrideContent: OverrideContent?,
    isFullScreen: Boolean,
    onFullScreenChange: (Boolean) -> Unit,
    onOverrideContent: (OverrideContent?) -> Unit
) {
    val contentList by remember(pagingItems.itemSnapshotList.items, overrideContent) {
        derivedStateOf {
            buildPlayerContentList(
                context = context,
                pagingItems = pagingItems,
                overrideContent = overrideContent
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.black))
    ) {
        // Player is now ALWAYS Full Screen as per requirement
        MtvVideoPlayerSdk(
            contentList = contentList,
            index = selectedIndex.intValue,
            onPlayerBack = { onFullScreenChange(false) },
            setFullScreen = { /* Always true */ }
        )
    }
}
