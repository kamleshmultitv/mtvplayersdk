package com.app.mtvdownloader.helper

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.app.mtvdownloader.model.DownloadModel
import com.app.mtvdownloader.repository.DownloadRepository
import com.app.mtvdownloader.service.MediaDownloadService
import com.app.mtvdownloader.worker.DownloadWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

object ReelDownloadHelper {

    fun handleDownloadClick(
        context: Context,
        contentItem: DownloadModel?
    ) {
        if (contentItem == null || contentItem.hlsUrl.isNullOrEmpty()) return

        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {

            try {

                val repository =
                    DownloadRepository.instance(appContext)

                // 1️⃣ ALREADY DOWNLOADED CHECK
                val existing =
                    repository.getDownloadedContentOnce(contentItem.id.toString())

                if (existing?.downloadStatus ==
                    DownloadWorker.DOWNLOAD_STATUS_COMPLETED
                ) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "${contentItem.title} already downloaded",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                // 2️⃣ CHECK IF SOME DOWNLOAD IS ALREADY RUNNING
                val hasActiveDownload = repository.hasActiveDownload()

                // 3️⃣ BUILD INPUT DATA
                val inputData = androidx.work.Data.Builder()
                    .putString(DownloadWorker.KEY_CONTENT_ID, contentItem.id.toString())
                    .putString(DownloadWorker.KEY_SEASON_ID, contentItem.seasonId.orEmpty())
                    .putString(DownloadWorker.KEY_CONTENT_TITLE, contentItem.title.orEmpty())
                    .putString(DownloadWorker.KEY_SEASON_NAME, contentItem.seasonTitle.orEmpty())
                    .putString(DownloadWorker.KEY_THUMBNAIL_URL, contentItem.imageUrl)
                    .putString(DownloadWorker.KEY_SEASON_THUMBNAIL_URL, contentItem.imageUrl)
                    .putString(DownloadWorker.KEY_HLS_URI, contentItem.hlsUrl)
                    .build()

                val workRequest =
                    OneTimeWorkRequestBuilder<DownloadWorker>()
                        .setInputData(inputData)
                        .addTag(contentItem.id.toString())
                        .build()

                WorkManager.getInstance(appContext)
                    .enqueueUniqueWork(
                        "reel_download_queue",
                        ExistingWorkPolicy.APPEND,
                        workRequest
                    )

                MediaDownloadService.start(appContext)

                // 4️⃣ CORRECT TOAST MESSAGE
                withContext(Dispatchers.Main) {
                    val message =
                        if (hasActiveDownload) {
                            "${contentItem.title} added to queue"
                        } else {
                            "${contentItem.title} downloading"
                        }

                    Toast.makeText(
                        context,
                        message,
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (t: Throwable) {
                Log.e("ReelDownloadHelper", "Download enqueue failed", t)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Failed to start download",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}

