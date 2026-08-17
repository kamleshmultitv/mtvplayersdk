# SDK Monetization Plan

This document tracks phase-wise work to make Mtv Video Player SDK easier to sell, integrate, maintain, and scale. Monetization here means improving the SDK as a product: cleaner code structure, lower integration risk, stronger public APIs, better documentation, reliable playback, optional ad support, analytics hooks, release discipline, and customer-ready QA.

## Status Legend

- `RUNNING`: Current active phase.
- `IN_PROGRESS`: Started, but not the current main focus.
- `COMPLETED`: Done and verified.
- `PENDING`: Not started.
- `BLOCKED`: Needs external input, account access, product decision, or customer feedback.

## Status Board

| Phase | Status | Goal |
| --- | --- | --- |
| Phase 0 | `COMPLETED` | Audit SDK quality, risk areas, public API, and monetization readiness. |
| Phase 1 | `COMPLETED` | Stabilize public API and reduce integration risk. |
| Phase 2 | `COMPLETED` | Improve code structure and separate responsibilities. |
| Phase 3 | `COMPLETED` | Harden playback, Cast, PiP, lifecycle, and controls behavior. |
| Phase 4 | `COMPLETED` | Build monetizable feature packages and configuration tiers. |
| Phase 5 | `COMPLETED` | Add observability, analytics, callbacks, and host-app reporting. |
| Phase 6 | `COMPLETED` | Strengthen documentation, sample app, QA matrix, and release process. |
| Phase 7 | `COMPLETED` | Prepare enterprise QA readiness, customer demo profiles, automated gates, and release evidence workflow. |

## Phase 0: SDK Audit and Baseline

Status: `COMPLETED`

### Target

Create a clear baseline of what must improve before the SDK can be treated as a commercial product.

### Current Findings

- `MtvVideoPlayerSdk.kt` is too large and owns too many responsibilities.
- `PlayerModel` mixes playback source, UI metadata, ads, offline cache objects, and feature flags.
- Some SDK manifest permissions are risky for host apps.
- Cast state is created in multiple UI components.
- Seekbar final seek can fire from more than one layer.
- Direct debug logs exist in production SDK paths.
- Documentation was outdated and has started being refreshed.

### Baseline Metrics

| Metric | Result |
| --- | --- |
| Kotlin files in `videosdk` | 8,318 total lines |
| Largest file | `MtvVideoPlayerSdk.kt` at 1,375 lines |
| Other large files | `PlayerUtils.kt` 771 lines, `CustomPlayerController.kt` 759 lines, `CastUtils.kt` 631 lines |
| SDK manifest permissions | `ACCESS_NETWORK_STATE`, `INTERNET`, `SYSTEM_ALERT_WINDOW`, `WAKE_LOCK`, `MODIFY_AUDIO_SETTINGS`, `WRITE_SETTINGS` |
| Sensitive manifest permissions to review first | `SYSTEM_ALERT_WINDOW`, `WRITE_SETTINGS` |
| Direct logging sites | Present in player, Cast, sprite, GAM, IMA, and clip paths |
| Force unwrap sites | Present in `CastUtils`, `SpriteThumbnail`, and `SelectorHeader` |

### Compile Baseline

Command:

```bash
sh gradlew :videosdk:compileDebugKotlin --rerun-tasks
```

Result: `BUILD SUCCESSFUL`

Warnings captured:

- `PlayerCustomControls.kt`: annotation target behavior will change in a future Kotlin version.
- `FullScreenHandler.kt`: Accompanist `rememberSystemUiController` is deprecated.
- `ScreenRotation.kt`: `ORIENTATION_SQUARE` is deprecated.
- `PlayerUtils.kt`: `setAdsLoaderProvider` and `setAdViewProvider` are deprecated Media3 APIs.

### Phase 0 Risk Register

