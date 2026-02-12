package com.app.videosdk.ui

import android.content.Context
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.app.videosdk.listener.PipListener
import com.app.videosdk.model.PlayerModel
import com.app.videosdk.utils.CastUtils

@Composable
fun TopBar(
    playerModel: PlayerModel? = null,
    title: String,
    isFullScreen: Boolean = false,
    context: Context,
    castUtils: CastUtils,
    pipListener: PipListener?,
    isPipEnabled: (Boolean) -> Unit,
    onBackPressed: () -> Unit,
    onSettingsClick: () -> Unit,
    onFullScreenToggle: () -> Unit,
    onLockScreenToggle: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(top = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        IconButton(onClick = onBackPressed) {
            CustomIcon(
                resId = playerModel?.customControls?.backIconRes,
                defaultIcon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                modifier = Modifier.size(24.dp),
                tint = playerModel?.customControls?.iconTintRes
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

        if (castUtils.isCastTVAvailable(context) && context is FragmentActivity) {
            CastButton()
        }

        // PIP and Settings remain visible only in FullScreen (VOD context)
        if (isFullScreen) {
            PipButton(
                playerModel,
                pipListener = pipListener,
                isPipEnabled = isPipEnabled
            )

            IconButton(onClick = onSettingsClick) {
                CustomIcon(
                    resId = playerModel?.customControls?.settingsIconRes,
                    defaultIcon = Icons.Default.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.size(24.dp),
                    tint = playerModel?.customControls?.iconTintRes
                )
            }
        }

        IconButton(onClick = onLockScreenToggle) {
            CustomIcon(
                resId = playerModel?.customControls?.lockIconRes,
                defaultIcon = Icons.Default.Lock,
                contentDescription = "Toggle Lock Screen",
                modifier = Modifier.size(24.dp),
                tint = playerModel?.customControls?.iconTintRes
            )
        }

        // FIXED: Moved Fullscreen button OUTSIDE the if(isFullScreen) block 
        // to make it visible in Portrait mode.
        IconButton(onClick = onFullScreenToggle) {
            CustomIcon(
                resId = if (isFullScreen) playerModel?.customControls?.exitFullScreenIconRes else playerModel?.customControls?.fullScreenIconRes,
                defaultIcon = if (isFullScreen)Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                contentDescription = "Toggle Fullscreen",
                modifier = Modifier.size(24.dp),
                tint = playerModel?.customControls?.iconTintRes
            )

        }
    }
}