package com.app.videosdk.diagnostics

import com.app.videosdk.diagnostics.model.MtvDeviceInfo
import com.app.videosdk.diagnostics.model.MtvPlaybackEvent
import com.app.videosdk.diagnostics.model.MtvPlaybackStats
import com.app.videosdk.diagnostics.model.MtvPlayerIssueReport
import com.app.videosdk.diagnostics.model.MtvPlayerIssueType
import com.app.videosdk.utils.SdkLogger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MtvPlayerIssueReportTest {

    @Test
    fun testJsonSerializationAndDeserialization() {
        val report = MtvPlayerIssueReport(
            sessionId = "test-session-123",
            issueType = MtvPlayerIssueType.SEGMENT_DOWNLOAD_FAILURE,
            severity = "ERROR",
            message = "Segment download failed with HTTP 503",
            errorCode = "503",
            playbackPositionMs = 15000L,
            playbackDurationMs = 120000L,
            deviceInfo = MtvDeviceInfo(
                manufacturer = "Google",
                model = "Pixel 8",
                brand = "google",
                device = "shiba",
                androidVersion = "14",
                sdkInt = 34
            ),
            playbackStats = MtvPlaybackStats(
                startupDurationMs = 1200L,
                totalBufferingEvents = 2,
                totalBufferingDurationMs = 3500L
            ),
            timeline = listOf(
                MtvPlaybackEvent(eventType = "PLAYBACK_REQUESTED", elapsedSessionTimeMs = 0L),
                MtvPlaybackEvent(eventType = "PLAYBACK_READY", elapsedSessionTimeMs = 1200L)
            )
        )

        val json = report.toJson()
        assertNotNull(json)
        assertTrue(json.contains("test-session-123"))
        assertTrue(json.contains(MtvPlayerIssueType.SEGMENT_DOWNLOAD_FAILURE))

        val parsedReport = MtvPlayerIssueReport.jsonToReport(json)
        assertNotNull(parsedReport)
        assertEquals(report.reportId, parsedReport?.reportId)
        assertEquals(report.sessionId, parsedReport?.sessionId)
        assertEquals(report.issueType, parsedReport?.issueType)
        assertEquals(report.severity, parsedReport?.severity)
        assertEquals(2, parsedReport?.timeline?.size)
    }

    @Test
    fun testRedactionOfSensitiveQueryParameters() {
        val sensitiveUrl = "https://example.com/stream.m3u8?token=secret123&authorization=Bearer_abc"
        val redactedUrl = SdkLogger.redactUrl(sensitiveUrl)

        assertTrue(!redactedUrl.contains("secret123"))
        assertTrue(!redactedUrl.contains("Bearer_abc"))
        assertTrue(redactedUrl.contains("<redacted>"))
    }
}