| Risk | Priority | Evidence | Next Phase |
| --- | --- | --- | --- |
| Host app receives sensitive permissions from SDK manifest | High | `SYSTEM_ALERT_WINDOW`, `WRITE_SETTINGS` in SDK manifest | Phase 1 |
| Public model is too broad | High | `PlayerModel` mixes source, metadata, ads, offline internals, feature flags | Phase 1 |
| Main player composable is too large | High | `MtvVideoPlayerSdk.kt` has 1,375 lines | Phase 2 |
| Duplicate seek path can create behavior bugs | High | `BottomControls` and `CustomSlider` can both call final seek | Phase 3 |
| Cast state is duplicated | Medium | `CastUtils` created in main player, overlay, and season selector | Phase 3 |
| Production logs are not gated | Medium | direct `Log.d/e/w` in SDK code | Phase 5 |
| Ad view lifecycle cleanup is incomplete | Medium | `LBandBanner` creates `AdManagerAdView` without explicit dispose cleanup | Phase 4 |
| Deprecated APIs remain | Medium | compiler warnings in fullscreen, rotation, and Media3 ad setup | Phase 2 / Phase 4 |

### Acceptance Checks

- `COMPLETED`: Current risks are documented.
- `COMPLETED`: SDK still compiles.
- `COMPLETED`: A phase-wise improvement path exists.
- `COMPLETED`: The next phase can start without more discovery.

## Phase 1: Public API and Integration Risk

Status: `COMPLETED`

### Target

Make the SDK easier and safer for another app to integrate.

### Steps

1. Define the stable public API surface:
   - `MtvVideoPlayerSdk`
   - `MtvVideoPlayerView`
   - `PlayerModel`
   - `PlayerConfig`
   - `PlayerController`
   - `PlayerStateListener`
2. Split `PlayerModel` into clearer parts:
   - playback source
   - content metadata
   - controls/actions
   - monetization options
   - offline playback handle
3. Remove risky permissions from the SDK manifest unless a feature absolutely needs them.
4. Decide which dependencies must be exposed as `api(...)` and which can stay `implementation(...)`.
5. Add backward-compatible constructors or migration helpers if existing apps already use the current model.

### Completed in Phase 1

- Removed `SYSTEM_ALERT_WINDOW` and `WRITE_SETTINGS` from the SDK manifest so host apps do not inherit sensitive permissions by default.
- Kept `INTERNET`, `ACCESS_NETWORK_STATE`, `WAKE_LOCK`, and `MODIFY_AUDIO_SETTINGS` because they match current playback/network/audio behavior.
- Removed duplicate final seek from `BottomControls`; final seek and chapter snapping now remain owned by `CustomSlider`.
- Changed Gradle dependency exposure to `api(...)` for libraries that appear in public SDK signatures:
  - AndroidX core annotations
  - Compose runtime/ui
  - Media3 common, datasource, exoplayer, and ui
- Removed the deprecated `package` attribute from the SDK manifest; namespace remains owned by `videosdk/build.gradle.kts`.
- Added `SDK_PUBLIC_API.md` to define stable entry points, compatibility rules, dependency exposure decisions, and the target `PlayerModel` split.
- Verified SDK and sample app builds:
  - `sh gradlew :videosdk:compileDebugKotlin`
  - `sh gradlew :app:assembleDebug`

### Deferred to Later Phases

- Implement the actual internal `PlayerModel` split after Phase 2 starts separating player/session internals.
- Move offline playback objects behind an SDK-owned offline handle after compatibility mapping is in place.

### Acceptance Checks

- `COMPLETED`: Host apps no longer inherit `SYSTEM_ALERT_WINDOW` or `WRITE_SETTINGS` from the SDK.
- `COMPLETED`: Public API surface and target model split are documented in `SDK_PUBLIC_API.md`.
- `COMPLETED`: Existing sample app still compiles after Phase 1 work.
- `COMPLETED`: Breaking changes are avoided in this phase; model split is design-only for now.

## Phase 2: Code Structure Cleanup

Status: `COMPLETED`

### Target

Reduce maintenance cost by separating playback, UI, ads, Cast, and configuration logic.

### Steps

1. Split `MtvVideoPlayerSdk.kt` into focused units:
   - `PlayerSession`
   - `PlayerStateObserver`
   - `PlayerChrome`
   - `PlayerGestureLayer`
   - `PlayerOverlayHost`
   - `PreviewLimitController`
