package com.app.sample.utils

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.paging.compose.LazyPagingItems
import com.app.mtvdownloader.DownloadUtil
import com.app.mtvdownloader.local.entity.DownloadedContentEntity
import com.app.mtvdownloader.model.DownloadModel
import com.app.mtvdownloader.utils.Constants.DRM_SCHEME_WIDEVINE
import com.app.sample.BuildConfig.DRM_LICENSE_URL
import com.app.sample.R
import com.app.sample.extra.ApiConstant.DRM_AUTHORIZATION_SOURCE
import com.app.sample.extra.ApiConstant.DRM_PACKAGE_ID
import com.app.sample.extra.ApiConstant.DRM_TYPE
import com.app.sample.extra.ApiConstant.DRM_USER_ID
import com.app.sample.extra.ApiConstant.PAID
import com.app.sample.extra.ApiConstant.TOKEN
import com.app.sample.model.ContentItem
import com.app.sample.model.DeepLinkResponse
import com.app.sample.model.OverrideContent
import com.app.videosdk.model.AdsConfig
import com.app.videosdk.model.Chapter
import com.app.videosdk.model.GAMAdsConfig
import com.app.videosdk.model.NextEpisode
import com.app.videosdk.model.PlayerCustomControls
import com.app.videosdk.model.PlayerModel
import com.app.videosdk.model.SkipIntro
import org.json.JSONObject

object FileUtils {
    private const val DRM_TAG = "SampleDrmLicense"
    private const val DEMO_AGE_RATING = "U/A 13+"

    /* ---------------------------------- */
    /* DRM TOKEN                           */
    /* ---------------------------------- */

    fun getSecondFromDays(downloadDays: String?): Int {
        return if (downloadDays != null && !TextUtils.isEmpty(downloadDays) && !downloadDays.equals(
                "0",
                ignoreCase = true
            )
        ) {
            downloadDays.toInt() * 24 * 60 * 60
        } else {
            0
        }
    }

    private fun getDrmTokenOrNull(context: Context, contentItems: ContentItem?): String? {
        if (contentItems?.drm != "1") return null
        if (contentItems.id.isNullOrBlank() || contentItems.kId.isNullOrBlank()) return null

        var accessType = contentItems.accessType
        accessType = if (accessType.equals(PAID)) "1"
        else "0"
        val downloadExpiry = if (getSecondFromDays(contentItems.downloadExpiry) == 0) {
            getSecondFromDays("30")
        } else {
            getSecondFromDays(contentItems.downloadExpiry)
        }

        val jsonObject = JSONObject()
        jsonObject.put("content_id", "" + contentItems.id)
        jsonObject.put("k_id", "" + contentItems.kId)
        jsonObject.put("user_id", DRM_USER_ID)
        jsonObject.put("package_id", DRM_PACKAGE_ID)
        jsonObject.put("licence_duration", "" + downloadExpiry)
        jsonObject.put("security_level", "0")
        jsonObject.put("rental_duration", "0")
        jsonObject.put("content_type", accessType)
        jsonObject.put("download", "1")
        jsonObject.put("can_renew", true)
        jsonObject.put("allow_persistent_license", true)
        val androidDeviceUniqueId = GUIDGenerator.generateGUID(context)
        val payload = ApiEncryptionHelper.convertStringToBase64(jsonObject.toString())
        val drmToken =
            DRM_LICENSE_URL.withQuerySeparator() +
                "user_id=" + androidDeviceUniqueId +
                "&type=" + DRM_TYPE +
                "&authorization=" + resolveAuthorizationToken() +
                "&payload=" + payload

        logDrmLicenseUrlBuilt(
            drmToken = drmToken,
            contentId = contentItems.id,
            kidPresent = true
        )

        return drmToken
    }

    private fun String.withQuerySeparator(): String =
        when {
            endsWith("?") || endsWith("&") -> this
            contains("?") -> "$this&"
            else -> "$this?"
        }

    private fun resolveAuthorizationToken(): String =
        when (DRM_AUTHORIZATION_SOURCE.lowercase()) {
            "jwt" -> TOKEN.jwtCandidate()
            "claim_token" -> TOKEN.jwtCandidate().claimTokenFromJwt() ?: TOKEN.jwtCandidate()
            "stored" -> TOKEN
            else -> TOKEN
        }

    private fun String.jwtCandidate(): String {
        val token = trim()
        if (token.isJwt()) return token
        return token.decodeBase64OrNull()?.takeIf { it.isJwt() } ?: token
    }

