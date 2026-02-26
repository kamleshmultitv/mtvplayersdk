package com.app.videosdk.model

import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.DownloadManager
import okhttp3.Cache

data class PlayerModel(
    val id: String? = null,
    val hlsUrl: String? = null,
    val mpdUrl: String? = null,
    val seekTo: Long? = null,
    val liveUrl: String? = null,
    val drm: String? = null,
    val drmToken: String? = null,
    val imageUrl: String? = null,
    val episodeTitle: String? = null,
    val episodeDescription: String? = null,
    val seasonTitle: String? = null,
    val seasonDescription: String? = null,
    val description: String? = null,
    val seasonNumber: String? = null,
    val episodeNumber: String? = null,
    val duration: String? = null,
    val srt: String? = null,
    val spriteUrl: String? = null,
    val playbackSpeed: Float = 1.0f,
    val selectedSubtitle: String? = null,
    val selectedVideoQuality: Int = 1080,
    val isLive: Boolean = false,
    val adsConfig: AdsConfig? = null,
    val gamAdsConfig: GAMAdsConfig? = null,
    val skipIntro: SkipIntro? = null,
    val nextEpisode: NextEpisode? = null,
    val customControls: PlayerCustomControls? = null,
    val isChapterEnabled: Boolean = false,
    val chapters: List<Chapter>? = null,
    val cacheFactory: CacheDataSource.Factory? = null,
    val downloadManager: DownloadManager? = null,
    val downloadCache: SimpleCache? = null
)