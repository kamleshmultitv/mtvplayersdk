# Reels Player Implementation Guide

This guide explains how a third-party Android app should integrate the MTV Video Player SDK reels player.

The reels player is a full-screen vertical feed player. It supports smooth page scrolling, next-item prefetch callbacks, first decoded video frame rendering, fast-start playback, seekbar, play/pause, expand/collapse, share sheet, settings bottom sheet, and app-provided control icons.

## 1. Requirements

- Android min SDK: 24
- Kotlin Android app
- Jetpack Compose enabled
- Internet playback URLs, preferably HLS (`.m3u8`) or DASH (`.mpd`)
- Playback URLs that can start quickly enough for vertical feed scrolling

## 2. Add The SDK Dependency

Add JitPack in project-level `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}
```

Add the SDK dependency in the app module:

```kotlin
dependencies {
    implementation("com.github.kamleshmultitv.mtvplayersdk:reelssdk:reels-1.0.1")
}
```

This artifact can be installed alongside the existing mobile player SDK because it uses a different artifactId and Kotlin package namespace:

```kotlin
dependencies {
    implementation("com.github.kamleshmultitv:mtvplayersdk:mobile-2.0.36")
    implementation("com.github.kamleshmultitv.mtvplayersdk:reelssdk:reels-1.0.1")
}
```

Use separate imports in the host app:

```kotlin
import com.app.videosdk.ui.MtvVideoPlayerSdk
import com.app.reelssdk.ui.MtvReelsPlayerSdk
```

This branch publishes the `reelssdk` Android library artifact. The current publication values are:

- `groupId`: `com.github.kamleshmultitv.mtvplayersdk`
- `artifactId`: `reelssdk`
- `version`: `reels-1.0.1`

This is a multi-module repository, so third-party apps should depend on the `reelssdk` module artifact, not the sample `app` module or the downloader module. If you publish a different Git tag/version, replace `reels-1.0.1` with that release version.

## 3. Android Setup

Enable Compose:

```kotlin
android {
    buildFeatures {
        compose = true
    }
}
```

Add required permissions:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

The SDK library manifest includes app package queries for WhatsApp, Facebook, Instagram, and generic text share. If your app overrides manifest merging or implements a custom share sheet, keep these queries:

```xml
<queries>
    <package android:name="com.whatsapp" />
    <package android:name="com.whatsapp.w4b" />
    <package android:name="com.facebook.katana" />
    <package android:name="com.facebook.lite" />
    <package android:name="com.instagram.android" />

    <intent>
        <action android:name="android.intent.action.SEND" />
        <data android:mimeType="text/plain" />
    </intent>
</queries>
```

## 4. Map Your API Data To ReelsPlayerModel

Each reel item must become a `ReelsPlayerModel`.

Use:

- `id`: stable content id.
- `hlsUrl`: HLS playback URL.
- `mpdUrl`: DASH playback URL, especially for DRM content.
- `drm = "1"` and `drmToken`: if playback uses Widevine license/token.
- `imageUrl` or `thumbnail`: optional image metadata. The reels player does not cover the video with this image during scroll; it keeps the Media3 surface visible so the first decoded video frame is rendered.
- `title` or `episodeTitle`: title used by share text and metadata.
- `shareUrl`: public content URL for share sheet.
- `srt`: subtitle URL, if available.
- `spriteUrl`: thumbnail sprite URL for seek preview, if available.
- `customControls`: optional app drawables for icons.
- `controlsConfig`: optional per-content control visibility override.

Example mapper:

```kotlin
import com.app.reelssdk.model.ReelsPlayerModel

fun ApiReel.toReelsPlayerModel(): ReelsPlayerModel {
    return ReelsPlayerModel(
        id = id,
        hlsUrl = playback.hlsUrl,
        mpdUrl = playback.dashUrl,
        drm = if (playback.isDrm) "1" else "0",
        drmToken = playback.licenseToken,
        imageUrl = posterUrl,
        thumbnail = thumbnailUrl,
        title = title,
        episodeTitle = title,
        description = description,
        srt = subtitleUrl,
        spriteUrl = spriteUrl,
        shareUrl = publicShareUrl,
        isLive = false
    )
}
```

For fastest perceived startup, provide optimized HLS/DASH streams with a short startup segment. The reels player keeps the video surface visible and renders the first decoded frame instead of showing a thumbnail overlay during scroll.

## 5. Basic Reels Screen

