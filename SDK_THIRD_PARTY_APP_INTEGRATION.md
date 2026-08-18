# Mtv Video Player SDK Third-Party App Integration Guide

Use this file when a third-party Android app does not have access to the SDK repository docs.

This is the app-facing integration contract. Do not use SDK internals or copy SDK source files into the host app.

## 1. Dependency Setup

Add JitPack in the host app root `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

Add the SDK dependency in the host app module `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.kamleshmultitv:mtvplayersdk:mobile-x.y.z")
}
```

Replace `mobile-x.y.z` with the release tag shared by the SDK owner.

## 2. Required Android Setup

Add permissions in the host app manifest:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

Enable Compose in the host app module if the app uses the Compose SDK entry point:

```kotlin
android {
    buildFeatures {
        compose = true
    }
}
```

For Picture-in-Picture, configure the host activity:

```xml
<activity
    android:name=".MainActivity"
    android:configChanges="screenSize|smallestScreenSize|screenLayout|orientation"
    android:resizeableActivity="true"
    android:supportsPictureInPicture="true" />
```

For Google Cast, provide the receiver app id:

```xml
<string name="app_id_prod">YOUR_CAST_RECEIVER_APP_ID</string>
```

For Google ads, add the host app AdMob app id:

```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy" />
```

## 3. Basic Compose Player

Map the host app content model into `PlayerModel` and render `MtvVideoPlayerSdk`.

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.app.videosdk.listener.PlayerController
import com.app.videosdk.listener.PlayerStateListener
import com.app.videosdk.model.PlayerConfig
import com.app.videosdk.model.PlayerFeatureTier
import com.app.videosdk.model.PlayerModel
import com.app.videosdk.ui.MtvVideoPlayerSdk
import com.app.videosdk.utils.PlayerMode

@Composable
fun ContentDetailPlayer(
    content: HostVideo,
    isFullScreen: Boolean,
    onFullScreenChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val controller = remember { PlayerController() }

    val video = PlayerModel(
        id = content.id,
        hlsUrl = content.hlsUrl,
        mpdUrl = content.dashUrl,
        videoUrl = content.mp4Url,
        liveUrl = content.liveUrl,
        isLive = content.isLive,
        drm = content.drmFlag,
        drmToken = content.drmLicenseUrlOrToken,
        imageUrl = content.thumbnailUrl,
        title = content.title,
        episodeTitle = content.episodeTitle,
        seasonTitle = content.seriesTitle,
        description = content.description,
        srt = content.subtitleUrl,
        ageRating = content.ageRating
    )

    MtvVideoPlayerSdk(
        contentList = listOf(video),
        index = 0,
        controller = controller,
        playerMode = if (isFullScreen) PlayerMode.FULL_SCREEN else PlayerMode.MINI,
        playerConfig = PlayerConfig(
            featureTier = PlayerFeatureTier.LEGACY_COMPAT
        ),
        playerStateListener = object : PlayerStateListener {
            override fun onPlayerReady(durationMs: Long) {}
            override fun onPlayStateChanged(isPlaying: Boolean) {}
            override fun onFullScreenChanged(isFullScreen: Boolean) {}
            override fun onPlaybackCompleted() {}
        },
        onPlayerBack = {
            if (isFullScreen) {
                onFullScreenChange(false)
            } else {
                onBack()
            }
        },
        setFullScreen = onFullScreenChange
    )
}
```

Keep the SDK mapping in one player screen or mapper. The rest of the host app should not depend on SDK internals.

## 4. Feature Package Selection

Use `PlayerConfig.featureTier` to choose the customer package:

```kotlin
PlayerConfig(featureTier = PlayerFeatureTier.BASIC_PLAYER)
PlayerConfig(featureTier = PlayerFeatureTier.PREMIUM_UX)
PlayerConfig(featureTier = PlayerFeatureTier.ENTERPRISE_PLAYBACK)
```

Feature tiers:

- `LEGACY_COMPAT`: compatibility default for existing integrations.
- `BASIC_PLAYER`: core playback, fullscreen, subtitles, quality, speed, and callbacks.
- `PREMIUM_UX`: Basic plus Cast, PiP, skip intro, next episode, episode selector, chapters, sprite thumbnails, and custom controls.
- `ENTERPRISE_PLAYBACK`: Premium UX plus DRM, offline playback, deep-link clips, watermark, age rating, and free preview.

Use ad monetization only when the app has ad setup:

```kotlin
PlayerConfig(
    featureTier = PlayerFeatureTier.ENTERPRISE_PLAYBACK,
    monetizationPackage = PlayerMonetizationPackage.AD_SUPPORTED,
    ads = PlayerAdsConfig(
        googleAdsEnabled = true,
        vmapAdsEnabled = true,
        bannerAdsEnabled = true
    )
)
```

## 5. Common PlayerModel Fields

Use only public `PlayerModel` fields:

- Playback: `hlsUrl`, `mpdUrl`, `videoUrl`, `liveUrl`, `isLive`
- DRM: `drm`, `drmToken`
- Metadata: `id`, `title`, `episodeTitle`, `seasonTitle`, `description`, `imageUrl`, `thumbnail`
- Subtitles: `srt`
- Age rating: `ageRating` or `contentRating`
- Timed actions: `skipIntro`, `nextEpisode`, `chapters`
- Ads: `adsConfig`, `gamAdsConfig`
- UI: `customControls`

