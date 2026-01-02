package com.app.videosdk.utils

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.drm.DefaultDrmSessionManagerProvider
import androidx.media3.exoplayer.ima.ImaAdsLoader
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.TrackGroupArray
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.trackselection.MappingTrackSelector
import androidx.media3.ui.PlayerView
import com.app.videosdk.listener.AdsListener
import com.app.videosdk.model.AdsConfig
import com.app.videosdk.model.SubTitleModel
import com.app.videosdk.model.VideoQualityModel
import com.google.ads.interactivemedia.v3.api.AdEvent
import com.google.common.collect.ImmutableList
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import kotlin.math.pow

object PlayerUtils {
    @OptIn(UnstableApi::class)
    fun createPlayer(
        context: Context,
        videoUrl: String,
        drmToken: String? = null,
        srt: String? = null,
        playerView: PlayerView? = null,
        adsConfig: AdsConfig? = null,
        adsListener: AdsListener? = null
    ): Pair<ExoPlayer, ImaAdsLoader?> {

        require(videoUrl.isNotBlank()) { "videoUrl cannot be blank" }

        val cleanUrl = videoUrl.substringBefore("?")
        val isDash = cleanUrl.endsWith(".mpd", true)

        /* =========================================================
           ADS LOADER (FIXED FOR COMPOSE)
           ========================================================= */

        val adsLoader =
            if (adsConfig?.enableAds == true && adsConfig.adTagUrl.isNotBlank()) {
                ImaAdsLoader.Builder(context)
                    .setAdEventListener { event ->
                        when (event.type) {
                            AdEvent.AdEventType.LOADED -> adsListener?.onAdsLoaded()
                            AdEvent.AdEventType.STARTED -> adsListener?.onAdStarted()
                            AdEvent.AdEventType.COMPLETED -> adsListener?.onAdCompleted()
                            AdEvent.AdEventType.ALL_ADS_COMPLETED -> adsListener?.onAllAdsCompleted()
                            else -> Unit
                        }
                    }
                    .setAdErrorListener { error ->
                        adsListener?.onAdError(error.error.message ?: "IMA error")
                        Log.e("IMA ADS", "Ad failed → content will continue")
                    }
                    .build()
            } else {
                null
            }

        /* =========================================================
           MEDIA SOURCE FACTORY
           ========================================================= */

        val mediaSourceFactory = DefaultMediaSourceFactory(context).apply {

            if (isDash && drmToken != null) {
                val drmProvider = DefaultDrmSessionManagerProvider().apply {
                    setDrmHttpDataSourceFactory(DefaultHttpDataSource.Factory())
                }
                setDrmSessionManagerProvider(drmProvider)
            }

            if (adsLoader != null) {
                setAdsLoaderProvider { adsLoader }
                if (playerView != null) {
                    setAdViewProvider { playerView }
                }
            }
        }

        /* =========================================================
           PLAYER
           ========================================================= */

        val exoPlayer =
            ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .build()
                .apply {
                    videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                    repeatMode = Player.REPEAT_MODE_OFF
                }

        /* =========================================================
           MEDIA ITEM
           ========================================================= */

        val mediaItemBuilder =
            MediaItem.Builder()
                .setUri(videoUrl.toUri())
                .setMimeType(
                    when {
                        cleanUrl.endsWith(".mpd", true) -> MimeTypes.APPLICATION_MPD
                        cleanUrl.endsWith(".m3u8", true) -> MimeTypes.APPLICATION_M3U8
                        cleanUrl.endsWith(".mp4", true) -> MimeTypes.VIDEO_MP4
                        else -> null
                    }
                )

        // DRM
        drmToken?.takeIf { isDash }?.let {
            mediaItemBuilder.setDrmConfiguration(
                MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
                    .setLicenseUri(it.toUri())
                    .build()
            )
        }

        // Subtitles
        if (!srt.isNullOrBlank()) {
            mediaItemBuilder.setSubtitleConfigurations(
                ImmutableList.of(initializeSubTitleTracker(srt))
            )
        }

        // Ads
        if (adsLoader != null && adsConfig != null) {
            mediaItemBuilder.setAdsConfiguration(
                MediaItem.AdsConfiguration.Builder(
                    Uri.parse(adsConfig.adTagUrl.trim())
                ).build()
            )
            adsLoader.setPlayer(exoPlayer)
        }

        /* =========================================================
           PREPARE
           ========================================================= */

        exoPlayer.setMediaItem(mediaItemBuilder.build())
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true

        return exoPlayer to adsLoader
    }

    private fun initializeSubTitleTracker(srt: String): MediaItem.SubtitleConfiguration {
        return MediaItem.SubtitleConfiguration.Builder(srt.toUri())
            .setMimeType(MimeTypes.APPLICATION_SUBRIP)
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()
    }

    @OptIn(UnstableApi::class)
    fun showAudioTrack(context: Context, exoPlayer: ExoPlayer?): List<SubTitleModel> {
        val exoPlayerInstance = exoPlayer ?: return emptyList()
        val trackSelector = exoPlayerInstance.trackSelector as? DefaultTrackSelector ?: return emptyList()
        val trackGroups = trackSelector.currentMappedTrackInfo?.getTrackGroups(1) ?: run {
            return emptyList()
        }
        val audioTracks = (0 until trackGroups.length).mapNotNull { index ->
            val format = trackGroups.get(index).getFormat(0)
            val label = format.language ?: format.label
            label.takeIf { it != null && it != "und" }
        }.distinct()

        return if (audioTracks.isNotEmpty()) {
            getAudioTrack(context, audioTracks)
        } else {
            emptyList()
        }
    }

