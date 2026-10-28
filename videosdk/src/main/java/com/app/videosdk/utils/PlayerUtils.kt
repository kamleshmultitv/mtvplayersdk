package com.app.videosdk.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.annotation.RequiresPermission
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
import java.io.File
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.Transformer
import java.util.Locale
import kotlin.math.pow


object PlayerUtils {

    data class TextTrackOption(
        val id: String,
        val displayName: String,
        val language: String?,
        val label: String? = null,
        val mediaTrackGroup: TrackGroup?,
        val trackIndex: Int,
        val isOff: Boolean = false,
        val isSelected: Boolean = false
    )

    data class AudioTrackOption(
        val id: String,
        val displayName: String,
        val language: String?,
        /** HLS NAME= / DASH Label — the human-readable name from the manifest, null if absent. */
        val name: String? = null,
        val mediaTrackGroup: TrackGroup,
        val trackIndex: Int,
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
        playWhenReady: Boolean = true
    ): Pair<ExoPlayer, ImaAdsLoader?> {
        return com.app.videosdk.player.PlayerFactory.createPlayer(
            context = context,
            contentList = contentList,
            selectedIndex = selectedIndex,
            videoUrl = videoUrl,
            drmToken = drmToken,
            srt = srt,
            playerView = playerView,
            adsConfig = adsConfig,
            adsListener = adsListener,
            existingAdsLoader = existingAdsLoader,
            playWhenReady = playWhenReady
        )
    }

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
    fun showAudioTrack(exoPlayer: ExoPlayer?): List<SubTitleModel> {
        return getAudioTrackOptions(exoPlayer).map { option ->
            SubTitleModel(id = option.id, name = option.displayName)
        }
    }

    @OptIn(UnstableApi::class)
    fun showAudioTrack(context: Context?, exoPlayer: ExoPlayer?): List<SubTitleModel> =
        showAudioTrack(exoPlayer)

    @OptIn(UnstableApi::class)
    fun getAudioTrackOptions(exoPlayer: ExoPlayer?): List<AudioTrackOption> {
        val trackOptions = mutableListOf<AudioTrackOption>()
        val audioGroups = exoPlayer?.currentTracks?.groups.orEmpty()
            .filter { it.type == C.TRACK_TYPE_AUDIO }
        val audioTrackCount = audioGroups.sumOf { group ->
            (0 until group.length).count { trackIndex ->
                group.isTrackSupported(trackIndex, true)
            }
        }

        audioGroups
            .forEachIndexed { groupIndex, group ->
                val mediaTrackGroup = group.mediaTrackGroup
                for (trackIndex in 0 until group.length) {
                    if (!group.isTrackSupported(trackIndex, true)) continue

                    val format = group.getTrackFormat(trackIndex)
                    val language = format.language.cleanTrackText()
                    // NAME= attribute from HLS #EXT-X-MEDIA (or DASH <Label>).
                    // When present (e.g. NAME="English"), it is used as both the
                    // display name and the identity key so callers can select tracks
                    // purely by NAME regardless of LANGUAGE= value.
                    val nameFromManifest = format.label.cleanTrackText()
                    val id = format.id.cleanTrackText()?.takeUnless { it.isNumericTrackId() }

                    // Display name priority:
                    //   1. NAME= from manifest  (e.g. "English", "Hindi") — human-readable, use directly
                    //   2. LANGUAGE= via Locale  (e.g. "en-IN" → "English (India)")
                    //   3. Track ID
                    //   4. Generic fallback
                    val displayName = audioTrackDisplayName(
                        language = language,
                        label = nameFromManifest,   // ← NAME passed as label now
                        id = id,
                        fallbackIndex = trackOptions.size + 1,
                        totalTrackCount = audioTrackCount
                    )

                    // Identity key priority: NAME (manifest) → language → track-id → positional fallback
                    val trackId = nameFromManifest
                        ?: language
                        ?: id
                        ?: "audio_${groupIndex}_$trackIndex"

                    trackOptions.add(
                        AudioTrackOption(
                            id = trackId,
                            displayName = displayName,
                            language = language,
                            name = nameFromManifest,
                            mediaTrackGroup = mediaTrackGroup,
                            trackIndex = trackIndex,
                            isSelected = group.isTrackSelected(trackIndex)
                        )
                    )
                }
            }

        val options = trackOptions.withUniqueAudioDisplayNames()
        options.forEach { option ->
            SdkLogger.info(
                "[Manifest Audio Track] Name: '${option.name.orEmpty()}', " +
                "Language: '${option.language.orEmpty()}', " +
                "ID: '${option.id}', " +
                "DisplayName: '${option.displayName}', " +
                "Selected: ${option.isSelected}"
            )
        }
        return options
    }

    @OptIn(UnstableApi::class)
    fun getAudioTrackOptions(context: Context?, exoPlayer: ExoPlayer?): List<AudioTrackOption> =
        getAudioTrackOptions(exoPlayer)

