package com.app.videosdk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp
import com.app.videosdk.listener.PipListener

@Composable
fun PipButton(pipListener: PipListener? = null, isPipEnabled: (Boolean) -> Unit = {}) {
    var isFocused by remember { mutableStateOf(false) }
    
    IconButton(
        modifier = Modifier
            .wrapContentSize()
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                if (isFocused) Color.White.copy(alpha = 0.2f) else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .border(
                if (isFocused) 2.dp else 0.dp,
                if (isFocused) Color.White else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .onKeyEvent {
                if (it.key == Key.DirectionCenter) {
                    isPipEnabled(true)
                    pipListener?.onPipRequested()
                    true
                } else false
            }
            .focusable(),
        onClick = {
            isPipEnabled(true)
            pipListener?.onPipRequested()
        }
    ) {
        Icon(
            imageVector = Icons.Default.PictureInPicture,
            contentDescription = "Toggle Picture In Picture",
            tint = Color.White
        )
    }
}