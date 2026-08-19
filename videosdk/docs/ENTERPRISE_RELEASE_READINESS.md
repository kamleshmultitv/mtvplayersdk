# Enterprise Release Readiness

This document defines the minimum release posture before offering Mtv Video Player SDK to large customers such as OTT apps, broadcasters, spiritual platforms, or TV/music clients.

## Current Release Decision

Status: `READY_FOR_ENTERPRISE_RELEASE`

Reason: SDK/sample builds pass, tester manual QA is reported passed over five days, and device log-redaction verification passed on physical device `RZCY604F6GE`.

The SDK is ready for enterprise release candidate use based on the available evidence. For each named customer rollout, still run a short profile-specific smoke test with that customer's streams, Cast receiver, DRM license endpoint, and ad tags.

## Current Risk Level

| Category | Level | Notes |
| --- | --- | --- |
| Build risk | `LOW` | SDK and sample app compile. |
| Manifest/security risk | `LOW` | Sensitive permissions were removed from the SDK manifest. |
| Logging/privacy risk | `LOW` | SDK logs are default-off, routed through redaction, and device log-redaction verification passed. |
| Crash-safety risk | `LOW` | Known force unwraps in SDK source were removed; tester QA passed. |
| Ad lifecycle risk | `LOW` | GAM banner AndroidView release now destroys the ad view. |
| Public API risk | `MEDIUM` | `PlayerModel` remains broad; public contract exists, but focused model split is still pending. |
| Playback device risk | `LOW` | Five-day tester QA is reported passed; keep customer-specific smoke tests for each rollout. |
| Enterprise release risk | `LOW` | Automated gates, tester QA, and device log-redaction verification are complete. |

## Enterprise Release Gates

All gates must be complete before release.

### 1. Build Gates

```bash
sh gradlew :videosdk:compileDebugKotlin
sh gradlew :app:assembleDebug
git diff --check
```

Required result: all pass.

For enterprise release candidates, run the combined gate:

```bash
sh scripts/enterprise_qa_gates.sh
```

This also checks for sensitive SDK manifest permissions, force unwraps, direct SDK log calls, and explicit release decision status.

Run the device log-redaction gate after enabling SDK DEBUG logs and exercising playback:

```bash
sh scripts/device_log_redaction_check.sh
```

Use `ANDROID_SERIAL=<device-id>` when more than one device is connected.

### 2. Device QA Gates

Record results in `videosdk/docs/QA_MATRIX.md`.

Execution order and evidence format are defined in `videosdk/docs/ENTERPRISE_QA_RUNBOOK.md`.

- HLS VOD playback.
- DASH playback.
- MP4/direct playback.
- Live playback.
- Fullscreen enter/exit.
- PiP enter/exit.
- Seekbar scrub and pointer.
- Skip intro.
- Next episode.
- Episode selector.
- Chapters.
- Subtitle selection.
- Quality selection.
- Playback speed.
- Cast start, pause, seek, resume, stop, receiver failure.
- DRM online playback.
- Offline playback.
- Offline DRM fallback behavior.
- IMA/VMAP ads.
- GAM/L-shape banner ads.
- Free preview.
- Analytics callback delivery.
- Diagnostics callback delivery.
- SDK logging redaction.

Current manual QA note: testers reported five-day SDK QA as passed. SDK log redaction passed on physical device `RZCY604F6GE`.

### 3. Customer Contract Gates

- Confirm customer package tier: `BASIC_PLAYER`, `PREMIUM_UX`, or `ENTERPRISE_PLAYBACK`.
- Confirm whether `PlayerMonetizationPackage.AD_SUPPORTED` is included.
- Confirm required features are enabled through `PlayerConfig`.
- Confirm unsupported features are disabled or hidden.
- Confirm host app integration follows `videosdk/THIRD_PARTY_INTEGRATION.md`.
- Confirm customer-style validation follows the matching profile in `videosdk/docs/CUSTOMER_DEMO_PROFILES.md`.

### 4. Privacy And Security Gates

- No media URLs, DRM tokens, license URLs, ad tags, or user identifiers appear in logs.
- SDK manifest does not add sensitive permissions to host apps.
- Host app owns account/session/customer identifiers.
- SDK diagnostics only send redacted, integration-safe attributes.

### 5. API Stability Gates

- Existing `MtvVideoPlayerSdk(...)` integrations compile.
- Existing `MtvVideoPlayerView` integrations compile.
- New `PlayerConfig` fields have defaults.
- New `PlayerStateListener` callbacks have default no-op implementations.
- Breaking changes have migration notes in `README.md` and `videosdk/docs/CHANGELOG.md`.

## No-Go Conditions

Do not release to a large customer if any item is true:

- Device QA matrix is blank or only partially tested.
- DRM behavior is untested for that customer's stream format.
- Cast behavior is untested with that customer's receiver.
- Ads are enabled but ad lifecycle and error behavior are untested.
- Logs expose URLs, tokens, ad tags, or identifiers.
- Device log-redaction verification is not recorded.
- The customer integration requires editing SDK internals.
- The sample app build fails.
- The SDK compile fails.

## Risk Target Before Enterprise Release

Target level: `LOW`

Allowed residual risks:

- Deprecated API warnings with documented migration plan.
- Public `PlayerModel` still broad, if compatibility contract remains documented.
- Customer-specific feature limitations documented in release notes.

Not allowed:

- Known crash paths.
- Known ad view leaks.
- Sensitive permissions.
- Ungated production logs.
- Unverified DRM/Cast/ads for the target customer.
