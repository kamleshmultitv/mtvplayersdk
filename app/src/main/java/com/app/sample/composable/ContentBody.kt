package com.app.sample.composable

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.app.mtvdownloader.entity.DownloadEntity
import com.app.sample.R
import com.app.sample.composable.download.DownloadPlayer
import com.app.sample.composable.download.DownloadedContentList
import com.app.sample.model.ContentItem
import com.app.sample.model.DeepLinkResponse
import com.app.sample.model.OverrideContent
import com.app.sample.utils.FileUtils.buildPlayerContentList
import com.app.videosdk.listener.PipListener
import com.app.videosdk.listener.PlayerStateListener
import com.app.videosdk.ui.MtvVideoPlayerSdk

@Composable
fun ContentBody(
    context: Context,
    contentItem: ContentItem? = null,
    pagingItems: LazyPagingItems<ContentItem>,
    selectedIndex: MutableIntState,
    overrideContent: OverrideContent?,
    deepLinkContent: DeepLinkResponse?,
    pipListener: PipListener,
    isDeepLink: Boolean? = false,
    isInPipMode: Boolean,
    isFullScreen: Boolean,
    onFullScreenChange: (Boolean) -> Unit,
    onOverrideContent: (OverrideContent?) -> Unit
) {
    // 🔥 IMPORTANT: no derivedStateOf here
    val contentList = remember(
        pagingItems.itemSnapshotList.items,
        overrideContent,
        deepLinkContent,
        contentItem
    ) {
        buildPlayerContentList(
            context = context,
            pagingItems = pagingItems,
            overrideContent = overrideContent,
            deepLinkContent = deepLinkContent,
            contentItem = contentItem
        )
    }

    val downloadedContentList = remember {
        mutableStateListOf<DownloadEntity>()
    }

    var showDownloadedList by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<DownloadEntity?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.black))
            .then(
                if (!isFullScreen) {
                    Modifier.statusBarsPadding()
                } else {
                    Modifier
                }
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // 🎬 SDK Video Player (KEYED)
            key(contentList) {
                MtvVideoPlayerSdk(
                    contentList = contentList,
                    index = selectedIndex.intValue,
                    isDeepLink = isDeepLink,

                    onIndexChanged = { newIndex ->
                        selectedIndex.intValue = newIndex
                    },

                    // ✅ Back handling (no logic change)
                    onPlayerBack = {
                        if (isFullScreen) {
                            onFullScreenChange(false)
                        }
                    },

                    // ✅ Fullscreen toggle (same as before)
                    setFullScreen = { isFull ->
                        onFullScreenChange(isFull)
                    },

                    playerStateListener = object : PlayerStateListener {

                        override fun onPlayerReady(durationMs: Long) {
                            Log.d("CLIENT", "Player ready: $durationMs")
                        }

                        override fun onPlayStateChanged(isPlaying: Boolean) {
                            Log.d("CLIENT", "Playing: $isPlaying")
                        }

                        override fun onPlaybackCompleted() {
                            Log.d("CLIENT", "Playback completed")
                        }

                        override fun onFullScreenChanged(isFullScreen: Boolean) {
                            Log.d("CLIENT", "Full screen: $isFullScreen")
                        }

                        override fun onAdStateChanged(isAdPlaying: Boolean) {
                            Log.d("CLIENT", "Ad playing = $isAdPlaying")
                        }
                    }
                )
            }

            // 📜 Content List
            ContentList(
                pagingItems = pagingItems,
                onItemClick = { index ->
                    selectedIndex.intValue = index
                    onOverrideContent(null)
                },
                downloadContentList = { list ->
                    downloadedContentList.clear()
                    downloadedContentList.addAll(list)
                }
            )
        }

        // ➕ Floating Action Button

        if (!isFullScreen && downloadedContentList.isNotEmpty()) {
            FloatingActionButton(
                onClick = {
                    showDownloadedList = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 32.dp, bottom = 160.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = colorResource(R.color.white),
                shape = RoundedCornerShape(8.dp)
            ) {
                // ✅ FAB content (ICON / TEXT REQUIRED)
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Downloads"
                )
            }
        }

        if (showDownloadedList) {
            DownloadedContentList(downloadContentList = downloadedContentList,
                onItemClick = { item ->
                    selectedItem = item
                },
                onBackClick = {
                    showDownloadedList = false
                })
        }

        selectedItem?.let { item ->
            DownloadPlayer(item,
                onBack = {
                    selectedItem = null
                })
        }
    }
}
