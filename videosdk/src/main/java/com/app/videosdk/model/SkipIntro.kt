package com.app.videosdk.model

data class SkipIntro(
    val startTime: Long? = 0L,
    val endTime: Long? = 0L,
    val enableSkipIntro: Boolean = false
)
