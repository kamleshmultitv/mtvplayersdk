# Customer Demo Profiles

These profiles help validate Mtv Video Player SDK for customer-style demos and pilots.

They are not customer contracts. Use them to prepare reliable configurations for OTT, broadcaster, spiritual/event, and TV/music integrations.

## Feature Tier Decision Guide

Choose `PlayerConfig.featureTier` first, then add `PlayerMonetizationPackage.AD_SUPPORTED` only when ads are part of the customer package.

### Tier Summary

| Tier | Use when | What the customer gets |
| --- | --- | --- |
| `LEGACY_COMPAT` | Existing apps that should keep old behavior. | All current SDK features stay enabled for compatibility. |
| `BASIC_PLAYER` | Customer needs simple playback and core controls. | Core playback, fullscreen, settings, subtitles/captions, audio track, speed, quality, and basic callbacks. |
| `PREMIUM_UX` | Customer needs OTT-style playback UX. | Basic Player plus Cast, PiP, skip intro, next episode, episode selector, chapters, sprite thumbnails, and custom controls. |
| `ENTERPRISE_PLAYBACK` | Customer needs protected/paid playback and brand controls. | Premium UX plus DRM, offline playback, deep-link clips, watermark, age rating, and free preview. |

### Player Controls By Tier

These are the controls the SDK keeps available after feature-tier gating. The host can still hide individual controls with `PlayerConfig.controls`.

| Player control | `LEGACY_COMPAT` | `BASIC_PLAYER` | `PREMIUM_UX` | `ENTERPRISE_PLAYBACK` |
| --- | --- | --- | --- | --- |
| Play | Yes | Yes | Yes | Yes |
| Pause | Yes | Yes | Yes | Yes |
| Seek backward | Yes | Yes | Yes | Yes |
| Seek forward | Yes | Yes | Yes | Yes |
| Mute | Yes | Yes | Yes | Yes |
| Unmute | Yes | Yes | Yes | Yes |
| Settings button | Yes | Yes | Yes | Yes |
| Fullscreen | Yes | Yes | Yes | Yes |
| Exit fullscreen | Yes | Yes | Yes | Yes |
| Previous episode | Yes | No | Yes | Yes |
| Next episode | Yes | No | Yes | Yes |
| Episode selector | Yes | No | Yes | Yes |
| Cast button | Yes | No | Yes | Yes |
| PiP button | Yes | No | Yes | Yes |

Control count summary:

- `BASIC_PLAYER`: 9 top-level controls: play, pause, seek backward, seek forward, mute, unmute, settings, fullscreen, and exit fullscreen.
- `PREMIUM_UX` and `ENTERPRISE_PLAYBACK`: 14 top-level controls: Basic controls plus previous, next, episode selector, Cast, and PiP.
- `LEGACY_COMPAT`: same 14 top-level controls as Premium/Enterprise for backward compatibility.

Settings menu availability by tier:

| Settings option | `LEGACY_COMPAT` | `BASIC_PLAYER` | `PREMIUM_UX` | `ENTERPRISE_PLAYBACK` |
| --- | --- | --- | --- | --- |
| Audio Track | Yes | Yes | Yes | Yes |
| Closed Caption | Yes | Yes | Yes | Yes |
| Speed Selector | Yes | Yes | Yes | Yes |
| Video Quality | Yes | Yes | Yes | Yes |

Settings selected rows are SDK-owned state. Audio track, caption/off, playback speed, and video quality remain visually selected while the same player session and media source are alive.

### Feature Logic By Tier

| SDK feature / logic | `LEGACY_COMPAT` | `BASIC_PLAYER` | `PREMIUM_UX` | `ENTERPRISE_PLAYBACK` |
| --- | --- | --- | --- | --- |
| HLS playback | Yes | Yes | Yes | Yes |
| DASH playback | Yes | Yes | Yes | Yes |
| MP4/direct playback | Yes | Yes | Yes | Yes |
| Live playback | Yes | Yes | Yes | Yes |
| Subtitles/captions | Yes | Yes | Yes | Yes |
| Playback speed | Yes | Yes | Yes | Yes |
| Quality selection | Yes | Yes | Yes | Yes |
| Cast | Yes | No | Yes | Yes |
| PiP | Yes | No | Yes | Yes |
| Skip intro | Yes | No | Yes | Yes |
| Next episode logic | Yes | No | Yes | Yes |
| Episode selector sheet | Yes | No | Yes | Yes |
| Chapters | Yes | No | Yes | Yes |
| Sprite thumbnails | Yes | No | Yes | Yes |
| Custom controls/icons | Yes | No | Yes | Yes |
| DRM playback | Yes | No | No | Yes |
| Offline playback handles | Yes | No | No | Yes |
| Deep-link clip trimming | Yes | No | No | Yes |
| Watermark | Yes | No | No | Yes |
| Age-rating overlay | Yes | No | No | Yes |
| Free preview | Yes | No | No | Yes |
| IMA/VMAP ads | Yes | Add-on only | Add-on only | Add-on only |
| GAM banner ads | Yes | Add-on only | Add-on only | Add-on only |
| L-shape ads | Yes | Add-on only | Add-on only | Add-on only |

### What Tier Gating Does Internally

When a feature is not included in the selected tier, the SDK ignores the related model/config fields before rendering or playback setup:

