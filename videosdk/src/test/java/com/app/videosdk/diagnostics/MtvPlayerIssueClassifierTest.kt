package com.app.videosdk.diagnostics

import androidx.media3.common.PlaybackException
import com.app.videosdk.diagnostics.classifier.MtvPlayerIssueClassifier
import com.app.videosdk.diagnostics.model.MtvPlayerIssueType
import org.junit.Assert.assertEquals
import org.junit.Test

class MtvPlayerIssueClassifierTest {

    @Test
    fun testClassifyDrmContentError() {
        val exception = PlaybackException(
            "DRM license error",
            null,
            PlaybackException.ERROR_CODE_DRM_CONTENT_ERROR
        )
        val classification = MtvPlayerIssueClassifier.classifyPlaybackException(exception)

        assertEquals(MtvPlayerIssueType.DRM_KEY_UNAVAILABLE, classification.issueType)
        assertEquals("CRITICAL", classification.severity)
    }

    @Test
    fun testClassifyDrmProvisioningFailed() {
        val exception = PlaybackException(
            "Provisioning failed",
            null,
            PlaybackException.ERROR_CODE_DRM_PROVISIONING_FAILED
        )
        val classification = MtvPlayerIssueClassifier.classifyPlaybackException(exception)

        assertEquals(MtvPlayerIssueType.DRM_PROVISIONING_FAILURE, classification.issueType)
        assertEquals("CRITICAL", classification.severity)
    }

    @Test
    fun testClassifyDecoderInitializationFailed() {
        val exception = PlaybackException(
            "Decoder init failed",
            null,
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED
        )
        val classification = MtvPlayerIssueClassifier.classifyPlaybackException(exception)

        assertEquals(MtvPlayerIssueType.DECODER_INITIALIZATION_FAILURE, classification.issueType)
        assertEquals("ERROR", classification.severity)
    }

    @Test
    fun testClassifyNetworkConnectionTimeout() {
        val exception = PlaybackException(
            "Connection timeout",
            null,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
        )
        val classification = MtvPlayerIssueClassifier.classifyPlaybackException(exception)

        assertEquals(MtvPlayerIssueType.NETWORK_TIMEOUT, classification.issueType)
        assertEquals("ERROR", classification.severity)
    }

    @Test
    fun testClassifyManifestParseFailure() {
        val exception = PlaybackException(
            "Malformed manifest",
            null,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED
        )
        val classification = MtvPlayerIssueClassifier.classifyPlaybackException(exception)

        assertEquals(MtvPlayerIssueType.MANIFEST_PARSE_FAILURE, classification.issueType)
        assertEquals("ERROR", classification.severity)
    }

    @Test
    fun testClassifyHttpSegmentLoadError() {
        val classification = MtvPlayerIssueClassifier.classifyHttpLoadError(
            httpStatusCode = 503,
            resourceType = "SEGMENT",
            exception = RuntimeException("Service Unavailable")
        )

        assertEquals(MtvPlayerIssueType.SEGMENT_DOWNLOAD_FAILURE, classification.issueType)
        assertEquals("ERROR", classification.severity)
    }

    @Test
    fun testClassifyHttpManifestLoadError() {
        val classification = MtvPlayerIssueClassifier.classifyHttpLoadError(
            httpStatusCode = 404,
            resourceType = "MANIFEST",
            exception = RuntimeException("Not Found")
        )

        assertEquals(MtvPlayerIssueType.MANIFEST_LOAD_FAILURE, classification.issueType)
        assertEquals("WARNING", classification.severity)
    }
}
