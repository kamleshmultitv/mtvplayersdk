# Reels Player Implementation Guide

This guide explains how a third-party Android app should integrate the MTV Video Player SDK reels player.

The reels player is a full-screen vertical feed player. It supports smooth page scrolling, next-item prefetch callbacks, first-frame poster display, fast-start playback, seekbar, play/pause, expand/collapse, share sheet, settings bottom sheet, and app-provided control icons.

## 1. Requirements

- Android min SDK: 24
- Kotlin Android app
- Jetpack Compose enabled
- Internet playback URLs, preferably HLS (`.m3u8`) or DASH (`.mpd`)
- Poster image URL per reel for best first-frame experience

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
    implementation("com.github.kamleshmultitv.mtvplayersdk:videosdk:reels-1.0.0")
}
```

This branch publishes the `videosdk` Android library artifact. The current publication values are:

- `groupId`: `com.github.kamleshmultitv.mtvplayersdk`
- `artifactId`: `videosdk`
- `version`: `reels-1.0.0`

This is a multi-module repository, so third-party apps should depend on the `videosdk` module artifact, not the sample `app` module or the downloader module. If you publish a different Git tag/version, replace `reels-1.0.0` with that release version.

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

## 4. Map Your API Data To PlayerModel

Each reel item must become a `PlayerModel`.

Use:

- `id`: stable content id.
- `hlsUrl`: HLS playback URL.
- `mpdUrl`: DASH playback URL, especially for DRM content.
- `drm = "1"` and `drmToken`: if playback uses Widevine license/token.
- `imageUrl` or `thumbnail`: poster shown until first video frame is rendered.
- `title` or `episodeTitle`: title used by share text and metadata.
- `shareUrl`: public content URL for share sheet.
- `srt`: subtitle URL, if available.
- `spriteUrl`: thumbnail sprite URL for seek preview, if available.
- `customControls`: optional app drawables for icons.
- `controlsConfig`: optional per-content control visibility override.

Example mapper:

```kotlin
import com.app.videosdk.model.PlayerModel

