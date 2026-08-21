package com.app.reelssdk.ui.reels

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.ImageView
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.app.reelssdk.model.ReelsPlayerModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelsShareSheet(
    playerModel: ReelsPlayerModel?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val shareText = remember(playerModel) { playerModel.buildShareText() }
    val targets = remember(context, shareText) {
        buildShareTargets(context = context, shareText = shareText)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF111111),
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 28.dp)
        ) {
            Text(
                text = "Share",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(18.dp))

            if (targets.isEmpty()) {
                Text(
                    text = "No share apps available",
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(22.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    targets.forEach { target ->
                        ShareTargetButton(
                            target = target,
                            onClick = {
                                launchShareTarget(
                                    context = context,
                                    target = target,
                                    shareText = shareText
                                )
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShareTargetButton(
    target: ShareTarget,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .size(width = 72.dp, height = 88.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            if (target.icon != null) {
                AndroidView(
                    factory = { viewContext ->
                        ImageView(viewContext).apply {
                            scaleType = ImageView.ScaleType.CENTER_INSIDE
                        }
                    },
                    update = { imageView ->
                        imageView.setImageDrawable(target.icon)
                    },
                    modifier = Modifier.size(34.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = target.label,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = target.label,
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

private data class ShareTarget(
    val label: String,
    val packageName: String?,
    val icon: Drawable?
)

private data class ShareTargetDefinition(
    val label: String,
    val packageNames: List<String>
)

private val appShareTargets = listOf(
    ShareTargetDefinition(
        label = "WhatsApp",
        packageNames = listOf("com.whatsapp", "com.whatsapp.w4b")
    ),
    ShareTargetDefinition(
        label = "Facebook",
        packageNames = listOf("com.facebook.katana", "com.facebook.lite")
    ),
    ShareTargetDefinition(
        label = "Instagram",
        packageNames = listOf("com.instagram.android")
    )
)

private fun buildShareTargets(
    context: Context,
    shareText: String
): List<ShareTarget> {
    val packageManager = context.packageManager
    val targets = appShareTargets.mapNotNull { definition ->
        definition.packageNames.firstNotNullOfOrNull { packageName ->
            val intent = createShareIntent(shareText).setPackage(packageName)
            if (intent.resolveActivity(packageManager) == null) {
                null
            } else {
                val appInfo = packageManager.getApplicationInfoCompat(packageName) ?: return@firstNotNullOfOrNull null
                ShareTarget(
                    label = definition.label,
                    packageName = packageName,
                    icon = packageManager.getApplicationIcon(appInfo)
                )
            }
        }
    }.toMutableList()

    if (createShareIntent(shareText).resolveActivity(packageManager) != null) {
        targets.add(
            ShareTarget(
                label = "More",
                packageName = null,
                icon = null
            )
        )
    }

    return targets
}

private fun launchShareTarget(
    context: Context,
    target: ShareTarget,
    shareText: String
) {
    val sendIntent = createShareIntent(shareText).apply {
        target.packageName?.let { setPackage(it) }
        addNewTaskFlagIfNeeded(context)
    }

    val finalIntent =
        if (target.packageName == null) {
            Intent.createChooser(sendIntent, "Share content").apply {
                addNewTaskFlagIfNeeded(context)
            }
        } else {
            sendIntent
        }

    try {
        context.startActivity(finalIntent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "Share app is not available", Toast.LENGTH_SHORT).show()
    }
}

private fun createShareIntent(shareText: String): Intent =
    Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }

private fun ReelsPlayerModel?.buildShareText(): String {
    if (this == null) return "Check this content"

    val title = episodeTitle?.takeIf { it.isNotBlank() }
        ?: title?.takeIf { it.isNotBlank() }
    val url = shareUrl?.takeIf { it.isNotBlank() }
        ?: videoUrl?.takeIf { it.isNotBlank() }
        ?: hlsUrl?.takeIf { it.isNotBlank() }
        ?: mpdUrl?.takeIf { it.isNotBlank() }
        ?: liveUrl?.takeIf { it.isNotBlank() }
        ?: id?.takeIf { it.isNotBlank() }?.let { "https://www.artofliving.app/watch?contentId=$it" }

    return listOfNotNull(title, url)
        .joinToString(separator = "\n")
        .ifBlank { "Check this content" }
}

private fun Intent.addNewTaskFlagIfNeeded(context: Context) {
    if (context !is Activity) {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

private fun PackageManager.getApplicationInfoCompat(packageName: String): ApplicationInfo? =
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            getApplicationInfo(packageName, 0)
        }
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }
