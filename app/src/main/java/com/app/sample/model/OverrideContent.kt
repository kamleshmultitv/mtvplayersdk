package com.app.sample.model

import com.app.reelssdk.model.AdsConfig
import com.app.reelssdk.model.NextEpisode
import com.app.reelssdk.model.SkipIntro

data class OverrideContent(
    val url: String? = null,
    val spriteUrl: String? = null,
    val drmToken: String? = null,
    val isLive: Boolean = false,
    val adsConfig: AdsConfig? = null,
    val skipIntro: SkipIntro? = null,
    val nextEpisode: NextEpisode? = null
)
