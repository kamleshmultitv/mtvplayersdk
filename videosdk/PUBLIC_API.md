# SDK Public API Contract

This document defines the app-facing API surface for Mtv Video Player SDK. The goal is to make integrations stable while the internal code structure improves.

## Stable Entry Points

These APIs are treated as app-facing and should remain source-compatible across minor releases unless a migration note is provided.

| API | Purpose |
| --- | --- |
| `MtvVideoPlayerSdk` | Main Jetpack Compose player entry point. |
| `MtvVideoPlayerView` | XML/View-system wrapper around the Compose SDK. |
| `PlayerModel` | Current content item model. |
| `PlayerConfig` | Global SDK behavior and feature configuration. |
| `PlayerControlsConfig` | Control visibility and seek interval config. |
| `PlayerFeatureTier` | Package tier selector for Basic, Premium UX, Enterprise, or legacy compatibility. |
| `PlayerMonetizationPackage` | Optional monetization add-on selector. |
| `PlayerFeatureGates` | Nullable per-feature overrides for customer-specific contracts. |
| `PlayerAnalyticsEvent` | Optional analytics event payload for host reporting. |
| `PlayerDiagnosticEvent` | Optional diagnostic payload for integration support. |
| `SdkLoggingConfig` | SDK log level and tag configuration. |
| `SdkLogLevel` | `OFF`, `ERROR`, `INFO`, and `DEBUG` log levels. |
| `PlayerController` | Programmatic play, play/pause, and mute control. |
| `PlayerStateListener` | Host-app playback, error, ad, Cast, PiP, and fullscreen callbacks. |
| `PipListener` | Host-app PiP request bridge. |
| `PlayerMode` | `MINI`, `FULL_SCREEN`, and `REELS` display modes. |

## Compatibility Rules

1. Do not remove existing constructor parameters from public data classes in a minor release.
2. Add new fields with defaults to preserve source and binary compatibility where possible.
3. Do not require host apps to declare sensitive permissions unless a feature explicitly needs them.
4. Keep default behavior safe: no ads, no premium overlays, and no special permissions unless configured.
5. Public APIs must not expose dependencies that are hidden behind Gradle `implementation(...)`.
6. Any breaking API cleanup must include a migration note and a compatibility window.

## Current Public Model Boundary

`PlayerModel` currently includes several responsibilities:

- playback source: `hlsUrl`, `mpdUrl`, `videoUrl`, `liveUrl`, `drm`, `drmToken`
- startup/deep-link playback: `seekTo`, `deepLinkEndMs`, `deepLinkClipDuration`
- metadata: `id`, `title`, `episodeTitle`, `seasonTitle`, `description`, `thumbnail`, `imageUrl`, `ageRating`
- timed actions: `skipIntro`, `nextEpisode`, `chapters`
- monetization: `adsConfig`, `gamAdsConfig`
- UI customization: `customControls`
- offline playback internals: `cacheFactory`, `downloadManager`, `downloadCache`, `drmOfflineKeySetId`, `drmOfflineKeySetIdBase64`

For downloaded DRM MPD/DASH playback, host apps pass the same Media3 download cache objects used by their downloader layer and pass the persisted Widevine offline key set through `drmOfflineKeySetId` or `drmOfflineKeySetIdBase64`.

`PlayerCustomControls.castConnectedIconRes` can be supplied when the host app wants a custom connected Cast icon. If it is omitted, the SDK uses its built-in connected Cast icon.

This shape stays supported during Phase 1. The SDK should not break existing callers while the model is redesigned.

## Phase 4 Feature Packages

`PlayerConfig` now includes package-level configuration:

```kotlin
PlayerConfig(
    featureTier = PlayerFeatureTier.BASIC_PLAYER,
    monetizationPackage = PlayerMonetizationPackage.NONE,
    featureGates = PlayerFeatureGates()
)
```

Available tiers:

