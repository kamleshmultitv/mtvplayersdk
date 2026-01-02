package com.app.videosdk.model

import com.app.videosdk.ui.CuePoint

data class PlayerModel(
    val hlsUrl: String? = null,
    val mpdUrl: String? = null,
    val liveUrl: String? = null,
    val drmToken: String? = null,
    val imageUrl: String? = null,
    val title: String? = null,
    val description: String? = null,
    val seasonTitle: String? = null,
    val seasonDescription: String? = null,
    val srt: String? = null,
    val spriteUrl: String? = null,
    val playbackSpeed: Float = 1.0f,
    val selectedSubtitle: String? = null,
    val selectedVideoQuality: Int = 1080,
    val isLive: Boolean = false,
    val adsConfig: AdsConfig? = null,
    val cuePoints: List<CuePoint> = emptyList()
)