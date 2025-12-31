package com.app.videosdk.model

data class PlayerModel(
    val hlsUrl: String? = null,
    val mpdUrl: String? = null,
    val liveUrl: String? = null, 
    val drmToken: String? = null,
    val imageUrl: String? = null,
    val title: String? = null,
    val description: String? = null,
    val season_title: String? = null,
    val season_des: String? = null,
    val srt: String? = null,
    val spriteUrl: String? = null,
    val playbackSpeed: Float = 1.0f,  // Default to normal speed
    val selectedSubtitle: String? = null,  // Subtitle language code (e.g., "en")
    val selectedVideoQuality: Int = 1080,  // Default video quality
    val isLive: Boolean = false
)