package com.app.reelssdk.model

import androidx.annotation.ColorRes

data class ReelsPlayerCustomControls(

    // Global tint applied to all SDK fallback/custom icons when provided.
    @param:ColorRes
    @field:ColorRes
    val iconTintRes: Int? = null,

    val playIconRes: Int? = null,
    val pauseIconRes: Int? = null,

    val backIconRes: Int? = null,
    val settingsIconRes: Int? = null,
    val fullScreenIconRes: Int? = null,
    val exitFullScreenIconRes: Int? = null,
    val crossFadeIconRes: Int? = null,

    val shareIconRes: Int? = null,
    val bookmarkIconRes: Int? = null,

)
