# Mtv Video Player SDK for Android

Mtv Video Player SDK is a Jetpack Compose video player built on AndroidX Media3. It supports HLS, DASH, MP4, Widevine DRM, Cast, PiP, ads, subtitles, skip intro, next episode, chapters, sprites, live playback, and fullscreen/reels style playback modes.

## Features

- HLS, DASH, MP4, and live stream playback
- Widevine DRM playback with license URL/token support
- Jetpack Compose controller UI
- Picture-in-Picture support
- Fullscreen and mini player modes
- Reels playback mode
- Chromecast / Google Cast controls
- SRT subtitles
- Playback speed and quality selection
- Skip intro and next episode actions
- Chapter markers and IMA ad cue markers
- Sprite thumbnail preview while scrubbing
- Free preview and preview-end callbacks
- IMA / Google ad configuration
- Custom control icons and control visibility config
- Age rating overlay
- Watermark config
- Feature tiers and monetization package gates
- Optional analytics, diagnostics, and redacted SDK logging

## Documentation Map

| Document | Purpose |
| --- | --- |
| `SDK_PUBLIC_API.md` | Stable SDK API surface and compatibility rules. |
| `SDK_THIRD_PARTY_APP_INTEGRATION.md` | Self-contained integration guide to share with third-party apps that cannot access repo docs. |
| `SDK_HOST_APP_INTEGRATION.md` | Step-by-step guide for Codex or a developer integrating the SDK into a third-party host app. |
| `SDK_ENTERPRISE_RELEASE_READINESS.md` | Enterprise customer release gates, no-go conditions, and target risk level. |
| `SDK_ENTERPRISE_QA_RUNBOOK.md` | Phase 7 enterprise QA execution order, evidence format, and release decision rules. |
| `SDK_CUSTOMER_DEMO_PROFILES.md` | Customer-style feature profiles for OTT, DRM/offline, live, spiritual/event, and ad-supported demos. |
| `SDK_FEATURE_PACKAGES.md` | Basic, Premium UX, Enterprise, and monetization add-on package definitions. |
| `SDK_OBSERVABILITY.md` | Analytics events, diagnostics callbacks, and SDK logging setup. |
| `SDK_INTEGRATION_GUIDE.md` | DRM, Cast, PiP, offline, ads, and custom-control setup notes. |
| `SDK_QA_MATRIX.md` | Scenario matrix for sample app and device validation. |
| `SDK_RELEASE_CHECKLIST.md` | Repeatable release checklist. |
| `CHANGELOG.md` | Release notes and pre-tag verification reminders. |

## Installation

Add JitPack in your project-level `settings.gradle.kts`:

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

Add the SDK dependency in your app module:

```kotlin
dependencies {
    implementation("com.github.kamleshmultitv:mtvplayersdk:mobile-2.0.30")
}
```

## Android Setup

Minimum supported SDK is API 24.

Add the required permissions:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

For Picture-in-Picture, configure your activity:

```xml
<activity
    android:name=".MainActivity"
    android:configChanges="screenSize|smallestScreenSize|screenLayout|orientation"
    android:resizeableActivity="true"
    android:supportsPictureInPicture="true" />
```

Enable Compose in your app module:

```kotlin
android {
    buildFeatures {
        compose = true
    }
}
```

If you use IMA ads, enable core library desugaring:

```kotlin
android {
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
}
```

If you use Google Mobile Ads, add your AdMob app id:

```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy" />
```

## Basic Usage

```kotlin
import androidx.compose.runtime.Composable
import com.app.videosdk.model.PlayerModel
import com.app.videosdk.ui.MtvVideoPlayerSdk
import com.app.videosdk.utils.PlayerMode

@Composable
fun PlayerScreen(
    isFullScreen: Boolean,
    onFullScreenChange: (Boolean) -> Unit
) {
    val contentList = listOf(
        PlayerModel(
            id = "episode-1",
            hlsUrl = "https://example.com/video/master.m3u8",
            mpdUrl = "https://example.com/video/manifest.mpd",
            imageUrl = "https://example.com/thumb.jpg",
            title = "Gita Gyan Episode 1",
            episodeTitle = "Gita Gyan Episode 1",
            seasonTitle = "Gita Gyan",
            srt = "https://example.com/subtitles.srt",
            isLive = false
        )
    )

    MtvVideoPlayerSdk(
        contentList = contentList,
        index = 0,
        playerMode = if (isFullScreen) PlayerMode.FULL_SCREEN else PlayerMode.MINI,
        onPlayerBack = {
            if (isFullScreen) onFullScreenChange(false)
        },
        setFullScreen = onFullScreenChange
    )
}
```