Use `MtvReelsPlayerSdk` directly. This artifact is reels-only, so mobile-player mode parameters are not part of the public API.

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.app.reelssdk.model.ReelsPlayerModel
import com.app.reelssdk.ui.MtvReelsPlayerSdk

@Composable
fun ReelsPlayerScreen(
    reels: List<ReelsPlayerModel>,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        MtvReelsPlayerSdk(
            contentList = reels,
            index = 0,
            onPlayerBack = { onClose() },
            setFullScreen = { /* reels handles expand/collapse internally */ }
        )
    }
}
```

The reels player internally uses a vertical pager and keeps one next page prepared for smoother scrolling.

## 6. Recommended Full Integration

Use a `ReelsPlayerStateListener` to observe current reel index and prefetch the next API item/page.

```kotlin
import android.util.Log
import androidx.compose.runtime.Composable
import com.app.reelssdk.listener.ReelsPlayerStateListener
import com.app.reelssdk.model.ReelsPlayerConfig
import com.app.reelssdk.model.ReelsPlayerControlsConfig
import com.app.reelssdk.model.ReelsPlayerModel
import com.app.reelssdk.ui.MtvReelsPlayerSdk

@Composable
fun ReelsFeedPlayer(
    reels: List<ReelsPlayerModel>,
    onPreloadMore: (nextIndex: Int) -> Unit,
    onReelChanged: (index: Int) -> Unit,
    onBack: () -> Unit
) {
    MtvReelsPlayerSdk(
        contentList = reels,
        index = 0,
        playerConfig = ReelsPlayerConfig(
            controls = ReelsPlayerControlsConfig(
                play = true,
                pause = true,
                share = true,
                bookmark = true,
                seekbar = true,
                settings = true,
                fullscreen = true,
                exitFullscreen = true
            )
        ),
        playerStateListener = object : ReelsPlayerStateListener {
            override fun onReelChanged(position: Int) {
                onReelChanged(position)
            }

            override fun onPreloadNext(index: Int) {
                onPreloadMore(index)
            }

            override fun onPlayerReady(durationMs: Long) {
                Log.d("Reels", "Ready duration=$durationMs")
            }

            override fun onPlayStateChanged(isPlaying: Boolean) {
                Log.d("Reels", "Playing=$isPlaying")
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e("Reels", "Playback failed", error)
            }
        },
        onPlayerBack = { onBack() },
        setFullScreen = { /* optional analytics */ }
    )
}
```

## 7. Paging And Prefetch Strategy

The SDK does not call a third-party app API directly. It does not know the app's offset, limit, cursor, auth token, endpoint, cache, or Paging 3 setup.

The host app owns:

- The current loaded `contentList`.
- The next `offset` or cursor.
- The `limit` / page size.
- `isLoadingMore` and `hasMore` guards.
- The API call that fetches the next page.
- Appending new items to the existing feed state.

The SDK only emits scroll/prefetch signals through `ReelsPlayerStateListener`:

- `onReelChanged(position)` when the visible reel changes.
- `onPreloadNext(index)` when the SDK is preparing the next reel index.

Recommended rule: when the user is close to the end of the loaded list, call the app's next offset API once and append the new items.

```kotlin
private const val LOAD_MORE_THRESHOLD = 3

override fun onPreloadNext(index: Int) {
    val remainingItems = reels.size - 1 - index
    val shouldLoadMore = remainingItems <= LOAD_MORE_THRESHOLD

    if (shouldLoadMore && !isLoadingNextPage && hasMorePages) {
        loadNextReelsPage()
    }
}
```

Offset-based API example:

```kotlin
class ReelsViewModel : ViewModel() {
    var reels by mutableStateOf<List<ReelsPlayerModel>>(emptyList())
        private set

    private var nextOffset = 0
    private val limit = 10
    private var isLoadingMore = false
    private var hasMorePages = true

    fun loadInitialPage() {
        if (reels.isNotEmpty()) return
        loadNextReelsPage()
    }

    fun loadNextReelsPage() {
        if (isLoadingMore || !hasMorePages) return

        isLoadingMore = true

        viewModelScope.launch {
            try {
                val response = api.getReels(
                    offset = nextOffset,
                    limit = limit
                )

                val newItems = response.items.map { it.toReelsPlayerModel() }

                reels = reels + newItems
                nextOffset += newItems.size
                hasMorePages = newItems.size == limit
            } finally {
                isLoadingMore = false
            }
        }
    }

