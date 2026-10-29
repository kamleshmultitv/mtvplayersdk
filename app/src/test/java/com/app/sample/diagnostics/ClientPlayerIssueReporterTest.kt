package com.app.sample.diagnostics

import android.content.Context
import com.app.videosdk.diagnostics.model.MtvPlayerIssueReport
import com.app.videosdk.diagnostics.model.MtvPlayerIssueType
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito
import java.util.concurrent.atomic.AtomicInteger

class ClientPlayerIssueReporterTest {

    @Test
    fun testIssueDetectionAndAsyncSubmission() = runBlocking {
        val submitCount = AtomicInteger(0)
        val mockApi = object : ClientIssueReportApi {
            override suspend fun submitReport(token: String, reportDto: ClientIssueReportDto): Boolean {
                submitCount.incrementAndGet()
                return true
            }
        }

        val reporter = ClientPlayerIssueReporter(
            context = Mockito.mock(Context::class.java),
            api = mockApi,
            authToken = "Bearer test_token"
        )

        val report = MtvPlayerIssueReport(
            sessionId = "session-abc",
            issueType = MtvPlayerIssueType.MANIFEST_LOAD_FAILURE,
            severity = "ERROR",
            message = "Manifest failed to load"
        )

        reporter.onIssueDetected(report)

        delay(800)

        assertEquals(1, submitCount.get())
        assertEquals(1, reporter.getSubmittedCount())
    }

    @Test
    fun testDuplicateReportPrevention() = runBlocking {
        val submitCount = AtomicInteger(0)
        val mockApi = object : ClientIssueReportApi {
            override suspend fun submitReport(token: String, reportDto: ClientIssueReportDto): Boolean {
                submitCount.incrementAndGet()
                return true
            }
        }

        val reporter = ClientPlayerIssueReporter(
            context = Mockito.mock(Context::class.java),
            api = mockApi
        )

        val report = MtvPlayerIssueReport(
            reportId = "fixed-report-id-100",
            sessionId = "session-xyz",
            issueType = MtvPlayerIssueType.DECODER_INITIALIZATION_FAILURE,
            severity = "ERROR",
            message = "Decoder failed"
        )

        reporter.onIssueDetected(report)
        reporter.onIssueDetected(report) // Duplicate call with same reportId

        delay(800)

        assertEquals(1, submitCount.get())
    }
}
