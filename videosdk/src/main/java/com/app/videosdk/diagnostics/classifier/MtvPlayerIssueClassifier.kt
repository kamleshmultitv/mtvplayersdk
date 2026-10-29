package com.app.videosdk.diagnostics.classifier

import androidx.media3.common.PlaybackException
import androidx.media3.datasource.HttpDataSource
import com.app.videosdk.diagnostics.model.MtvPlayerIssueType

/**
 * Result of issue classification.
 */
data class MtvIssueClassification(
    val issueType: String,
    val severity: String, // "INFO", "WARNING", "ERROR", "CRITICAL"
    val message: String
)

/**
 * Centralized classifier that converts Media3 errors, HTTP exceptions, DRM failures,
 * decoder errors, and playback events into standardized issue types and severity levels.
 */
object MtvPlayerIssueClassifier {

    /**
     * Classifies a Media3 [PlaybackException] into an [MtvIssueClassification].
     */
    fun classifyPlaybackException(
        error: PlaybackException,
        resourceType: String? = null
    ): MtvIssueClassification {
        val cause = error.cause
        val errorCode = error.errorCode

        // 1. DRM Exceptions
        if (isDrmErrorCode(errorCode) || isDrmCause(cause)) {
            val (issueType, severity) = classifyDrmError(errorCode, cause)
            return MtvIssueClassification(
                issueType = issueType,
                severity = severity,
                message = error.message ?: "DRM license/session error: ${error.errorCodeName}"
            )
        }

        // 2. Decoder & Rendering Exceptions
        if (isDecoderErrorCode(errorCode) || isDecoderCause(cause)) {
            val (issueType, severity) = classifyDecoderError(errorCode, cause)
            return MtvIssueClassification(
                issueType = issueType,
                severity = severity,
                message = error.message ?: "Decoder error: ${error.errorCodeName}"
            )
        }

        // 3. HTTP / Network Exceptions
        if (isNetworkErrorCode(errorCode) || cause is HttpDataSource.HttpDataSourceException) {
            val (issueType, severity) = classifyNetworkError(errorCode, cause, resourceType)
            return MtvIssueClassification(
                issueType = issueType,
                severity = severity,
                message = error.message ?: "Network HTTP error: ${error.errorCodeName}"
            )
        }

        // 4. Parsing / Manifest Errors
        if (errorCode == PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED ||
            errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED ||
            errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED
        ) {
            return MtvIssueClassification(
                issueType = MtvPlayerIssueType.MANIFEST_PARSE_FAILURE,
                severity = "ERROR",
                message = error.message ?: "Failed to parse manifest/container format"
            )
        }

        // 5. General Playback Errors
        val issueType = when (errorCode) {
            PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW -> MtvPlayerIssueType.LIVE_PLAYBACK_STALL
            PlaybackException.ERROR_CODE_TIMEOUT -> MtvPlayerIssueType.PLAYBACK_TIMEOUT
            PlaybackException.ERROR_CODE_REMOTE_ERROR -> MtvPlayerIssueType.CAST_PLAYBACK_FAILURE
            else -> MtvPlayerIssueType.PLAYBACK_FAILURE
        }

        return MtvIssueClassification(
            issueType = issueType,
            severity = "ERROR",
            message = error.message ?: "Playback error: ${error.errorCodeName}"
        )
    }

    /**
     * Classifies network load failures based on HTTP response code and resource type.
     */
    fun classifyHttpLoadError(
        httpStatusCode: Int?,
        resourceType: String?,
        exception: Throwable?
    ): MtvIssueClassification {
        val type = when {
            resourceType.equals("MANIFEST", ignoreCase = true) ||
                    resourceType.equals("HLS_MASTER", ignoreCase = true) ||
                    resourceType.equals("DASH_MANIFEST", ignoreCase = true) -> {
                if (httpStatusCode in 400..599) MtvPlayerIssueType.MANIFEST_LOAD_FAILURE
                else MtvPlayerIssueType.MANIFEST_PARSE_FAILURE
            }

            resourceType.equals("SEGMENT", ignoreCase = true) ||
                    resourceType.equals("MEDIA", ignoreCase = true) -> {
                MtvPlayerIssueType.SEGMENT_DOWNLOAD_FAILURE
            }

            resourceType.equals("DRM", ignoreCase = true) ||
                    resourceType.equals("LICENSE", ignoreCase = true) -> {
                MtvPlayerIssueType.DRM_LICENSE_REQUEST_FAILURE
            }

            resourceType.equals("SUBTITLE", ignoreCase = true) -> {
                MtvPlayerIssueType.SUBTITLE_PARSING_FAILURE
            }

            exception is HttpDataSource.InvalidResponseCodeException -> {
                MtvPlayerIssueType.HTTP_REQUEST_FAILURE
            }

            else -> MtvPlayerIssueType.HTTP_REQUEST_FAILURE
        }

        val severity = if (httpStatusCode != null && httpStatusCode >= 500) "ERROR" else "WARNING"
        val msg = "HTTP load error: status=$httpStatusCode, resource=$resourceType, cause=${exception?.message}"

        return MtvIssueClassification(issueType = type, severity = severity, message = msg)
    }

