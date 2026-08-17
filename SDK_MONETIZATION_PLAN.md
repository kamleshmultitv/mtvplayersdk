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
| Phase 0 | `RUNNING` | Audit SDK quality, risk areas, public API, and monetization readiness. |
| Phase 1 | `PENDING` | Stabilize public API and reduce integration risk. |
| Phase 2 | `PENDING` | Improve code structure and separate responsibilities. |
| Phase 3 | `PENDING` | Harden playback, Cast, PiP, lifecycle, and controls behavior. |
| Phase 4 | `PENDING` | Build monetizable feature packages and configuration tiers. |
| Phase 5 | `PENDING` | Add observability, analytics, callbacks, and host-app reporting. |
| Phase 6 | `PENDING` | Strengthen documentation, sample app, QA matrix, and release process. |

## Phase 0: SDK Audit and Baseline

Status: `RUNNING`

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

### Acceptance Checks

- Current risks are documented.
- SDK still compiles.
- A phase-wise improvement path exists.
- The next phase can start without more discovery.

## Phase 1: Public API and Integration Risk

Status: `PENDING`

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

### Acceptance Checks

- Host apps can integrate without sensitive unexpected permissions.
- Public models are easy to understand.
- Existing sample app still compiles.
- Breaking changes are documented or avoided.

## Phase 2: Code Structure Cleanup

Status: `PENDING`

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

### Acceptance Checks

- Main composable is small enough to review.
- Playback setup can be tested separately from UI.
- Control visibility logic has one source of truth.
- No duplicate seek or timing code remains.

## Phase 3: Playback Reliability

Status: `PENDING`

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

### Acceptance Checks

- HLS, DASH, MP4, DRM, live, and offline playback work in sample app.
- Cast behavior does not conflict with local controls.
- PiP suppresses overlays correctly.
- Scrub, chapter snap, skip intro, and next episode work without duplicate actions.

## Phase 4: Monetizable Feature Packages

Status: `PENDING`

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

### Acceptance Checks

- A customer can understand what they get in each tier.
- Optional features do not affect apps that do not enable them.
- Premium features are documented and testable.

## Phase 5: Observability and Customer Reporting

Status: `PENDING`

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

### Acceptance Checks

- Host app can measure playback success and failures.
- Logs are safe for production.
- Debugging can be enabled during integration without code changes.

## Phase 6: Documentation, QA, and Release Process

Status: `PENDING`

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

### Acceptance Checks

- A new developer can integrate the SDK from docs.
- Sample app demonstrates paid features.
- Every release has a repeatable checklist.

## Risk Reduction Checklist

| Risk | Status | Fix |
| --- | --- | --- |
| Sensitive permissions merged into host app | `PENDING` | Remove or document only when feature-required. |
| Large main composable | `PENDING` | Split into session, controller, overlays, and UI layers. |
| Unclear public model | `PENDING` | Split `PlayerModel` into focused SDK models. |
| Duplicate playback actions | `PENDING` | Centralize seek and timing actions. |
| Direct production logs | `PENDING` | Add gated SDK logger. |
| Cast state duplication | `PENDING` | Use one Cast controller/state source. |
| Ad view lifecycle leaks | `PENDING` | Add cleanup in ad AndroidView wrappers. |
| Weak QA coverage | `PENDING` | Add scenario matrix and release checklist. |

## Work Log

| Date | Phase | Status | Notes |
| --- | --- | --- | --- |
| 2026-08-17 | Phase 0 | `RUNNING` | Created SDK monetization plan focused on code quality, structure, commercial readiness, and risk reduction. |

## Immediate Next Action

Start Phase 1 by reducing integration risk: clean SDK manifest permissions, fix duplicate seek behavior, and define the stable public API surface before adding more features.
