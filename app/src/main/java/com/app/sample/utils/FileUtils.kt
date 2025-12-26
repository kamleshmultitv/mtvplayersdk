package com.app.sample.utils

import android.content.Context
import androidx.paging.compose.LazyPagingItems
import com.app.sample.model.ContentItem
import com.app.sample.BuildConfig.DRM_LICENSE_URL
import com.app.sample.extra.ApiConstant.DRM_TYPE
import com.app.sample.extra.ApiConstant.PAID
import com.app.sample.extra.ApiConstant.TOKEN
import com.app.sample.model.OverrideContent
import com.app.videosdk.model.PlayerModel
import org.json.JSONObject

/**
 * Created by kamle on 19,August,2024,MultiDownloader
 */
object FileUtils {

    private fun getSecondFromDays(downloadDays: String?): Int {
        return if (!downloadDays.isNullOrEmpty() && !downloadDays.equals("0", ignoreCase = true)) {
            downloadDays.toInt() * 24 * 60 * 60
        } else {
            0
        }
    }

    fun getContentStatus(context: Context, contentItems: ContentItem?): String {
        var accessType = contentItems?.accessType
        accessType = if (accessType.equals(PAID)) "1"
        else "0"
        val downloadExpiry = if (getSecondFromDays(contentItems?.downloadExpiry) == 0) {
            getSecondFromDays("30")
        } else {
            getSecondFromDays(contentItems?.downloadExpiry)
        }

        val jsonObject = JSONObject()
        jsonObject.put("content_id", "" + contentItems?.id)
        jsonObject.put("k_id", "" + contentItems?.kId)
        jsonObject.put("user_id", "" + "943592")
        jsonObject.put("package_id", "" + "2")
        jsonObject.put("licence_duration", "" + downloadExpiry)
        jsonObject.put("security_level", "0")
        jsonObject.put("rental_duration", "0")
        jsonObject.put("content_type", accessType)
        jsonObject.put("download", "1")
        val androidDeviceUniqueId = GUIDGenerator.generateGUID(context)
        val drmToken = DRM_LICENSE_URL +
                "user_id=" + androidDeviceUniqueId +
                "&type=" + DRM_TYPE +
                "&authorization=" + TOKEN +
                "&payload=" + ApiEncryptionHelper.convertStringToBase64(jsonObject.toString())
        return drmToken
    }

    fun buildPlayerContentList(
        context: Context,
        pagingItems: LazyPagingItems<ContentItem>,
        overrideContent: OverrideContent?
    ): List<PlayerModel> {

        overrideContent?.let {
            return listOf(
                PlayerModel(
                    hlsUrl = it.url,
                    mpdUrl = it.url,
                    drmToken = it.drmToken.orEmpty(),
                    imageUrl = "",
                    title = "Demo Content",
                    description = "Demo Content",
                    srt = "",
                    spriteUrl = it.spriteUrl,
                    playbackSpeed = 1.0f,
                    selectedSubtitle = null,
                    selectedVideoQuality = 1080,
                    isLive = false
                )
            )
        }

        return pagingItems.itemSnapshotList.items.map { content ->
            PlayerModel(
                hlsUrl = content.hlsUrl.orEmpty(),
                mpdUrl = content.url.orEmpty(),
                liveUrl = "https://livesim.dashif.org/livesim/testpic_2s/Manifest.mpd",
                drmToken = getContentStatus(context, content),
                imageUrl = content.layoutThumbs
                    ?.firstOrNull()
                    ?.imageSize
                    ?.firstOrNull()
                    ?.url.orEmpty(),
                title = content.title.orEmpty(),
                description = content.shortDesc.orEmpty(),
                season_title = content.seasonTitle,
                season_des = content.seasonDes,
                srt = content.subtitle?.firstOrNull()?.srt.orEmpty(),
                playbackSpeed = 1.0f,
                selectedSubtitle = null,
                selectedVideoQuality = 1080,
                isLive = false
            )
        }
    }

}
