package com.app.sample.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CardWithTextFieldsAndButton(expanded: (Boolean) -> Unit,   playContent: (String, String, String) -> Unit) {
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
            var textUrl by remember { mutableStateOf("https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8") }
            var sprite by remember { mutableStateOf("") }
            var drmToken by remember { mutableStateOf("") }

            TextField(
                value = textUrl,
                onValueChange = { textUrl = it },
                label = { Text("Please enter DASH/HLS") },
                modifier = Modifier.fillMaxWidth()
            )

            TextField(
                value = sprite,
                onValueChange = { sprite = it },
                label = { Text("Please enter Sprite Url") },
                modifier = Modifier.fillMaxWidth()
            )

            TextField(
                value = drmToken,
                onValueChange = { drmToken = it },
                label = { Text("Please Enter Drm Token") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    expanded(false)
                    playContent(textUrl, sprite, drmToken) },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Play")
            }
        }
    }
}