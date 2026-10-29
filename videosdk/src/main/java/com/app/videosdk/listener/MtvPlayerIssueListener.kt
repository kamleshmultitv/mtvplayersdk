package com.app.videosdk.listener

import com.app.videosdk.diagnostics.model.MtvPlayerIssueReport

/**
 * Public SDK Listener for receiving structured player issue diagnostic reports.
 * Integrating OTT applications implement this interface to receive reports and upload
 * them to their own backend API.
 */
fun interface MtvPlayerIssueListener {
    /**
     * Called when a player issue is detected or when a diagnostic report is manually generated.
     *
     * Note: This callback is thread-safe and non-blocking. Integrating applications should perform
     * network operations asynchronously on their own background worker threads or dispatchers.
     *
     * @param report The sanitized, structured diagnostic report containing session metrics, timeline, and issue context.
     */
    fun onIssueDetected(report: MtvPlayerIssueReport)
}
