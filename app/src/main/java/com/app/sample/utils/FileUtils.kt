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
import com.app.videosdk.model.PlayerModel
import com.app.videosdk.ui.CuePoint
import com.app.videosdk.ui.CueType
import org.json.JSONObject

/**
 * Created by kamle on 19,August,2024
 */
object FileUtils {

    /* ---------------------------------- */
    /* DOWNLOAD / DRM UTILS                */
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

        // ✅ VERIFIED IMA MID-ROLL (PRE + MID + POST)
        val adTagUrl =
            "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/ad_rule_samples&ciu_szs=300x250&ad_rule=1&impl=s&gdfp_req=1&env=vp&output=vast&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ar%3Dpremidpost&correlator=\n"

        /* ---------------- OVERRIDE CONTENT ---------------- */

        overrideContent?.let {
            return listOf(
                PlayerModel(
                    hlsUrl = it.url,
                    liveUrl = it.url,
                    drmToken = it.drmToken.orEmpty(),
                    isLive = it.isLive,

                    adsConfig = AdsConfig(
                        adTagUrl = adTagUrl,
                        enableAds = true
                    ),

                    // No cue points needed here
                    cuePoints = emptyList()
                )
            )
        }

        /* ---------------- PAGED CONTENT ---------------- */

        return pagingItems.itemSnapshotList.items.map { content ->

            PlayerModel(
                hlsUrl = content.hlsUrl.orEmpty(),
                mpdUrl = content.url.orEmpty(),
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

                playbackSpeed = 1.0f,
                selectedSubtitle = null,
                selectedVideoQuality = 1080,

                // ✅ IMA AD-RULE CONTROLS REAL ADS
                adsConfig = AdsConfig(
                    adTagUrl = adTagUrl,
                    enableAds = true
                ),

                // ✅ MULTIPLE UI / ANALYTICS MARKERS
                cuePoints = generateCuePoints(
                    durationMs = 20 * 60 * 1000L, // 20 min
                    intervalMs = 5 * 60 * 1000L   // every 5 min
                )
            )
        }

    }
}

private fun generateCuePoints(
    durationMs: Long,
    intervalMs: Long
): List<CuePoint> {
    return (intervalMs until durationMs step intervalMs).mapIndexed { index, pos ->
        CuePoint(
            positionMs = pos,
            id = "marker_${index + 1}",
            type = CueType.AD
        )
    }
}
