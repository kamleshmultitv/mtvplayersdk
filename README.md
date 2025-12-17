🎬 Mtv Video Player SDK – Android

This document provides complete integration and usage guidelines for the
Mtv Video Player SDK, built with Media3 and Jetpack Compose.

✨ Features

HLS & DASH playback

DRM (Widevine) support

Jetpack Compose–based UI

Paging support

- HLS & DASH playback  
- DRM (Widevine) support  
- Jetpack Compose–based UI  
- Paging support  
- Picture-in-Picture (PiP)  
- Fullscreen playback  
- Subtitles (SRT)  
- Playback speed & quality selection  

Fullscreen playback

Subtitles (SRT)

Playback speed & quality selection

📦 Installation
Add JitPack Repository

Add the following in your project-level Gradle file:

repositories {
    maven { url "https://jitpack.io" }
}

Add SDK Dependency
dependencies {
    implementation "com.github.<github-username>:MPlayerSdk:1.0.0"
}


Replace <github-username> with the actual GitHub username and use the latest version.

🔧 Requirements

Min SDK: 21

Target SDK: 34

Kotlin: 1.9+

UI Framework: Jetpack Compose

🚀 Usage

Use the MtvVideoPlayerSdk composable with a list of PlayerModel objects to start video playback inside your Compose screen.

MtvVideoPlayerSdk(
    contentList = contentList,
    index = selectedIndex,
    pipListener = pipListener,
    onPlayerBack = { },
    setFullScreen = { }
)

🧩 PlayerModel

Each video item must be mapped to a PlayerModel.

PlayerModel(
    hlsUrl = "https://example.com/video.m3u8",
    mpdUrl = "https://example.com/video.mpd",
    drmToken = "DRM_TOKEN",
    imageUrl = "https://example.com/thumbnail.jpg",
    title = "Sample Video",
    description = "Sample Description",
    srt = "https://example.com/subtitle.srt",
    playbackSpeed = 1.0f,
    selectedSubtitle = null,
    selectedVideoQuality = 1080
)

📜 Paging, PiP & Fullscreen

Paging 3 is supported for large content feeds

PiP (Picture-in-Picture) state is exposed using PipListener

Fullscreen changes are notified via callback

🤝 Support

This SDK is private and intended only for authorized partners.

For integration support or feature requests, please contact your SDK provider.
