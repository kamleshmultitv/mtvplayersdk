package com.app.reelssdk.ui

import android.content.Context
import android.os.Looper
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.app.reelssdk.model.ReelsPlayerModel

/**
 * XML-compatible player host.
 *
 * Add this view to XML and call `player.play(video)`. The complete [ReelsPlayerModel],
 * including its backend-provided age rating, is passed into the SDK renderer.
 */
class MtvReelsPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val currentVideo = mutableStateOf<ReelsPlayerModel?>(null)

    private val composeView = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            val video = currentVideo.value ?: return@setContent
            MtvReelsPlayerSdk(
                contentList = listOf(video),
                index = 0,
                onPlayerBack = { onBackRequested?.invoke() },
                setFullScreen = { onFullScreenChanged?.invoke(it) }
            )
        }
    }

    /** Optional callbacks for controls owned by the host Activity/Fragment. */
    var onBackRequested: (() -> Unit)? = null
    var onFullScreenChanged: ((Boolean) -> Unit)? = null

    init {
        addView(composeView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    /** Starts a video and reads all player metadata directly from [video]. */
    fun play(video: ReelsPlayerModel) {
        runOnMainThread { currentVideo.value = video }
    }

    private inline fun runOnMainThread(crossinline action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) action() else post { action() }
    }
}