fun ApiReel.toPlayerModel(): PlayerModel {
    return PlayerModel(
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

For fastest perceived startup, always pass `imageUrl` or `thumbnail`. The reels player displays this image until Media3 renders the first video frame.

## 5. Basic Reels Screen

Use `MtvVideoPlayerSdk` directly. This branch is reels-only, so `mode` and `playerMode` are not required.

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.app.videosdk.model.PlayerModel
import com.app.videosdk.ui.MtvVideoPlayerSdk

@Composable
fun ReelsPlayerScreen(
    reels: List<PlayerModel>,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        MtvVideoPlayerSdk(
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

Use a `PlayerStateListener` to observe current reel index and prefetch the next API item/page.

```kotlin
import android.util.Log
import androidx.compose.runtime.Composable
import com.app.videosdk.listener.PlayerStateListener
import com.app.videosdk.model.PlayerConfig
import com.app.videosdk.model.PlayerControlsConfig
import com.app.videosdk.model.PlayerModel
import com.app.videosdk.ui.MtvVideoPlayerSdk

@Composable
fun ReelsFeedPlayer(
    reels: List<PlayerModel>,
    onPreloadMore: (nextIndex: Int) -> Unit,
    onReelChanged: (index: Int) -> Unit,
    onBack: () -> Unit
) {
    MtvVideoPlayerSdk(
        contentList = reels,
        index = 0,
        playerConfig = PlayerConfig(
            controls = PlayerControlsConfig(
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
        playerStateListener = object : PlayerStateListener {
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

The SDK calls:

- `onReelChanged(position)` when the visible reel changes.
- `onPreloadNext(index)` when the SDK needs the next item to be available soon.

Recommended app-side behavior:

```kotlin
override fun onPreloadNext(index: Int) {
    val shouldLoadMore = index >= reels.lastIndex - 2
    if (shouldLoadMore && !isLoadingNextPage) {
        loadNextReelsPage()
    }
}
```

Do not rebuild a completely new `contentList` object on every frame. Update the list only when new data arrives.

## 8. Control Visibility Flags

Controls can be configured globally through `PlayerConfig.controls`:

```kotlin
PlayerConfig(
    controls = PlayerControlsConfig(
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
PlayerModel(
    id = "reel-1",
    hlsUrl = "https://example.com/reel.m3u8",
    imageUrl = "https://example.com/reel.jpg",
    controlsConfig = PlayerControlsConfig(
        share = true,
        bookmark = false,
        seekbar = true,
        settings = true,
        fullscreen = true,
        exitFullscreen = true
    )
)
```

Per-item `controlsConfig` takes priority over global `PlayerConfig.controls`.
In the reels UI, the rendered controls are `play`, `pause`, `share`, `bookmark`, `seekbar`, `settings`, `fullscreen`, and `exitFullscreen`.

## 9. Custom Icons And Color

The host app can provide icons through `PlayerCustomControls`.

```kotlin
import com.app.videosdk.model.PlayerCustomControls

val controls = PlayerCustomControls(
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

val reel = PlayerModel(
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
- Poster image until first video frame.
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
PlayerModel(
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

Before creating a release tag, verify the SDK from a clean `androidReels` checkout:

```bash
git switch androidReels
git pull --ff-only origin androidReels
git status --short
./gradlew :videosdk:assembleRelease
./gradlew :videosdk:publishToMavenLocal
./gradlew :videosdk:lintRelease
```

The repository contains `jitpack.yml` with:

```yaml
jdk:
  - openjdk17

install:
  - ./gradlew :videosdk:publishToMavenLocal
```

The `videosdk` module uses `maven-publish`, publishes the `release` variant, and includes a sources jar. For a new public release, update `videosdk/build.gradle.kts`:

```kotlin
groupId = "com.github.kamleshmultitv.mtvplayersdk"
artifactId = "videosdk"
version = "reels-1.0.0"
```

After `publishToMavenLocal`, verify the generated Maven artifact exists:

```bash
ls ~/.m2/repository/com/github/kamleshmultitv/mtvplayersdk/videosdk/reels-1.0.0
```

Expected files include:

- `videosdk-reels-1.0.0.aar`
- `videosdk-reels-1.0.0.pom`
- `videosdk-reels-1.0.0-sources.jar`

Then commit, push, and tag the exact commit:

```bash
git add .
git commit -m "Release reels SDK reels-1.0.0"
git tag reels-1.0.0
git push origin androidReels
git push origin reels-1.0.0
```

After JitPack finishes building the tag, third-party apps can use:

```kotlin
implementation("com.github.kamleshmultitv.mtvplayersdk:videosdk:reels-1.0.0")
```

Keep release work branch-safe:

```bash
git status --short
git switch androidReels
git switch androidMobile
```

Switch branches only with a clean working tree or after committing/stashing. Reels work should be committed and pushed only on `androidReels`; OTT/mobile work should be committed and pushed only on `androidMobile`.

## 17. DRM Content

For Widevine/DASH:

```kotlin
PlayerModel(
    id = "drm-reel",
    mpdUrl = "https://example.com/manifest.mpd",
    drm = "1",
    drmToken = "https://license.example.com/widevine?token=abc",
    imageUrl = "https://example.com/poster.jpg"
)
```

For non-DRM HLS:

```kotlin
PlayerModel(
    id = "hls-reel",
    hlsUrl = "https://example.com/master.m3u8",
    imageUrl = "https://example.com/poster.jpg"
)
```

## 18. Subtitles

Pass an SRT URL:

```kotlin
PlayerModel(
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
- Poster image appears before first frame.
- No black frame appears on slow networks.
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

### Black screen before playback

Provide `imageUrl` or `thumbnail` for every reel. The SDK uses it as the startup poster until the first frame renders.

### Controls do not appear

Make sure the item/global config has the relevant flags enabled:

```kotlin
PlayerControlsConfig(
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

Use stable `PlayerModel.id` values and do not reorder `contentList` unexpectedly while the user is scrolling.

### App reloads all players on data update

Append new items to the existing feed state instead of replacing the entire screen state on every callback.

## 21. Minimal Production Template

```kotlin
@Composable
fun ThirdPartyReelsScreen(
    state: ReelsUiState,
    loadMore: () -> Unit,
    close: () -> Unit
) {
    val playerModels = state.items.map { item ->
        PlayerModel(
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
            customControls = PlayerCustomControls(
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

    MtvVideoPlayerSdk(
        contentList = playerModels,
        index = 0,
        playerConfig = PlayerConfig(
            controls = PlayerControlsConfig(
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
        playerStateListener = object : PlayerStateListener {
            override fun onPreloadNext(index: Int) {
                if (index >= playerModels.lastIndex - 2 && !state.isLoadingMore) {
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
