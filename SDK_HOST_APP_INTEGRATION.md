# Host App SDK Integration Guide

This file is the implementation guide for a developer or coding agent integrating Mtv Video Player SDK into a third-party Android app.

Use this file when the task is: "read the SDK docs and implement this video SDK in my main app."

## Source Of Truth

- Public API contract: `SDK_PUBLIC_API.md`
- Feature setup details: `SDK_INTEGRATION_GUIDE.md`
- Package/tier rules: `SDK_FEATURE_PACKAGES.md`
- Analytics and logging: `SDK_OBSERVABILITY.md`
- Working sample app examples:
  - `app/src/main/java/com/app/sample/composable/ContentBody.kt`
  - `app/src/main/java/com/app/sample/composable/SdkEdgeCaseScreen.kt`
  - `app/src/main/java/com/app/sample/composable/download/DownloadPlayer.kt`

Do not guess SDK APIs. Use the imports and constructor parameters documented in `README.md` and `SDK_PUBLIC_API.md`.

## Integration Decision

Choose one dependency mode.

### Published SDK Dependency

Use this for a real third-party app.

In root `settings.gradle.kts`:

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

In the app module `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.kamleshmultitv:mtvplayersdk:mobile-2.0.30")
}
```

Use the version from `README.md` when it changes.

### Local SDK Module

Use this only while testing SDK source changes with a host app.

In host app `settings.gradle.kts`:

```kotlin
include(":videosdk")
project(":videosdk").projectDir = file("../mtvplayersdk/videosdk")
```

In host app module `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":videosdk"))
}
```

## Required Android Setup

Add permissions in the host app manifest:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

Enable Compose in the host app module:

```kotlin
android {
    buildFeatures {
        compose = true
    }
}
```

For Picture-in-Picture, update the host Activity:

```xml
<activity
    android:name=".MainActivity"
    android:configChanges="screenSize|smallestScreenSize|screenLayout|orientation"
    android:resizeableActivity="true"
    android:supportsPictureInPicture="true" />
```

For Google ads, add the host app AdMob id:

```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy" />
```

For Cast, add or override this host app resource:

```xml
<string name="app_id_prod">YOUR_CAST_RECEIVER_APP_ID</string>
```

## Compose Implementation

Create a player screen in the host app and map the host app content model into `PlayerModel`.

```kotlin
import androidx.compose.runtime.Composable
import com.app.videosdk.listener.PlayerStateListener
import com.app.videosdk.model.PlayerConfig
import com.app.videosdk.model.PlayerFeatureTier
import com.app.videosdk.model.PlayerModel
import com.app.videosdk.ui.MtvVideoPlayerSdk
import com.app.videosdk.utils.PlayerMode

@Composable
fun HostVideoPlayerScreen(
    content: HostVideo,
    isFullScreen: Boolean,
    onFullScreenChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val playerItem = PlayerModel(
        id = content.id,
        hlsUrl = content.hlsUrl,
        mpdUrl = content.dashUrl,
        videoUrl = content.mp4Url,
        imageUrl = content.thumbnailUrl,
        title = content.title,
        episodeTitle = content.episodeTitle,
        seasonTitle = content.seriesTitle,
        srt = content.subtitleUrl,
        isLive = content.isLive
    )

    MtvVideoPlayerSdk(
        contentList = listOf(playerItem),
        index = 0,
        playerMode = if (isFullScreen) PlayerMode.FULL_SCREEN else PlayerMode.MINI,
        playerConfig = PlayerConfig(
            featureTier = PlayerFeatureTier.LEGACY_COMPAT
        ),
        playerStateListener = object : PlayerStateListener {
            override fun onError(error: String) {
                // Send to host app logging or analytics.
            }
        },
        onPlayerBack = {
            if (isFullScreen) {
                onFullScreenChange(false)
            } else {
                onBack()
            }
        },
        setFullScreen = onFullScreenChange
    )
}
```

Replace `HostVideo` with the host app model. Keep SDK-specific mapping inside one mapper or player screen so the rest of the app does not depend on SDK internals.

## XML / View-System Implementation

