package com.app.reelssdk.model

data class GAMAdsConfig(
    val verticalBan: String? = "/21775744923/example/fixed-size-banner",
    val horizontalBan: String? = "/21775744923/example/fixed-size-banner",
    val timeIntervalInMilliseconds: Long = 10000,
    val isAdsEnabled: Boolean = false
)
