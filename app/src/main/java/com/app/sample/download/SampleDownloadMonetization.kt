package com.app.sample.download

import android.content.Context
import com.app.mtvdownloader.entity.DownloadEntity

object SampleDownloadMonetization {
    suspend fun canStartDownload(
        context: Context,
        contentItem: DownloadEntity
    ): Boolean = true
}
