package com.app.sample.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.sample.model.ContentItem
import com.app.sample.R

/**
 * Created by kamle on 08,July,2024,artofliving
 */

@Composable
fun ContentCard(
    content: ContentItem?,
    playContent: () -> Unit
) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable {
                playContent()
            },
        textAlign = TextAlign.Start,
        text = content?.title.toString(),
        fontSize = 16.sp,
        color = colorResource(id = R.color.white),
        fontWeight = FontWeight.Medium
    )
}