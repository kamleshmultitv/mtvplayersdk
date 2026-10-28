package com.app.videosdk.utils

import android.content.Context
import java.util.concurrent.TimeUnit

object LowBandwidthHintManager {
    private const val PREF_NAME = "videosdk_low_bandwidth_hint_prefs"
    private const val KEY_LAST_DAY = "last_hint_day"
    private const val KEY_COUNT_TODAY = "hint_count_today"
    private const val MAX_HINTS_PER_DAY = 3

    fun canShowHint(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val currentDay = System.currentTimeMillis() / TimeUnit.DAYS.toMillis(1)
        val lastDay = prefs.getLong(KEY_LAST_DAY, 0L)
        val countToday = prefs.getInt(KEY_COUNT_TODAY, 0)

        if (currentDay != lastDay) {
            return true
        }
        return countToday < MAX_HINTS_PER_DAY
    }

    fun recordHintShown(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val currentDay = System.currentTimeMillis() / TimeUnit.DAYS.toMillis(1)
        val lastDay = prefs.getLong(KEY_LAST_DAY, 0L)
        val countToday = prefs.getInt(KEY_COUNT_TODAY, 0)

        if (currentDay != lastDay) {
            prefs.edit()
                .putLong(KEY_LAST_DAY, currentDay)
                .putInt(KEY_COUNT_TODAY, 1)
                .apply()
        } else if (countToday < MAX_HINTS_PER_DAY) {
            prefs.edit()
                .putInt(KEY_COUNT_TODAY, countToday + 1)
                .apply()
        }
    }
}
