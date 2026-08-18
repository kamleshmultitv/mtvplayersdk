# SDK Integration Guide

This guide captures feature-specific setup for Mtv Video Player SDK.

## DRM Playback

Use DASH plus Widevine fields on `PlayerModel`:

```kotlin
PlayerModel(
    id = "drm-episode-1",
    mpdUrl = "https://example.com/video/manifest.mpd",
    drm = "1",
    drmToken = "https://license.example.com/widevine"
)
```

Recommended package gate:

```kotlin
PlayerConfig(
    featureTier = PlayerFeatureTier.ENTERPRISE_PLAYBACK
)
```

Checklist:

- `mpdUrl` is present.
- `drm == "1"`.
- `drmToken` points to a valid Widevine license endpoint.
- Host backend returns a license token valid for the target device/session.
- Test online DRM and offline DRM separately.

## Cast

The SDK includes `CastOptionsProvider` in the SDK manifest.

Override the Cast receiver app id in the host app resources:

```xml
<string name="app_id_prod">YOUR_CAST_RECEIVER_APP_ID</string>
```

Recommended package gate:

```kotlin
PlayerConfig(
    featureTier = PlayerFeatureTier.PREMIUM_UX
)
```

For `BASIC_PLAYER`, explicitly enable Cast:

```kotlin
PlayerConfig(
    featureTier = PlayerFeatureTier.BASIC_PLAYER,
    featureGates = PlayerFeatureGates(cast = true)
)
```

Checklist:

- Chromecast and phone are on the same network.
- Receiver supports the supplied HLS/DASH/MP4 stream.
- DRM streams require a receiver that understands the SDK Cast custom data.
- Validate Cast start, pause, resume, seek, stop, and receiver error reporting.

## Picture-in-Picture

Host activity manifest:

```xml
<activity
    android:name=".MainActivity"
    android:configChanges="screenSize|smallestScreenSize|screenLayout|orientation"
    android:resizeableActivity="true"
    android:supportsPictureInPicture="true" />
```

Pass a `PipListener`:

```kotlin
val pipListener = object : PipListener {
    override fun onPipRequested(isPipActive: Boolean) {
        // Call enterPictureInPictureMode(...) from the Activity.
    }
}
```

Keep SDK state synced:

```kotlin
MtvVideoPlayerSdk(
    pipListener = pipListener,
    isInPipMode = isInPictureInPictureMode
)
```

Checklist:

- Enter PiP from fullscreen.
- Exit PiP and verify overlays return correctly.
- Verify lock/settings/age-rating overlays are suppressed in PiP.

## Offline Playback

Offline playback uses the host app download layer and passes completed download handles through `PlayerModel`:

```kotlin
PlayerModel(
    id = contentId,
    mpdUrl = onlineMpdUrl,
    drm = "1",
    drmToken = licenseUrl,
    downloadManager = downloadManager,
    downloadCache = downloadCache
)
```

Recommended package gate:

```kotlin
PlayerConfig(
    featureTier = PlayerFeatureTier.ENTERPRISE_PLAYBACK
)
```

Checklist:

- Download state is completed before playback.
- Download id matches `PlayerModel.id`.
- DASH DRM downloads include a valid offline `keySetId`.
- Test expired license and missing-license fallback.

## Ads And Monetization Add-On

Enable the add-on:

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

IMA/VMAP:

```kotlin
PlayerModel(
    hlsUrl = "https://example.com/video/master.m3u8",
    adsConfig = AdsConfig(
        adTagUrl = "https://pubads.g.doubleclick.net/gampad/ads?...",
        enableAds = true
    )
)
```

GAM / L-shape:

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

Checklist:

- Host manifest includes the Google Mobile Ads app id when GAM/IMA requires it.
- Ad tags are reachable on device.
- VMAP cue points appear on the seekbar.
- Ads pause normal controls and restore controls after completion/error.
- Analytics emits `AD_LOADED`, `AD_STARTED`, `AD_COMPLETED`, and `AD_ERROR`.

## Custom Controls

Pass drawable resource ids through `PlayerCustomControls`:

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

If `castConnectedIconRes` is omitted, the SDK switches to its built-in connected Cast icon when a Cast route is connected.

Recommended package gate:

```kotlin
PlayerConfig(
    featureTier = PlayerFeatureTier.PREMIUM_UX
)
```

Checklist:

- Icons are vector drawables or density-safe raster assets.
- Icons are visible on light and dark video frames.
- Touch targets remain at expected sizes.

## Observability

Use `SDK_OBSERVABILITY.md` for analytics, diagnostics, and SDK logging setup.