    private fun isDrmErrorCode(code: Int): Boolean {
        return code in 6000..6008
    }

    private fun isDrmCause(cause: Throwable?): Boolean {
        if (cause == null) return false
        val className = cause.javaClass.simpleName
        return className.contains("Drm", ignoreCase = true) ||
                className.contains("KeysExpired", ignoreCase = true) ||
                className.contains("Widevine", ignoreCase = true)
    }

    private fun classifyDrmError(errorCode: Int, cause: Throwable?): Pair<String, String> {
        val causeName = cause?.javaClass?.simpleName.orEmpty()
        val issueType = when {
            errorCode == PlaybackException.ERROR_CODE_DRM_PROVISIONING_FAILED -> MtvPlayerIssueType.DRM_PROVISIONING_FAILURE
            errorCode == PlaybackException.ERROR_CODE_DRM_LICENSE_EXPIRED -> MtvPlayerIssueType.DRM_LICENSE_REJECTED
            errorCode == PlaybackException.ERROR_CODE_DRM_SCHEME_UNSUPPORTED -> MtvPlayerIssueType.DRM_UNSUPPORTED_OPERATION
            errorCode == PlaybackException.ERROR_CODE_DRM_CONTENT_ERROR -> MtvPlayerIssueType.DRM_KEY_UNAVAILABLE
            causeName.contains("Provisioning", ignoreCase = true) -> MtvPlayerIssueType.DRM_PROVISIONING_FAILURE
            causeName.contains("KeysExpired", ignoreCase = true) -> MtvPlayerIssueType.DRM_LICENSE_REJECTED
            else -> MtvPlayerIssueType.DRM_SESSION_FAILURE
        }
        return issueType to "CRITICAL"
    }

    private fun isDecoderErrorCode(code: Int): Boolean {
        return code in 4001..4005
    }

    private fun isDecoderCause(cause: Throwable?): Boolean {
        if (cause == null) return false
        val className = cause.javaClass.simpleName
        return className.contains("Decoder", ignoreCase = true) ||
                className.contains("Codec", ignoreCase = true) ||
                className.contains("MediaCodec", ignoreCase = true)
    }

    private fun classifyDecoderError(errorCode: Int, cause: Throwable?): Pair<String, String> {
        val causeName = cause?.javaClass?.simpleName.orEmpty()
        val issueType = when {
            errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> MtvPlayerIssueType.DECODER_INITIALIZATION_FAILURE
            errorCode == PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED -> MtvPlayerIssueType.DECODER_QUERY_FAILURE
            causeName.contains("Init", ignoreCase = true) -> MtvPlayerIssueType.DECODER_INITIALIZATION_FAILURE
            else -> MtvPlayerIssueType.VIDEO_DECODING_FAILURE
        }
        return issueType to "ERROR"
    }

    private fun isNetworkErrorCode(code: Int): Boolean {
        return code in 2000..2008
    }

    private fun classifyNetworkError(
        errorCode: Int,
        cause: Throwable?,
        resourceType: String?
    ): Pair<String, String> {
        if (resourceType.equals("MANIFEST", ignoreCase = true)) {
            return MtvPlayerIssueType.MANIFEST_LOAD_FAILURE to "ERROR"
        }
        if (resourceType.equals("SEGMENT", ignoreCase = true) || resourceType.equals("MEDIA", ignoreCase = true)) {
            return MtvPlayerIssueType.SEGMENT_DOWNLOAD_FAILURE to "ERROR"
        }

        val issueType = when (errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> MtvPlayerIssueType.NETWORK_TIMEOUT
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> MtvPlayerIssueType.CONNECTION_FAILURE
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> MtvPlayerIssueType.HTTP_REQUEST_FAILURE
            else -> MtvPlayerIssueType.HTTP_REQUEST_FAILURE
        }
        return issueType to "ERROR"
    }
}
