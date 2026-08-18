# Changelog

All notable Mtv Video Player SDK changes should be recorded here before release.

## Unreleased

### Added

- SDK feature package model with `PlayerFeatureTier`, `PlayerMonetizationPackage`, and `PlayerFeatureGates`.
- Optional analytics events through `PlayerAnalyticsEvent`.
- Optional diagnostics events through `PlayerDiagnosticEvent`.
- Default-off SDK logging through `SdkLoggingConfig` and `SdkLogLevel`.
- Documentation for feature packages, observability, integration setup, QA, and release flow.
- Enterprise QA runbook, customer demo profiles, enterprise release readiness gates, and an automated enterprise gate script.

### Changed

- SDK logs are routed through a redacting logger.
- Cast, playback progress, player chrome mode, and playback recovery paths are more centralized.
- README now reflects the current public API and integration workflow.
- GAM banner AndroidView cleanup now destroys the ad view on release.

### Fixed

- Sensitive SDK manifest permissions were removed.
- Duplicate final seek path was reduced.
- Seekbar pointer and scrub time behavior were refined.
- Settings selections for audio, captions, playback speed, and video quality now stay visually selected while the same player session is alive.
- Offline DASH DRM fallback and unsupported-audio recovery are guarded.
- Remaining SDK force unwrap crash-risk patterns were removed from Cast, selector, and sprite thumbnail paths.
- Sample app raw DRM token logging was removed, and SDK redaction now handles sensitive `:` and query-fragment values such as authorization and payload fields.

### Release Checklist

- Run `sh gradlew :videosdk:compileDebugKotlin`.
- Run `sh gradlew :app:assembleDebug`.
- Run `git diff --check`.
- Run `sh scripts/enterprise_qa_gates.sh` before enterprise release candidates.
- Run `sh scripts/device_log_redaction_check.sh` after exercising SDK DEBUG logs on a device.
- Record manual QA results in `SDK_QA_MATRIX.md`.
