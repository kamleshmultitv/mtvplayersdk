package com.app.videosdk.utils

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Intelligent Low-Bandwidth Fallback Monitor implementing:
 * 1. **Auto Mode**: ABR steps down (1080p -> 720p -> 480p -> 240p). In dead zones (<100 kbps / persistent buffering), triggers Audio-First Podcast Mode.
 * 2. **Manual Mode**: If a user explicitly selects a quality (e.g. 1080p/720p) and bandwidth is insufficient, directly triggers Audio-First Podcast Mode (skipping jittery low-res downgrades).
 * 3. **Recovery**: Automatically restores video when bandwidth/buffering stabilizes.
 */
@OptIn(UnstableApi::class)
class LowBandwidthFallbackMonitor(
    private val exoPlayer: ExoPlayer,
    private val onFallbackTriggered: (Boolean) -> Unit
) {
    private var isFallbackActive = false
    private var bufferingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            when (state) {
                Player.STATE_BUFFERING -> {
                    val isLiveStream = exoPlayer.isCurrentMediaItemDynamic || exoPlayer.duration == C.TIME_UNSET
                    // Check if online playback and playWhenReady is true
                    if (!isFallbackActive && exoPlayer.playWhenReady && !isLiveStream) {
                        bufferingJob?.cancel()
                        bufferingJob = scope.launch {
                            delay(3000L) // 3-second persistent buffering threshold for dead zones / insufficient bandwidth
                            if (exoPlayer.playbackState == Player.STATE_BUFFERING) {
                                val isManualMode = isUserInManualQualityMode(exoPlayer)
                                SdkLogger.debug("LowBandwidthFallbackMonitor: Triggering Audio-First Fallback (Manual Mode: $isManualMode)")
                                triggerFallback(true)
                            }
                        }
                    }
                }
                Player.STATE_READY -> {
                    bufferingJob?.cancel()
                    // If fallback is active and playback is stable for 10 seconds, recover video
                    if (isFallbackActive) {
                        scheduleBandwidthRecoveryCheck()
                    }
                }
            }
        }
    }

    init {
        exoPlayer.addListener(listener)
    }

    private fun isUserInManualQualityMode(player: ExoPlayer): Boolean {
        val trackSelector = player.trackSelector as? DefaultTrackSelector ?: return false
        val params = trackSelector.parameters
        return params.maxVideoWidth < Int.MAX_VALUE || params.maxVideoHeight < Int.MAX_VALUE
    }

    private fun triggerFallback(enable: Boolean) {
        if (isFallbackActive == enable) return
        isFallbackActive = enable

        val currentParams = exoPlayer.trackSelectionParameters
        val newParams = currentParams
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, enable)
            .build()
        exoPlayer.trackSelectionParameters = newParams

        onFallbackTriggered(enable)
    }

    private fun scheduleBandwidthRecoveryCheck() {
        scope.launch {
            delay(10_000L) // 10 seconds stable playback before restoring video
            if (isFallbackActive && exoPlayer.isPlaying) {
                SdkLogger.debug("LowBandwidthFallbackMonitor: Bandwidth recovered. Restoring video playback.")
                triggerFallback(false)
            }
        }
    }

    fun release() {
        bufferingJob?.cancel()
        exoPlayer.removeListener(listener)
    }
}
