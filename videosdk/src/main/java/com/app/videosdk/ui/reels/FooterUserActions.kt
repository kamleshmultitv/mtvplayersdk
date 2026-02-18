package com.app.videosdk.ui.reels

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.app.videosdk.R


@Composable
fun FooterUserActions(
    onLikeClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(end = 12.dp)
    ) {

        UserActionWithText(
            drawableRes = R.drawable.ic_share,
            text = "Share",
            iconColor = Color.White,
            onClick = onShareClick
        )

        Spacer(modifier = Modifier.height(20.dp))

        UserActionWithText(
            drawableRes = R.drawable.ic_settings,
            text = "Settings",
            iconColor = Color.White,
            onClick = onSettingsClick
        )

        Spacer(modifier = Modifier.height(20.dp))

        UserActionWithoutText(
            drawableRes = R.drawable.ic_bookmark,
            iconColor = Color.White,
            onClick = onLikeClick
        )

        Spacer(modifier = Modifier.height(72.dp))
    }
}
