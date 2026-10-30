# Mtv Video Player SDK - Basic Player Integration Guide

This guide describes how to integrate the **Basic Player** tier of the Mtv Video Player SDK into your Android Kotlin / Jetpack Compose application.

---

## 1. Overview & Basic Player Features

The **Basic Player** tier provides lightweight, high-performance video and audio playback for standard VOD (HLS, DASH, MP4) and Live streams.

### Included Features:
* Core playback: Play, Pause, Seek forward (10s), Seek backward (10s), Seekbar scrubbing.
* Fullscreen toggle (Portrait / Landscape mode).
* Settings menu:
  * **Audio Track Selection** (Multi-language audio track switching).
  * **Playback Speed** (0.25x - 2.0x).
  * **Video Quality Selection** (Auto, 1080p, 720p, 480p, 240p).
* Player state callbacks (`PlayerStateListener`).

---

## 2. Dependency Setup

### Step 1: Add JitPack Repository

Add JitPack to your `settings.gradle.kts` file:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### Step 2: Add SDK Dependency

Add the SDK dependency to your app module's `build.gradle.kts`:

```kotlin
dependencies {
    // Mtv Video Player SDK - Basic Player
    implementation("com.github.kamleshmultitv:videosdk:mobile-monetization-1.0.7")

    // Jetpack Compose & Media3 (Ensure Compose is enabled in buildFeatures)
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
}
```

---

## 3. Basic Player Implementation

To display the player, construct a `PlayerModel` with your media URL and render `MtvVideoPlayerSdk`.

```kotlin
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.app.videosdk.model.PlayerConfig
import com.app.videosdk.model.PlayerModel
import com.app.videosdk.ui.MtvVideoPlayerSdk

@Composable
fun SimpleBasicPlayerScreen() {
    val content = PlayerModel(
        id = "video_001",
        title = "Sample Video Title",
        hlsUrl = "https://sample.vod.com/master.m3u8", // Or videoUrl = "https://sample.com/video.mp4"
        imageUrl = "https://sample.vod.com/thumbnail.jpg"
    )

    MtvVideoPlayerSdk(
        contentList = listOf(content),
        modifier = Modifier.fillMaxSize()
    )
}
```

---

## 4. Programmatic Control (Optional)

You can pass a `PlayerController` to control playback programmatically from your app UI (e.g. custom play/pause or mute buttons):

```kotlin
val controller = remember { PlayerController() }

MtvVideoPlayerSdk(
    contentList = listOf(content),
    controller = controller
)

// Programmatic actions:
controller.togglePlayPause()
controller.toggleMute()
controller.play(newVideoModel)
```

---

## 5. Complete Working Activity Example

```kotlin
package com.example.basicapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.app.videosdk.listener.PlayerController
import com.app.videosdk.model.PlayerModel
import com.app.videosdk.ui.MtvVideoPlayerSdk

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val controller = remember { PlayerController() }

            val video = PlayerModel(
                id = "demo_01",
                title = "Basic Player Demo Video",
                hlsUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                imageUrl = "https://images.unsplash.com/photo-1574375927938-d5a98e8ffe85"
            )

            MtvVideoPlayerSdk(
                contentList = listOf(video),
                controller = controller,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
```

---

## 6. Troubleshooting & Verification

* **Video not playing?**
  * Ensure internet permission is declared in `AndroidManifest.xml`:
    ```xml
    <uses-permission android.permission.INTERNET />
    ```
  * Verify that your stream URL (`hlsUrl` / `videoUrl`) is accessible over HTTPS.
