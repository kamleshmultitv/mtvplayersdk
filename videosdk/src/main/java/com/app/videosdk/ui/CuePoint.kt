package com.app.videosdk.ui

data class CuePoint(
    val positionMs: Long,          // timestamp in milliseconds
    val id: String,                // unique id
    val type: CueType,             // CUSTOM / AD / CHAPTER / ANALYTICS
    val payload: Any? = null       // optional extra data
)

enum class CueType {
    CUSTOM,
    AD,
    CHAPTER,
    ANALYTICS
}