2. Move player creation out of `PlayerUtils` into a dedicated factory.
3. Move track, subtitle, quality, and speed logic into separate helpers.
4. Keep UI composables mostly stateless where possible.
5. Replace duplicated skip-intro / next-episode timing logic with one shared state source.
6. Remove old comments such as `FIX`, `UNCHANGED`, and debug-only notes after the code is stable.

### Completed in Phase 2

- Added `PlayerFactory` so actual ExoPlayer, MediaSource, DRM, offline, subtitle, and IMA setup no longer lives in `PlayerUtils`.
- Added `PlaybackSourceResolver` and kept `PlayerUtils.resolveToPlayableUri(...)` as a compatibility wrapper.
- Added `PlayerEventObserver` to own ExoPlayer listener registration, duration updates, IMA cue-point collection, playback callbacks, age-rating replay triggers, and player release.
- Added `PreviewLimitController` to own free-preview limit polling and pause/dialog triggering.
- Added `PlayerGestureLayer` to own tap, double-tap reset, and fullscreen pinch-zoom behavior.
- Added `PlayerOverlays` and `PlaybackSessionKeys` to move preview dialog, live placeholder, watermark, playback-session key, preroll key, and URL log redaction helpers out of the main player file.
- Added `PlayerChrome` to own controller, loading, age-rating, lock, settings, and cut-sheet chrome rendering.
- Added `PlayerTimingObserver` to own skip-intro, next-episode, chapter, and L-band timing checks.
- Added `TrackSelectionUtils` and `PlayerTimeUtils`; `PlayerUtils` now delegates to focused helpers while preserving existing wrapper methods.
- Updated `SelectorHeader` to use `TrackSelectionUtils` directly.
- Reduced `MtvVideoPlayerSdk.kt` from 1,375 lines to 731 lines.
- Reduced `PlayerUtils.kt` from 771 lines to 227 lines while preserving its public wrapper methods.
- Verified SDK and sample app builds:
  - `sh gradlew :videosdk:compileDebugKotlin`
  - `sh gradlew :app:assembleDebug`

### Deferred to Later Phases

- Centralize Cast into one observable controller in Phase 3.
- Add gated SDK logging in Phase 5 for remaining direct logs.
- Split public `PlayerModel` into focused compatibility-backed models in a later API cleanup phase.

### Acceptance Checks

- `COMPLETED`: Main composable is smaller and focused on session wiring.
- `COMPLETED`: Playback setup is isolated in `PlayerFactory`.
- `COMPLETED`: Control visibility state has one owner in `MtvVideoPlayerSdk`, with behavior routed through focused observer/chrome components.
- `COMPLETED`: Final scrub seek has one owner, and skip-intro / next-episode / chapter / L-band timing is centralized in `PlayerTimingObserver`.

## Phase 3: Playback Reliability

Status: `COMPLETED`

### Target

Make the SDK dependable across normal, fullscreen, PiP, Cast, live, DRM, offline, and deep-link playback.

### Steps

1. Fix duplicate final seek on scrub release.
2. Centralize Cast handling and pass one Cast controller through UI.
3. Make Cast state observable instead of checking `isCasting()` once inside remembered UI.
4. Replace polling loops where player listeners can provide events.
5. Harden DRM/offline fallback behavior.
6. Make PiP state, lock state, and fullscreen state explicit.
7. Add controlled error recovery paths.

### Completed in Phase 3

