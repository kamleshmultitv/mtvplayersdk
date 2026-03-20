package com.app.videosdk.listener

import androidx.media3.common.Player

class PlayerController {

    var exoPlayer: Player? = null

    fun togglePlayPause() {
        exoPlayer?.let {
            it.playWhenReady = !it.isPlaying
        }
    }

    fun toggleMute() {
        exoPlayer?.let {
            it.volume = if (it.volume > 0f) 0f else 1f
        }
    }

    fun isPlaying(): Boolean = exoPlayer?.isPlaying ?: false

    fun isMuted(): Boolean = (exoPlayer?.volume ?: 1f) == 0f
}