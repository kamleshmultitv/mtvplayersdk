package com.app.videosdk.model

data class PlayerConfig(
    val controls: PlayerControlsConfig = PlayerControlsConfig(),
    val ads: PlayerAdsConfig = PlayerAdsConfig(),
    val freePreview: FreePreviewConfig? = null,
    val freePreviewEnd: FreePreviewEndConfig? = null,
    val watermark: WatermarkConfig? = null,
    val liveImageUrl: String? = null,
    val subtitleEnabled: Boolean = false,
    val autoPlayFeature: Boolean = false,
    val autoPlayDetail: Boolean = false,
    val autoPlayAssets: Boolean = false
)

data class PlayerControlsConfig(
    val play: Boolean = true,
    val pause: Boolean = true,
    val seekBack: ControlSeekConfig = ControlSeekConfig(enabled = true, seconds = 10),
    val seekForward: ControlSeekConfig = ControlSeekConfig(enabled = true, seconds = 10),
    val previous: Boolean = true,
    val next: Boolean = true,
    val mute: Boolean = true,
    val unmute: Boolean = true,
    val seasonSelector: Boolean = true,
    val settings: Boolean = true,
    val pip: Boolean = true,
    val fullscreen: Boolean = true,
    val exitFullscreen: Boolean = true
)

data class ControlSeekConfig(
    val enabled: Boolean,
    val seconds: Int
) {
    val safeSeconds: Int
        get() = seconds.coerceAtLeast(0)
}

data class PlayerAdsConfig(
    val googleAdsEnabled: Boolean = false,
    val vmapAdsEnabled: Boolean = false,
    val bannerAdsEnabled: Boolean = false,
    val startTimeSec: Int = 0,
    val endTimeSec: Int = 0,
    val gapDurationMs: Long = 600_000L,
    val closeButtonEnabled: Boolean = true
)

data class FreePreviewConfig(
    val enabled: Boolean,
    val durationMs: Long,
    val popupAllowed: Boolean,
    val popupText: String?,
    val buttonLabel: String?
)

data class FreePreviewEndConfig(
    val enabled: Boolean,
    val durationMs: Long,
    val popupText: String?,
    val primaryButtonLabel: String?,
    val secondaryButtonLabel: String?
)

data class WatermarkConfig(
    val enabled: Boolean,
    val imageUrl: String?,
    val type: String?,
    val position: WatermarkPosition,
    val textColor: String?,
    val fontSize: String?
)

enum class WatermarkPosition {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    CENTER
}
