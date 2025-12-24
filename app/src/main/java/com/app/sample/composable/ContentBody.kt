package com.app.sample.composable

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
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
    // Optimized contentList derivation to prevent frequent player re-initialization
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
        if (isFullScreen) {
            MtvVideoPlayerSdk(
                contentList = contentList,
                index = selectedIndex.intValue,
                onPlayerBack = { onFullScreenChange(false) },
                setFullScreen = onFullScreenChange
            )
        } else {
            // New Layout: Right-Top anchored column taking 75% width
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.75f) // 75% Width
                    .fillMaxHeight()
                    .align(Alignment.TopEnd) // Right Top Corner
                    .padding(12.dp)
            ) {
                /* ---------- PLAYER (Top 75% Height) ---------- */
                Box(
                    modifier = Modifier
                        .weight(0.75f)
                        .fillMaxWidth()
                ) {
                    MtvVideoPlayerSdk(
                        contentList = contentList,
                        index = selectedIndex.intValue,
                        onPlayerBack = { },
                        setFullScreen = onFullScreenChange
                    )
                }

                /* ---------- LIST (Bottom 25% Height) ---------- */
                Box(
                    modifier = Modifier
                        .weight(0.25f)
                        .fillMaxWidth()
                        .padding(top = 12.dp)
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

        // Floating Action Button
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