## Composable API

```kotlin
@OptIn(UnstableApi::class)
@Composable
fun MtvVideoPlayerSdk(
    contentList: List<PlayerModel>? = null,
    index: Int? = 0,
    pipListener: PipListener? = null,
    isInPipMode: Boolean = false,
    isDeepLink: Boolean? = false,
    playerMode: PlayerMode = PlayerMode.MINI,
    playerStateListener: PlayerStateListener? = null,
    controller: PlayerController? = null,
    isMutedInitially: Boolean = true,
    onPlayerBack: (Boolean) -> Unit = {},
    setFullScreen: (Boolean) -> Unit = {},
    onIndexChanged: (Int) -> Unit = {},
    episodeNowPlayingStyle: EpisodeNowPlayingStyle = EpisodeNowPlayingStyle(),
    showControls: Boolean = true,
    playerConfig: PlayerConfig = PlayerConfig(),
    onCurrentIndexChanged: (Int) -> Unit = {},
    onPreviewPrimaryAction: () -> Unit = {},
    onPreviewSecondaryAction: () -> Unit = {}
)
```

`PlayerMode` values:

```kotlin
enum class PlayerMode {
    MINI,
    FULL_SCREEN,
    REELS
}
```

## PlayerModel

Use `PlayerModel` to describe each playable item.

```kotlin
data class PlayerModel(
    val id: String? = null,
    val hlsUrl: String? = null,
    val mpdUrl: String? = null,
    val videoUrl: String? = null,
    val liveUrl: String? = null,
    val seekTo: Long? = null,
    var deepLinkEndMs: Long? = null,
    var deepLinkClipDuration: Long? = null,
    val drm: String? = null,
    val drmToken: String? = null,
    val imageUrl: String? = null,
    val thumbnail: String? = null,
    val title: String? = null,
    val episodeTitle: String? = null,
    val episodeDescription: String? = null,
    val seasonTitle: String? = null,
    val seasonDescription: String? = null,
    val description: String? = null,
    val seasonNumber: String? = null,
    val episodeNumber: String? = null,
    val duration: String? = null,
    val srt: String? = null,
    val spriteUrl: String? = null,
    val playbackSpeed: Float = 1.0f,
    val selectedSubtitle: String? = null,
    val selectedVideoQuality: Int = 1080,
    val isLive: Boolean = false,
    val adsConfig: AdsConfig? = null,
    val gamAdsConfig: GAMAdsConfig? = null,
    val skipIntro: SkipIntro? = null,
    val nextEpisode: NextEpisode? = null,
    val customControls: PlayerCustomControls? = null,
    val isClipEnabled: Boolean = false,
    val isChapterEnabled: Boolean = false,
    val chapters: List<Chapter>? = null,
    val ageRating: String? = null,
    val contentRating: String? = null
)
```

For offline playback, `PlayerModel` also supports cache/download fields: `cacheFactory`, `downloadManager`, and `downloadCache`.

## Skip Intro and Next Episode

```kotlin
PlayerModel(
    hlsUrl = "https://example.com/video/master.m3u8",
    skipIntro = SkipIntro(
        startTime = 5_000L,
        endTime = 95_000L,
        enableSkipIntro = true
    ),
    nextEpisode = NextEpisode(
        showBeforeEndMs = "160000",
        enableNextEpisode = true
    )
)
```

`showBeforeEndMs` accepts a duration-like string parsed by the SDK. Numeric strings are treated as milliseconds.

## Player Config

Use `PlayerConfig` to control SDK behavior and feature visibility.