    fun onSdkPreloadNext(index: Int) {
        val remainingItems = reels.size - 1 - index
        if (remainingItems <= 3) {
            loadNextReelsPage()
        }
    }
}
```

Compose integration:

```kotlin
@Composable
fun ThirdPartyReelsRoute(
    viewModel: ReelsViewModel,
    close: () -> Unit
) {
    val reels = viewModel.reels

    LaunchedEffect(Unit) {
        viewModel.loadInitialPage()
    }

    MtvReelsPlayerSdk(
        contentList = reels,
        index = 0,
        playerStateListener = object : ReelsPlayerStateListener {
            override fun onReelChanged(position: Int) {
                // Optional analytics/current item tracking.
            }

            override fun onPreloadNext(index: Int) {
                viewModel.onSdkPreloadNext(index)
            }
        },
        onPlayerBack = { close() },
        setFullScreen = {}
    )
}
```

If the app uses Android Paging 3, the same rule applies: keep Paging in the app layer, convert the loaded page items to `ReelsPlayerModel`, and append/submit the expanded list to the SDK. The SDK callback tells the app that the feed is near the end; the app's `PagingSource` decides the next offset:

```kotlin
override fun onPreloadNext(index: Int) {
    val remainingItems = playerModels.size - 1 - index
    if (remainingItems <= 3) {
        pagingViewModel.requestNextPageIfNeeded()
    }
}
```

Important integration rules:

- Append new page items; do not replace the list with only the new page.
- Do not reset `index` to `0` when appending more data.
- Guard every next-page request with `isLoadingMore` and `hasMorePages`.
- Use stable `ReelsPlayerModel.id` values.
- Keep API pagination logic in the third-party app or its ViewModel, not inside the SDK.
- Update the list only when new data arrives, not on every scroll callback.

## 8. Control Visibility Flags

Controls can be configured globally through `ReelsPlayerConfig.controls`:

```kotlin
ReelsPlayerConfig(
    controls = ReelsPlayerControlsConfig(
        play = true,
        pause = true,
        share = true,
        bookmark = false,
        seekbar = true,
        settings = true,
        fullscreen = true,
        exitFullscreen = true
    )
)
```

Controls can also be configured per item:

```kotlin
ReelsPlayerModel(
    id = "reel-1",
    hlsUrl = "https://example.com/reel.m3u8",
    imageUrl = "https://example.com/reel.jpg",
    controlsConfig = ReelsPlayerControlsConfig(
        share = true,
        bookmark = false,
        seekbar = true,
        settings = true,
        fullscreen = true,
        exitFullscreen = true
    )
)
```

Per-item `controlsConfig` takes priority over global `ReelsPlayerConfig.controls`.
In the reels UI, the rendered controls are `play`, `pause`, `share`, `bookmark`, `seekbar`, `settings`, `fullscreen`, and `exitFullscreen`.

## 9. Custom Icons And Color

The host app can provide icons through `ReelsPlayerCustomControls`.

```kotlin
import com.app.reelssdk.model.ReelsPlayerCustomControls

val controls = ReelsPlayerCustomControls(
    iconTintRes = R.color.white,
    playIconRes = R.drawable.ic_play,
    pauseIconRes = R.drawable.ic_pause,
    settingsIconRes = R.drawable.ic_settings,
    fullScreenIconRes = R.drawable.ic_expand,
    exitFullScreenIconRes = R.drawable.ic_collapse,
    shareIconRes = R.drawable.ic_share,
    bookmarkIconRes = R.drawable.ic_bookmark,
    crossFadeIconRes = R.drawable.ic_close
)

