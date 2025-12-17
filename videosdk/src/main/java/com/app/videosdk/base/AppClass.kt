package com.app.videosdk.base

import android.app.Application
import com.google.android.gms.cast.framework.CastContext

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            CastContext.getSharedInstance(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}