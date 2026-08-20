package com.app.videosdk.ui.reels

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.app.videosdk.ui.CustomIcon
import com.app.videosdk.utils.SingleClickGuard

@Composable
fun UserActionWithText(
    resId: Int?,
    defaultIcon: ImageVector,
    text: String,
    iconTintRes: Int?,
    onClick: () -> Unit // Add a click listener
) {
    val guard = remember { SingleClickGuard() }
    val contentColor = iconTintRes?.let { colorResource(id = it) } ?: Color.White

    Column(
        modifier = Modifier.clickable {
            guard.tryPerform(action = {
                onClick()
            })
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(ReelsActionDefaults.IconSlotSize),
            contentAlignment = Alignment.Center
        ) {
            CustomIcon(
                resId = resId,
                defaultIcon = defaultIcon,
                modifier = Modifier.size(ReelsActionDefaults.IconSize),
                contentDescription = text,
                tint = iconTintRes
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
