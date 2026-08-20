package com.app.videosdk.ui.reels

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.app.videosdk.ui.CustomIcon
import com.app.videosdk.utils.SingleClickGuard
import kotlinx.coroutines.launch

@Composable
fun UserActionWithoutText(
    resId: Int?,
    defaultIcon: ImageVector,
    iconTintRes: Int?,
    contentDescription: String? = null,
    onClick: () -> Unit
) {
    val guard = remember { SingleClickGuard() }

    val alpha = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .graphicsLayer { this.alpha = alpha.value }
            .clickable {
                guard.tryPerform(action = {
                    scope.launch {
                        alpha.animateTo(
                            targetValue = 0.35f,
                            animationSpec = tween(durationMillis = 80)
                        )
                        alpha.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = 160)
                        )
                    }
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
                contentDescription = contentDescription,
                tint = iconTintRes
            )
        }
    }
}