- Added `CastPlaybackState` and exposed it from `CastUtils` as a `StateFlow`.
- Updated `CastUtils` to publish state from Cast session start, resume, suspend, stop, media status, seek, play, pause, mute, and failure paths.
- Updated `CustomPlayerController` to collect observable Cast state instead of deriving `isCasting` from a one-time `castUtils.isCasting()` call.
- Routed the shared Cast controller into `BottomControls`, `SeasonSelector`, `CenterControls`, and `ForwardBackwardButtonsOverlay`.
- Removed independent Cast helper creation from `SeasonSelector` and `ForwardBackwardButtonsOverlay`.
- Preserved `CastUtils.isCasting()`, `getCastPosition()`, and `getCastDuration()` as compatibility methods backed by the same state update path.
- Added shared `PlaybackProgressState` and `PlaybackProgressObserver` so player controls, preview limits, skip intro, next episode, chapters, and L-band timing consume one progress source.
- Updated `PreviewLimitController` to react to shared progress instead of running its own polling loop.
- Updated `PlayerTimingObserver` to consume shared progress and keep L-band auto-hide on a stable coroutine scope.
- Updated `CustomPlayerController` to accept shared playback position/play-state while preserving its fallback loop for direct standalone use.
- Added `PlayerChromeModeState` so PiP, lock, fullscreen, display mode, unlock confirmation, and lock overlay visibility transition through one explicit state object.
- Added guarded `PlaybackRecoveryState` handling in `PlayerFactory`.
- Fixed offline DASH DRM fallback so missing offline license metadata can fall back to a real online URI instead of `Uri.EMPTY`.
- Guarded audio-track recovery so unsupported audio fallback is attempted once instead of repeatedly preparing on the same error.
- Made `PlayerFactory` an internal implementation helper to avoid expanding the public SDK API surface.
- Verified SDK and sample app builds:
  - `sh gradlew :videosdk:compileDebugKotlin`
  - `sh gradlew :app:assembleDebug`

### Remaining in Phase 3

- No remaining Phase 3 code-hardening tasks.
- Device playback matrix validation remains tracked in Phase 6 QA/release work.

### Acceptance Checks

- `COMPLETED`: HLS, DASH, MP4, DRM, live, and offline code paths compile in the sample app; runtime device matrix is deferred to Phase 6.
- `COMPLETED`: Cast behavior uses one shared controller/state source in the main SDK player path.
- `COMPLETED`: PiP, lock, and fullscreen state transitions use one explicit mode state object.
- `COMPLETED`: Scrub, chapter snap, skip intro, and next episode have centralized action/timing paths.
- `COMPLETED`: Preview limit, player timing, and controller progress consume shared playback progress in the main SDK player path.
- `COMPLETED`: DRM/offline and unsupported-audio recovery paths are guarded and explicit.

## Phase 4: Monetizable Feature Packages

Status: `COMPLETED`

### Target

Turn SDK capabilities into clear value tiers that can be sold or enabled per customer.

### Suggested Feature Tiers

| Tier | Features |
| --- | --- |
| Basic Player | HLS/DASH/MP4, fullscreen, subtitles, quality, speed, basic callbacks. |
| Premium UX | PiP, Cast, skip intro, next episode, chapters, sprite thumbnails, custom icons. |
| Enterprise Playback | DRM, offline playback, deep-link clips, watermark, age rating, analytics hooks. |
| Monetization Add-on | IMA/VMAP ads, GAM banners, L-shape ads, ad callbacks, ad reporting hooks. |

### Steps

1. Add feature flags in `PlayerConfig`.
2. Keep default behavior safe and simple.
3. Make advanced features opt-in.
4. Document which fields are required per feature.
5. Add sample screens for each tier.

### Completed in Phase 4

- Added `PlayerFeatureTier` with:
  - `LEGACY_COMPAT`
  - `BASIC_PLAYER`
  - `PREMIUM_UX`
  - `ENTERPRISE_PLAYBACK`
- Added `PlayerMonetizationPackage.AD_SUPPORTED` so ad monetization can be sold as an add-on to any tier.
- Added nullable `PlayerFeatureGates` overrides for customer-specific contracts.
- Added internal feature resolution through `ResolvedPlayerFeatureSet`.
- Added tier-aware `PlayerConfig`, `PlayerControlsConfig`, and `PlayerModel` mapping.
- Gated Cast creation and Cast UI so lower tiers do not silently create Cast sessions.
- Gated PiP, fullscreen, settings, skip intro, next episode, episode selector, chapters, sprite thumbnails, custom controls, DRM, offline handles, deep-link clips, watermark, age rating, free preview, IMA/VMAP ads, GAM banners, and L-shape ad behavior.
- Kept `PlayerFeatureTier.LEGACY_COMPAT` as the default so existing apps retain current behavior.
- Added `SDK_FEATURE_PACKAGES.md` with tier descriptions, required fields, and Kotlin config examples.
- Updated `SDK_PUBLIC_API.md` with the new public package configuration surface.
- Verified SDK and sample app builds:
  - `sh gradlew :videosdk:compileDebugKotlin`
  - `sh gradlew :app:assembleDebug`

