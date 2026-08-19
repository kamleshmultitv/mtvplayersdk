package com.app.sample.download

import android.content.Context
import com.app.mtvdownloader.model.DownloadModel
import com.app.mtvdownloader.model.DownloadMonetizationGate

object SampleDownloadMonetization : DownloadMonetizationGate {
    override suspend fun canStartDownload(
        context: Context,
        contentItem: DownloadModel
    ): Boolean = true
}
