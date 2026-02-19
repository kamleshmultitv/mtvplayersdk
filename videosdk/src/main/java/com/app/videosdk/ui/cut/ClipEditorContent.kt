package com.app.videosdk.ui.cut

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.app.videosdk.utils.PlayerUtils.exportClip
import com.app.videosdk.utils.PlayerUtils.formatTime

@Composable
fun ClipEditorContent(
    videoUri: Uri,
    duration: Long
) {

    val context = LocalContext.current

    val clipDuration = 120_000L // 2 minutes
    val maxStart = (duration - clipDuration).coerceAtLeast(0L)

    var clipStart by remember { mutableLongStateOf(0L) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {

        Text(text = "Select 2 minute clip")

        Spacer(modifier = Modifier.height(16.dp))

        Slider(
            value = clipStart.toFloat(),
            onValueChange = {
                clipStart = it.toLong()
            },
            valueRange = 0f..maxStart.toFloat()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Start: ${formatTime(clipStart)}  |  End: ${formatTime(clipStart + clipDuration)}"
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                exportClip(
                    context = context,
                    videoUri = videoUri,
                    clipStart = clipStart
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save & Share")
        }
    }
}
