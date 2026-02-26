package com.app.sample

import android.app.PictureInPictureParams
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.mutableStateOf
import com.app.mtvdownloader.utils.NotificationPermission
import com.app.sample.composable.ContentScreen
import com.app.sample.viewModel.ContentViewModel
import com.app.videosdk.listener.PipListener

class MainActivity : ComponentActivity(), PipListener {

    private val viewModel: ContentViewModel by viewModels()

    private val pipState = mutableStateOf(false)

    // ✅ Deep link states
    private val isDeepLinkState = mutableStateOf(false)
    private val deepLinkContentId = mutableStateOf<String?>(null)
    private val deepLinkUrl = mutableStateOf<String?>(null)
    private val deepLinkStart = mutableStateOf(0L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleDeepLink(intent)

        NotificationPermission.requestIfRequired(this)
        enableEdgeToEdge()

        setContent {
            ContentScreen(
                viewModel = viewModel,
                pipListener = this@MainActivity,
                isInPipMode = pipState.value,
                isDeepLink = isDeepLinkState.value,
                deepLinkContentId = deepLinkContentId.value,
                deepLinkUrl = deepLinkUrl.value,
                deepLinkStart = deepLinkStart.value
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

    override fun onPipRequested(isPipActive: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        }
    }

    @Deprecated("Deprecated in android.app.Activity")
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        pipState.value = isInPictureInPictureMode
    }
}