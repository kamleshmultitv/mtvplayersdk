# Customer Demo Profiles

These profiles help validate Mtv Video Player SDK for customer-style demos and pilots.

They are not customer contracts. Use them to prepare reliable configurations for OTT, broadcaster, spiritual/event, and TV/music integrations.

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
