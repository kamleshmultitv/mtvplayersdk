package com.app.videosdk.ui

import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.videosdk.utils.PlayerUtils.setScreenBrightness

@Composable
fun CustomBrightnessController(
    modifier: Modifier = Modifier,
    onShowControls: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current

    // Get system brightness as default
    val systemBrightness = remember {
        getSystemBrightness(context) // Retrieve system brightness
    }

    val currentBrightness = remember { mutableFloatStateOf(systemBrightness) }
    val brightnessPercentage = (currentBrightness.floatValue * 100).toInt()
    val isDragging = remember { mutableStateOf(false) }

    // Detect touch gestures to increase/decrease brightness
    val dragGesture = remember {
        Modifier.pointerInput(Unit) {
            detectVerticalDragGestures(
                onDragStart = {
                    isDragging.value = true
                    onShowControls(true) // Keep controller visible
                },
                onDragEnd = {
                    isDragging.value = false
                    onShowControls(false) // Hide only after dragging stops
                },
                onVerticalDrag = { _, dragAmount ->
                    val newBrightness =
                        (currentBrightness.floatValue - dragAmount / 500).coerceIn(0f, 1f)
                    currentBrightness.floatValue = newBrightness
                    setScreenBrightness(context, newBrightness)
                }
            )
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .background(Color.Transparent)
            .padding(16.dp)
            .size(60.dp, 200.dp) // Custom slider size
            .then(dragGesture) // Attach gesture detector
    ) {

        IconButton(
            modifier = Modifier
                .wrapContentSize()
                .size(24.dp),
            onClick = {

            }
        ) {
            Icon(
                imageVector = Icons.Default.Brightness6,
                contentDescription = "Brightness",
                modifier = Modifier.size(24.dp),
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Custom Brightness Indicator Bar
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(120.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Yellow)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(1f - currentBrightness.floatValue)
                    .background(Color.Gray, shape = RoundedCornerShape(4.dp))
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Brightness Percentage
        Text(
            text = "$brightnessPercentage%",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Retrieves the system screen brightness and converts it to a float (0f - 1f).
 */
fun getSystemBrightness(context: Context): Float {
    return try {
        val brightnessInt =
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        brightnessInt / 255f // Convert to range 0f - 1f
    } catch (e: Exception) {
        0.5f // Default to 50% if retrieval fails
    }
}