| Gated field or behavior | Ignored unless tier/override enables |
| --- | --- |
| `drm`, `drmToken`, protected `mpdUrl` | DRM |
| `adsConfig` | IMA ads |
| `gamAdsConfig` | GAM or L-shape ads |
| `skipIntro` | Skip intro |
| `nextEpisode` | Next episode |
| `customControls` | Custom controls |
| `isChapterEnabled`, `chapters` | Chapters |
| `spriteUrl` | Sprite thumbnails |
| `cacheFactory`, `downloadManager`, `downloadCache` | Offline |
| `deepLinkEndMs`, `deepLinkClipDuration` | Deep-link clips |
| `ageRating`, `contentRating` | Age rating |
| `PlayerConfig.watermark` | Watermark |
| `PlayerConfig.freePreview`, `freePreviewEnd` | Free preview |

### Monetization Add-On

Ads are controlled separately from `featureTier`.

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

Use this add-on when the customer needs IMA/VMAP, GAM banner, or L-shape ads. Without `AD_SUPPORTED`, ad model fields are ignored by Basic, Premium, and Enterprise tiers.

### Custom Overrides

Use `featureGates` only for customer-specific contracts where the package needs one exception:

```kotlin
PlayerConfig(
    featureTier = PlayerFeatureTier.BASIC_PLAYER,
    featureGates = PlayerFeatureGates(
        cast = true,
        pip = true
    )
)
```

`null` means use the tier default, `true` enables the feature, and `false` disables it.

## Profile 1: Basic VOD Player

Use for customers that need simple playback first.

```kotlin
PlayerConfig(
    featureTier = PlayerFeatureTier.BASIC_PLAYER
)
```

Required content fields:

- `hlsUrl` or `videoUrl`
- `title`
- `imageUrl` or `thumbnail`

QA focus:

- HLS or MP4 playback.
- Play/pause.
- Seekbar.
- Fullscreen.
- Back handling.
- Mute/unmute.

## Profile 2: Premium OTT UX

Use for ALTT/SonyLIV-style premium VOD demos where UX features matter.

```kotlin
PlayerConfig(
    featureTier = PlayerFeatureTier.PREMIUM_UX
)
```

Recommended content fields:

- `hlsUrl` or `mpdUrl`
- `title`, `episodeTitle`, `seasonTitle`
- `imageUrl`
- `srt`
- `skipIntro`
- `nextEpisode`
- `chapters`
- `spriteUrl`

QA focus:

- Episode selector.
- Skip intro.
- Next episode.
- Chapters.
- Subtitles.
- Quality selection.
- Playback speed.
- Sprite thumbnail preview.
- Cast.
- PiP.

## Profile 3: Enterprise DRM And Offline

Use for paid-content customers that require protected playback.

```kotlin
PlayerConfig(
    featureTier = PlayerFeatureTier.ENTERPRISE_PLAYBACK
)
```

Required content fields:

- `mpdUrl`
- `drm = "1"`
- `drmToken`
- `id`

Offline fields when needed:

- `downloadManager`
- `downloadCache`
- `cacheFactory`

QA focus:

- Online Widevine playback.
- License failure diagnostics.
- Offline non-DRM playback.
- Offline DRM playback.
- Missing offline license fallback.
- Token and license URL redaction.

## Profile 4: Live / Linear TV

Use for Mastii-style TV/music or broadcaster live playback.

```kotlin
PlayerConfig(
    featureTier = PlayerFeatureTier.PREMIUM_UX
)
```

Required content fields:

- `liveUrl`
- `isLive = true`
- `title`
- `imageUrl` or `thumbnail`

QA focus:

- Live startup.
- Live placeholder hides after first frame.
- Go Live behavior.
- Seek restrictions.
- Fullscreen.
- Cast if included.
- PiP if included.

## Profile 5: Spiritual / Event VOD And Live

Use for Art of Living-style event, discourse, or mixed live/VOD demos.

```kotlin
PlayerConfig(
    featureTier = PlayerFeatureTier.PREMIUM_UX,
    analyticsEnabled = true,
    diagnosticsEnabled = true
)
```

Recommended content fields:

- `hlsUrl` for VOD or `liveUrl` for live.
- `title`
- `episodeTitle`
- `seasonTitle`
- `imageUrl`
- `srt` when multilingual subtitles are available.
- `ageRating` or `contentRating` when required.

QA focus:

- Long title layout.
- Subtitle language switching.
- Cast.
- PiP.
- Analytics event delivery.
- Diagnostics for network failures.
- Large-screen layout.

## Profile 6: Ad-Supported Monetization

Use when customer demos include IMA, VMAP, GAM, or L-shape ads.

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

Required content fields:

- `adsConfig` for IMA/VMAP.
- `gamAdsConfig` for GAM/L-shape.
- `hlsUrl`, `mpdUrl`, or `videoUrl`.

QA focus:

- Preroll starts.
- VMAP cue markers appear.
- Content resumes after ad complete.
- Ad error returns to content.
- GAM banner appears at configured time.
- L-shape close behavior.
- Ad analytics events.
- Ad tag redaction in logs.

## Profile Selection Checklist

Before a demo or pilot, confirm:

- Target package tier.
- Monetization add-on included or not.
- DRM included or not.
- Offline included or not.
- Cast receiver app id.
- AdMob app id and ad tags.
- Test stream URLs.
- Expected unsupported features.
- QA evidence owner.
