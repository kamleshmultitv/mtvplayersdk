package com.app.sample.composable

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.app.sample.R
import com.app.sample.model.ContentItem
import com.app.sample.model.OverrideContent
import com.app.sample.utils.FileUtils.buildPlayerContentList
import com.app.videosdk.listener.PipListener
import com.app.videosdk.ui.MtvVideoPlayerSdk

@Composable
fun ContentBody(
    context: Context,
    pagingItems: LazyPagingItems<ContentItem>,
    selectedIndex: MutableIntState,
    overrideContent: OverrideContent?,
    pipListener: PipListener,
    isFullScreen: Boolean,
    onFullScreenChange: (Boolean) -> Unit,
    onOverrideContent: (OverrideContent?) -> Unit
) {
    val contentList by remember(
        pagingItems.itemSnapshotList.items,
        overrideContent
    ) {
        mutableStateOf(
            buildPlayerContentList(
                context = context,
                pagingItems = pagingItems,
                overrideContent = overrideContent
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.black))
    ) {
        if (isFullScreen) {
            MtvVideoPlayerSdk(
                contentList = contentList,
                index = selectedIndex.intValue,
                pipListener = pipListener,
                onPlayerBack = { },
                setFullScreen = onFullScreenChange
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxHeight()
                        .fillMaxWidth()
                ) {
                    MtvVideoPlayerSdk(
                        contentList = contentList,
                        index = selectedIndex.intValue,
                        pipListener = pipListener,
                        onPlayerBack = { },
                        setFullScreen = onFullScreenChange
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(start = 12.dp)
                ) {
                    ContentList(
                        pagingItems = pagingItems,
                        onItemClick = { index ->
                            selectedIndex.intValue = index
                            onOverrideContent(null)
                        }
                    )
                }
            }
        }

        // ➕ Floating Action Button (hidden in fullscreen for TV experience)
        if (!isFullScreen) {
            FloatButton { url, sprite, token ->
                selectedIndex.intValue = 0
                onOverrideContent(
                    OverrideContent(url, sprite, token)
                )
            }
        }
    }
}
