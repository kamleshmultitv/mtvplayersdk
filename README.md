# Mtv Video Player SDK

Mtv Video Player SDK is an Android Media3 and Jetpack Compose player for HLS, DASH/MPD, MP4, Widevine DRM, Cast, PiP, ads, subtitles, offline playback, and reels-style playback.

## Start Here

Use only the guide that matches your task:

| Task | Read This |
| --- | --- |
| Third-party app wants to integrate VideoPlayer SDK | `videosdk/THIRD_PARTY_INTEGRATION.md` |
| Third-party app also wants downloads | `mtvdownloader/THIRD_PARTY_INTEGRATION.md` |
| Main/sample app needs Downloader SDK plus VideoPlayer offline playback | `app/main-application-full-change-guide.md` |
| Developer is changing VideoPlayer SDK source code | `videosdk/README.md` |
| Maintainer needs public API compatibility rules | `videosdk/PUBLIC_API.md` |

Do not use `videosdk/README.md` as a third-party app integration guide. It is only for SDK source-level work.

## Third-Party App Guide

Share this file with any external app team:

```text
videosdk/THIRD_PARTY_INTEGRATION.md
```

It contains the dependency setup, manifest requirements, Compose player usage, `PlayerModel` mapping, DRM rules, Cast/PiP/ads setup, and downloaded DRM MPD playback mapping.

## Quick Dependency

Add JitPack:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

Add the SDK dependency:

```kotlin
dependencies {
    implementation("com.github.kamleshmultitv:mtvplayersdk:mobile-2.0.30")
}
```

Use the release tag shared by the SDK owner.

## Important Notes

- For normal HLS playback, do not force `drm = "1"`.
- For Widevine MPD/DASH playback, pass `mpdUrl`, `drm = "1"`, and `drmToken`.
- For downloaded DRM MPD playback, pass `downloadManager`, `downloadCache`, `drmOfflineKeySetId`, or `drmOfflineKeySetIdBase64`.
- Downloader integration belongs in `mtvdownloader/THIRD_PARTY_INTEGRATION.md`.
- SDK-source implementation details belong in `videosdk/README.md`.

## Maintenance Docs

These are for SDK maintainers, not third-party app teams:

- `videosdk/PUBLIC_API.md`
- `videosdk/docs/FEATURE_PACKAGES.md`
- `videosdk/docs/OBSERVABILITY.md`
- `videosdk/docs/QA_MATRIX.md`
- `videosdk/docs/RELEASE_CHECKLIST.md`
- `videosdk/docs/ENTERPRISE_RELEASE_READINESS.md`
- `videosdk/docs/ENTERPRISE_QA_RUNBOOK.md`
- `videosdk/docs/CUSTOMER_DEMO_PROFILES.md`
- `videosdk/docs/MONETIZATION_PLAN.md`
- `videosdk/docs/CHANGELOG.md`
