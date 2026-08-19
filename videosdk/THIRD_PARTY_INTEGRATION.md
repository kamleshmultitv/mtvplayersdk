# Mtv Video Player SDK Third-Party App Integration Guide

Use this file when a third-party Android app does not have access to the SDK repository docs.

This is the app-facing integration contract. Do not use SDK internals or copy SDK source files into the host app.

## Which Guide To Use

- VideoPlayer SDK in a third-party app: follow this file.
- Downloader SDK implementation: follow `../mtvdownloader/THIRD_PARTY_INTEGRATION.md`.
- Combined Downloader SDK plus VideoPlayer SDK main-app migration: follow `../app/main-application-full-change-guide.md`.
- VideoPlayer SDK source changes: `README.md` is internal SDK module material, not a third-party app integration guide.

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
- Offline/download: `cacheFactory`, `downloadManager`, `downloadCache`, `drmOfflineKeySetId`, `drmOfflineKeySetIdBase64`
- Metadata: `id`, `title`, `episodeTitle`, `seasonTitle`, `description`, `imageUrl`, `thumbnail`
- Subtitles: `srt`
- Age rating: `ageRating` or `contentRating`
- Timed actions: `skipIntro`, `nextEpisode`, `chapters`
- Ads: `adsConfig`, `gamAdsConfig`
- UI: `customControls`

For online DRM, pass the DASH URL in `mpdUrl`, set `drm = "1"`, and pass the license/token value in `drmToken`.

Do not set `drm = "1"` globally for normal HLS/API-list playback. Set it only when the player should choose MPD/DASH Widevine playback. If the app sets `drm = "1"`, the SDK prefers `mpdUrl` and the license server must accept the supplied license URL/token.

## 6. Offline Downloads With MTV Downloader SDK

If the app also uses the MTV Downloader SDK, follow `../mtvdownloader/THIRD_PARTY_INTEGRATION.md` for the download layer. Playback still uses this VideoPlayer SDK.

When a user opens a completed download, map `DownloadedContentEntity` into `PlayerModel`. The app must pass the same Media3 `DownloadManager` and `SimpleCache` that the downloader uses. For DRM MPD/DASH downloads, also pass the persisted Widevine offline key set.

```kotlin
import android.content.Context
import com.app.mtvdownloader.DownloadUtil
import com.app.mtvdownloader.local.entity.DownloadedContentEntity
import com.app.videosdk.model.PlayerModel

fun buildDownloadedPlayerModel(
    context: Context,
    entity: DownloadedContentEntity
): PlayerModel {
    val contentUrl = entity.contentUrl?.takeIf { it.isNotBlank() }
    val mimeType = entity.contentMimeType.orEmpty()
    val licenseUri = entity.licenseUri?.takeIf { it.isNotBlank() }
    val offlineKeySetBase64 = entity.drmOfflineKeySetIdBase64?.takeIf { it.isNotBlank() }
    val isDrm = licenseUri != null ||
        offlineKeySetBase64 != null ||
        entity.drmOfflineKeySetId?.isNotEmpty() == true

    return PlayerModel(
        id = entity.contentId,
        hlsUrl = contentUrl?.takeIf {
            mimeType == "application/x-mpegURL" ||
                it.endsWith(".m3u8", ignoreCase = true)
        },
        mpdUrl = contentUrl?.takeIf {
            mimeType == "application/dash+xml" ||
                it.endsWith(".mpd", ignoreCase = true)
        },
        videoUrl = contentUrl?.takeIf {
            mimeType == "video/mp4" ||
                it.endsWith(".mp4", ignoreCase = true) ||
                it.endsWith(".m4v", ignoreCase = true)
        },
        drm = if (isDrm) "1" else null,
        drmToken = licenseUri,
        imageUrl = entity.thumbnailUrl ?: entity.seasonImage,
        title = entity.title,
        episodeTitle = entity.title,
        seasonTitle = entity.seasonName,
        selectedVideoQuality = entity.videoHeight ?: 1080,
        isLive = false,
        downloadManager = DownloadUtil.getDownloadManager(context),
        downloadCache = DownloadUtil.getDownloadCache(context),
        drmOfflineKeySetId = entity.drmOfflineKeySetId,
        drmOfflineKeySetIdBase64 = offlineKeySetBase64
    )
}
```

Then open the downloaded item with the same SDK entry point:

```kotlin
MtvVideoPlayerSdk(
    contentList = listOf(buildDownloadedPlayerModel(context, entity)),
    index = 0,
    playerMode = PlayerMode.FULL_SCREEN
)
```

For DRM MPD/DASH offline playback, the player restores the offline license from `DownloadRequest.keySetId`, `drmOfflineKeySetId`, or `drmOfflineKeySetIdBase64`. The offline MPD media item should not need a fresh online license while the device is offline.

## 7. Subtitles, Settings, Speed, And Quality

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

## 8. Skip Intro, Next Episode, And Chapters

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

## 9. Ads

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

## 10. Custom Controls

```kotlin
PlayerModel(
    hlsUrl = "https://example.com/video/master.m3u8",
    customControls = PlayerCustomControls(
        playIconRes = R.drawable.ic_play,
        pauseIconRes = R.drawable.ic_pause,
        forwardIconRes = R.drawable.ic_forward_10,
        rewindIconRes = R.drawable.ic_replay_10,
        settingsIconRes = R.drawable.ic_settings,
        castIconRes = R.drawable.ic_cast,
        castConnectedIconRes = R.drawable.ic_cast_connected,
        fullScreenIconRes = R.drawable.ic_fullscreen,
        exitFullScreenIconRes = R.drawable.ic_fullscreen_exit
    )
)
```

`iconTintRes` tints SDK icons plus Cast, seekbar progress, brightness progress, and volume progress.
If `castConnectedIconRes` is omitted, the SDK switches to its built-in connected Cast icon when a Cast route is connected.

Use vector drawables or density-safe raster assets.

## 11. Callbacks And Observability

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

## 12. Programmatic Control

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

## 13. Integration Rules

- Do not edit SDK internals in the host app.
- Do not copy SDK source files into the host app.
- Do not use `README.md` as the app integration guide.
- Do not rely on undocumented composables or internal state.
- Keep `PlayerController` stable with `remember`.
- Keep `contentList` and `index` stable for the active detail page.
- Let the SDK own settings selected-state UI.
- Use `PlayerStateListener` callbacks for host analytics.
- Use the exact SDK version tag provided by the SDK owner.

## 14. Smoke Test Checklist

Before release, test:

- HLS playback.
- DASH playback if used.
- DRM playback if used.
- Downloaded HLS/MP4 playback if the app uses downloads.
- Downloaded DRM MPD/DASH playback in airplane mode after keySetId is saved.
- Fullscreen enter/exit.
- PiP enter/exit if enabled.
- Settings reopen: audio, caption, speed, and quality selected rows stay checked.
- Subtitles on/off.
- Quality auto/manual.
- Cast if enabled.
- Ads if enabled.
- Skip intro, next episode, and chapters if configured.
- Back behavior from fullscreen and mini player.
