package com.app.sample.composable

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.focusable
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.sample.R
import com.app.sample.model.ContentItem

/**
 * Created by kamle on 08,July,2024,artofliving
 */

@Composable
fun ContentCard(
    content: ContentItem?,
    modifier: Modifier = Modifier,
    playContent: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1f,
        label = "card-focus-scale"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .scale(scale)
            .onFocusChanged { isFocused = it.hasFocus }
            .clickable { playContent() }
            .focusable()
            .background(
                if (isFocused) colorResource(id = R.color.purple_200).copy(alpha = 0.2f)
                else Color.Transparent
            ),
        tonalElevation = if (isFocused) 6.dp else 0.dp,
        shadowElevation = if (isFocused) 3.dp else 0.dp
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            textAlign = TextAlign.Start,
            text = content?.title.orEmpty(),
            fontSize = 18.sp,
            color = colorResource(id = R.color.white),
            fontWeight = FontWeight.Medium
        )
    }
}