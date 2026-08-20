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
import com.app.videosdk.model.EpisodeNowPlayingStyle
import com.app.videosdk.ui.MtvVideoPlayerSdk
import com.app.videosdk.utils.PlayerMode

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
    var showSdkEdgeCases by remember { mutableStateOf(false) }
    var isSdkEdgeCaseFullScreen by remember { mutableStateOf(false) }
    var isSdkEdgeCaseStatusBarSafe by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.black))
            .then(
                if (!isFullScreen && (!showSdkEdgeCases || (!isSdkEdgeCaseFullScreen && isSdkEdgeCaseStatusBarSafe))) {
                    Modifier.statusBarsPadding()
                } else {
                    Modifier
                }
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // 🎬 SDK Video Player (KEYED)
            key(contentList) {

                // ✅ derive mode from existing boolean
                val playerMode = if (isFullScreen) {
                    PlayerMode.FULL_SCREEN
                } else {
                    PlayerMode.MINI
                }

                MtvVideoPlayerSdk(
                    contentList = contentList,
                    index = selectedIndex.intValue,
                    mode = PlayerMode.REELS,
                    pipListener = pipListener,
                    isInPipMode = isInPipMode,
                    isDeepLink = isDeepLink,

                    // ✅ FIXED (dynamic mode)
                    playerMode = playerMode,

                    onIndexChanged = { newIndex ->
                        selectedIndex.intValue = newIndex
                    },

                    episodeNowPlayingStyle = EpisodeNowPlayingStyle(
                        pillBackgroundColor = Color(0xFF00C853),
                        pillTextColor = Color.White,
                        pillDotColor = Color.White
                    ),

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
        if (!isFullScreen) {
            FloatButton { config ->

                if (config.url.isBlank()) {
                    // ✅ APPLY CONFIG TO EXISTING API CONTENT
                    onOverrideContent(
                        OverrideContent(
                            url = null,                 // 👈 IMPORTANT
                            drmToken = null,
                            isLive = false,
                            adsConfig = config.adsConfig,
                            skipIntro = config.skipIntro,
                            nextEpisode = config.nextEpisode
                        )
                    )
                } else {
                    // ✅ OVERRIDE CONTENT
                    selectedIndex.intValue = 0
                    onOverrideContent(
                        OverrideContent(
                            url = config.url,
                            drmToken = config.drmToken,
                            isLive = config.isLive,
                            adsConfig = config.adsConfig,
                            skipIntro = config.skipIntro,
                            nextEpisode = config.nextEpisode
                        )
                    )
                }
            }

        }

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

        if (false && !isFullScreen) {
            FloatingActionButton(
                onClick = {
                    showSdkEdgeCases = true
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 32.dp, bottom = 160.dp),
                containerColor = Color(0xFFFFB300),
                contentColor = Color.Black,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("QA")
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

        if (showSdkEdgeCases) {
            SdkEdgeCaseScreen(
                pipListener = pipListener,
                isInPipMode = isInPipMode,
                onFullScreenChange = { isSdkEdgeCaseFullScreen = it },
                onStatusBarSafeAreaChange = { isSdkEdgeCaseStatusBarSafe = it },
                onClose = {
                    isSdkEdgeCaseFullScreen = false
                    isSdkEdgeCaseStatusBarSafe = true
                    showSdkEdgeCases = false
                }
            )
        }
    }
}