```kotlin
MtvVideoPlayerSdk(
    contentList = contentList,
    playerConfig = PlayerConfig(
        controls = PlayerControlsConfig(
            play = true,
            pause = true,
            seekBack = ControlSeekConfig(enabled = true, seconds = 10),
            seekForward = ControlSeekConfig(enabled = true, seconds = 10),
            previous = true,
            next = true,
            mute = true,
            unmute = true,
            seasonSelector = true,
            settings = true,
            cast = true,
            pip = true,
            fullscreen = true,
            exitFullscreen = true
        ),
        freePreview = FreePreviewConfig(
            enabled = true,
            durationMs = 60_000L,
            popupAllowed = true,
            popupText = "Continue watching?",
            buttonLabel = "Subscribe"
        ),
        watermark = WatermarkConfig(
            enabled = true,
            imageUrl = "https://example.com/watermark.png",
            type = "image",
            position = WatermarkPosition.TOP_RIGHT,
            textColor = "#FFFFFF",
            fontSize = "12"
        ),
        featureTier = PlayerFeatureTier.LEGACY_COMPAT,
        monetizationPackage = PlayerMonetizationPackage.NONE,
        analyticsEnabled = false,
        diagnosticsEnabled = false,
        logging = SdkLoggingConfig(level = SdkLogLevel.OFF)
    )
)
```

`PlayerFeatureTier.LEGACY_COMPAT` is the default and keeps existing integrations source-compatible. New integrations can choose `BASIC_PLAYER`, `PREMIUM_UX`, or `ENTERPRISE_PLAYBACK`.

```kotlin
PlayerConfig(
    featureTier = PlayerFeatureTier.PREMIUM_UX,
    monetizationPackage = PlayerMonetizationPackage.AD_SUPPORTED,
    ads = PlayerAdsConfig(
        googleAdsEnabled = true,
        vmapAdsEnabled = true,
        bannerAdsEnabled = true
    )
)
```

Use `PlayerFeatureGates` for customer-specific overrides:

```kotlin
PlayerConfig(
    featureTier = PlayerFeatureTier.BASIC_PLAYER,
    featureGates = PlayerFeatureGates(
        cast = true,
        pip = true,
        chapters = true
    )
)
```

See `SDK_FEATURE_PACKAGES.md` for the full package matrix.

## Ads

Ads require the monetization package gate plus the normal ad config.

```kotlin
PlayerConfig(
    monetizationPackage = PlayerMonetizationPackage.AD_SUPPORTED,
    ads = PlayerAdsConfig(
        googleAdsEnabled = true,
        vmapAdsEnabled = true,
        bannerAdsEnabled = true
    )
)
```

Enable IMA/VMAP ads per content item:

```kotlin
PlayerModel(
    hlsUrl = "https://example.com/video/master.m3u8",
    adsConfig = AdsConfig(
        adTagUrl = "https://pubads.g.doubleclick.net/gampad/ads?...",
        enableAds = true
    )
)
```

Enable GAM banner configuration:

```kotlin
PlayerModel(
    hlsUrl = "https://example.com/video/master.m3u8",
    gamAdsConfig = GAMAdsConfig(
        verticalBan = "/21775744923/example/fixed-size-banner",
        horizontalBan = "/21775744923/example/fixed-size-banner",
        timeIntervalInMilliseconds = 600_000L,
        isAdsEnabled = true
    )
)
```

## Listener Callbacks

```kotlin
MtvVideoPlayerSdk(
    contentList = contentList,
    playerStateListener = object : PlayerStateListener {
        override fun onPlayerReady(durationMs: Long) {}
        override fun onBuffering(isBuffering: Boolean) {}
        override fun onPlayStateChanged(isPlaying: Boolean) {}
        override fun onPlaybackCompleted() {}
        override fun onFullScreenChanged(isFullScreen: Boolean) {}
        override fun onPipModeChanged(isInPip: Boolean) {}
        override fun onAdStateChanged(isAdPlaying: Boolean) {}
        override fun onMuteStateChanged(isMuted: Boolean) {}
        override fun onVideoChanged(index: Int) {}
        override fun onSeekStarted(positionMs: Long) {}
        override fun onSeekCompleted(positionMs: Long) {}
        override fun onQualityChanged(width: Int, height: Int, label: String?) {}
        override fun onSubtitleChanged(language: String?, label: String?, enabled: Boolean) {}
        override fun onPlaybackSpeedChanged(speed: Float) {}
        override fun onAnalyticsEvent(event: PlayerAnalyticsEvent) {}
        override fun onDiagnosticEvent(event: PlayerDiagnosticEvent) {}
    }
)
```

Enable analytics, diagnostics, or logs with `PlayerConfig`:

