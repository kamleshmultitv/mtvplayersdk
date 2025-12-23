package com.app.videosdk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TopBar(
    title: String,
    onBackPressed: () -> Unit,
    backButtonFocusRequester: FocusRequester,
    playFocusRequester: FocusRequester
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        var isBackFocused by remember { mutableStateOf(false) }
        IconButton(
            onClick = onBackPressed,
            modifier = Modifier
                .focusRequester(backButtonFocusRequester)
                .onFocusChanged { isBackFocused = it.isFocused }
                .background(
                    if (isBackFocused) Color.White.copy(alpha = 0.2f) else Color.Transparent,
                    RoundedCornerShape(8.dp)
                )
                .border(
                    width = if (isBackFocused) 2.dp else 0.dp,
                    color = if (isBackFocused) Color.White else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                )
                .onKeyEvent {
                    when (it.key) {
                        Key.DirectionCenter -> {
                            onBackPressed()
                            true
                        }
                        Key.DirectionDown -> {
                            playFocusRequester.requestFocus()
                            true
                        }
                        else -> false
                    }
                }
                .focusable()
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        Text(
            text = title,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