For DRM, pass the DASH URL in `mpdUrl`, set `drm = "1"`, and pass the license/token value in `drmToken`.

## 6. Subtitles, Settings, Speed, And Quality

Pass subtitle URL:

```kotlin
PlayerModel(
    hlsUrl = "https://example.com/video/master.m3u8",
    srt = "https://example.com/subtitles/en.srt"
)
```

The SDK owns settings selection state internally. Host apps do not need to pass selected state for:

- audio track
- closed caption / caption off
- playback speed
- video quality / auto quality

Selections remain visually checked while the same player/content-detail session and media source are alive. They reset when the player page is disposed or the media source changes.

## 7. Skip Intro, Next Episode, And Chapters

```kotlin
PlayerModel(
    hlsUrl = "https://example.com/video/master.m3u8",
    skipIntro = SkipIntro(
        startTime = 5_000L,
        endTime = 95_000L,
        enableSkipIntro = true
    ),
    nextEpisode = NextEpisode(
        showBeforeEndMs = "30000",
        enableNextEpisode = true
    ),
    isChapterEnabled = true,
    chapters = listOf(
        Chapter(id = "intro", title = "Intro", startMs = 0L),
        Chapter(id = "middle", title = "Middle", startMs = 60_000L)
    )
)
```

## 8. Ads

IMA / VMAP:

```kotlin
PlayerModel(
    hlsUrl = "https://example.com/video/master.m3u8",
    adsConfig = AdsConfig(
        adTagUrl = "https://pubads.g.doubleclick.net/gampad/ads?...",
        enableAds = true
    )
)
```

GAM / banner:

```kotlin
PlayerModel(
    hlsUrl = "https://example.com/video/master.m3u8",
    gamAdsConfig = GAMAdsConfig(
        verticalBan = "/network/ad-unit-vertical",
        horizontalBan = "/network/ad-unit-horizontal",
        timeIntervalInMilliseconds = 600_000L,
        isAdsEnabled = true
    )
)
```

The host app must provide valid ad ids and reachable ad tags.

## 9. Custom Controls

```kotlin
PlayerModel(
    hlsUrl = "https://example.com/video/master.m3u8",
    customControls = PlayerCustomControls(
        playIconRes = R.drawable.ic_play,
        pauseIconRes = R.drawable.ic_pause,
        forwardIconRes = R.drawable.ic_forward_10,
        rewindIconRes = R.drawable.ic_replay_10,
        settingsIconRes = R.drawable.ic_settings,
        fullScreenIconRes = R.drawable.ic_fullscreen,
        exitFullScreenIconRes = R.drawable.ic_fullscreen_exit
    )
)
```

Use vector drawables or density-safe raster assets.

## 10. Callbacks And Observability

Use `PlayerStateListener` for host analytics and UI updates:

```kotlin
playerStateListener = object : PlayerStateListener {
    override fun onPlayerReady(durationMs: Long) {}
    override fun onBuffering(isBuffering: Boolean) {}
    override fun onPlayStateChanged(isPlaying: Boolean) {}
    override fun onPlaybackCompleted() {}
    override fun onFullScreenChanged(isFullScreen: Boolean) {}
    override fun onPipModeChanged(isInPip: Boolean) {}
    override fun onAdStateChanged(isAdPlaying: Boolean) {}
    override fun onQualityChanged(width: Int, height: Int, label: String?) {}
    override fun onSubtitleChanged(language: String?, label: String?, enabled: Boolean) {}
    override fun onPlaybackSpeedChanged(speed: Float) {}
}
```

Enable SDK analytics, diagnostics, or logs only when needed:

```kotlin
PlayerConfig(
    analyticsEnabled = true,
    diagnosticsEnabled = true,
    logging = SdkLoggingConfig(level = SdkLogLevel.DEBUG)
)
```

SDK logs are default-off and redact sensitive media/ad/DRM values.

## 11. Programmatic Control

Create a stable `PlayerController` with `remember` and pass it to the SDK:

```kotlin
val controller = remember { PlayerController() }

MtvVideoPlayerSdk(
    contentList = listOf(video),
    controller = controller
)
```

Use:

```kotlin
controller.togglePlayPause()
controller.toggleMute()
controller.play(nextVideo)
```

## 12. Integration Rules

- Do not edit SDK internals in the host app.
- Do not copy SDK source files into the host app.
- Do not rely on undocumented composables or internal state.
- Keep `PlayerController` stable with `remember`.
- Keep `contentList` and `index` stable for the active detail page.
- Let the SDK own settings selected-state UI.
- Use `PlayerStateListener` callbacks for host analytics.
- Use the exact SDK version tag provided by the SDK owner.

## 13. Smoke Test Checklist

Before release, test:

- HLS playback.
- DASH playback if used.
- DRM playback if used.
- Fullscreen enter/exit.
- PiP enter/exit if enabled.
- Settings reopen: audio, caption, speed, and quality selected rows stay checked.
- Subtitles on/off.
- Quality auto/manual.
- Cast if enabled.
- Ads if enabled.
- Skip intro, next episode, and chapters if configured.
- Back behavior from fullscreen and mini player.
