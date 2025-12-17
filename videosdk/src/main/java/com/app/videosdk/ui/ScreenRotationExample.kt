package com.app.videosdk.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration

@Composable
fun ScreenRotationExample(setOrientation: (Boolean) -> Unit) {
    val configuration = LocalConfiguration.current
    var lastOrientation by remember { mutableIntStateOf(configuration.orientation) }

    // Detect Orientation Change
    if (lastOrientation != configuration.orientation) {
        lastOrientation = configuration.orientation
        when (configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> setOrientation(true)
            Configuration.ORIENTATION_PORTRAIT -> setOrientation(false)
            Configuration.ORIENTATION_SQUARE -> {

            }

            Configuration.ORIENTATION_UNDEFINED -> {

            }
        }
    }
}