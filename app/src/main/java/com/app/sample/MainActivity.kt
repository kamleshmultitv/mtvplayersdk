package com.app.sample

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.fragment.app.FragmentActivity
import com.app.mtvdownloader.utils.NotificationPermission
import com.app.sample.composable.ContentScreen
import com.app.sample.viewModel.ContentViewModel

class MainActivity : FragmentActivity() {

    private val viewModel: ContentViewModel by viewModels()

    // ✅ Deep link states
    private val isDeepLinkState = mutableStateOf(false)
    private val deepLinkContentId = mutableStateOf<String?>(null)
    private val deepLinkUrl = mutableStateOf<String?>(null)
    private val deepLinkStart = mutableLongStateOf(0L)
    private val deepLinkEnd = mutableLongStateOf(0L)
    private val deepLinkTotalClipDuration = mutableLongStateOf(0L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleDeepLink(intent)

        NotificationPermission.requestIfRequired(this)

        setContent {
            ContentScreen(
                viewModel = viewModel,
                isDeepLink = isDeepLinkState.value,
                deepLinkContentId = deepLinkContentId.value,
                deepLinkUrl = deepLinkUrl.value,
                deepLinkStart = deepLinkStart.longValue,
                deepLinkEnd = deepLinkEnd.longValue,
                deepLinkTotalClipDuration = deepLinkTotalClipDuration.longValue
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {

        val data = intent?.data ?: return

        if (data.host == "www.artofliving.app") {

            val contentId = data.getQueryParameter("contentId")
            val url = data.getQueryParameter("url")
            val start = data.getQueryParameter("start")?.toLongOrNull() ?: 0L
            val end = data.getQueryParameter("end")?.toLongOrNull() ?: 0L
            val totalClipDuration = data.getQueryParameter("totalClipDuration")?.toLongOrNull() ?: 0L

            if (!contentId.isNullOrEmpty()) {

                isDeepLinkState.value = true
                deepLinkContentId.value = contentId
                deepLinkUrl.value = url
                deepLinkStart.value = start

                Toast.makeText(
                    this,
                    "DeepLink -> url: $url, contentId: $contentId, start: $start",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