val reel = ReelsPlayerModel(
    id = "reel-1",
    hlsUrl = "https://example.com/reel.m3u8",
    imageUrl = "https://example.com/reel.jpg",
    customControls = controls
)
```

Use vector drawables or PNG drawables from the host app. If a drawable is not supplied, the SDK uses its default Compose icon.

## 10. Reels UI Behavior

The reels player provides:

- Vertical scroll feed.
- Active page playback only.
- Next page prefetch signal.
- Media3 video surface remains visible while the first frame is decoded.
- Fast-start playback using initial low bitrate and short startup buffer.
- Automatic adaptive quality upgrade after first rendered frame.
- Tap anywhere to show hidden controls.
- Auto-hide controls while video is playing.
- Controls stay visible while paused, settings/share sheet is open, or seekbar is being scrubbed.
- Expand/collapse changes only video surface size, not the controls positions.
- No floating plus button on the player.

## 11. Share Functionality

If `controls.share = true`, the reels player shows a custom bottom sheet with installed share apps:

- WhatsApp
- Facebook
- Instagram
- More

Apps that are not installed or not resolvable are hidden.

Share text is built from:

1. `title` / `episodeTitle`
2. `shareUrl`
3. fallback playback URL if no public share URL exists

Recommended:

```kotlin
ReelsPlayerModel(
    id = "reel-1",
    title = "Yoga Short",
    hlsUrl = "https://cdn.example.com/reel/master.m3u8",
    imageUrl = "https://cdn.example.com/reel/poster.jpg",
    shareUrl = "https://example.com/reels/yoga-short"
)
```

## 12. Settings Bottom Sheet

If `controls.settings = true`, reels mode opens a bottom sheet with:

- Audio Track
- Close Caption
- Speed Selector
- Video Quality

The left side shows the categories vertically. The right side shows the selected category items. Selected items remain checked while the current reel player page remains open.

Settings are applied directly to the active Media3 player.

## 13. Expand And Collapse

If `controls.fullscreen` and `controls.exitFullscreen` are enabled:

- Collapsed mode keeps video in normal 16:9-style surface centered in the reels screen.
- Expanded mode scales the video edge-to-edge.
- Controls keep the same screen position while expand/collapse changes only the video surface.

## 14. Recommended Activity Setup

For a reels-only screen, make the screen edge-to-edge and let the player fill available space.

```kotlin
class ReelsActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ReelsPlayerScreen(
                reels = buildReels(),
                onClose = { finish() }
            )
        }
    }
}
```

If your app uses a Compose `NavHost`, place `ReelsPlayerScreen` as a full-screen destination.

## 15. Legacy Parameters

This reels branch always renders the reels player. Existing SDK parameters such as `mode`, `playerMode`, `pipListener`, and `isInPipMode` are kept only for source compatibility with older app code and are ignored by the reels implementation. New integrations should not pass those parameters.

## 16. Publishing On JitPack

Before creating a release tag, verify the SDK from a clean `reelssdk` branch checkout:

```bash
git switch reelssdk
git pull --ff-only origin reelssdk
git status --short
./gradlew :reelssdk:assembleRelease
./gradlew :reelssdk:publishToMavenLocal
./gradlew :reelssdk:lintRelease
```

The repository contains `jitpack.yml` with:

```yaml
jdk:
  - openjdk17

install:
  - ./gradlew :reelssdk:publishToMavenLocal
