# SDK Release Checklist

Use this checklist before publishing a new Mtv Video Player SDK version.

## 1. Version And Scope

- Confirm the release type: patch, minor, or major.
- Confirm whether public APIs changed.
- Update version references in build/publishing config.
- Update README dependency snippet if the published version changes.
- Add migration notes for any behavior change.

## 2. Required Build Gates

Run:

```bash
sh gradlew :videosdk:compileDebugKotlin
sh gradlew :app:assembleDebug
git diff --check
```

All three must pass before release.

For enterprise/customer release candidates, also run:

```bash
sh scripts/enterprise_qa_gates.sh
sh scripts/device_log_redaction_check.sh
```

## 3. Documentation Gates

Verify these files are current:

- `README.md`
- `SDK_PUBLIC_API.md`
- `SDK_HOST_APP_INTEGRATION.md`
- `SDK_ENTERPRISE_RELEASE_READINESS.md`
- `SDK_ENTERPRISE_QA_RUNBOOK.md`
- `SDK_CUSTOMER_DEMO_PROFILES.md`
- `SDK_FEATURE_PACKAGES.md`
- `SDK_OBSERVABILITY.md`
- `SDK_INTEGRATION_GUIDE.md`
- `SDK_QA_MATRIX.md`
- `SDK_MONETIZATION_PLAN.md`
- `CHANGELOG.md`

Check that the docs mention any new:

- `PlayerConfig` fields.
- `PlayerModel` fields.
- Listener callbacks.
- Permissions.
- Required host app manifest entries.
- Feature-tier or monetization behavior.

## 4. Compatibility Gates

- Existing `MtvVideoPlayerSdk(...)` calls compile without new required params.
- Existing `PlayerModel(...)` calls compile without new required params.
- Existing `PlayerConfig()` behavior remains compatible through `LEGACY_COMPAT`.
- Existing `PlayerStateListener` implementations compile because new methods have defaults.
- SDK manifest does not reintroduce sensitive permissions unless explicitly approved.

## 5. Manual Smoke Tests

Use `SDK_QA_MATRIX.md`.

Minimum smoke set:

- HLS playback.
- Fullscreen toggle.
- Seekbar scrub and pointer.
- Skip intro.
- Next episode.
- PiP.
- Cast.
- One DRM stream.
- One ad-enabled stream.
- One offline stream when available.

## 6. Logging And Privacy Check

With:

```kotlin
PlayerConfig(
    analyticsEnabled = true,
    diagnosticsEnabled = true,
    logging = SdkLoggingConfig(level = SdkLogLevel.DEBUG)
)
```

Verify:

- Media URLs are redacted.
- DRM tokens and license URLs are redacted.
- Ad tag query strings are redacted.
- Diagnostics are useful without exposing sensitive values.

## 7. Release Artifact

Before tagging, capture:

- SDK version.
- Commit hash.
- Build command results.
- QA matrix result summary.
- Known issues.
- Migration notes.

## 8. Tagging

Recommended tag format:

```bash
git tag mobile-x.y.z
git push origin mobile-x.y.z
```

Only tag after build, docs, compatibility, and smoke-test gates are complete.

## 9. Post-Release

- Verify the dependency can be resolved by a clean sample app.
- Verify README installation snippet matches the release tag.
- Open follow-up issues for deferred QA failures or known limitations.
