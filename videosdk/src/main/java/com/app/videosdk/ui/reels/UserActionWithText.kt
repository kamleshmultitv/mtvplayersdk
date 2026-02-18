package com.app.videosdk.ui.reels

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.app.videosdk.utils.SingleClickGuard

@Composable
fun UserActionWithText(
    @DrawableRes drawableRes: Int,
    text: String,
    iconColor: Color,
    onClick: () -> Unit // Add a click listener
) {
    val guard = remember { SingleClickGuard() }
    Column(
        modifier = Modifier.clickable {
            guard.tryPerform(action = {
                onClick()
            })
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = drawableRes),
            tint = iconColor,
            modifier = Modifier.size(32.dp),
            contentDescription = null
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}