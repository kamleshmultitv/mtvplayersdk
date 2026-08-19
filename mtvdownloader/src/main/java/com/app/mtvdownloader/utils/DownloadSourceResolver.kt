package com.app.mtvdownloader.utils

import com.app.mtvdownloader.entity.DownloadEntity

object DownloadSourceResolver {

    enum class SourceType {
        HLS,
        DASH
    }

    data class ResolvedDownloadSource(
        val uri: String,
        val type: SourceType,
        val drmLicenseUri: String?
    ) {
        val isDrmDash: Boolean = type == SourceType.DASH && !drmLicenseUri.isNullOrBlank()
    }

    fun resolve(contentItem: DownloadEntity): ResolvedDownloadSource? {
        val hls = contentItem.hlsUrl.cleanUrl()
        val mpd = contentItem.mpdUrl.cleanUrl()
        val isDrm = contentItem.drm == "1"

        return when {
            isDrm && hls != null ->
                ResolvedDownloadSource(
                    uri = hls,
                    type = SourceType.HLS,
                    drmLicenseUri = null
                )

            isDrm && mpd != null && !contentItem.drmToken.isNullOrBlank() ->
                ResolvedDownloadSource(
                    uri = mpd,
                    type = SourceType.DASH,
                    drmLicenseUri = contentItem.drmToken
                )

            hls != null ->
                ResolvedDownloadSource(
                    uri = hls,
                    type = SourceType.HLS,
                    drmLicenseUri = null
                )

            mpd != null ->
                ResolvedDownloadSource(
                    uri = mpd,
                    type = SourceType.DASH,
                    drmLicenseUri = null
                )

            else -> null
        }
    }

    fun qualityUrl(contentItem: DownloadEntity): String? =
        resolve(contentItem)?.uri

    private fun String?.cleanUrl(): String? =
        this?.trim()?.takeIf { it.isNotBlank() }
}