- `LEGACY_COMPAT`: compatibility default for existing integrations.
- `BASIC_PLAYER`: core playback, fullscreen, subtitles, quality, speed, and callbacks.
- `PREMIUM_UX`: Basic plus Cast, PiP, skip intro, next episode, episode selector, chapters, sprite thumbnails, and custom controls.
- `ENTERPRISE_PLAYBACK`: Premium UX plus DRM, offline playback, deep-link clips, watermark, age rating, and free preview.

`PlayerMonetizationPackage.AD_SUPPORTED` enables ad package gates when combined with `PlayerAdsConfig` and content ad fields.

Detailed package requirements are documented in `videosdk/docs/FEATURE_PACKAGES.md`.

## Autoplay Defaults

`PlayerConfig` enables autoplay by default for feature, detail, and reels/assets player modes:

```kotlin
PlayerConfig(
    autoPlayFeature = true,
    autoPlayDetail = true,
    autoPlayAssets = true
)
```

Host apps can disable autoplay per mode by setting the matching field to `false`.

## Phase 5 Observability

`PlayerConfig` includes opt-in observability controls:

```kotlin
PlayerConfig(
    analyticsEnabled = true,
    diagnosticsEnabled = true,
    logging = SdkLoggingConfig(level = SdkLogLevel.DEBUG)
)
```

`PlayerStateListener` includes additive default methods for:

- analytics events
- diagnostics events
- seek started / completed
- quality changed
- subtitle changed
- playback speed changed

Detailed observability behavior is documented in `videosdk/docs/OBSERVABILITY.md`.

## Target Model Split

Future cleanup should split `PlayerModel` into focused types while keeping compatibility helpers.

```kotlin
data class PlaybackSource(
    val hlsUrl: String? = null,
    val mpdUrl: String? = null,
    val videoUrl: String? = null,
    val liveUrl: String? = null,
    val drm: String? = null,
    val drmToken: String? = null
)

data class OfflinePlaybackOptions(
    val cacheFactory: CacheDataSource.Factory? = null,
    val downloadManager: DownloadManager? = null,
    val downloadCache: SimpleCache? = null,
    val drmOfflineKeySetId: ByteArray? = null,
    val drmOfflineKeySetIdBase64: String? = null
)

data class PlaybackMetadata(
    val id: String? = null,
    val title: String? = null,
    val episodeTitle: String? = null,
    val seasonTitle: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val thumbnail: String? = null,
    val ageRating: String? = null,
    val contentRating: String? = null
)

data class PlaybackActions(
    val skipIntro: SkipIntro? = null,
    val nextEpisode: NextEpisode? = null,
    val chapters: List<Chapter>? = null
)

data class MonetizationOptions(
    val imaAds: AdsConfig? = null,
    val gamAds: GAMAdsConfig? = null
)
```

## Migration Strategy

1. Add the new focused models without removing `PlayerModel`.
2. Add mapper helpers from `PlayerModel` to the focused models.
3. Move internal player/session code to consume the focused models.
4. Keep `PlayerModel` as a compatibility facade.
5. Deprecate only after sample app and README use the new structure.
6. Remove deprecated fields only in a major version.

## Dependency Exposure Decision

The SDK exposes Compose and Media3 types in public signatures. These dependencies should be published with `api(...)`:

- `androidx.core:core-ktx`
- `androidx.compose.ui:ui`
- `androidx.compose.runtime:runtime`
- `androidx.media3:media3-common`
- `androidx.media3:media3-datasource`
- `androidx.media3:media3-exoplayer`
- `androidx.media3:media3-ui`

Dependencies used only by internals should stay `implementation(...)`.

## Phase 1 Exit Criteria

- Sensitive permissions are removed from the SDK manifest.
- Final scrub seek has one owner.
- Public dependency exposure matches public signatures.
- Stable public API surface is documented.
- `PlayerModel` split is designed without breaking current consumers.
- `sh gradlew :videosdk:compileDebugKotlin` passes.
