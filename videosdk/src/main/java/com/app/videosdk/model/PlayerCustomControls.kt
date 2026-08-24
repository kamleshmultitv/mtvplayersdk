package com.app.videosdk.model

import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes

data class PlayerCustomControls(

    // 🎯 Global Tint (applies to all icons if provided)
    @param:ColorRes val iconTintRes: Int? = null,

    // Center Controls
    @param:DrawableRes val playIconRes: Int? = null,
    @param:DrawableRes val pauseIconRes: Int? = null,
    @param:DrawableRes val forwardIconRes: Int? = null,
    @param:DrawableRes val rewindIconRes: Int? = null,

    // Top Bar
    @param:DrawableRes val backIconRes: Int? = null,
    @param:DrawableRes val settingsIconRes: Int? = null,
    @param:DrawableRes val castIconRes: Int? = null,
    @param:DrawableRes val pipIconRes: Int? = null,
    @param:DrawableRes val fullScreenIconRes: Int? = null,
    @param:DrawableRes val exitFullScreenIconRes: Int? = null,

    // Lock Controls
    @param:DrawableRes val lockIconRes: Int? = null,
    @param:DrawableRes val unlockIconRes: Int? = null,

    @param:DrawableRes val muteIconRes: Int? = null,
    @param:DrawableRes val unMuteIconRes: Int? = null,
    @param:DrawableRes val crossFadeIconRes: Int? = null,
    @param:DrawableRes val seasonSelectorIconRes: Int? = null,
    @param:DrawableRes val brightnessIconRes: Int? = null,
    @param:DrawableRes val nextEpisodeIconRes: Int? = null,

    @param:DrawableRes val previousEpisodeIconRes: Int? = null,
    @param:DrawableRes val cutIconRes: Int? = null,
    @param:DrawableRes val chapterIconRes: Int? = null,

    @param:ColorInt val iconTintColorInt: Int? = null,
    val iconTintHex: String? = null,

    val playIconUrl: String? = null,
    val pauseIconUrl: String? = null,
    val forwardIconUrl: String? = null,
    val rewindIconUrl: String? = null,
    val backIconUrl: String? = null,
    val settingsIconUrl: String? = null,
    val castIconUrl: String? = null,
    val pipIconUrl: String? = null,
    val fullScreenIconUrl: String? = null,
    val exitFullScreenIconUrl: String? = null,
    val lockIconUrl: String? = null,
    val unlockIconUrl: String? = null,
    val muteIconUrl: String? = null,
    val unMuteIconUrl: String? = null,
    val crossFadeIconUrl: String? = null,
    val seasonSelectorIconUrl: String? = null,
    val brightnessIconUrl: String? = null,
    val nextEpisodeIconUrl: String? = null,
    val previousEpisodeIconUrl: String? = null,
    val cutIconUrl: String? = null,
    val chapterIconUrl: String? = null,

)
