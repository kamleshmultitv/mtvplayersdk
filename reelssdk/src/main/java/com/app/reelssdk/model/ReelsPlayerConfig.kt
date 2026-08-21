package com.app.reelssdk.model

data class ReelsPlayerConfig(
    val controls: ReelsPlayerControlsConfig = ReelsPlayerControlsConfig()
)

data class ReelsPlayerControlsConfig(
    val play: Boolean = true,
    val pause: Boolean = true,
    val settings: Boolean = true,
    val fullscreen: Boolean = true,
    val exitFullscreen: Boolean = true,
    val share: Boolean = true,
    val bookmark: Boolean = true,
    val seekbar: Boolean = true
)