```

The `reelssdk` module uses `maven-publish`, publishes the `release` variant, and includes a sources jar. For a new public release, update `reelssdk/build.gradle.kts`:

```kotlin
groupId = "com.github.kamleshmultitv.mtvplayersdk"
artifactId = "reelssdk"
version = "reels-1.0.1"
```

After `publishToMavenLocal`, verify the generated Maven artifact exists:

```bash
ls ~/.m2/repository/com/github/kamleshmultitv/mtvplayersdk/reelssdk/reels-1.0.1
```

Expected files include:

- `reelssdk-reels-1.0.1.aar`
- `reelssdk-reels-1.0.1.pom`
- `reelssdk-reels-1.0.1-sources.jar`

Then commit, push, and tag the exact commit:

```bash
git add .
git commit -m "Release reels SDK reels-1.0.1"
git tag reels-1.0.1
git push origin reelssdk
git push origin reels-1.0.1
```

After JitPack finishes building the tag, third-party apps can use:

```kotlin
implementation("com.github.kamleshmultitv.mtvplayersdk:reelssdk:reels-1.0.1")
```

Keep release work branch-safe:

```bash
git status --short
git switch androidReels
git switch androidMobile
```

Switch branches only with a clean working tree or after committing/stashing. Reels SDK work should be committed and pushed only on `reelssdk`; OTT/mobile work should be committed and pushed only on `androidMobile`.

## 17. DRM Content

For Widevine/DASH:

```kotlin
ReelsPlayerModel(
    id = "drm-reel",
    mpdUrl = "https://example.com/manifest.mpd",
    drm = "1",
    drmToken = "https://license.example.com/widevine?token=abc",
    imageUrl = "https://example.com/poster.jpg"
)
```

For non-DRM HLS:

```kotlin
ReelsPlayerModel(
    id = "hls-reel",
    hlsUrl = "https://example.com/master.m3u8",
    imageUrl = "https://example.com/poster.jpg"
)
```

## 18. Subtitles

Pass an SRT URL:

```kotlin
ReelsPlayerModel(
    id = "reel-with-subtitles",
    hlsUrl = "https://example.com/master.m3u8",
    srt = "https://example.com/subtitles.srt"
)
```

The settings bottom sheet shows available text tracks and lets the user select a caption.

## 19. Testing Checklist

Before release, verify:

- Reels list opens full screen.
- First visible reel starts automatically.
- Next reel starts when scrolled into view.
- Previous/inactive reels pause.
- First decoded frame is rendered by the player surface instead of a thumbnail overlay.
- Slow networks show loading state until playback is ready.
- Tap anywhere shows controls.
- Playing video auto-hides controls after a short delay.
- Paused video keeps controls visible.
- Seekbar scrub keeps controls visible.
- Expand/collapse changes video size only.
- Share sheet shows only installed apps.
- More opens Android chooser.
- Settings sheet preserves selected items when reopened.
- Disabled controls are hidden.
- Back navigation closes the reels screen.
- Paging loads more data before the user reaches the end.

## 20. Troubleshooting

### Black screen or loader before playback

Optimize the playback stream for fast startup. Prefer HLS/DASH ladders with short initial segments and reachable CDN URLs. The reels player keeps the Media3 surface visible and does not use `imageUrl` or `thumbnail` as a startup overlay.

### Controls do not appear

Make sure the item/global config has the relevant flags enabled:

```kotlin
ReelsPlayerControlsConfig(
    play = true,
    pause = true,
    seekbar = true,
    settings = true,
    share = true
)
```

### Share apps do not show

Confirm apps are installed and the manifest `queries` entries are present after manifest merge.

### Settings selected item does not show

Keep the same reel page alive. Selection state is preserved while the current reels player page remains composed.

### Wrong item plays after scroll

Use stable `ReelsPlayerModel.id` values and do not reorder `contentList` unexpectedly while the user is scrolling.

### App reloads all players on data update

Append new items to the existing feed state instead of replacing the entire screen state on every callback.

## 21. Minimal Production Template

Example state shape:

```kotlin
data class ReelsUiState(
    val items: List<ApiReel> = emptyList(),
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true
)
```

```kotlin
@Composable
fun ThirdPartyReelsScreen(
    state: ReelsUiState,
    loadMore: () -> Unit,
    close: () -> Unit
) {
    val playerModels = state.items.map { item ->
        ReelsPlayerModel(
            id = item.id,
            hlsUrl = item.hlsUrl,
            mpdUrl = item.mpdUrl,
            drm = if (item.isDrm) "1" else "0",
            drmToken = item.drmLicenseUrl,
            imageUrl = item.posterUrl,
            thumbnail = item.thumbnailUrl,
            title = item.title,
            episodeTitle = item.title,
            description = item.description,
            shareUrl = item.shareUrl,
            srt = item.subtitleUrl,
            customControls = ReelsPlayerCustomControls(
                iconTintRes = R.color.white,
                playIconRes = R.drawable.ic_play,
                pauseIconRes = R.drawable.ic_pause,
                settingsIconRes = R.drawable.ic_settings,
                fullScreenIconRes = R.drawable.ic_expand,
                exitFullScreenIconRes = R.drawable.ic_collapse,
                shareIconRes = R.drawable.ic_share,
                bookmarkIconRes = R.drawable.ic_bookmark
            )
        )
    }

    MtvReelsPlayerSdk(
        contentList = playerModels,
        index = 0,
        playerConfig = ReelsPlayerConfig(
            controls = ReelsPlayerControlsConfig(
                play = true,
                pause = true,
                seekbar = true,
                settings = true,
                share = true,
                bookmark = true,
                fullscreen = true,
                exitFullscreen = true
            )
        ),
        playerStateListener = object : ReelsPlayerStateListener {
            override fun onPreloadNext(index: Int) {
                val remainingItems = playerModels.size - 1 - index
                if (remainingItems <= 3 && !state.isLoadingMore && state.hasMore) {
                    loadMore()
                }
            }
        },
        onPlayerBack = { close() },
        setFullScreen = {}
    )
}
```

This is the recommended baseline for third-party apps integrating reels playback.
