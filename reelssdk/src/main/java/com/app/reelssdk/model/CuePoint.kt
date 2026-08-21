package com.app.reelssdk.model

data class CuePoint(
    val positionMs: Long,
    val id: String,
    val type: CueType,
    val payload: Any? = null
)

enum class CueType {
    AD,
    L_BAND
}
