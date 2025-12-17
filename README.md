# 🎬 Mtv Video Player SDK – Android

This document provides complete integration and usage guidelines for the  
**Mtv Video Player SDK**, built with **Media3** and **Jetpack Compose**.

---

## ✨ Features

- HLS & DASH playback  
- DRM (Widevine) support  
- Jetpack Compose–based UI  
- Paging support  
- Picture-in-Picture (PiP)  
- Fullscreen playback  
- Subtitles (SRT)  
- Playback speed & quality selection  

---

## 📦 Installation

### Add JitPack Repository

Add the following in your **project-level Gradle file**:

```gradle
repositories {
    maven { url "https://jitpack.io" }
}

dependencies {
    implementation "com.github.<github-username>:MPlayerSdk:1.0.0"
}


