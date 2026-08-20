package com.app.videosdk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.videosdk.model.PlayerModel

@Composable
fun TopBar(
    playerModel: PlayerModel? = null,
    isFullScreen: Boolean = false,
    showContentTitle: Boolean = true,
    onBackPressed: () -> Unit
) {
    val episodeTitle = playerModel?.episodeTitle?.takeIf { it.isNotBlank() }
    val seasonTitle = playerModel?.seasonTitle?.takeIf { it.isNotBlank() }
    val heading = episodeTitle
        ?: playerModel?.title?.takeIf { it.isNotBlank() }
        ?: seasonTitle
    val subheading = seasonTitle?.takeUnless { it == heading }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isFullScreen) {
                    Modifier.background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.70f),
                                Color.Black.copy(alpha = 0.35f),
                                Color.Transparent
                            )
                        )
                    )
                } else {
                    Modifier
                }
            )
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .then(if (isFullScreen) Modifier.statusBarsPadding() else Modifier)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackPressed) {
            CustomIcon(
                resId = playerModel?.customControls?.backIconRes,
                defaultIcon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = playerModel?.customControls?.iconTintRes
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(start = 2.dp, end = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (showContentTitle) {
                Column {
                    heading?.let {
                        Text(
                            text = it,
                            color = Color.White,
                            fontSize = if (isFullScreen) 16.sp else 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (isFullScreen) {
                        subheading?.let {
                            Text(
                                text = it,
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
