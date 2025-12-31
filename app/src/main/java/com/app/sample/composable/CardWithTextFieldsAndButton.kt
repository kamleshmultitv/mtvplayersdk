package com.app.sample.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.sample.utils.StreamDetector
import com.app.sample.utils.StreamInfo
import com.app.sample.utils.StreamType
import com.app.sample.utils.PlaybackMode

@Composable
fun CardWithTextFieldsAndButton(
    expanded: (Boolean) -> Unit,
    playContent: (String, String, Boolean) -> Unit
) {
    var textUrl by remember { mutableStateOf("") }
    var drmToken by remember { mutableStateOf("") }
    var streamInfo by remember { mutableStateOf<StreamInfo?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            TextField(
                value = textUrl,
                onValueChange = {
                    textUrl = it
                    streamInfo = null
                },
                label = { Text("Enter DASH / HLS / Live URL") },
                modifier = Modifier.fillMaxWidth()
            )

            // -------- Detect stream --------
            LaunchedEffect(textUrl) {
                if (textUrl.isNotBlank()) {
                    streamInfo = StreamDetector.detectStreamInfo(textUrl)
                }
            }

            // -------- Info --------
            streamInfo?.let {
                Text(
                    text = "Detected: ${it.streamType} • ${it.playbackMode}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // -------- DRM only for DASH --------
            if (streamInfo?.streamType == StreamType.DASH) {
                TextField(
                    value = drmToken,
                    onValueChange = { drmToken = it },
                    label = { Text("Enter DRM Token (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = {
                    expanded(false)
                    playContent(
                        textUrl,
                        drmToken,
                        streamInfo?.playbackMode == PlaybackMode.LIVE
                    )
                },
                enabled = streamInfo != null &&
                        streamInfo?.streamType != StreamType.UNKNOWN,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Play")
            }
        }
    }
}
