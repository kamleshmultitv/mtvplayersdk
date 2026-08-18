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

## 8. JitPack Release Criteria

Use this section directly for future JitPack releases. Do not create or push a tag until every item here is complete.

- The worktree contains only intended release changes. Do not include `.idea` files unless the release intentionally changes project metadata.
- `videosdk/build.gradle.kts` has the new SDK version in the `release` Maven publication.
- The Git tag matches the SDK version exactly, for example `mobile-2.0.32`.
- The JitPack build task passes locally:

```bash
sh gradlew :videosdk:publishToMavenLocal
```

- The sample app still compiles:

```bash
sh gradlew :app:compileDebugKotlin
```

- Whitespace checks pass:

```bash
git diff --check
```

For this repo, always use this local command before tagging:

```bash
sh gradlew :videosdk:publishToMavenLocal
```

`jitpack.yml` also uses `sh gradlew ...` so the JitPack remote build works even when the wrapper file is not executable.

## 9. JitPack Tag And Push Flow

Use this exact flow after the criteria above pass and the release changes are not committed yet:

```bash
git status --short
git add videosdk/build.gradle.kts videosdk/src/main/java/com/app/videosdk
git add README.md CHANGELOG.md SDK_RELEASE_CHECKLIST.md
git commit -m "Release mobile-x.y.z"

git tag mobile-x.y.z
git push origin main
git push origin mobile-x.y.z
```

Replace `mobile-x.y.z` with the real version, for example `mobile-2.0.32`.

If the release commit is already created and pushed to `main`, the minimum JitPack publish commands are only:

```bash
git tag mobile-x.y.z
git push origin mobile-x.y.z
```

If the release commit exists locally but is not pushed yet, use:

```bash
git push origin main
git tag mobile-x.y.z
git push origin mobile-x.y.z
```

If the release branch is not `main`, push the active release branch instead:

```bash
git push origin HEAD
git push origin mobile-x.y.z
```

## 10. Legacy Tagging Reference

Recommended tag format:

```bash
git tag mobile-x.y.z
git push origin mobile-x.y.z
```

Only tag after build, docs, compatibility, and smoke-test gates are complete.

## 11. Post-Release

- Verify the dependency can be resolved by a clean sample app.
- Verify README installation snippet matches the release tag.
- Open the JitPack project page for `kamleshmultitv/mtvplayersdk`, request the new tag, and confirm the build is green.
- Open follow-up issues for deferred QA failures or known limitations.