```kotlin
PlayerConfig(
    analyticsEnabled = true,
    diagnosticsEnabled = true,
    logging = SdkLoggingConfig(level = SdkLogLevel.ERROR)
)
```

Logs are `OFF` by default and are redacted before being written. See `SDK_OBSERVABILITY.md`.

## PiP

Pass a `PipListener` and keep `isInPipMode` in sync with your activity callback.

```kotlin
val pipListener = object : PipListener {
    override fun onPipRequested(isPipActive: Boolean) {
        // Call activity.enterPictureInPictureMode(...) from your Activity.
    }
}

MtvVideoPlayerSdk(
    contentList = contentList,
    pipListener = pipListener,
    isInPipMode = isInPictureInPictureMode
)
```

## Programmatic Control

```kotlin
val controller = remember { PlayerController() }

MtvVideoPlayerSdk(
    contentList = contentList,
    controller = controller
)

controller.togglePlayPause()
controller.toggleMute()
controller.play(
    PlayerModel(hlsUrl = "https://example.com/another-video/master.m3u8")
)
```

## Cast

The SDK includes a Cast options provider. To use your own Cast receiver application id, override `app_id_prod` in your app resources:

```xml
<string name="app_id_prod">YOUR_CAST_RECEIVER_APP_ID</string>
```

Cast is enabled by `LEGACY_COMPAT`, `PREMIUM_UX`, and `ENTERPRISE_PLAYBACK`. For `BASIC_PLAYER`, enable it with `PlayerFeatureGates(cast = true)`.

## Chapters

```kotlin
PlayerModel(
    hlsUrl = "https://example.com/video/master.m3u8",
    isChapterEnabled = true,
    chapters = listOf(
        Chapter(id = "intro", title = "Intro", startMs = 0L),
        Chapter(id = "topic-1", title = "Topic 1", startMs = 120_000L)
    )
)
```

## Custom Icons

```kotlin
PlayerModel(
    hlsUrl = "https://example.com/video/master.m3u8",
    customControls = PlayerCustomControls(
        iconTintRes = R.color.white,
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

## Deep Link Clips

Use `seekTo`, `deepLinkEndMs`, and `deepLinkClipDuration` for clipped/deep-link playback.

```kotlin
PlayerModel(
    id = "clip-1",
    hlsUrl = "https://example.com/video/master.m3u8",
    seekTo = 30_000L,
    deepLinkEndMs = 90_000L,
    deepLinkClipDuration = 60_000L
)
```

## Supported Formats

- HLS (`.m3u8`)
- DASH (`.mpd`)
- MP4 / direct video URLs
- Widevine DRM streams
- Live URLs

## Requirements

- Android API 24+
- Kotlin
- Jetpack Compose
- AndroidX Media3
- Google Cast services for Cast support

## Migration Notes

- Existing integrations use `PlayerFeatureTier.LEGACY_COMPAT` by default and do not need to change code.
- `SYSTEM_ALERT_WINDOW` and `WRITE_SETTINGS` are no longer declared by the SDK manifest.
- Logs are now routed through `SdkLogger` and are off unless `PlayerConfig.logging` enables them.
- New listener methods have default no-op implementations, so existing `PlayerStateListener` implementations remain source-compatible.
- Paid-package behavior is now explicit through `PlayerFeatureTier`, `PlayerMonetizationPackage`, and `PlayerFeatureGates`.

## ProGuard / R8

The SDK is designed to work with standard Android R8 settings. If your app enables shrinking and receives missing-rule warnings from dependencies, keep the generated rules from AGP and add app-specific rules as needed.

Common app-side rules:

```proguard
-keepattributes SourceFile,LineNumberTable
-dontwarn com.app.videosdk.listener.PipListener
-dontwarn com.app.videosdk.listener.PlayerStateListener
-dontwarn com.app.videosdk.model.PlayerModel
-dontwarn com.app.videosdk.ui.MtvVideoPlayerSdkKt
```

## Development

Build the SDK module:

```bash
sh gradlew :videosdk:compileDebugKotlin
```

Build the sample app:

```bash
sh gradlew :app:assembleDebug
```

Check whitespace before release:

```bash
git diff --check
```

See `SDK_QA_MATRIX.md` and `SDK_RELEASE_CHECKLIST.md` before publishing a version.

## Support

Use GitHub Issues for bug reports, integration questions, and feature requests.
