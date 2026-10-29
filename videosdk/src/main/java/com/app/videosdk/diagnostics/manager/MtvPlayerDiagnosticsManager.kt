package com.app.videosdk.diagnostics.manager

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import com.app.videosdk.diagnostics.classifier.MtvPlayerIssueClassifier
import com.app.videosdk.diagnostics.model.MtvDeviceInfo
import com.app.videosdk.diagnostics.model.MtvDrmDiagnostics
import com.app.videosdk.diagnostics.model.MtvNetworkDiagnostics
import com.app.videosdk.diagnostics.model.MtvPlaybackEvent
import com.app.videosdk.diagnostics.model.MtvPlaybackStats
import com.app.videosdk.diagnostics.model.MtvPlayerIssueReport
import com.app.videosdk.diagnostics.model.MtvPlayerIssueType
import com.app.videosdk.diagnostics.model.MtvRecoveryInfo
import com.app.videosdk.listener.MtvPlayerIssueListener
import com.app.videosdk.utils.SdkLogger
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

@OptIn(UnstableApi::class)
class MtvPlayerDiagnosticsManager(
    private val context: Context,
    var issueListener: MtvPlayerIssueListener? = null,
    var diagnosticsEnabled: Boolean = true
) {
    var sessionId: String = UUID.randomUUID().toString()
        private set

    private val sessionStartTimeMs = System.currentTimeMillis()
    private val reportExecutor = Executors.newSingleThreadExecutor()

    // De-duplication tracking
    private var lastReportKey: String? = null
    private var lastReportTimeMs: Long = 0L
    private val DEDUPLICATION_WINDOW_MS = 3_000L

    // Bounded event timeline (max 100 entries)
    private val timeline = ConcurrentLinkedQueue<MtvPlaybackEvent>()
    private val MAX_TIMELINE_EVENTS = 100

    // Playback Stats
    private val loadStartTimeMs = AtomicLong(0L)
    private val startupDurationMs = AtomicLong(0L)
    private val timeToFirstFrameMs = AtomicLong(0L)
    private val bufferingStartTimeMs = AtomicLong(0L)
    private val totalBufferingEvents = AtomicInteger(0)
    private val totalBufferingDurationMs = AtomicLong(0L)
    private val longestBufferingEventMs = AtomicLong(0L)
    private val rebufferCount = AtomicInteger(0)
    private val droppedVideoFrames = AtomicInteger(0)
    private val audioUnderruns = AtomicInteger(0)
    private val hasRenderedFirstFrame = AtomicBoolean(false)

    // Network & DRM State
    @Volatile private var lastFailedUrlSanitized: String? = null
    @Volatile private var lastHttpResponseCode: Int? = null
    @Volatile private var lastResourceType: String? = null
    @Volatile private var drmScheme: String? = null
    @Volatile private var lastDrmErrorCode: String? = null

    // Recovery State
    @Volatile private var recoveryInfo = MtvRecoveryInfo()

    // Device Info cache
    private val deviceInfo: MtvDeviceInfo by lazy {
        val displayMetrics = context.resources?.displayMetrics
        val width = displayMetrics?.widthPixels ?: 0
        val height = displayMetrics?.heightPixels ?: 0
        MtvDeviceInfo(
            manufacturer = Build.MANUFACTURER ?: "Unknown",
            model = Build.MODEL ?: "Unknown",
            brand = Build.BRAND ?: "Unknown",
            device = Build.DEVICE ?: "Unknown",
            androidVersion = Build.VERSION.RELEASE ?: "Unknown",
            sdkInt = Build.VERSION.SDK_INT,
            screenResolution = "${width}x${height}"
        )
    }

    /**
     * Resets the diagnostic session for a new playback item/url.
     */
    fun resetSession(newSessionId: String = UUID.randomUUID().toString()) {
        sessionId = newSessionId
        timeline.clear()
        loadStartTimeMs.set(System.currentTimeMillis())
        startupDurationMs.set(0L)
        timeToFirstFrameMs.set(0L)
        bufferingStartTimeMs.set(0L)
        totalBufferingEvents.set(0)
        totalBufferingDurationMs.set(0L)
        longestBufferingEventMs.set(0L)
        rebufferCount.set(0)
        droppedVideoFrames.set(0)
        audioUnderruns.set(0)
        hasRenderedFirstFrame.set(false)
        lastFailedUrlSanitized = null
        lastHttpResponseCode = null
        lastResourceType = null
        drmScheme = null
        lastDrmErrorCode = null
        recoveryInfo = MtvRecoveryInfo()

        recordTimelineEvent("PLAYBACK_REQUESTED")
    }

    /**
     * Records a recovery attempt outcome.
     */
    fun recordRecovery(attempted: Boolean, succeeded: Boolean, retryCount: Int, action: String?, message: String?) {
        recoveryInfo = MtvRecoveryInfo(
            attempted = attempted,
            succeeded = succeeded,
            retryCount = retryCount,
            recoveryAction = action,
            message = SdkLogger.redact(message.orEmpty())
        )
        recordTimelineEvent(
            if (succeeded) "RECOVERY_SUCCEEDED" else "RECOVERY_FAILED",
            mapOf("action" to action.orEmpty(), "retryCount" to retryCount.toString())
        )
    }

    /**
     * Adds an event to the bounded timeline queue.
     */
    fun recordTimelineEvent(
        eventType: String,
        attributes: Map<String, String> = emptyMap(),
        positionMs: Long? = null
    ) {
        val event = MtvPlaybackEvent(
            eventType = eventType,
            timestamp = System.currentTimeMillis(),
            elapsedSessionTimeMs = System.currentTimeMillis() - sessionStartTimeMs,
            playbackPositionMs = positionMs,
            attributes = SdkLogger.redactAttributes(attributes)
        )
        timeline.add(event)
        while (timeline.size > MAX_TIMELINE_EVENTS) {
            timeline.poll()
        }
    }

    /**
     * Generates a complete diagnostic report on demand without triggering a playback error.
     */
    fun generateDiagnosticReport(
        customMessage: String? = null,
        issueTypeOverride: String? = null,
        contentType: String? = null,
        streamType: String? = null,
        positionMs: Long? = null,
        durationMs: Long? = null
    ): MtvPlayerIssueReport {
        val issueType = issueTypeOverride ?: MtvPlayerIssueType.UNKNOWN
        val message = customMessage ?: "Manual diagnostic report requested"

        val report = MtvPlayerIssueReport(
            sessionId = sessionId,
            issueType = issueType,
            severity = "INFO",
            message = SdkLogger.redact(message),
            contentType = contentType,
            streamType = streamType,
            playbackPositionMs = positionMs,
            playbackDurationMs = durationMs,
            deviceInfo = deviceInfo,
            playbackStats = buildPlaybackStats(),
            networkInfo = buildNetworkDiagnostics(),
            drmInfo = buildDrmDiagnostics(),
            timeline = timeline.toList(),
            recoveryInfo = recoveryInfo
        )

        dispatchReportToListener(report)
        return report
    }

    private fun buildPlaybackStats(): MtvPlaybackStats {
        val totalBuffering = totalBufferingEvents.get()
        val totalDuration = totalBufferingDurationMs.get()
        val avgDuration = if (totalBuffering > 0) totalDuration / totalBuffering else 0L

        return MtvPlaybackStats(
            startupDurationMs = startupDurationMs.get(),
            timeToFirstFrameMs = timeToFirstFrameMs.get(),
            totalBufferingEvents = totalBuffering,
            totalBufferingDurationMs = totalDuration,
            averageBufferingDurationMs = avgDuration,
            longestBufferingEventMs = longestBufferingEventMs.get(),
            rebufferCount = rebufferCount.get(),
            droppedVideoFrames = droppedVideoFrames.get(),
            audioUnderruns = audioUnderruns.get()
        )
    }

    private fun buildNetworkDiagnostics(): MtvNetworkDiagnostics {
        var networkType = "NONE"
        var isConnected = false

        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (cm != null) {
                val activeNetwork = cm.activeNetwork
                val capabilities = cm.getNetworkCapabilities(activeNetwork)
                if (capabilities != null) {
                    isConnected = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    networkType = when {
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
                        else -> "OTHER"
                    }
                }
            }
        } catch (_: Exception) {}

        return MtvNetworkDiagnostics(
            networkType = networkType,
            isConnected = isConnected,
            lastFailedUrlSanitized = lastFailedUrlSanitized,
            lastHttpResponseCode = lastHttpResponseCode,
            lastResourceType = lastResourceType
        )
    }

    private fun buildDrmDiagnostics(): MtvDrmDiagnostics {
        return MtvDrmDiagnostics(
            drmScheme = drmScheme,
            licenseServerUrlSanitized = null,
            lastDrmErrorCode = lastDrmErrorCode
        )
    }

    private fun dispatchReportToListener(report: MtvPlayerIssueReport) {
        if (!diagnosticsEnabled) return
        val listener = issueListener ?: return

        // De-duplication check within window
        val key = "${report.issueType}_${report.errorCode}_${report.message}"
        val now = System.currentTimeMillis()
        if (key == lastReportKey && (now - lastReportTimeMs) < DEDUPLICATION_WINDOW_MS) {
            SdkLogger.debug("Skipping duplicate issue report within de-duplication window: $key")
            return
        }
        lastReportKey = key
        lastReportTimeMs = now

        // Dispatch on single thread executor asynchronously to avoid blocking playback thread
        reportExecutor.execute {
            try {
                listener.onIssueDetected(report)
            } catch (e: Throwable) {
                SdkLogger.error("Client MtvPlayerIssueListener threw an exception", e)
            }
        }
    }

    /**
     * The Media3 [AnalyticsListener] instance to attach to ExoPlayer.
     */
    val analyticsListener: AnalyticsListener = object : AnalyticsListener {

        override fun onPlayerError(
            eventTime: AnalyticsListener.EventTime,
            error: PlaybackException
        ) {
            val classification = MtvPlayerIssueClassifier.classifyPlaybackException(
                error = error,
                resourceType = lastResourceType
            )

            recordTimelineEvent(
                eventType = "PLAYBACK_ERROR",
                attributes = mapOf(
                    "errorCode" to error.errorCodeName,
                    "issueType" to classification.issueType,
                    "message" to classification.message
                ),
                positionMs = eventTime.currentPlaybackPositionMs
            )

            val report = MtvPlayerIssueReport(
                sessionId = sessionId,
                issueType = classification.issueType,
                severity = classification.severity,
                message = SdkLogger.redact(classification.message),
                errorCode = error.errorCode.toString(),
                errorCodeName = error.errorCodeName,
                playbackPositionMs = eventTime.currentPlaybackPositionMs,
                playbackDurationMs = if (eventTime.timeline.isEmpty) null else eventTime.eventPlaybackPositionMs,
                deviceInfo = deviceInfo,
                playbackStats = buildPlaybackStats(),
                networkInfo = buildNetworkDiagnostics(),
                drmInfo = buildDrmDiagnostics(),
                timeline = timeline.toList(),
                recoveryInfo = recoveryInfo
            )

            dispatchReportToListener(report)
        }

        override fun onLoadStarted(
            eventTime: AnalyticsListener.EventTime,
            loadEventInfo: LoadEventInfo,
            mediaLoadData: MediaLoadData
        ) {
            if (loadStartTimeMs.get() == 0L) {
                loadStartTimeMs.set(System.currentTimeMillis())
            }
            lastResourceType = getResourceTypeName(mediaLoadData.dataType)
            recordTimelineEvent(
                eventType = "LOAD_STARTED",
                attributes = mapOf(
                    "resourceType" to lastResourceType.orEmpty(),
                    "url" to SdkLogger.redactUrl(loadEventInfo.uri.toString())
                ),
                positionMs = eventTime.currentPlaybackPositionMs
            )
        }

        override fun onLoadError(
            eventTime: AnalyticsListener.EventTime,
            loadEventInfo: LoadEventInfo,
            mediaLoadData: MediaLoadData,
            error: IOException,
            wasCanceled: Boolean
        ) {
            lastFailedUrlSanitized = SdkLogger.redactUrl(loadEventInfo.uri.toString())
            lastResourceType = getResourceTypeName(mediaLoadData.dataType)

            var statusCode: Int? = null
            if (error is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException) {
                statusCode = error.responseCode
                lastHttpResponseCode = statusCode
            }

            val classification = MtvPlayerIssueClassifier.classifyHttpLoadError(
                httpStatusCode = statusCode,
                resourceType = lastResourceType,
                exception = error
            )

            recordTimelineEvent(
                eventType = "NETWORK_REQUEST_FAILED",
                attributes = mapOf(
                    "issueType" to classification.issueType,
                    "resourceType" to lastResourceType.orEmpty(),
                    "statusCode" to (statusCode?.toString() ?: "UNKNOWN"),
                    "url" to lastFailedUrlSanitized.orEmpty()
                ),
                positionMs = eventTime.currentPlaybackPositionMs
            )

            val report = MtvPlayerIssueReport(
                sessionId = sessionId,
                issueType = classification.issueType,
                severity = classification.severity,
                message = SdkLogger.redact(classification.message),
                errorCode = statusCode?.toString() ?: "HTTP_LOAD_ERROR",
                errorCodeName = classification.issueType,
                playbackPositionMs = eventTime.currentPlaybackPositionMs,
                deviceInfo = deviceInfo,
                playbackStats = buildPlaybackStats(),
                networkInfo = buildNetworkDiagnostics(),
                drmInfo = buildDrmDiagnostics(),
                timeline = timeline.toList(),
                recoveryInfo = recoveryInfo
            )

            dispatchReportToListener(report)
        }

        override fun onPlaybackStateChanged(
            eventTime: AnalyticsListener.EventTime,
            state: Int
        ) {
            val now = System.currentTimeMillis()
            when (state) {
                Player.STATE_BUFFERING -> {
                    bufferingStartTimeMs.set(now)
                    val count = totalBufferingEvents.incrementAndGet()
                    if (hasRenderedFirstFrame.get()) {
                        rebufferCount.incrementAndGet()
                    }
                    recordTimelineEvent(
                        eventType = "BUFFERING_STARTED",
                        attributes = mapOf("count" to count.toString()),
                        positionMs = eventTime.currentPlaybackPositionMs
                    )
                }

                Player.STATE_READY -> {
                    val start = bufferingStartTimeMs.getAndSet(0L)
                    if (start > 0L) {
                        val duration = now - start
                        totalBufferingDurationMs.addAndGet(duration)
                        if (duration > longestBufferingEventMs.get()) {
                            longestBufferingEventMs.set(duration)
                        }

                        // Excessive buffering check (>15 seconds)
                        if (duration > 15_000L) {
                            val report = MtvPlayerIssueReport(
                                sessionId = sessionId,
                                issueType = MtvPlayerIssueType.EXCESSIVE_BUFFERING,
                                severity = "WARNING",
                                message = "Buffering duration exceeded 15 seconds (${duration}ms)",
                                playbackPositionMs = eventTime.currentPlaybackPositionMs,
                                deviceInfo = deviceInfo,
                                playbackStats = buildPlaybackStats(),
                                networkInfo = buildNetworkDiagnostics(),
                                timeline = timeline.toList()
                            )
                            dispatchReportToListener(report)
                        }
                    }
                    recordTimelineEvent(
                        eventType = "PLAYBACK_READY",
                        positionMs = eventTime.currentPlaybackPositionMs
                    )
                }

                Player.STATE_ENDED -> {
                    recordTimelineEvent(
                        eventType = "PLAYBACK_ENDED",
                        positionMs = eventTime.currentPlaybackPositionMs
                    )
                }
            }
        }

        override fun onRenderedFirstFrame(
            eventTime: AnalyticsListener.EventTime,
            output: Any,
            renderTimeMs: Long
        ) {
            val now = System.currentTimeMillis()
            if (hasRenderedFirstFrame.compareAndSet(false, true)) {
                val start = loadStartTimeMs.get()
                if (start > 0L) {
                    val duration = now - start
                    startupDurationMs.set(duration)
                    timeToFirstFrameMs.set(duration)
                }
            }
            recordTimelineEvent(
                eventType = "FIRST_VIDEO_FRAME_RENDERED",
                positionMs = eventTime.currentPlaybackPositionMs
            )
        }

        override fun onDroppedVideoFrames(
            eventTime: AnalyticsListener.EventTime,
            droppedFrames: Int,
            elapsedMs: Long
        ) {
            val totalDropped = droppedVideoFrames.addAndGet(droppedFrames)
            if (droppedFrames > 50) {
                recordTimelineEvent(
                    eventType = "DROPPED_FRAMES",
                    attributes = mapOf(
                        "droppedInPeriod" to droppedFrames.toString(),
                        "totalDropped" to totalDropped.toString()
                    ),
                    positionMs = eventTime.currentPlaybackPositionMs
                )
            }
        }

        override fun onAudioUnderrun(
            eventTime: AnalyticsListener.EventTime,
            bufferSize: Int,
            bufferSizeMs: Long,
            elapsedSinceLastFeedMs: Long
        ) {
            val underruns = audioUnderruns.incrementAndGet()
            recordTimelineEvent(
                eventType = "AUDIO_UNDERRUN",
                attributes = mapOf("totalUnderruns" to underruns.toString()),
                positionMs = eventTime.currentPlaybackPositionMs
            )
        }

        override fun onDrmSessionManagerError(
            eventTime: AnalyticsListener.EventTime,
            error: Exception
        ) {
            lastDrmErrorCode = error.javaClass.simpleName
            recordTimelineEvent(
                eventType = "DRM_ERROR",
                attributes = mapOf("error" to lastDrmErrorCode.orEmpty()),
                positionMs = eventTime.currentPlaybackPositionMs
            )

            val classification = MtvPlayerIssueClassifier.classifyPlaybackException(
                error = PlaybackException(
                    /* message = */ error.message,
                    /* cause = */ error,
                    /* errorCode = */ PlaybackException.ERROR_CODE_DRM_CONTENT_ERROR
                )
            )

            val report = MtvPlayerIssueReport(
                sessionId = sessionId,
                issueType = classification.issueType,
                severity = classification.severity,
                message = SdkLogger.redact(error.message ?: "DRM session manager error"),
                errorCode = lastDrmErrorCode,
                playbackPositionMs = eventTime.currentPlaybackPositionMs,
                deviceInfo = deviceInfo,
                playbackStats = buildPlaybackStats(),
                drmInfo = buildDrmDiagnostics(),
                timeline = timeline.toList()
            )

            dispatchReportToListener(report)
        }
    }

    private fun getResourceTypeName(dataType: Int): String {
        return when (dataType) {
            C.DATA_TYPE_MANIFEST -> "MANIFEST"
            C.DATA_TYPE_MEDIA -> "MEDIA"
            C.DATA_TYPE_MEDIA_INITIALIZATION -> "MEDIA_INIT"
            C.DATA_TYPE_DRM -> "DRM"
            C.DATA_TYPE_TIME_SYNCHRONIZATION -> "TIME_SYNC"
            C.DATA_TYPE_AD -> "AD"
            else -> "UNKNOWN"
        }
    }

    fun release() {
        reportExecutor.shutdown()
    }
}
