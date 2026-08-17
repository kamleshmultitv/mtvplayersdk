# Enterprise QA Runbook

This runbook is the Phase 7 execution guide for moving Mtv Video Player SDK from integration-ready to enterprise-release-ready.

Use it before any large customer release, pilot, or tagged SDK version.

## Release Rule

The SDK is not enterprise-release-ready until:

- `scripts/enterprise_qa_gates.sh` passes.
- `SDK_QA_MATRIX.md` has recorded device results.
- `scripts/device_log_redaction_check.sh` passes after SDK DEBUG logs are enabled and playback is exercised.
- Customer demo profile checks pass for the target package.
- `SDK_ENTERPRISE_RELEASE_READINESS.md` is updated from `NOT_READY_FOR_ENTERPRISE_RELEASE` only after QA evidence is recorded.

## 1. Automated Gates

Run:

```bash
sh scripts/enterprise_qa_gates.sh
```

This script verifies:

- SDK Kotlin compile.
- Sample app debug build.
- Whitespace diff check.
- SDK manifest does not ship `SYSTEM_ALERT_WINDOW` or `WRITE_SETTINGS`.
- SDK source does not contain force unwraps.
- SDK source routes logs through `SdkLogger`.
- Enterprise release readiness has an explicit decision status.

Do not continue to manual QA if this script fails.

## 1.1 Device Log Redaction Gate

Enable SDK logs in the QA build:

```kotlin
PlayerConfig(
    logging = SdkLoggingConfig(level = SdkLogLevel.DEBUG)
)
```

Exercise playback paths that can log media, DRM, Cast, ads, diagnostics, and recovery events.

Then run:

```bash
sh scripts/device_log_redaction_check.sh
```

For multiple connected devices:

```bash
ANDROID_SERIAL=<device-id> sh scripts/device_log_redaction_check.sh
```

Pass condition:

- No full query strings.
- No raw DRM tokens.
- No raw license URLs.
- No raw ad tag URLs.
- No authorization or bearer credentials.

If this fails, do not mark enterprise release as ready.

## 2. Required Test Devices

Use at least:

| Device Type | Required | Notes |
| --- | --- | --- |
| Android phone | Yes | Current or recent Android version. |
| Android tablet / large viewport | Yes | Confirms player layout, sheets, controls, and fullscreen. |
| Low or mid-range phone | Yes | Confirms loading, seekbar, ad, and thumbnail behavior under pressure. |
| Chromecast device | Required for Cast customers | Use customer receiver when available. |
| Offline-capable test device | Required for offline customers | Must be able to download and replay content. |

## 3. Required Test Streams

Prepare these before QA starts:

| Stream Type | Required For | Evidence |
| --- | --- | --- |
| HLS VOD | All customers | Playback, seek, subtitles, quality where available. |
| DASH VOD | Enterprise customers | Playback and quality switching. |
| MP4/direct URL | Basic fallback | Direct playback without adaptive assumptions. |
| Live stream | Live/linear customers | Live placeholder, Go Live, seek rules. |
| Widevine DASH | DRM customers | License success and failure behavior. |
| Offline non-DRM | Offline customers | Completed download replay. |
| Offline DRM | Offline DRM customers | Offline license and fallback behavior. |
| IMA/VMAP ad tag | Ad customers | Ad lifecycle and content resume. |
| GAM/L-shape ad unit | Ad customers | Banner render, timing, close behavior. |

## 4. Manual QA Order

Run in this order so failures are easier to isolate:

1. Basic HLS playback.
2. Fullscreen, lock, back handling.
3. Seekbar scrub, pointer, thumbnail preview, current time.
4. Subtitles, quality, playback speed.
5. Skip intro, next episode, episode selector, chapters.
6. PiP.
7. Cast.
8. DASH and DRM.
9. Offline.
10. Ads.
11. Analytics, diagnostics, and logging redaction.

Record every result in `SDK_QA_MATRIX.md`.

## 5. Evidence Format

For each scenario, record:

```text
Date:
Tester:
Device:
Android version:
SDK commit:
Scenario:
Result: PASS / FAIL / BLOCKED
Evidence: screenshot, screen recording, logs, or notes
Issue link:
Fix commit:
Retest result:
```

## 6. Customer Profile Validation

Use `SDK_CUSTOMER_DEMO_PROFILES.md` to choose a profile:

- OTT premium VOD.
- Enterprise DRM and offline.
- Live/linear TV.
- Spiritual/event VOD and live.
- Ad-supported free preview.

Validate only the features included in the customer contract, but make sure unsupported features are hidden or disabled.

## 7. Exit Criteria

Phase 7 readiness is complete when:

- Automated gate script passes.
- QA matrix has real-device results for the target customer profile.
- Every failed item has a fix or a documented customer-approved limitation.
- Release checklist is complete.
- Changelog has the release note.
- Enterprise release readiness status is updated only when evidence supports it.

## 8. Release Decision

Use this rule:

| Condition | Decision |
| --- | --- |
| Automated gates fail | `NO_GO` |
| Any target-customer playback path is untested | `NO_GO` |
| Any DRM/Cast/ad path fails for a customer that needs it | `NO_GO` |
| All target-customer paths pass with evidence | `GO_FOR_RELEASE_CANDIDATE` |
| Release candidate passes clean-host integration | `GO_FOR_TAG` |
