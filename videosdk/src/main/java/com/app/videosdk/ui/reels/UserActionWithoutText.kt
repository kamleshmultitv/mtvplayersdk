package com.app.videosdk.ui.reels

import android.content.Context
import android.os.Vibrator
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.app.videosdk.utils.SingleClickGuard
import kotlinx.coroutines.launch

@Composable
fun UserActionWithoutText(
    @DrawableRes drawableRes: Int,
    iconColor: Color,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
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
        Icon(
            imageVector = ImageVector.vectorResource(id = drawableRes),
            tint = iconColor,
            modifier = Modifier.size(32.dp),
            contentDescription = null
        )
    }
}