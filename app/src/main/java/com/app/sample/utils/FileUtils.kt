package com.app.sample.utils

import android.content.Context
import androidx.paging.compose.LazyPagingItems
import com.app.sample.BuildConfig.DRM_LICENSE_URL
import com.app.sample.extra.ApiConstant.DRM_TYPE
import com.app.sample.extra.ApiConstant.PAID
import com.app.sample.extra.ApiConstant.TOKEN
import com.app.sample.model.ContentItem
import com.app.sample.model.OverrideContent
import com.app.videosdk.model.AdsConfig
import com.app.videosdk.model.NextEpisode
import com.app.videosdk.model.PlayerModel
import com.app.videosdk.model.SkipIntro
import org.json.JSONObject

object FileUtils {

    /* ---------------------------------- */
    /* DRM TOKEN                           */
    /* ---------------------------------- */

    private fun getSecondFromDays(downloadDays: String?): Int {
        return if (!downloadDays.isNullOrEmpty() && !downloadDays.equals("0", true)) {
            downloadDays.toInt() * 24 * 60 * 60
        } else {
            0
        }
    }

    fun getDrmToken(context: Context, contentItems: ContentItem?): String {
        val accessType = if (contentItems?.accessType == PAID) "1" else "0"

        val downloadExpiry =
            if (getSecondFromDays(contentItems?.downloadExpiry) == 0)
                getSecondFromDays("30")
            else
                getSecondFromDays(contentItems?.downloadExpiry)

        val payload = JSONObject().apply {
            put("content_id", contentItems?.id)
            put("k_id", contentItems?.kId)
            put("user_id", "943592")
            put("package_id", "2")
            put("licence_duration", downloadExpiry)
            put("security_level", "0")
            put("rental_duration", "0")
            put("content_type", accessType)
            put("download", "1")
        }

        val deviceId = GUIDGenerator.generateGUID(context)

        return DRM_LICENSE_URL +
                "user_id=$deviceId" +
                "&type=$DRM_TYPE" +
                "&authorization=$TOKEN" +
                "&payload=${ApiEncryptionHelper.convertStringToBase64(payload.toString())}"
    }

    /* ---------------------------------- */
    /* PLAYER MODEL BUILDER                */
    /* ---------------------------------- */

    fun buildPlayerContentList(
        context: Context,
        pagingItems: LazyPagingItems<ContentItem>,
        overrideContent: OverrideContent?
    ): List<PlayerModel> {

        // ✅ GUARANTEED TEST AD (PRE / MID WHEN SEEKED)
        val adTagUrl =
            "https://pubads.g.doubleclick.net/gampad/ads?iu=/21775744923/external/vmap_ad_samples&sz=640x480&cust_params=sample_ar%3Dpremidpost&ciu_szs=300x250&gdfp_req=1&ad_rule=1&output=vmap&unviewed_position_start=1&env=vp&cmsid=496&vid=short_onecue&correlator="

        /* ---------------- OVERRIDE CONTENT ---------------- */

        overrideContent?.let {
            return listOf(
                PlayerModel(
                    hlsUrl = it.url,
                    liveUrl = it.url,
                    drmToken = it.drmToken.orEmpty(),
                    isLive = it.isLive
                )
            )
        }

        /* ---------------- PAGED CONTENT ---------------- */

        return pagingItems.itemSnapshotList.items.map { content ->
            PlayerModel(
                hlsUrl = content.hlsUrl.orEmpty(),
                mpdUrl = content.url.orEmpty(),
                liveUrl = "https://livesim.dashif.org/livesim/testpic_2s/Manifest.mpd",
                isLive = false,
                drmToken = getDrmToken(context, content),
                imageUrl = content.layoutThumbs
                    ?.firstOrNull()
                    ?.imageSize
                    ?.firstOrNull()
                    ?.url.orEmpty(),

                title = content.title.orEmpty(),
                description = content.shortDesc.orEmpty(),
                srt = content.subtitle?.firstOrNull()?.srt.orEmpty(),
                adsConfig = AdsConfig(
                    adTagUrl = adTagUrl,
                    enableAds = false
                ),
                skipIntro = SkipIntro(
                    startTime = 5000L,
                    endTime = 95000L,
                    enableSkipIntro = false
                ),
                nextEpisode = NextEpisode(
                    showBeforeEndMs = "160000",
                    enableNextEpisode = true
                )
            )
        }
    }

}
