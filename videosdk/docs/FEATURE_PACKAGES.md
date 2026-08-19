# SDK Feature Packages

This document describes the Phase 4 package model for Mtv Video Player SDK. The SDK now supports product tiers through `PlayerConfig.featureTier`, an optional monetization add-on through `PlayerConfig.monetizationPackage`, and per-customer overrides through `PlayerConfig.featureGates`.

## Compatibility Default

Existing integrations keep legacy behavior unless they opt into a tier:

```kotlin
PlayerConfig()
```

This uses `PlayerFeatureTier.LEGACY_COMPAT`, which keeps previously available features enabled and avoids breaking current apps.

## Basic Player

For customers that need core playback only:

```kotlin
PlayerConfig(
    featureTier = PlayerFeatureTier.BASIC_PLAYER
)
```

Includes:

- HLS, DASH, MP4, and live playback when supplied by `PlayerModel`.
- Fullscreen controls.
- Subtitle, playback-speed, and quality-selection settings.
- Basic playback callbacks through `PlayerStateListener`.

Disabled by default:

- Cast, PiP, skip intro, next episode, episode selector, chapters, sprite thumbnails, custom controls, DRM, offline playback, clip/deep-link trimming, watermark, age rating, free preview, and ads.

## Premium UX

For customers that need engagement features:

```kotlin
PlayerConfig(
    featureTier = PlayerFeatureTier.PREMIUM_UX
)
```

Includes Basic Player plus:

- Cast.
- PiP.
- Skip intro.
- Next episode.
- Episode selector.
- Chapters.
- Sprite thumbnails.
- Custom icons and custom controls.

## Enterprise Playback

For customers that need protected playback and brand controls:

```kotlin
PlayerConfig(
    featureTier = PlayerFeatureTier.ENTERPRISE_PLAYBACK
)
```

Includes Premium UX plus:

- DRM playback when `PlayerModel.drm == "1"` and `drmToken` is supplied.
- Offline playback handles when download objects are supplied.
- Clip/deep-link trimming.
- Watermark overlay.
- Age-rating overlay.
- Free-preview gates.

## Monetization Add-On

Ads are packaged separately so a customer can buy ads with any tier:

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

Includes:

- IMA/VMAP preroll, midroll, or postroll when `PlayerModel.adsConfig` is supplied.
- GAM/L-shape banner behavior when `PlayerModel.gamAdsConfig` is supplied.
- Existing ad callbacks through `AdsListener` and `PlayerStateListener`.

## Feature Overrides

Use `featureGates` for customer-specific contracts:

```kotlin
PlayerConfig(
    featureTier = PlayerFeatureTier.BASIC_PLAYER,
    featureGates = PlayerFeatureGates(
        pip = true,
        cast = true,
        chapters = true
    )
)
```

Overrides are nullable. `null` means "use the tier default"; `true` enables the feature; `false` disables it.

## Required Fields

| Feature | Required SDK config | Required `PlayerModel` fields |
| --- | --- | --- |
| Cast | `featureTier = PREMIUM_UX` or `featureGates.cast = true` | Playback URL fields. |
| PiP | `featureTier = PREMIUM_UX` or `featureGates.pip = true` | Host must pass `PipListener`. |
| Skip Intro | `skipIntro = true` by tier/override | `skipIntro`. |
| Next Episode | `nextEpisode = true` by tier/override | `nextEpisode` and more than one content item. |
| Chapters | `chapters = true` by tier/override | `isChapterEnabled = true`, `chapters`. |
| Sprite Thumbnails | `spriteThumbnails = true` by tier/override | `spriteUrl`. |
| DRM | `featureTier = ENTERPRISE_PLAYBACK` or `featureGates.drm = true` | `drm = "1"`, `mpdUrl`, `drmToken`. |
| Offline | `featureTier = ENTERPRISE_PLAYBACK` or `featureGates.offline = true` | `downloadManager`, `downloadCache`, completed download. |
| Watermark | `featureTier = ENTERPRISE_PLAYBACK` or `featureGates.watermark = true` | `PlayerConfig.watermark`. |
| Free Preview | `featureTier = ENTERPRISE_PLAYBACK` or `featureGates.freePreview = true` | `PlayerConfig.freePreview` and optional `freePreviewEnd`. |
| IMA/VMAP Ads | `monetizationPackage = AD_SUPPORTED`, `ads.googleAdsEnabled = true`, `ads.vmapAdsEnabled = true` | `adsConfig`. |
| GAM/L-shape Ads | `monetizationPackage = AD_SUPPORTED`, `ads.googleAdsEnabled = true`, `ads.bannerAdsEnabled = true` | `gamAdsConfig`. |

## Phase 4 Notes

- Tier gates are enforced internally before playback setup and UI rendering.
- Legacy integrations remain compatible by default.
- Runtime device scenario validation is tracked in Phase 6.
