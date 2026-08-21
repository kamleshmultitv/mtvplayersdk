package com.app.reelssdk.listener

interface AdsListener {
    fun onAdsLoaded()
    fun onAdStarted()
    fun onAdCompleted()
    fun onAllAdsCompleted()
    fun onAdError(message: String)

}