    private fun String.claimTokenFromJwt(): String? {
        val payload = split(".").getOrNull(1)?.decodeBase64OrNull() ?: return null
        return runCatching {
            JSONObject(payload).optString("token").takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun String.isJwt(): Boolean =
        split(".").size == 3

    private fun String.decodeBase64OrNull(): String? {
        val token = trim()
        val flags = listOf(
            Base64.DEFAULT,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )

        return flags.firstNotNullOfOrNull { flag ->
            runCatching {
                String(Base64.decode(token, flag), Charsets.UTF_8)
            }.getOrNull()
        }
    }

    private fun logDrmLicenseUrlBuilt(
        drmToken: String,
        contentId: String?,
        kidPresent: Boolean
    ) {
        val uri = Uri.parse(drmToken)
        Log.d(
            DRM_TAG,
            "DRM_LICENSE_URL_BUILT tokenSource=$DRM_AUTHORIZATION_SOURCE " +
                "payloadSource=content packageSource=constant contentId=$contentId " +
                "kidPresent=$kidPresent host=${uri.host} path=${uri.path} " +
                "queryKeys=${uri.queryParameterNames.joinToString()}"
        )
    }

    /* ---------------------------------- */
    /* PLAYER MODEL BUILDER                */
    /* ---------------------------------- */

    @OptIn(UnstableApi::class)
    fun buildPlayerContentList(
        context: Context,
        pagingItems: LazyPagingItems<ContentItem>,
        overrideContent: OverrideContent?,
        deepLinkContent: DeepLinkResponse?,
        contentItem: ContentItem? = null
    ): List<PlayerModel> {

        /* =========================================================
           CASE 1 & 2 : SUBMIT (overrideContent has highest priority)
           ========================================================= */

        overrideContent?.let { override ->

            // ---------- CASE 1: Submit WITHOUT URL ----------
            if (override.url.isNullOrBlank()) {

                return pagingItems.itemSnapshotList.items.mapNotNull { content ->
                    createPlayerModelFromContent(
                        context = context,
                        content = content,
                        override = override
                    )
                }
            }

            // ---------- CASE 2: Submit WITH URL ----------
            return listOf(
                PlayerModel(
                    hlsUrl = if (!override.isLive) override.url else null,
                    liveUrl = if (override.isLive) override.url else null,
                    mpdUrl = override.url,
                    drmToken = override.drmToken.orEmpty(),
                    isLive = override.isLive,
                    ageRating = DEMO_AGE_RATING,
                    adsConfig = override.adsConfig ?: AdsConfig(enableAds = false),
                    skipIntro = override.skipIntro ?: SkipIntro(enableSkipIntro = false),
                    nextEpisode = override.nextEpisode ?: NextEpisode(enableNextEpisode = false),
                    customControls = defaultControls()
                )
            )
        }

        deepLinkContent?.let { deeplink ->
            // ---------- CASE 2: Submit WITH URL ----------
            return listOf(
                PlayerModel(
                    hlsUrl =  deeplink.url ,
                    mpdUrl = deeplink.url,
                    id = deeplink.contentId,
                    seekTo = deeplink.seekTo,
                    ageRating = DEMO_AGE_RATING,
                    deepLinkEndMs = deeplink.clipEndTime,
                    deepLinkClipDuration = deeplink.totalClipDuration,
                    customControls = defaultControls()
                )
            )
        }

        /* =========================================================
           CASE 3 : Single contentItem (NEW CASE)
           ========================================================= */

        contentItem?.let { content ->
            createPlayerModelFromContent(
                context = context,
                content = content,
                override = null
            )?.let {
                return listOf(it)
            }
        }

        /* =========================================================
           CASE 4 : Pure Paging API Data (default behavior)
           ========================================================= */

        return pagingItems.itemSnapshotList.items.mapNotNull { content ->
            createPlayerModelFromContent(
                context = context,
                content = content,
                override = null
            )
        }
    }

    private fun createPlayerModelFromContent(
        context: Context,
        content: ContentItem,
        override: OverrideContent?
    ): PlayerModel? {

        val hls = content.hlsUrl?.takeIf { it.isNotBlank() }
        val mpd = content.url?.takeIf { it.isNotBlank() }
        val direct = content.source.asDirectVideoUrl()
        if (hls == null && mpd == null && direct == null) return null

        return PlayerModel(
            id = content.id.orEmpty(),
            ageRating = content.ageRating?.takeIf { it.isNotBlank() } ?: DEMO_AGE_RATING,
            hlsUrl = hls,
            mpdUrl = mpd,
            videoUrl = direct,
            liveUrl = null,
            isLive = false,

            drm = null,
            drmToken = getDrmTokenOrNull(context, content),

            imageUrl = content.layoutThumbs
                ?.firstOrNull()
                ?.imageSize
                ?.firstOrNull()
                ?.url.orEmpty(),

            episodeTitle = content.title.orEmpty(),
            episodeDescription = content.seriesDes.orEmpty(),
            seasonTitle = content.seasonTitle.orEmpty(),
            seasonDescription = content.seasonDes.orEmpty(),
            description = content.des.orEmpty(),
            seasonNumber = content.seasonNumber.orEmpty(),
            episodeNumber = content.episodeNumber.orEmpty(),
            duration = content.duration.orEmpty(),
            srt = content.subtitle?.firstOrNull()?.srt.orEmpty(),

            adsConfig = override?.adsConfig ?: defaultAdsConfig(),
            gamAdsConfig = defaultGamConfig(),
            skipIntro = override?.skipIntro ?: defaultSkipIntro(),
            nextEpisode = override?.nextEpisode ?: defaultNextEpisode(),

            cacheFactory = null,
            isClipEnabled = false,
            isChapterEnabled = false,
            chapters = defaultChapters(),

            customControls = defaultControls()
        )
    }

    private fun defaultAdsConfig() = AdsConfig(
        adTagUrl = "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/vmap_ad_samples&sz=640x480&cust_params=sample_ar%3Dpremidpost&ciu_szs=300x250&gdfp_req=1&ad_rule=1&output=vmap&unviewed_position_start=1&env=vp&cmsid=496&vid=short_onecue&correlator=",
        enableAds = false
    )

    private fun defaultGamConfig() = GAMAdsConfig(
        verticalBan = "ca-app-pub-3940256099942544/6300978111",
        horizontalBan = "/21775744923/example/fixed-size-banner",
        timeIntervalInMilliseconds = 600000,
        isAdsEnabled = false
    )

    private fun defaultSkipIntro() = SkipIntro(
        startTime = 5000L,
        endTime = 95000L,
        enableSkipIntro = true
    )

    private fun defaultNextEpisode() = NextEpisode(
        showBeforeEndMs = "160000",
        enableNextEpisode = true
    )

    private fun defaultChapters() = listOf(
        Chapter("intro", "Intro", 0L),
        Chapter("main", "Main Content", 186000L),
        Chapter("end", "Special Thanks", 2004000L)
    )

    private fun defaultControls() = PlayerCustomControls(
        iconTintRes = com.app.mtvdownloader.R.color.purple_700,
        playIconRes = R.drawable.ic_play,
        pauseIconRes = R.drawable.ic_pause,
        forwardIconRes = R.drawable.ic_forward,
        rewindIconRes = R.drawable.ic_rewined,
        backIconRes = R.drawable.ic_back_arrow,
        settingsIconRes = R.drawable.ic_settings,
        pipIconRes = R.drawable.ic_pip,
        fullScreenIconRes = R.drawable.ic_collapse,
        exitFullScreenIconRes = R.drawable.ic_expand,
        lockIconRes = R.drawable.ic_lock,
        unlockIconRes = R.drawable.ic_unlock,
        muteIconRes = R.drawable.ic_mute,
        unMuteIconRes = R.drawable.ic_unmute,
        crossFadeIconRes = R.drawable.ic_cross,
        seasonSelectorIconRes = R.drawable.ic_episode,
        brightnessIconRes = R.drawable.ic_brightness,
        nextEpisodeIconRes = R.drawable.ic_next_episode,
        castIconRes = R.drawable.ic_cast,
        castConnectedIconRes = R.drawable.ic_cast_connected
    )

    @OptIn(UnstableApi::class)
    fun buildContentListFromDownloaded(
        downloadedContentEntity: DownloadedContentEntity,
        context: Context
    ): List<PlayerModel> {
        val contentUrl = downloadedContentEntity.contentUrl.takeIf { it.isNotBlank() }
        val contentMimeType = downloadedContentEntity.contentMimeType.orEmpty()
        val hlsUrl = contentUrl?.takeIf {
            contentMimeType == "application/x-mpegURL" ||
                it.endsWith(".m3u8", ignoreCase = true)
        }
        val mpdUrl = contentUrl?.takeIf {
            contentMimeType == "application/dash+xml" ||
                it.endsWith(".mpd", ignoreCase = true)
        }
        val videoUrl = contentUrl?.takeIf {
            contentMimeType == "video/mp4" ||
                it.endsWith(".mp4", ignoreCase = true) ||
                it.endsWith(".m4v", ignoreCase = true)
        }

        val offlineKeySetIdBase64 =
            downloadedContentEntity.drmOfflineKeySetIdBase64?.takeIf { it.isNotBlank() }
        val isDrm = !downloadedContentEntity.licenseUri.isNullOrBlank() ||
            !offlineKeySetIdBase64.isNullOrBlank() ||
            downloadedContentEntity.drmOfflineKeySetId?.isNotEmpty() == true

        return listOf(
            PlayerModel(
                id = downloadedContentEntity.contentId,
                // ▶️ Playback URL
                hlsUrl = hlsUrl,
                mpdUrl = mpdUrl,
                videoUrl = videoUrl,

                // 🔐 DRM
                drm = if (isDrm) "1" else null,
                drmToken = downloadedContentEntity.licenseUri.takeIf { isDrm },
                drmOfflineKeySetId = downloadedContentEntity.drmOfflineKeySetId,
                drmOfflineKeySetIdBase64 = offlineKeySetIdBase64,

                // 🖼️ Artwork
                imageUrl = downloadedContentEntity.thumbnailUrl
                    ?: downloadedContentEntity.seasonImage,

                // 📝 Metadata
                episodeTitle = downloadedContentEntity.title.orEmpty(),
                title = downloadedContentEntity.title,
                seasonTitle = downloadedContentEntity.seasonName,
                ageRating = DEMO_AGE_RATING,

                // 🎞️ Quality preference (fallback to 1080)
                selectedVideoQuality = downloadedContentEntity.videoHeight ?: 1080,

                // 📡 Downloaded content is NOT live
                isLive = false,
                downloadManager = DownloadUtil.getDownloadManager(context),
                downloadCache = DownloadUtil.getDownloadCache(context),
                customControls = defaultControls()
            )
        )
    }

    fun buildDownloadContentList(
        context: Context,
        contentItem: ContentItem?
    ): DownloadModel? {

        if (contentItem == null) return null

        val hlsUrl = contentItem.hlsUrl?.takeIf { it.isNotBlank() }
        val mpdUrl = contentItem.url?.takeIf { it.isNotBlank() }
        val directVideoUrl = contentItem.source.asDirectVideoUrl()

        // Skip if no playable URL is available
        if (hlsUrl == null && mpdUrl == null && directVideoUrl == null) return null

        return DownloadModel(
            id = contentItem.id.orEmpty(),
            seasonId = contentItem.seasonId.orEmpty(),
            hlsUrl = hlsUrl,
            mpdUrl = mpdUrl,
            drm = contentItem.drm.takeIf { it == "1" },
            drmToken = getDrmTokenOrNull(context, contentItem),
            mp4Url = directVideoUrl,
            drmLicenseExpiresAt = contentItem.drmLicenseExpiresAtOrNull(),
            drmKeyId = contentItem.kId,
            drmScheme = DRM_SCHEME_WIDEVINE,
            imageUrl = contentItem.layoutThumbs
                ?.firstOrNull()
                ?.imageSize
                ?.firstOrNull()
                ?.url
                .orEmpty(),

            title = contentItem.title.orEmpty(),
            description = contentItem.shortDesc.orEmpty(),
            srt = contentItem.subtitle
                ?.firstOrNull()
                ?.srt
                .orEmpty()
        )
    }

    private fun ContentItem.drmLicenseExpiresAtOrNull(): Long? {
        if (drm != "1") return null

        val durationSec = getSecondFromDays(downloadExpiry)
            .takeIf { it > 0 }
            ?: getSecondFromDays("30")

        return System.currentTimeMillis() + durationSec * 1000L
    }

    private fun String?.asDirectVideoUrl(): String? {
        val value = this?.trim()?.takeIf { it.isNotBlank() } ?: return null
        if (!value.startsWith("http", ignoreCase = true)) return null
        if (value.contains(".m3u8", ignoreCase = true)) return null
        if (value.contains(".mpd", ignoreCase = true)) return null
        return value
    }
}
