package com.app.sample.model

data class DeepLinkResponse(
    val contentId: String? = null,
    val url: String? = null,
    val seekTo: Long? = null,
    val clipEndTime: Long? = null,
    val totalClipDuration: Long? = null
)