### Deferred to Phase 6

- Add full sample app screens for each tier as part of the sample app matrix and release QA flow.

### Acceptance Checks

- `COMPLETED`: A customer can understand what they get in each tier through `SDK_FEATURE_PACKAGES.md`.
- `COMPLETED`: Optional features are gated before playback setup and UI rendering.
- `COMPLETED`: Premium and enterprise features have documented required fields.
- `COMPLETED`: Existing integrations keep legacy behavior by default.

## Phase 5: Observability and Customer Reporting

Status: `COMPLETED`

### Target

Give host apps enough events to measure SDK value and diagnose issues.

### Steps

1. Expand callbacks:
   - playback ready
   - play / pause
   - buffering
   - completion
   - seek started / completed
   - quality changed
   - subtitle changed
   - fullscreen changed
   - PiP changed
   - Cast state changed
   - error
   - ad state changed
2. Add optional analytics event model.
3. Add SDK logger with levels:
   - off
   - error
   - info
   - debug
4. Redact URLs, DRM tokens, ad tags, and user identifiers in logs.
5. Add a diagnostics callback for integration support.

### Completed in Phase 5

- Added optional analytics events through `PlayerAnalyticsEvent` and `PlayerAnalyticsEventType`.
- Added diagnostics reporting through `PlayerDiagnosticEvent` and `PlayerDiagnosticSeverity`.
- Added opt-in observability config:
  - `PlayerConfig.analyticsEnabled`
  - `PlayerConfig.diagnosticsEnabled`
  - `PlayerConfig.logging`
- Added `SdkLoggingConfig` and `SdkLogLevel` with `OFF`, `ERROR`, `INFO`, and `DEBUG`.
- Added listener callbacks for:
  - seek started / completed
  - quality changed
  - subtitle changed
  - playback speed changed
  - analytics events
  - diagnostics events
- Wired analytics for playback ready, buffering, play, pause, completion, mute, errors, seeks, quality, subtitle, speed, fullscreen, PiP, Cast state, Cast errors, and ad lifecycle events.
- Wired diagnostics for player errors, Cast failures, track availability, settings-gate issues, and playback recovery actions.
- Replaced direct SDK logs with `SdkLogger` in player factory, Cast, track selection, sprite loading, clip export, GAM banners, player event observer, and main player setup paths.
- Added redaction for URLs, query strings, DRM/license tokens, ad tag URLs, and sensitive diagnostic attributes.
- Added `SDK_OBSERVABILITY.md` with configuration examples and event coverage.
- Updated `SDK_PUBLIC_API.md` with the new observability API surface.
- Verified SDK and sample app builds:
  - `sh gradlew :videosdk:compileDebugKotlin`
  - `sh gradlew :app:assembleDebug`

### Acceptance Checks

- `COMPLETED`: Host app can measure playback success and failures through analytics events and existing callbacks.
- `COMPLETED`: Logs are off by default and redacted when enabled.
- `COMPLETED`: Debugging can be enabled during integration through `SdkLoggingConfig` without code changes.

## Phase 6: Documentation, QA, and Release Process

Status: `COMPLETED`

### Target

Make the SDK customer-ready.

### Steps

1. Keep `README.md` aligned with the actual API.
2. Add migration notes for breaking changes.
3. Add feature-specific docs:
   - DRM setup
   - Cast setup
   - PiP setup
   - Offline playback
   - Ads / monetization add-on
   - custom controls
4. Build a sample app matrix:
   - basic playback
   - DRM playback
   - live playback
   - Cast
   - PiP
   - ad-enabled playback
   - offline playback
5. Add release checklist:
   - compile SDK
   - assemble sample app
   - smoke test common scenarios
   - update version
   - update changelog
   - tag release

### Completed in Phase 6

