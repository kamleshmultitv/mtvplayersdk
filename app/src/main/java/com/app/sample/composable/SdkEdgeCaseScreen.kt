package com.app.sample.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.app.videosdk.listener.PipListener
import com.app.videosdk.model.Chapter
import com.app.videosdk.model.EpisodeNowPlayingStyle
import com.app.videosdk.model.NextEpisode
import com.app.videosdk.model.PlayerModel
import com.app.videosdk.model.SkipIntro
import com.app.videosdk.ui.MtvVideoPlayerSdk
import com.app.videosdk.utils.PlayerMode

@Composable
fun SdkEdgeCaseScreen(
    pipListener: PipListener,
    isInPipMode: Boolean,
    onFullScreenChange: (Boolean) -> Unit,
    onStatusBarSafeAreaChange: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    val cases = remember { sdkEdgeCases() }
    var selectedCase by remember { mutableStateOf(cases.first()) }
    var isFullScreen by remember { mutableStateOf(false) }
    val selectedIndex = remember(selectedCase) { mutableIntStateOf(0) }

    LaunchedEffect(isFullScreen) {
        onFullScreenChange(isFullScreen)
    }

    LaunchedEffect(selectedCase.statusBarSafeArea) {
        onStatusBarSafeAreaChange(selectedCase.statusBarSafeArea)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
        ) {
        Column(modifier = Modifier.fillMaxSize()) {
            MtvVideoPlayerSdk(
                contentList = selectedCase.contentList,
                index = selectedIndex.intValue,
                pipListener = pipListener,
                isInPipMode = isInPipMode,
                playerMode = if (isFullScreen) PlayerMode.FULL_SCREEN else PlayerMode.MINI,
                onIndexChanged = { selectedIndex.intValue = it },
                episodeNowPlayingStyle = selectedCase.nowPlayingStyle,
                onPlayerBack = {
                    if (isFullScreen) {
                        isFullScreen = false
                    } else {
                        onClose()
                    }
                },
                setFullScreen = { isFullScreen = it }
            )

            if (!isFullScreen) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(cases) { edgeCase ->
                        EdgeCaseRow(
                            edgeCase = edgeCase,
                            isSelected = edgeCase.title == selectedCase.title,
                            onClick = {
                                selectedCase = edgeCase
                                isFullScreen = false
                                onStatusBarSafeAreaChange(edgeCase.statusBarSafeArea)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EdgeCaseRow(
    edgeCase: SdkEdgeCase,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF263238) else Color(0xFF151515)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = edgeCase.title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = edgeCase.description,
                color = Color.White.copy(alpha = 0.70f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private data class SdkEdgeCase(
    val title: String,
    val description: String,
    val initialMode: PlayerMode,
    val contentList: List<PlayerModel>,
    val statusBarSafeArea: Boolean = true,
    val nowPlayingStyle: EpisodeNowPlayingStyle = EpisodeNowPlayingStyle()
)

private fun sdkEdgeCases(): List<SdkEdgeCase> {
    val baseUrl = "https://storage.googleapis.com/shaka-demo-assets/angel-one-hls/hls.m3u8"
    val portraitImage = "https://picsum.photos/360/640"
    val wideImage = "https://picsum.photos/640/360"

    fun item(
        id: String,
        title: String,
        episodeNumber: String,
        imageUrl: String = wideImage,
        description: String = "SDK QA sample episode used to verify player chrome, controls, episode sheet, and title behavior across modes."
    ) = PlayerModel(
        id = id,
        hlsUrl = baseUrl,
        imageUrl = imageUrl,
        episodeTitle = title,
        episodeDescription = description,
        seasonTitle = "SDK QA Season",
        seasonNumber = "420",
        episodeNumber = episodeNumber,
        duration = "00:44:07",
        skipIntro = SkipIntro(startTime = 5_000L, endTime = 15_000L, enableSkipIntro = true),
        nextEpisode = NextEpisode(showBeforeEndMs = "30000", enableNextEpisode = true),
        isChapterEnabled = true,
        chapters = listOf(
            Chapter(id = "intro", title = "Intro", startMs = 0L),
            Chapter(id = "middle", title = "Middle", startMs = 60_000L),
            Chapter(id = "ending", title = "Ending", startMs = 120_000L)
        )
    )

    val multipleEpisodes = listOf(
        item("qa-1", "Saajan Ghar Maha Episode Pooja, Kabir, Aur Vivan Ki Kahani", "1"),
        item("qa-2", "Saajan Ghar Maha Episode Pooja, Kabir, Aur Vivan Ki Kahani (2)", "2"),
        item("qa-3", "Saajan Ghar Maha Episode Pooja, Kabir, Aur Vivan Ki Kahani (5)", "3"),
        item("qa-4", "Very Long Episode Title To Verify Mini Top Bar Ellipsis And Premium Header Layout", "4")
    )

    return listOf(
        SdkEdgeCase(
            title = "Mini 16:9 Long Title",
            description = "Checks mini TopBar position, ellipsis, and fullscreen button on normal video.",
            initialMode = PlayerMode.MINI,
            contentList = multipleEpisodes
        ),
        SdkEdgeCase(
            title = "Status Bar Visible",
            description = "Host keeps normal status-bar safe area. SDK title should stay aligned and not jump.",
            initialMode = PlayerMode.MINI,
            contentList = multipleEpisodes.map {
                it.copy(episodeTitle = "Status Bar Visible - Title Alignment Should Not Shift")
            },
            statusBarSafeArea = true
        ),
        SdkEdgeCase(
            title = "Status Bar Hidden Area",
            description = "Host gives SDK the top edge with no status safe area. Use this to compare title alignment.",
            initialMode = PlayerMode.MINI,
            contentList = multipleEpisodes.map {
                it.copy(episodeTitle = "No Status Safe Area - Title Alignment Should Stay Premium")
            },
            statusBarSafeArea = false
        ),
        SdkEdgeCase(
            title = "Fullscreen Episode Sheet",
            description = "Open Episodes in fullscreen; sheet must cover header and close without exiting fullscreen.",
            initialMode = PlayerMode.FULL_SCREEN,
            contentList = multipleEpisodes
        ),
        SdkEdgeCase(
            title = "Single Episode",
            description = "Verifies episode/next controls stay hidden when only one episode exists.",
            initialMode = PlayerMode.MINI,
            contentList = listOf(item("single", "Single Episode QA Title", "1"))
        ),
        SdkEdgeCase(
            title = "Missing Metadata",
            description = "Verifies null image/title/description do not break SDK chrome.",
            initialMode = PlayerMode.MINI,
            contentList = listOf(
                PlayerModel(
                    id = "missing-meta",
                    hlsUrl = baseUrl,
                    episodeTitle = null,
                    episodeDescription = null,
                    seasonTitle = "Fallback Season Title",
                    episodeNumber = "1"
                )
            )
        ),
        SdkEdgeCase(
            title = "Portrait Artwork List",
            description = "Checks episode sheet cards and TopBar with portrait thumbnails.",
            initialMode = PlayerMode.FULL_SCREEN,
            contentList = multipleEpisodes.map { it.copy(imageUrl = portraitImage) }
        ),
        SdkEdgeCase(
            title = "Custom Now Playing Style",
            description = "Verifies app branding can change the green pill without SDK layout hacks.",
            initialMode = PlayerMode.FULL_SCREEN,
            contentList = multipleEpisodes,
            nowPlayingStyle = EpisodeNowPlayingStyle(
                pillText = "Now",
                pillBackgroundColor = Color(0xFFFFB300),
                pillTextColor = Color.Black,
                pillDotColor = Color.Black
            )
        )
    )
}