Use `MtvVideoPlayerView` when the host app is not Compose-first.

XML layout:

```xml
<com.app.videosdk.ui.MtvVideoPlayerView
    android:id="@+id/mtvPlayer"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

Activity or Fragment:

```kotlin
val player = findViewById<MtvVideoPlayerView>(R.id.mtvPlayer)

player.onBackRequested = {
    finish()
}

player.onFullScreenChanged = { fullScreen ->
    // Update host app fullscreen state.
}

player.play(
    PlayerModel(
        id = content.id,
        hlsUrl = content.hlsUrl,
        title = content.title,
        imageUrl = content.thumbnailUrl
    )
)
```

Forward PiP state when the Activity receives it:

```kotlin
override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
    super.onPictureInPictureModeChanged(isInPictureInPictureMode)
    player.setInPictureInPictureMode(isInPictureInPictureMode)
}
```

## Feature Configuration

Start with compatibility mode:

```kotlin
PlayerConfig(
    featureTier = PlayerFeatureTier.LEGACY_COMPAT
)
```

Use package tiers when the host app has a contract:

```kotlin
PlayerConfig(
    featureTier = PlayerFeatureTier.BASIC_PLAYER
)
```

```kotlin
PlayerConfig(
    featureTier = PlayerFeatureTier.PREMIUM_UX
)
```

```kotlin
PlayerConfig(
    featureTier = PlayerFeatureTier.ENTERPRISE_PLAYBACK
)
```

Use ad monetization only when the app has ad tags and ad setup:

```kotlin
PlayerConfig(
    monetizationPackage = PlayerMonetizationPackage.AD_SUPPORTED,
    ads = PlayerAdsConfig(
        googleAdsEnabled = true,
        vmapAdsEnabled = true,
        bannerAdsEnabled = true
    )
)
```

## Content Mapping Checklist

Map only fields the host app really has.

| Host App Need | SDK Field |
| --- | --- |
| HLS playback | `PlayerModel.hlsUrl` |
| DASH playback | `PlayerModel.mpdUrl` |
| MP4 playback | `PlayerModel.videoUrl` |
| Live stream | `PlayerModel.liveUrl` or `isLive = true` |
| DRM license | `drm = "1"` and `drmToken` |
| Poster image | `imageUrl` or `thumbnail` |
| Title | `title`, `episodeTitle`, `seasonTitle` |
| Subtitles | `srt` |
| Skip intro | `skipIntro` |
| Next episode | `nextEpisode` |
| Chapters | `chapters` and `isChapterEnabled = true` |
| IMA ads | `adsConfig` |
| GAM / L-shape ads | `gamAdsConfig` |
| Age/content rating | `ageRating`, `contentRating` |

## Do Not Do This

- Do not copy SDK source files into the host app.
- Do not call internal SDK classes such as `PlayerFactory`.
- Do not edit SDK internals for a host app integration unless the task is explicitly SDK development.
- Do not add sensitive permissions such as `SYSTEM_ALERT_WINDOW` or `WRITE_SETTINGS`.
- Do not hardcode test video URLs in production host app code.
- Do not enable logs in production unless the host app explicitly wants SDK debug logging.

## Verification In Host App

Run the host app build:

```bash
./gradlew :app:assembleDebug
```

If the host app uses a different module name, run:

```bash
./gradlew assembleDebug
```

Manual smoke test:

- Player screen opens.
- Video starts.
- Back button exits fullscreen or player screen correctly.
- Fullscreen toggles correctly.
- Seekbar scrubs correctly.
- Audio mute/unmute works.
- Subtitles, quality, speed, Cast, PiP, DRM, offline, and ads work only when configured.

## Minimum Codex Task Prompt

Use this prompt in a host app:

```text
Read SDK_HOST_APP_INTEGRATION.md, README.md, and SDK_PUBLIC_API.md from the Mtv Video Player SDK repo. Integrate the SDK into this Android app using the published dependency unless I say local module. Add the required Gradle repository/dependency, manifest permissions, a Compose player screen, host content to PlayerModel mapping, fullscreen handling, and build verification. Do not edit SDK internals.
```