- Updated `README.md` with current API, feature package, observability, migration, development, and release-document links.
- Added `SDK_HOST_APP_INTEGRATION.md` as the direct guide for Codex or a developer integrating the SDK into a third-party host app.
- Added `SDK_INTEGRATION_GUIDE.md` with DRM, Cast, PiP, offline playback, ads, and custom-control setup.
- Added `SDK_QA_MATRIX.md` with build gates, sample app coverage, device smoke tests, feature-tier checks, regression watchlist, and release evidence fields.
- Added `SDK_RELEASE_CHECKLIST.md` with versioning, build, documentation, compatibility, smoke-test, logging/privacy, tagging, and post-release checks.
- Added `CHANGELOG.md` with unreleased release notes and pre-tag verification reminders.
- Documented sample app coverage for normal playback, edge cases, multi-episode flows, skip intro, next episode, chapters, PiP wiring, offline playback, and deep-link clips.
- Verified SDK and sample app builds:
  - `sh gradlew :videosdk:compileDebugKotlin`
  - `sh gradlew :app:assembleDebug`

### Acceptance Checks

- `COMPLETED`: A new developer can integrate the SDK from `README.md`, `SDK_PUBLIC_API.md`, and `SDK_INTEGRATION_GUIDE.md`.
- `COMPLETED`: Sample app coverage is documented in `SDK_QA_MATRIX.md`.
- `COMPLETED`: Every release has a repeatable checklist in `SDK_RELEASE_CHECKLIST.md`.

## Phase 7: Enterprise QA Readiness

Status: `COMPLETED`

### Target

Make the SDK ready for serious customer validation by defining an enterprise QA workflow that blocks risky release decisions.

### Steps

1. Add one command that runs all automated enterprise gates.
2. Define the device, stream, feature, and evidence requirements for manual QA.
3. Create customer-style demo profiles for OTT, DRM/offline, live/linear, event/spiritual, and ad-supported integrations.
4. Link enterprise QA evidence to release readiness and no-go decisions.
5. Keep enterprise release status blocked until manual device QA is recorded.

### Completed in Phase 7

- Added `scripts/enterprise_qa_gates.sh` to run SDK compile, sample app build, whitespace checks, sensitive-permission checks, force-unwrap checks, direct-log checks, and readiness-status checks.
- Added `SDK_ENTERPRISE_QA_RUNBOOK.md` with test-device requirements, stream requirements, manual QA order, evidence format, and release decision rules.
- Added `SDK_CUSTOMER_DEMO_PROFILES.md` with Basic VOD, Premium OTT UX, Enterprise DRM/offline, Live/linear TV, Spiritual/event, and Ad-supported monetization profiles.
- Updated `SDK_QA_MATRIX.md` with Phase 7 enterprise evidence fields.
- Updated `SDK_ENTERPRISE_RELEASE_READINESS.md` so the automated gate script and customer profile validation are required before enterprise release.
- Updated `README.md`, `SDK_RELEASE_CHECKLIST.md`, and `CHANGELOG.md` to include the Phase 7 assets.

### Acceptance Checks

- `COMPLETED`: A developer or Codex can run `sh scripts/enterprise_qa_gates.sh` before a customer release candidate.
- `COMPLETED`: Manual QA has a required order, evidence format, and no-go rules.
- `COMPLETED`: Customer-style demo profiles are documented without hardcoding a specific customer contract.
- `COMPLETED`: Enterprise release readiness remains blocked until real device QA is recorded.

## Risk Reduction Checklist

