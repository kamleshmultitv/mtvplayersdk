package com.app.videosdk.utils

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.drm.DefaultDrmSessionManagerProvider
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.TrackGroupArray
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.trackselection.MappingTrackSelector
import com.app.videosdk.model.SubTitleModel
import com.app.videosdk.model.VideoQualityModel
import com.google.common.collect.ImmutableList
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import kotlin.math.pow

object PlayerUtils {
    @OptIn(UnstableApi::class)
    fun createExoPlayer(
        context: Context,
        videoUrl: String,
        drmToken: String?,
        srt: String,
        isLive: Boolean = false // Support for Live Stream configuration
    ): ExoPlayer {
        val cleanUrl = videoUrl.substringBefore("?")
        val isDash = cleanUrl.endsWith(".mpd", ignoreCase = true)
        val isMp4 = cleanUrl.endsWith(".mp4", ignoreCase = true)

        val mediaSourceFactory = if (isDash && drmToken != null) {
            DefaultMediaSourceFactory(context)
                .setDrmSessionManagerProvider(DefaultDrmSessionManagerProvider())
        } else {
            DefaultMediaSourceFactory(context)
        }

        val exoPlayer = ExoPlayer.Builder(context, mediaSourceFactory).build()

        val mediaItemBuilder = MediaItem.Builder().setUri(videoUrl)

        // Set Live Configuration if requested
        if (isLive) {
            mediaItemBuilder.setLiveConfiguration(
                MediaItem.LiveConfiguration.Builder()
                    .setTargetOffsetMs(5000) // Lower latency for live
                    .build()
            )
        }

        mediaItemBuilder.setMimeType(
            when {
                isDash -> MimeTypes.APPLICATION_MPD
                cleanUrl.endsWith(".m3u8", ignoreCase = true) -> MimeTypes.APPLICATION_M3U8
                isMp4 -> MimeTypes.VIDEO_MP4
                else -> null
            }
        )

        drmToken?.takeIf { isDash }?.let {
            mediaItemBuilder.setDrmConfiguration(
                MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
                    .setLicenseUri(it)
                    .build()
            )
        }

        if (srt.isNotBlank()) {
            val subtitleConfig = initializeSubTitleTracker(srt)
            mediaItemBuilder.setSubtitleConfigurations(ImmutableList.of(subtitleConfig))
        }

        exoPlayer.apply {
            setMediaItem(mediaItemBuilder.build())
            prepare()
            playWhenReady = true
            videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            // For live, repeat mode should be off
            repeatMode = if (isLive) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ONE
        }

        return exoPlayer
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