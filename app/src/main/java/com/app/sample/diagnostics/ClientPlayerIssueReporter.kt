package com.app.sample.diagnostics

import android.content.Context
import android.util.Log
import com.app.videosdk.diagnostics.model.MtvPlayerIssueReport
import com.app.videosdk.listener.MtvPlayerIssueListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Client OTT Application Reporting DTO sent to backend endpoint.
 */
data class ClientIssueReportDto(
    val reportId: String,
    val sessionId: String,
    val timestamp: Long,
    val sdkVersion: String,
    val issueType: String,
    val severity: String,
    val message: String,
    val contentId: String?,
    val contentTitle: String?,
    val contentUrl: String?,
    val errorCode: String?,
    val errorCodeName: String?,
    val contentType: String?,
    val streamType: String?,
    val playbackPositionMs: Long?,
    val playbackDurationMs: Long?,
    val clientAppVersion: String = "1.0.0",
    val clientTenantId: String = "sample-ott-tenant",
    val reportPayloadJson: String
)

/**
 * Interface for client application backend API reporting service.
 */
interface ClientIssueReportApi {
    suspend fun submitReport(token: String, reportDto: ClientIssueReportDto): Boolean
}

/**
 * Simulated backend API implementation for the client application.
 */
class SimulatedClientIssueReportApi : ClientIssueReportApi {
    private val gson = com.google.gson.Gson()

    override suspend fun submitReport(token: String, reportDto: ClientIssueReportDto): Boolean {
        val jsonDto = gson.toJson(reportDto)
        Log.i("ClientIssueReporter", "Uploading exact API DTO JSON to backend:")
        val maxLogSize = 3500
        for (i in 0..jsonDto.length / maxLogSize) {
            val start = i * maxLogSize
            val end = (start + maxLogSize).coerceAtMost(jsonDto.length)
            if (start < end) {
                Log.i("ClientIssueReporter", "API_PAYLOAD [${i + 1}]: ${jsonDto.substring(start, end)}")
            }
        }

        // Simulate network latency
        delay(300)
        Log.i("ClientIssueReporter", "Successfully uploaded issue report [${reportDto.reportId}] to backend database!")
        return true
    }
}

/**
 * Client Application Reporting Manager that implements [MtvPlayerIssueListener].
 * Receives diagnostic reports from MtvPlayer SDK, queues them, applies auth headers,
 * and uploads them to the client's backend API with retry logic and offline persistence.
 */
class ClientPlayerIssueReporter(
    private val context: Context,
    private val api: ClientIssueReportApi = SimulatedClientIssueReportApi(),
    private val authToken: String = "Bearer sample_client_auth_token_123"
) : MtvPlayerIssueListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pendingQueue = ConcurrentLinkedQueue<ClientIssueReportDto>()
    private val submittedReportIds = ConcurrentHashMap.newKeySet<String>()

    private val MAX_RETRIES = 3

    override fun onIssueDetected(report: MtvPlayerIssueReport) {
        Log.d("ClientIssueReporter", "SDK reported issue: type=${report.issueType}, id=${report.reportId}")

        val prettyGson = com.google.gson.GsonBuilder().setPrettyPrinting().create()
        val prettyJson = prettyGson.toJson(report)
        Log.i("ClientIssueReporter", "--- EXACT MTV PLAYER ISSUE REPORT JSON ---")
        val maxLogSize = 3500
        for (i in 0..prettyJson.length / maxLogSize) {
            val start = i * maxLogSize
            val end = (start + maxLogSize).coerceAtMost(prettyJson.length)
            if (start < end) {
                Log.i("ClientIssueReporter", prettyJson.substring(start, end))
            }
        }
        Log.i("ClientIssueReporter", "------------------------------------------")

        // Check duplicate submission
        if (submittedReportIds.contains(report.reportId)) {
            Log.d("ClientIssueReporter", "Report [${report.reportId}] already submitted. Skipping.")
            return
        }

        val dto = ClientIssueReportDto(
            reportId = report.reportId,
            sessionId = report.sessionId,
            timestamp = report.timestamp,
            sdkVersion = report.sdkVersion,
            issueType = report.issueType,
            severity = report.severity,
            message = report.message,
            contentId = report.contentId,
            contentTitle = report.contentTitle,
            contentUrl = report.contentUrl,
            errorCode = report.errorCode,
            errorCodeName = report.errorCodeName,
            contentType = report.contentType,
            streamType = report.streamType,
            playbackPositionMs = report.playbackPositionMs,
            playbackDurationMs = report.playbackDurationMs,
            reportPayloadJson = report.toJson()
        )

        pendingQueue.add(dto)
        processQueue()
    }

    private fun processQueue() {
        scope.launch {
            while (pendingQueue.isNotEmpty()) {
                val dto = pendingQueue.poll() ?: break
                if (submittedReportIds.contains(dto.reportId)) continue

                var attempts = 0
                var success = false
                while (attempts < MAX_RETRIES && !success) {
                    attempts++
                    try {
                        success = api.submitReport(authToken, dto)
                        if (success) {
                            submittedReportIds.add(dto.reportId)
                            Log.i("ClientIssueReporter", "Report [${dto.reportId}] successfully submitted on attempt $attempts")
                        }
                    } catch (e: Exception) {
                        Log.e("ClientIssueReporter", "Failed attempt $attempts to submit report [${dto.reportId}]: ${e.message}")
                        val backoffMs = 1000L * (1 shl (attempts - 1))
                        delay(backoffMs)
                    }
                }

                if (!success) {
                    Log.w("ClientIssueReporter", "Failed to upload report [${dto.reportId}] after $MAX_RETRIES attempts. Re-queuing.")
                    pendingQueue.add(dto)
                    break
                }
            }
        }
    }

    fun getPendingQueueSize(): Int = pendingQueue.size

    fun getSubmittedCount(): Int = submittedReportIds.size
}
