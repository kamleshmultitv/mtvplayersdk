package com.app.videosdk.ui

import android.content.Context
import android.content.ContextWrapper
import android.view.ContextThemeWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.mediarouter.app.MediaRouteButton
import com.app.videosdk.R
import com.google.android.gms.cast.framework.CastButtonFactory

@Composable
fun CastButton() {
    val context = LocalContext.current
    val fragmentActivity = remember(context) { context.findFragmentActivity() } ?: return
    val themedContext =
        remember(fragmentActivity) { ContextThemeWrapper(fragmentActivity, R.style.ThemeOverlay_CastButton) }
    val mediaRouteButton = remember {
        MediaRouteButton(themedContext).apply {
            CastButtonFactory.setUpMediaRouteButton(themedContext, this)
        }
    }
    AndroidView(
        factory = { mediaRouteButton }
    )
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity? {
    return when (this) {
        is FragmentActivity -> this
        is ContextWrapper -> baseContext.findFragmentActivity()
        else -> null
    }
}
