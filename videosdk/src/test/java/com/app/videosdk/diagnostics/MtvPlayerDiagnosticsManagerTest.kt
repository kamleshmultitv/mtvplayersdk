package com.app.videosdk.diagnostics

import android.content.Context
import com.app.videosdk.diagnostics.manager.MtvPlayerDiagnosticsManager
import com.app.videosdk.diagnostics.model.MtvPlayerIssueReport
import com.app.videosdk.diagnostics.model.MtvPlayerIssueType
import com.app.videosdk.listener.MtvPlayerIssueListener
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.mockito.Mockito
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class MtvPlayerDiagnosticsManagerTest {

    private fun mockContext(): Context {
        val context = Mockito.mock(Context::class.java)
        val resources = Mockito.mock(android.content.res.Resources::class.java)
        val metrics = android.util.DisplayMetrics()
        metrics.widthPixels = 1080
        metrics.heightPixels = 1920
        Mockito.`when`(resources.displayMetrics).thenReturn(metrics)
        Mockito.`when`(context.resources).thenReturn(resources)
        return context
    }

    @Test
    fun testTimelineRecordingAndBoundedQueue() {
        val manager = MtvPlayerDiagnosticsManager(context = mockContext())

        for (i in 1..150) {
            manager.recordTimelineEvent("EVENT_$i")
        }

        val report = manager.generateDiagnosticReport(customMessage = "Test bounded queue")
        assertNotNull(report)
        assertEquals(100, report.timeline.size)
        assertEquals("EVENT_51", report.timeline.first().eventType)
        assertEquals("EVENT_150", report.timeline.last().eventType)
    }

    @Test
    fun testManualReportGenerationAndListenerCallback() {
        val latch = CountDownLatch(1)
        var receivedReport: MtvPlayerIssueReport? = null

        val listener = MtvPlayerIssueListener { report ->
            receivedReport = report
            latch.countDown()
        }

        val manager = MtvPlayerDiagnosticsManager(context = mockContext(), issueListener = listener)
        manager.recordRecovery(attempted = true, succeeded = true, retryCount = 1, action = "FALLBACK_ONLINE", message = "Recovered")

        val generatedReport = manager.generateDiagnosticReport(
            customMessage = "Manual test report",
            issueTypeOverride = MtvPlayerIssueType.PLAYBACK_START_FAILURE
        )

        latch.await(2, TimeUnit.SECONDS)

        assertNotNull(receivedReport)
        assertEquals(MtvPlayerIssueType.PLAYBACK_START_FAILURE, generatedReport.issueType)
        assertEquals(true, generatedReport.recoveryInfo?.succeeded)
        assertEquals("FALLBACK_ONLINE", generatedReport.recoveryInfo?.recoveryAction)
    }
}
