# VideoPlayer SDK Module

This is the source-level README for the `videosdk` module.

App teams should use `THIRD_PARTY_INTEGRATION.md`. SDK maintainers should use this file plus `PUBLIC_API.md` and the maintenance docs under `docs/`.

## Which Guide To Use

| Need | File |
| --- | --- |
| Third-party app wants to add VideoPlayer SDK | `THIRD_PARTY_INTEGRATION.md` |
| Developer or coding agent is implementing the player in a host app | `THIRD_PARTY_INTEGRATION.md` |
| App needs Downloader SDK only | `../mtvdownloader/THIRD_PARTY_INTEGRATION.md` |
| App needs Downloader SDK plus VideoPlayer offline playback | `../app/main-application-full-change-guide.md` |
| Public API compatibility rules | `PUBLIC_API.md` |
| VideoPlayer SDK source rules | This file |

Do not copy SDK internals into a host app. Host apps should depend on the published SDK or a local `:videosdk` module while testing SDK source changes.

## Public Offline Contract

`PlayerModel` must keep these downloader/offline fields:

```kotlin
val cacheFactory: CacheDataSource.Factory?
val downloadManager: DownloadManager?
val downloadCache: SimpleCache?
val drmOfflineKeySetId: ByteArray?
val drmOfflineKeySetIdBase64: String?
```

The host app passes these fields only for downloaded playback. For downloaded DRM MPD/DASH, the key set is the saved Widevine offline license handle.

## Source Resolution

Online playback should keep this order:

1. If `drm == "1"` and `mpdUrl` exists, play `mpdUrl` as Widevine DASH.
2. If `drm != "1"` and `hlsUrl` exists, play `hlsUrl`.
3. If `liveUrl` exists, play `liveUrl`.
4. If `videoUrl` exists, play `videoUrl`.

Do not force `drm = "1"` for normal HLS/API-list playback. Set it only when the player should choose MPD/DASH DRM.

For online DRM, build Media3 DRM config with:

```kotlin
MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
    .setLicenseUri(drmToken)
    .setForceDefaultLicenseUri(true)
    .setMultiSession(true)
    .build()
```

## Downloaded Playback

Before creating the media item, check for a completed Media3 download using `PlayerModel.id`:

```kotlin
val download = content.downloadManager
    ?.downloadIndex
    ?.getDownload(content.id.toString())

val completedDownload = download?.takeIf { it.state == Download.STATE_COMPLETED }
```

If a completed download exists:

- Use app-provided `downloadCache` or `cacheFactory`.
- Use `CacheDataSource.FLAG_BLOCK_ON_CACHE`.
- For HLS and MP4 downloads, use `completedDownload.request.toMediaItem()`.
- For DRM MPD/DASH downloads, rebuild the media item with the offline Widevine key set.

Offline DRM key set restore order:

1. `completedDownload.request.keySetId`
2. `content.drmOfflineKeySetId`
3. Base64-decoded `content.drmOfflineKeySetIdBase64`

For offline DRM MPD/DASH:

```kotlin
MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
    .setKeySetId(keySetId)
    .build()
```

Do not set the online license URL as the offline fallback inside this offline media item. True offline playback must use the persisted key set.

## Important Source Files

- `src/main/java/com/app/videosdk/model/PlayerModel.kt`
- `src/main/java/com/app/videosdk/model/PlayerFeatureResolver.kt`
- `src/main/java/com/app/videosdk/player/PlayerFactory.kt`
- `src/main/java/com/app/videosdk/player/PlaybackSourceResolver.kt`

## Acceptance Checks

- Online HLS plays when `drm` is not forced to `"1"`.
- Online MPD DRM plays with a valid license URL.
- Downloaded HLS and MP4 play from the Media3 download cache.
- Downloaded DRM MPD plays offline with a persisted key set.
- Downloaded DRM MPD still plays after app restart.
- Airplane-mode playback works for completed non-DRM downloads.
- Airplane-mode playback works for completed DRM MPD downloads when the key set exists and the license has not expired.

Run:

```bash
sh gradlew :videosdk:compileDebugKotlin
sh gradlew :videosdk:testDebugUnitTest
```