    fun getAudioTrack(audioTracks: List<String>): List<SubTitleModel> {
        return audioTracks.mapNotNull { code ->
            val clean = code.cleanTrackText() ?: return@mapNotNull null
            val displayName = resolveLanguageDisplayName(clean)
            SubTitleModel(id = clean, name = displayName.ifEmpty { clean })
        }
    }

    fun getAudioTrack(context: Context?, audioTracks: List<String>): List<SubTitleModel> =
        getAudioTrack(audioTracks)


    @OptIn(UnstableApi::class)
    fun selectAudioTrack(language: String, exoPlayer: ExoPlayer?) {
        val trackSelector = exoPlayer?.trackSelector as? DefaultTrackSelector ?: return
        val languageVariants = language.toPreferredAudioLanguageVariants()
        val parameters = trackSelector
            .buildUponParameters()
            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
            .apply {
                if (languageVariants.isNotEmpty()) {
                    setPreferredAudioLanguages(*languageVariants.toTypedArray())
                } else {
                    setPreferredAudioLanguage(language)
                }
            }
            .build()
        trackSelector.setParameters(parameters)
    }

    @OptIn(UnstableApi::class)
    fun selectAudioTrack(option: AudioTrackOption, exoPlayer: ExoPlayer?) {
        val player = exoPlayer ?: return
        val builder = player.trackSelectionParameters
            .buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
            .setOverrideForType(TrackSelectionOverride(option.mediaTrackGroup, option.trackIndex))

        val languageVariants = option.language.toPreferredAudioLanguageVariants()
        if (languageVariants.isNotEmpty()) {
            builder.setPreferredAudioLanguages(*languageVariants.toTypedArray())
        } else {
            builder.setPreferredAudioLanguages()
        }

        player.trackSelectionParameters = builder.build()
    }