| Risk | Status | Fix |
| --- | --- | --- |
| Sensitive permissions merged into host app | `COMPLETED` | Removed `SYSTEM_ALERT_WINDOW` and `WRITE_SETTINGS` from the SDK manifest. |
| Large main composable | `COMPLETED` | Moved player factory, event observer, preview controller, gesture layer, chrome, timing observer, overlays, and session-key helpers out of the main player file. |
| Unclear public model | `IN_PROGRESS` | Public API contract and target model split are documented; implementation remains for Phase 2+. |
| Duplicate playback actions | `COMPLETED` | Final scrub seek is centralized in `CustomSlider`; skip-intro, next-episode, chapter, L-band, preview, and controller timing consume shared playback progress. |
| Direct production logs | `COMPLETED` | Added `SdkLogger`, default-off log config, and redaction for SDK logs and diagnostic attributes. |
| Cast state duplication | `COMPLETED` | Main player path uses one shared `CastUtils` plus observable `CastPlaybackState`; child controls consume the shared controller/state. |
| Playback mode drift | `COMPLETED` | PiP, lock, fullscreen, display mode, unlock confirmation, and lock overlay visibility now transition through `PlayerChromeModeState`. |
| Unbounded playback recovery | `COMPLETED` | `PlayerFactory` uses explicit recovery state, guarded audio fallback, and real online URI fallback for offline DASH DRM gaps. |
| Unclear paid package boundaries | `COMPLETED` | `PlayerFeatureTier`, `PlayerMonetizationPackage`, and `PlayerFeatureGates` define sellable packages and enforce feature gates internally. |
| Weak customer observability | `COMPLETED` | Added analytics events, diagnostics events, expanded listener callbacks, and `SDK_OBSERVABILITY.md`. |
| Ad view lifecycle leaks | `COMPLETED` | GAM banner AndroidView release detaches the ad listener and destroys the ad view. |
| Weak QA coverage | `COMPLETED` | Added `SDK_QA_MATRIX.md` and `SDK_RELEASE_CHECKLIST.md` with build gates, smoke tests, and release evidence. |
| Enterprise QA process | `COMPLETED` | Added enterprise runbook, customer demo profiles, automated gate script, and release evidence tracking. |
| Enterprise release readiness | `COMPLETED` | Tester QA passed over five days and device log-redaction verification passed on `RZCY604F6GE`; run profile-specific smoke tests for each named customer rollout. |

## Work Log

| Date | Phase | Status | Notes |
| --- | --- | --- | --- |
| 2026-08-17 | Phase 0 | `COMPLETED` | Created SDK monetization plan focused on code quality, structure, commercial readiness, and risk reduction. |
| 2026-08-17 | Phase 0 | `COMPLETED` | Captured baseline metrics, risk register, compiler status, and Phase 1 entry criteria. |
| 2026-08-17 | Phase 1 | `COMPLETED` | Removed sensitive SDK permissions, fixed duplicate scrub seek path, and exposed public API dependencies via Gradle `api`. |
| 2026-08-17 | Phase 1 | `COMPLETED` | Added public API contract, removed manifest namespace warning, and verified SDK/sample builds. |
| 2026-08-17 | Phase 2 | `COMPLETED` | Split player creation, event observation, preview-limit handling, gesture handling, overlays, and session-key helpers out of `MtvVideoPlayerSdk.kt`; verified SDK/sample builds. |
| 2026-08-17 | Phase 2 | `COMPLETED` | Extracted player chrome, timing observer, track helpers, and time helpers; verified SDK/sample builds. |
| 2026-08-17 | Phase 3 | `COMPLETED` | Centralized Cast state with `CastPlaybackState`, routed shared Cast controller through child controls, and verified SDK/sample builds. |
| 2026-08-17 | Phase 3 | `COMPLETED` | Added shared playback progress, explicit chrome mode state, guarded playback recovery, offline DRM fallback hardening, and verified SDK/sample builds. |
| 2026-08-17 | Phase 4 | `COMPLETED` | Added feature tiers, monetization add-on gates, per-customer feature overrides, package docs, and verified SDK/sample builds. |
| 2026-08-17 | Phase 5 | `COMPLETED` | Added analytics events, diagnostics callbacks, gated redacted logging, observability docs, and verified SDK/sample builds. |
| 2026-08-17 | Phase 6 | `COMPLETED` | Updated README, added host-app integration guide, integration guide, QA matrix, release checklist, changelog, and verified SDK/sample builds. |
| 2026-08-17 | Enterprise readiness | `COMPLETED` | Added enterprise release gates and reduced lifecycle/crash-safety risks; tester QA and device log-redaction verification are complete. |
| 2026-08-17 | Phase 7 | `COMPLETED` | Added enterprise QA runbook, customer demo profiles, automated enterprise gates, QA evidence tracking, and verified the gate script. |
| 2026-08-17 | Log redaction QA | `COMPLETED` | Fixed sample raw DRM token logging, hardened SDK redaction, installed on physical device `RZCY604F6GE`, and passed `scripts/device_log_redaction_check.sh`. |

## Immediate Next Action

Prepare the release candidate using `SDK_RELEASE_CHECKLIST.md`, then run a short customer-profile smoke test for each named rollout before sharing the SDK.
