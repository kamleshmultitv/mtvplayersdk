package com.app.videosdk.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.Composition
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.drm.DefaultDrmSessionManagerProvider
import androidx.media3.exoplayer.hls.SampleQueueMappingException
import androidx.media3.exoplayer.ima.ImaAdsLoader
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.TrackGroupArray
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.trackselection.MappingTrackSelector
import androidx.media3.ui.PlayerView
import com.app.videosdk.listener.AdsListener
import com.app.videosdk.model.AdsConfig
import com.app.videosdk.model.PlayerModel
import com.app.videosdk.model.SubTitleModel
import com.app.videosdk.model.VideoQualityModel
import com.google.ads.interactivemedia.v3.api.AdEvent
import com.google.common.collect.ImmutableList
import com.google.gson.Gson
import java.io.File
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.Transformer
import kotlin.math.pow


object PlayerUtils {

    data class TextTrackOption(
        val id: String,
        val displayName: String,
        val language: String?,
        val mediaTrackGroup: TrackGroup?,
        val trackIndex: Int,
        val isOff: Boolean = false,
        val isSelected: Boolean = false
    )

    /* =========================================================
       PLAYER + IMA
       ========================================================= */
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    @OptIn(UnstableApi::class)
    fun createPlayer(
        context: Context,
        contentList: List<PlayerModel>? = null,
        selectedIndex: Int = 0,
        videoUrl: String,
        drmToken: String? = null,
        srt: String? = null,
        playerView: PlayerView? = null,
        adsConfig: AdsConfig? = null,
        adsListener: AdsListener? = null,
        existingAdsLoader: ImaAdsLoader? = null,
        playWhenReady: Boolean = true,
        fastStart: Boolean = false
    ): Pair<ExoPlayer, ImaAdsLoader?> {

        val content = contentList?.get(selectedIndex)
        val contentId = content?.id

        val download =
            content?.downloadManager
                ?.downloadIndex
                ?.getDownload(contentId.toString())

        val isOffline = download?.state == Download.STATE_COMPLETED

        Log.d(
            "PlayerUtils",
            "isOffline=$isOffline, contentId=$contentId, downloadExists=${download != null}"
        )

        val resolvedUri =
            if (isOffline) Uri.EMPTY
            else resolveToPlayableUri(contentList, selectedIndex)

        /* ================= ADS ================= */

        val adsLoader =
            existingAdsLoader
                ?: if (
                    adsConfig?.enableAds == true &&
                    !adsConfig.adTagUrl.isNullOrBlank()
                ) {

                    ImaAdsLoader.Builder(context)
                        .setAdEventListener { event ->

                            when (event.type) {

                                AdEvent.AdEventType.LOADED -> {
                                    adsListener?.onAdsLoaded()
                                }

                                AdEvent.AdEventType.STARTED ->
                                    adsListener?.onAdStarted()

                                AdEvent.AdEventType.COMPLETED ->
                                    adsListener?.onAdCompleted()

                                AdEvent.AdEventType.ALL_ADS_COMPLETED ->
                                    adsListener?.onAllAdsCompleted()

                                else -> Unit
                            }
                        }
                        .setAdErrorListener { error ->
                            adsListener?.onAdError(error.error.message)
                            Log.e("IMA", "Ad error", error.error)
                        }
                        .build()

                } else null


        /* ================= DATASOURCE ================= */

        val cache = content?.downloadCache

        val dataSourceFactory: DataSource.Factory =
            if (cache != null) {
                CacheDataSource.Factory()
                    .setCache(cache)
                    .setUpstreamDataSourceFactory(DefaultHttpDataSource.Factory())
                    .setCacheWriteDataSinkFactory(null)
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
            } else {
                DefaultHttpDataSource.Factory()
            }

        /* ================= MEDIA SOURCE ================= */

        val mediaSourceFactory =
            DefaultMediaSourceFactory(context)
                .setDataSourceFactory(dataSourceFactory)
                .apply {
                    if (!drmToken.isNullOrBlank()) {
                        setDrmSessionManagerProvider(
                            DefaultDrmSessionManagerProvider().apply {
                                setDrmHttpDataSourceFactory(
                                    DefaultHttpDataSource.Factory()
                                )
                            }
                        )
                    }

                    adsLoader?.let { loader ->
                        setAdsLoaderProvider { loader }
                        playerView?.let { view ->
                            setAdViewProvider { view }
                        }
                    }
                }

        /* ================= PLAYER ================= */

        val trackSelector = DefaultTrackSelector(context).apply {
            var parameters = buildUponParameters()
                .setForceHighestSupportedBitrate(false)

            if (fastStart) {
                parameters = parameters.setMaxVideoBitrate(FAST_START_MAX_VIDEO_BITRATE)
            }

            setParameters(parameters)
        }

        val playerBuilder = ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setTrackSelector(trackSelector)

        if (fastStart) {
            playerBuilder.setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        FAST_START_MIN_BUFFER_MS,
                        FAST_START_MAX_BUFFER_MS,
                        FAST_START_BUFFER_FOR_PLAYBACK_MS,
                        FAST_START_BUFFER_FOR_REBUFFER_MS
                    )
                    .setPrioritizeTimeOverSizeThresholds(true)
                    .build()
            )
        }

        val exoPlayer =
            playerBuilder.build()

        /* ================= ERROR LOGGING ================= */

        exoPlayer.addListener(object : Player.Listener {

            override fun onPlayerError(error: PlaybackException) {
                Log.e("PlayerUtils", "Playback error", error)

                Log.e("DRM_DEBUG", "errorCode=${error.errorCodeName}")
                Log.e("DRM_DEBUG", "message=${error.message}")
                Log.e("DRM_DEBUG", "cause=${error.cause?.javaClass?.simpleName}")

                when (error.errorCode) {
                    PlaybackException.ERROR_CODE_DRM_CONTENT_ERROR ->
                        Log.e("DRM_DEBUG", "❌ Offline DRM license NOT FOUND")

                    PlaybackException.ERROR_CODE_DRM_LICENSE_EXPIRED ->
                        Log.e("DRM_DEBUG", "❌ Offline DRM license EXPIRED")

                    PlaybackException.ERROR_CODE_DRM_PROVISIONING_FAILED ->
                        Log.e("DRM_DEBUG", "❌ Widevine provisioning failed")

                    else -> Unit
                }

                // Existing audio fallback (unchanged)
                if (
                    error.cause is SampleQueueMappingException ||
                    error.message?.contains("audio/mp4a-latm") == true
                ) {
                    Log.w("PlayerUtils", "Audio broken → falling back to video-only")

                    exoPlayer.trackSelectionParameters =
                        exoPlayer.trackSelectionParameters
                            .buildUpon()
                            .setDisabledTrackTypes(setOf(C.TRACK_TYPE_AUDIO))
                            .build()

                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = true
                }
            }
        })

        /* ================= MEDIA ITEM ================= */

        val mediaItem: MediaItem =
            if (isOffline) {
                Log.d("PlayerUtils", "Using OFFLINE MediaItem")

                val isDashOffline =
                    download.request.uri.toString()
                        .substringBefore("?")
                        .endsWith(".mpd", ignoreCase = true)

                if (isDashOffline) {
                    // Try offline DRM → fallback to online
                    buildOfflineDrmMediaItemOrNull(download)
                        ?: run {
                            Log.w(
                                "PlayerUtils",
                                "Offline DASH without license → fallback to online"
                            )
                            MediaItem.fromUri(resolvedUri)
                        }
                } else {
                    // HLS or non-DRM offline
                    download.request.toMediaItem()
                }
            } else {
                Log.d("PlayerUtils", "Using ONLINE MediaItem: $resolvedUri")

                val deepStart = content?.seekTo ?: 0L
                val deepEnd = content?.deepLinkEndMs

                MediaItem.Builder()
                    .setUri(resolvedUri)
                    .apply {

                        // ✅ THIS MAKES SEEK BAR = CLIP DURATION
                        if (deepEnd != null && deepEnd > deepStart) {
                            setClippingConfiguration(
                                MediaItem.ClippingConfiguration.Builder()
                                    .setStartPositionMs(deepStart)
                                    .setEndPositionMs(deepEnd)
                                    .build()
                            )
                        }

                        if (!drmToken.isNullOrBlank()) {
                            setDrmConfiguration(
                                MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
                                    .setLicenseUri(drmToken)
                                    .setMultiSession(true)
                                    .build()
                            )
                        }

                        if (!srt.isNullOrBlank()) {
                            setSubtitleConfigurations(
                                ImmutableList.of(initializeSubTitleTracker(srt))
                            )
                        }

                        if (
                            adsLoader != null &&
                            adsConfig?.enableAds == true &&
                            !adsConfig.adTagUrl.isNullOrBlank()
                        ) {
                            setAdsConfiguration(
                                MediaItem.AdsConfiguration.Builder(
                                    adsConfig.adTagUrl.trim().toUri()
                                ).build()
                            )
                            adsLoader.setPlayer(exoPlayer)
                        }
                    }
                    .build()
            }


        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        contentList?.get(selectedIndex)?.seekTo?.let { position ->
            exoPlayer.seekTo(position)
        }
        exoPlayer.playWhenReady = playWhenReady

        return exoPlayer to adsLoader
    }

    const val FAST_START_MAX_VIDEO_BITRATE = 450_000
    private const val FAST_START_MIN_BUFFER_MS = 500
    private const val FAST_START_MAX_BUFFER_MS = 5_000
    private const val FAST_START_BUFFER_FOR_PLAYBACK_MS = 150
    private const val FAST_START_BUFFER_FOR_REBUFFER_MS = 500

    @OptIn(UnstableApi::class)
    private fun buildOfflineDrmMediaItemOrNull(
        download: Download
    ): MediaItem? {

        val keySetId = download.request.keySetId
        if (keySetId == null) {
            Log.e(
                "DRM_DEBUG",
                "❌ Offline DASH but keySetId missing — falling back to online"
            )
            return null
        }

        return download.request
            .toMediaItem()
            .buildUpon()
            .setDrmConfiguration(
                MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
                    .setKeySetId(keySetId)
                    .build()
            )
            .build()
    }


    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    fun resolveToPlayableUri(
        contentList: List<PlayerModel>? = null,
        selectedIndex: Int = 0
    ): Uri {
        if (contentList?.isEmpty() == true) return Uri.EMPTY

        val content = contentList?.get(selectedIndex)

        val mpd = content?.mpdUrl
        val hls = content?.hlsUrl
        val live = content?.liveUrl
        val direct = content?.videoUrl

        // ✅ STRICT RULE
        // DRM → DASH ONLY
        // NON-DRM → HLS
        val primaryUrl = when {
            content?.drm == "1" && !mpd.isNullOrBlank() -> mpd   // ✅ FIX
            content?.drm != "1" && !hls.isNullOrBlank() -> hls
            !live.isNullOrBlank() -> live
            !direct.isNullOrBlank() -> direct
            else -> null
        }?.trim()

        if (primaryUrl.isNullOrBlank()) return Uri.EMPTY

        return primaryUrl.toUri()
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
        val trackSelector =
            exoPlayerInstance.trackSelector as? DefaultTrackSelector ?: return emptyList()
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

    fun getAudioTrack(
        context: Context,
        audioTracks: List<String>
    ): List<SubTitleModel> {

        val json = context.assets
            .open("hls.json")
            .bufferedReader()
            .use { it.readText() }

        val gson = Gson()

        // ✅ RELEASE-SAFE: no generics, no reflection
        val subtitleArray =
            gson.fromJson(json, Array<SubTitleModel>::class.java)
                ?: emptyArray()

        val subtitleDataList = subtitleArray.toList()

        val result = mutableListOf<SubTitleModel>()

        for (audioTrack in audioTracks) {
            for (subtitleData in subtitleDataList) {
                if (audioTrack == subtitleData.id) {
                    result.add(subtitleData)
                    break
                }
            }
        }

        return result
    }


    @OptIn(UnstableApi::class)
    fun selectAudioTrack(language: String, exoPlayer: ExoPlayer?) {
        val trackSelector = exoPlayer?.trackSelector as? DefaultTrackSelector ?: return
        val parameters = trackSelector.buildUponParameters()
            .setPreferredAudioLanguage(language)
            .build()
        trackSelector.setParameters(parameters)
    }

    fun getTextTrackOptions(exoPlayer: ExoPlayer?): List<TextTrackOption> {
        val trackOptions = mutableListOf<TextTrackOption>()
        val trackGroups = exoPlayer?.currentTracks?.groups.orEmpty()
        var hasSelectedTextTrack = false

        trackGroups
            .filter { it.type == C.TRACK_TYPE_TEXT }
            .forEachIndexed { groupIndex, group ->
                val mediaTrackGroup = group.mediaTrackGroup
                for (trackIndex in 0 until group.length) {
                    val format = group.getTrackFormat(trackIndex)
                    val language = format.language.cleanTrackText()
                    val label = format.label.cleanTrackText()
                    val displayName = label
                        ?: language
                        ?: format.id.cleanTrackText()
                        ?: "Subtitle ${trackOptions.size + 1}"
                    val isSelected = group.isTrackSelected(trackIndex)

                    if (isSelected) {
                        hasSelectedTextTrack = true
                    }

                    trackOptions.add(
                        TextTrackOption(
                            id = language
                                ?: label
                                ?: format.id.cleanTrackText()
                                ?: "text_${groupIndex}_$trackIndex",
                            displayName = displayName,
                            language = language,
                            mediaTrackGroup = mediaTrackGroup,
                            trackIndex = trackIndex,
                            isSelected = isSelected
                        )
                    )
                }
            }

        return listOf(
            TextTrackOption(
                id = "off",
                displayName = "Off",
                language = null,
                mediaTrackGroup = null,
                trackIndex = C.INDEX_UNSET,
                isOff = true,
                isSelected = !hasSelectedTextTrack
            )
        ) + trackOptions
    }

    fun selectTextTrack(option: TextTrackOption, exoPlayer: ExoPlayer?) {
        val player = exoPlayer ?: return
        val builder = player.trackSelectionParameters
            .buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)

        if (option.isOff) {
            player.trackSelectionParameters = builder
                .setPreferredTextLanguages()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
            return
        }

        builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)

        option.mediaTrackGroup?.let { group ->
            builder.setOverrideForType(TrackSelectionOverride(group, option.trackIndex))
        }

        val languageVariants = option.language.toPreferredTextLanguageVariants()
        if (languageVariants.isNotEmpty()) {
            builder.setPreferredTextLanguages(*languageVariants.toTypedArray())
        }

        player.trackSelectionParameters = builder.build()
    }

    private fun String?.cleanTrackText(): String? =
        this?.trim()?.takeIf { it.isNotEmpty() && !it.equals("und", ignoreCase = true) }

    private fun String?.toPreferredTextLanguageVariants(): List<String> {
        val language = cleanTrackText() ?: return emptyList()
        val normalized = language.replace('_', '-')
        val lower = normalized.lowercase()
        return listOf(language, normalized, lower).distinct()
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

    fun timeToMillis(
        time: String?,
        offset: String? = "0"
    ): Long {
        val durationMs = parseDurationToMillis(time)
        val offsetMs = parseDurationToMillis(offset)

        return (durationMs - offsetMs).coerceAtLeast(0L)
    }

    @JvmStatic
    fun parseDurationToMillis(input: String?): Long {
        if (input.isNullOrBlank()) return 0L

        val value = input.trim()

        // Digits only → milliseconds
        if (value.all { it.isDigit() }) {
            return value.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
        }

        val parts = value.split(":")
        val numbers = parts.map { it.toLongOrNull() ?: return 0L }

        return when (numbers.size) {
            3 -> { // HH:mm:ss
                val (h, m, s) = numbers
                (h * 3600 + m * 60 + s) * 1000
            }

            2 -> { // mm:ss
                val (m, s) = numbers
                (m * 60 + s) * 1000
            }

            else -> 0L
        }
    }

    // share clip
    @OptIn(UnstableApi::class)
    fun exportClip(
        context: Context,
        videoUri: Uri,
        clipStart: Long
    ) {

        val clipDuration = 120_000L

        val mediaItem = MediaItem.Builder()
            .setUri(videoUri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(clipStart)
                    .setEndPositionMs(clipStart + clipDuration)
                    .build()
            )
            .build()

        val editedMediaItem = EditedMediaItem.Builder(mediaItem).build()

        val transformer = Transformer.Builder(context).build()

        val outputFile = File(
            context.cacheDir,
            "clip_${System.currentTimeMillis()}.mp4"
        )

        transformer.addListener(object : Transformer.Listener {

            override fun onCompleted(
                composition: androidx.media3.transformer.Composition,
                exportResult: ExportResult
            ) {
                Log.d("Clip", "Export completed")

                // ✅ CALL SHARE HERE
                shareVideo(context, outputFile)
            }

            override fun onError(
                composition: androidx.media3.transformer.Composition,
                exportResult: ExportResult,
                exception: ExportException
            ) {
                exception.printStackTrace()
            }
        })

        transformer.start(
            editedMediaItem,
            outputFile.absolutePath
        )
    }


    fun shareVideo(context: Context, file: File) {

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(
            Intent.createChooser(intent, "Share Clip")
        )
    }

    fun createShareUrl(
        contentId: String? = null,
        url: String? = null,
        clipStart: Long,
        clipEnd: Long,
        totalClipDuration: Long
    ): String {
        return "https://www.artofliving.app/watch?url=$url&contentId=$contentId&start=$clipStart&end=$clipEnd&totalClipDuration=$totalClipDuration"
    }

    fun shareLink(
        context: Context,
        shareUrl: String
    ) {

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareUrl)
        }

        context.startActivity(
            Intent.createChooser(intent, "Share Clip")
        )
    }
}