    /**
     * Select an audio track by its NAME attribute from the HLS manifest
     * (e.g. `#EXT-X-MEDIA:NAME="Hindi"`), or fall back to matching by
     * language code or track id.
     *
     * Matching priority:
     *  1. Exact match on [AudioTrackOption.name]  (HLS NAME= / DASH Label)
     *  2. Case-insensitive match on [AudioTrackOption.name]
     *  3. Exact match on [AudioTrackOption.id]    (language code or track-id)
     *  4. Case-insensitive match on [AudioTrackOption.id]
     *
     * Once a matching [AudioTrackOption] is found, selection is done via
     * [TrackSelectionOverride] (group + index), which guarantees the exact
     * track is chosen regardless of language-code variants.
     */
    @OptIn(UnstableApi::class)
    fun selectAudioTrackByName(name: String, exoPlayer: ExoPlayer?) {
        val options = getAudioTrackOptions(exoPlayer)
        val match = options.firstOrNull { it.name == name }
            ?: options.firstOrNull { it.displayName.equals(name, ignoreCase = true) }
            ?: options.firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?: options.firstOrNull { it.language.equals(name, ignoreCase = true) }
            ?: options.firstOrNull { it.id == name }
            ?: options.firstOrNull { it.id.equals(name, ignoreCase = true) }
            ?: return  // no matching track found

        selectAudioTrack(match, exoPlayer)
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
                        ?: language?.let { code ->
                            val resolved = resolveLanguageDisplayName(code)
                            if (resolved.isNotEmpty()) resolved else code
                        }
                        ?: format.id.cleanTrackText()
                        ?: "Subtitle ${trackOptions.size + 1}"
                    val isSelected = group.isTrackSelected(trackIndex)

                    if (isSelected) {
                        hasSelectedTextTrack = true
                    }

                    trackOptions.add(
                        TextTrackOption(
                            id = label
                                ?: language
                                ?: format.id.cleanTrackText()
                                ?: "text_${groupIndex}_$trackIndex",
                            displayName = displayName,
                            language = language,
                            label = label,
                            mediaTrackGroup = mediaTrackGroup,
                            trackIndex = trackIndex,
                            isSelected = isSelected
                        )
                    )
                }
            }

        val options = listOf(
            TextTrackOption(
                id = "off",
                displayName = "Off",
                language = null,
                label = null,
                mediaTrackGroup = null,
                trackIndex = C.INDEX_UNSET,
                isOff = true,
                isSelected = !hasSelectedTextTrack
            )
        ) + trackOptions

        options.forEach { option ->
            SdkLogger.info(
                "[Manifest Text Track / Closed Caption] Label/Name: '${option.label.orEmpty()}', " +
                "Language: '${option.language.orEmpty()}', " +
                "ID: '${option.id}', " +
                "DisplayName: '${option.displayName}', " +
                "Selected: ${option.isSelected}"
            )
        }
        return options
    }

    @OptIn(UnstableApi::class)
    fun selectTextTrackByName(name: String, exoPlayer: ExoPlayer?) {
        val options = getTextTrackOptions(exoPlayer)
        val match = options.firstOrNull { it.label == name }
            ?: options.firstOrNull { it.displayName.equals(name, ignoreCase = true) }
            ?: options.firstOrNull { it.label.equals(name, ignoreCase = true) }
            ?: options.firstOrNull { it.language.equals(name, ignoreCase = true) }
            ?: options.firstOrNull { it.id == name }
            ?: options.firstOrNull { it.id.equals(name, ignoreCase = true) }
            ?: return

        selectTextTrack(match, exoPlayer)
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

    private fun String?.toPreferredAudioLanguageVariants(): List<String> {
        val language = cleanTrackText() ?: return emptyList()
        val normalized = language.replace('_', '-')
        val lower = normalized.lowercase(Locale.US)
        return listOf(language, normalized, lower).distinct()
    }

    internal fun audioTrackDisplayName(
        language: String?,
        label: String?,
        id: String?,
        fallbackIndex: Int,
        totalTrackCount: Int
    ): String {
        // 1. NAME= from manifest (e.g. HLS NAME="English" / DASH <Label>English</Label>).
        label?.let { return it }

        // 2. Language code from manifest (e.g. HLS LANGUAGE="en-IN" or DASH lang="hin").
        //    Resolved to a human-readable name via Android Locale APIs.
        language?.let { code ->
            val resolvedName = resolveLanguageDisplayName(code)
            if (resolvedName.isNotEmpty()) {
                return resolvedName
            }
        }

        // 3. Track ID from manifest (non-numeric)
        id?.let { return it }

        // 4. Fallback when manifest carries no usable identifier
        return if (totalTrackCount <= 1) {
            "Default Audio"
        } else {
            "Audio $fallbackIndex"
        }
    }

    fun resolveLanguageDisplayName(code: String): String {
        val clean = code.trim().replace('_', '-')
        if (clean.isEmpty() || clean.equals("und", ignoreCase = true)) return ""

        val primary = clean.substringBefore("-").lowercase(Locale.US)

        val localeByTag = runCatching { Locale.forLanguageTag(clean) }.getOrNull()
        val displayNameByTag = localeByTag?.getDisplayLanguage(Locale.getDefault())?.cleanTrackText()
        if (!displayNameByTag.isNullOrBlank() &&
            !displayNameByTag.equals(clean, ignoreCase = true) &&
            !displayNameByTag.equals(primary, ignoreCase = true)
        ) {
            return displayNameByTag.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
        }

        val localeByCode = runCatching {
            Locale.Builder().setLanguage(primary).build()
        }.getOrNull() ?: runCatching { Locale.forLanguageTag(primary) }.getOrNull()

        val displayNameByCode = localeByCode?.getDisplayLanguage(Locale.getDefault())?.cleanTrackText()
        if (!displayNameByCode.isNullOrBlank() &&
            !displayNameByCode.equals(clean, ignoreCase = true) &&
            !displayNameByCode.equals(primary, ignoreCase = true)
        ) {
            return displayNameByCode.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
        }

        val displayNameInEnglish = localeByCode?.getDisplayLanguage(Locale.ENGLISH)?.cleanTrackText()
        if (!displayNameInEnglish.isNullOrBlank() &&
            !displayNameInEnglish.equals(clean, ignoreCase = true) &&
            !displayNameInEnglish.equals(primary, ignoreCase = true)
        ) {
            return displayNameInEnglish.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString()
            }
        }

        return clean.uppercase(Locale.US)
    }

    private fun String.isNumericTrackId(): Boolean = all(Char::isDigit)

    private fun List<AudioTrackOption>.withUniqueAudioDisplayNames(): List<AudioTrackOption> {
        val totalByName = groupingBy { it.displayName }.eachCount()
        val seenByName = mutableMapOf<String, Int>()

        return map { option ->
            if ((totalByName[option.displayName] ?: 0) <= 1) {
                option
            } else {
                val next = (seenByName[option.displayName] ?: 0) + 1
                seenByName[option.displayName] = next
                option.copy(displayName = "${option.displayName} $next")
            }
        }
    }

    private fun String?.toPreferredTextLanguageVariants(): List<String> {
        val language = cleanTrackText() ?: return emptyList()
        val normalized = language.replace('_', '-')
        val lower = normalized.lowercase(Locale.US)
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

        val videoQualityList = mutableListOf<VideoQualityModel>()

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

        val options = listOf(VideoQualityModel("auto", 0, 0, 0, "Auto")) +
            videoQualityList.sortedByDescending { it.height }

        options.forEach { quality ->
            SdkLogger.info(
                "[Manifest Video Quality] Title: '${quality.title}', " +
                "Width: ${quality.width}, " +
                "Height: ${quality.height}, " +
                "Bitrate: ${quality.bitrate}, " +
                "ID: '${quality.id}'"
            )
        }
        return options
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
            Log.d("Utils", "Changed video resolution to ${width}x$height")
        }
    }

    @SuppressLint("DefaultLocale")
    fun formatTime(milliseconds: Long, forceHours: Boolean = false): String {
        val totalSeconds = (milliseconds / 1000).toInt().coerceAtLeast(0)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0 || forceHours) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
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
