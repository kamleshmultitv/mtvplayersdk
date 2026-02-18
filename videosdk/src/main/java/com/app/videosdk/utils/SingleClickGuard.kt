package com.app.videosdk.utils

import android.os.Handler
import android.os.Looper
import java.util.concurrent.atomic.AtomicBoolean

class SingleClickGuard {
    private val clicked = AtomicBoolean(false)

    fun tryPerform(action: () -> Unit, resetAfterMs: Long = 500L) {
        if (!clicked.compareAndSet(false, true)) return
        try {
            action()
        } finally {
            Handler(Looper.getMainLooper()).postDelayed({
                clicked.set(false)
            }, resetAfterMs)
        }
    }
}
