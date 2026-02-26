package com.app.videosdk.ui.cut

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.app.videosdk.utils.PlayerUtils
import com.app.videosdk.utils.PlayerUtils.formatTime

@Composable
fun ClipEditorContent(
    contentId: String? = null,
    url: String? = null,
    duration: Long
) {

    val context = LocalContext.current


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {

        Text("Select clip (30s - 2m)")

        Spacer(modifier = Modifier.height(12.dp))

        var startMs by remember { mutableLongStateOf(0L) }
        var endMs by remember { mutableLongStateOf(120_000L) }

        YoutubeStyleTrimBar(
            duration = duration,
            modifier = Modifier.fillMaxWidth()
        ) { start, end ->
            startMs = start
            endMs = end
        }
        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                val shareUrl = PlayerUtils.createShareUrl(
                    contentId = contentId,
                    url = url,
                    clipStart = startMs,
                    clipEnd = endMs
                )
                PlayerUtils.shareLink(context, shareUrl)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Share Clip")
        }
    }
}