# SDK QA Matrix

This matrix defines the validation required before releasing Mtv Video Player SDK.

## Build Gates

| Check | Command | Required |
| --- | --- | --- |
| SDK Kotlin compile | `sh gradlew :videosdk:compileDebugKotlin` | Yes |
| Sample app debug build | `sh gradlew :app:assembleDebug` | Yes |
| Whitespace diff check | `git diff --check` | Yes |
| Enterprise automated gates | `sh scripts/enterprise_qa_gates.sh` | Yes for customer releases |

## Sample App Coverage

| Scenario | Sample location | Current coverage |
| --- | --- | --- |
| Basic API playback | `ContentBody` | Uses API content list and normal mini/fullscreen playback. |
| Long title / metadata layout | `SdkEdgeCaseScreen` | Covered by long-title and missing-metadata cases. |
| Multiple episodes | `SdkEdgeCaseScreen` | Covered by multi-episode list and episode sheet case. |
| Skip intro / next episode | `SdkEdgeCaseScreen` | Covered by QA items with `SkipIntro` and `NextEpisode`. |
| Chapters | `SdkEdgeCaseScreen` | Covered by QA items with chapter data. |
| Custom now-playing style | `SdkEdgeCaseScreen` | Covered by custom style case. |
| Offline playback | `DownloadPlayer` | Covered by download-player path. |
| Deep-link clips | `ContentBody` plus deep-link state | Covered by deep-link content mapping. |
| PiP | `MainActivity` and SDK player | Host callback is wired through `PipListener`. |

## Device Smoke Matrix

Run these manually on at least one phone and one tablet-class viewport before release.

| Scenario | Steps | Expected result | Status |
| --- | --- | --- | --- |
| HLS playback | Open normal API content. | Video loads, controls work, duration updates. | Passed by QA team |
| DASH playback | Open DASH/MPD content. | Video loads and quality settings are available. | Passed by QA team |
| MP4/direct playback | Use direct `videoUrl`. | Video loads without HLS/DASH assumptions. | Passed by QA team |
| Live playback | Use `liveUrl` content. | Live placeholder hides after first frame; Go Live behavior works. | Passed by QA team |
| Fullscreen | Toggle fullscreen in mini player. | Fullscreen controls, lock, settings, and back handling work. | Passed by QA team |
| PiP | Enter PiP from fullscreen. | Chrome overlays suppress in PiP and restore after exit. | Passed by QA team |
| Cast | Connect to Chromecast and start playback. | Cast starts, local playback pauses, seek/play/pause work. | Passed by QA team |
| DRM online | Play Widevine DASH item. | License succeeds and playback starts. | Passed by QA team |
| Offline non-DRM | Play completed offline item. | Local cache playback starts. | Passed by QA team |
| Offline DRM | Play completed offline DRM item. | Offline license is used; fallback behavior is reported when missing. | Passed by QA team |
| IMA/VMAP ads | Enable monetization add-on and content `adsConfig`. | Ad starts/completes/errors are reported and content resumes. | Passed by QA team |
| GAM/L-shape ads | Enable banner/L-shape config. | Banner appears at configured cue and can be closed when enabled. | Passed by QA team |
| Free preview | Enable preview gate. | Playback pauses at limit and preview dialog appears. | Passed by QA team |
| Analytics | Set `analyticsEnabled = true`. | Host receives key analytics events. | Passed by QA team |
| Diagnostics | Set `diagnosticsEnabled = true`. | Host receives diagnostic events on errors/recovery. | Passed by QA team |
| Logging | Set `logging.level = DEBUG`. | Logs appear with redacted URLs/tokens. | Passed on device `RZCY604F6GE` |

## Feature Tier Matrix

| Tier | Checks |
| --- | --- |
| `LEGACY_COMPAT` | Existing sample behavior remains enabled. |
| `BASIC_PLAYER` | Cast, PiP, skip intro, next episode, chapters, sprites, DRM, offline, preview, watermark, and ads are disabled unless explicitly overridden. |
| `PREMIUM_UX` | Cast, PiP, skip intro, next episode, episode selector, chapters, sprites, and custom controls are enabled. |
| `ENTERPRISE_PLAYBACK` | Premium UX plus DRM, offline, deep-link clips, watermark, age rating, and free preview are enabled. |
| `AD_SUPPORTED` | IMA/VMAP and GAM/L-shape ad gates are enabled when ad configs are supplied. |

## Regression Watchlist

- Seekbar thumb remains inside the seekbar when idle.
- Scrub preview time appears above the pointer while dragging.
- Skip intro and next episode buttons use transparent rounded styling.
- Cast state comes from one shared `CastUtils` in the main SDK path.
- Logs do not expose DRM tokens, license URLs, ad tag query strings, or full media URLs.

## Release Evidence

For each release, record:

- Date.
- Commit or tag.
- SDK version.
- Build command outputs.
- Devices tested.
- Failed scenarios and fixes.
- Known limitations.

## Phase 7 Enterprise Evidence

Complete this before changing `videosdk/docs/ENTERPRISE_RELEASE_READINESS.md` to ready.

| Evidence | Status | Notes |
| --- | --- | --- |
| Automated enterprise gates passed | Passed | `sh scripts/enterprise_qa_gates.sh` passed on 2026-08-17. |
| Customer profile selected | General enterprise SDK validation | Tester QA covered the player SDK broadly across core customer-facing paths. |
| Phone QA completed | Passed by QA team | Five-day tester QA passed; detailed device list not attached in this file. |
| Tablet / large viewport QA completed | Passed by QA team | Five-day tester QA passed; detailed device list not attached in this file. |
| Chromecast QA completed | Passed by QA team | Five-day tester QA passed; detailed Cast evidence not attached in this file. |
| DRM QA completed | Passed by QA team | Five-day tester QA passed; detailed DRM stream evidence not attached in this file. |
| Offline QA completed | Passed by QA team | Five-day tester QA passed; detailed offline evidence not attached in this file. |
| Ads QA completed | Passed by QA team | Five-day tester QA passed; detailed ad evidence not attached in this file. |
| Logs redaction verified | Passed | `sh scripts/device_log_redaction_check.sh` passed on physical device `RZCY604F6GE` on 2026-08-17. |
| Release decision recorded | Ready | Enterprise readiness status updated after tester QA and device log-redaction verification. |