    fun getAudioTrack(context: Context, audioTracks: List<String>): List<SubTitleModel> {
        val json = context.assets.open("hls.json").bufferedReader().use { it.readText() }
        val gson = Gson()
        val type = object : TypeToken<List<SubTitleModel>>() {}.type
        val subtitleDataList: List<SubTitleModel> = gson.fromJson(json, type)
        val newList = mutableListOf<SubTitleModel>()
        for (audioTrack in audioTracks) {
            for (subtitleData in subtitleDataList) {
                if (audioTrack == subtitleData.id) {
                    newList.add(subtitleData)
                    break
                }
            }
        }
        return newList
    }

    @OptIn(UnstableApi::class)
    fun selectAudioTrack(language: String, exoPlayer: ExoPlayer?) {
        val trackSelector = exoPlayer?.trackSelector as? DefaultTrackSelector ?: return
        val parameters = trackSelector.buildUponParameters()
            .setPreferredAudioLanguage(language)
            .build()
        trackSelector.setParameters(parameters)
    }

    @OptIn(UnstableApi::class)
    fun getSubTitleFormats(exoPlayer: ExoPlayer?): List<Format> {
        val subTitleFormatList = mutableListOf<Format>()
        val trackSelector = exoPlayer?.trackSelector as? DefaultTrackSelector
        val mappedTrackInfo = trackSelector?.currentMappedTrackInfo ?: return emptyList()

        val subtitleRendererIndices = (0 until mappedTrackInfo.rendererCount).filter { index ->
            videoRendererInfo(mappedTrackInfo, index, isSubtitle = true)
        }

        subtitleRendererIndices.forEach { subtitleRendererIndex ->
            val override = mappedTrackInfo.getTrackGroups(subtitleRendererIndex)
            subTitleFormatList.addAll(getVideoQualityList(override))
        }

        if (subTitleFormatList.isEmpty()) {
            exoPlayer?.trackSelectionParameters = exoPlayer?.trackSelectionParameters?.buildUpon()
                ?.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                ?.build()!!
        }

        return subTitleFormatList
    }

    @OptIn(UnstableApi::class)
    private fun videoRendererInfo(
        mappedTrackInfo: MappingTrackSelector.MappedTrackInfo,
        rendererIndex: Int,
        isSubtitle: Boolean = false
    ): Boolean {
        return mappedTrackInfo.getTrackGroups(rendererIndex).length > 0 &&
                mappedTrackInfo.getRendererType(rendererIndex) == if (isSubtitle) C.TRACK_TYPE_TEXT else C.TRACK_TYPE_VIDEO
    }

    @OptIn(UnstableApi::class)
    private fun getVideoQualityList(trackGroups: TrackGroupArray): List<Format> {
        val videoQuality = mutableListOf<Format>()
        for (groupIndex in 0 until trackGroups.length) {
            val group = trackGroups[groupIndex]
            for (trackIndex in 0 until group.length) {
                videoQuality.add(group.getFormat(trackIndex))
            }
        }
        return videoQuality
    }

    fun getMimeTypeFromExtension(videoUrl: String): Boolean {
        val cleanUrl = videoUrl.substringBefore("?")
        return getUrlExtension(cleanUrl).equals("mpd", ignoreCase = true)
    }

    private fun getUrlExtension(url: String): String {
        return url.substringAfterLast(".")
    }

    fun calculatePitch(speed: Float): Float {
        val basePitch = 1.0f
        val pitchChangeRatio = 0.05f
        return basePitch / speed.pow(pitchChangeRatio)
    }

    @OptIn(UnstableApi::class)
    fun getVideoFormats(exoPlayer: ExoPlayer?): List<VideoQualityModel> {
        val trackSelector = exoPlayer?.trackSelector as? DefaultTrackSelector
        val mappedTrackInfo = trackSelector?.currentMappedTrackInfo ?: return emptyList()

        val videoQualityList = mutableListOf(VideoQualityModel("auto", 0, 0, 0, "Auto"))

        for (index in 0 until mappedTrackInfo.rendererCount) {
            if (videoRendererInfo(mappedTrackInfo, index, false)) {
                val formats = getVideoQualityList(mappedTrackInfo.getTrackGroups(index))
                formats.forEach { format ->
                    videoQualityList.add(
                        VideoQualityModel(
                            id = format.id.toString(),
                            height = format.height,
                            width = format.width,
                            bitrate = format.bitrate,
                            title = format.height.toString()
                        )
                    )
                }
            }
        }

        return videoQualityList.sortedByDescending { it.height }
    }

    @OptIn(UnstableApi::class)
    fun setAutoVideoResolution(exoPlayer: ExoPlayer?) {
        val trackSelector = exoPlayer?.trackSelector as? DefaultTrackSelector
        trackSelector?.buildUponParameters()?.clearVideoSizeConstraints()?.let {
            trackSelector.setParameters(it)
        }
    }

    @OptIn(UnstableApi::class)
    fun changeVideoResolution(exoPlayer: ExoPlayer?, width: Int, height: Int) {
        val trackSelector = exoPlayer?.trackSelector as? DefaultTrackSelector
        trackSelector?.buildUponParameters()?.setMaxVideoSize(width, height)?.let {
            trackSelector.setParameters(it)
            Log.d("Utils", "Changed video resolution to ${width}x${height}")
        }
    }

    @SuppressLint("DefaultLocale")
    fun formatTime(milliseconds: Long): String {
        val totalSeconds = (milliseconds / 1000).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}