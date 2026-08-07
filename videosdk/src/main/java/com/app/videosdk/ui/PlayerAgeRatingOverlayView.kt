package com.app.videosdk.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.app.videosdk.model.AgeRatingResolver
import com.app.videosdk.model.PlayerModel

/**
 * XML/View equivalent of [PlayerAgeRatingOverlay]. Place this view after the video
 * surface in a [FrameLayout]. It does not consume touch events.
 *
 * Call [showForPlayback] from the player's playback-start/restart event and
 * [setInPictureInPictureMode] from the host's PiP callback.
 */
class PlayerAgeRatingOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr), DefaultLifecycleObserver {

    private val density = resources.displayMetrics.density
    private val baseStartMargin = (64 * density).toInt()
    private val baseTopMargin = (44 * density).toInt()
    private var systemInsets = Insets.NONE
    private var lifecycleOwner: LifecycleOwner? = null
    private var inPictureInPicture = false
    private var pendingRating: String? = null

    private val revealRunnable = Runnable { revealPendingRating() }
    private val hideRunnable = Runnable { collapseToLeft() }

    init {
        setTextColor(Color.WHITE)
        setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f)
        setTypeface(typeface, Typeface.BOLD)
        gravity = Gravity.CENTER
        setPadding(
            (10 * density).toInt(),
            (6 * density).toInt(),
            (10 * density).toInt(),
            (6 * density).toInt()
        )
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 6 * density
            setColor(0xAD000000.toInt())
        }
        alpha = 0f
        visibility = View.GONE
        isClickable = false
        isLongClickable = false
        isFocusable = false

        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            systemInsets = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            updateSafeMargins()
            insets
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        lifecycleOwner = findViewTreeLifecycleOwner()?.also { it.lifecycle.addObserver(this) }
        ViewCompat.requestApplyInsets(this)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(revealRunnable)
        removeCallbacks(hideRunnable)
        animate().cancel()
        lifecycleOwner?.lifecycle?.removeObserver(this)
        lifecycleOwner = null
        super.onDetachedFromWindow()
    }

    override fun onStop(owner: LifecycleOwner) {
        hideImmediately()
    }

    /** Displays the exact backend value for five seconds. */
    fun showForPlayback(video: PlayerModel?) {
        showRating(AgeRatingResolver.resolve(video))
    }

    /** Displays [rating] verbatim; null and blank values remove the overlay. */
    fun showRating(rating: String?) {
        removeCallbacks(revealRunnable)
        removeCallbacks(hideRunnable)
        animate().cancel()
        if (rating.isNullOrBlank() || inPictureInPicture ||
            lifecycleOwner?.lifecycle?.currentState?.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED) == false
        ) {
            hideImmediately()
            return
        }

        pendingRating = rating
        text = null
        visibility = View.GONE
        alpha = 0f
        scaleX = 0f
        postDelayed(revealRunnable, 2_500L)
    }

    /** Hides immediately in PiP; a fresh playback event is required after PiP exits. */
    fun setInPictureInPictureMode(inPictureInPicture: Boolean) {
        this.inPictureInPicture = inPictureInPicture
        if (inPictureInPicture) hideImmediately()
    }

    /** Retained for compatibility. Controls no longer move the shared title/rating slot. */
    @Suppress("UNUSED_PARAMETER")
    fun setPlayerControlsVisible(visible: Boolean) {
        updateSafeMargins()
    }

    private fun revealPendingRating() {
        val rating = pendingRating
        if (rating.isNullOrBlank() || inPictureInPicture ||
            lifecycleOwner?.lifecycle?.currentState?.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED) == false
        ) {
            hideImmediately()
            return
        }

        text = rating
        visibility = View.VISIBLE
        pivotX = 0f
        alpha = 0f
        scaleX = 0f
        animate()
            .alpha(1f)
            .scaleX(1f)
            .setDuration(450L)
            .start()
        postDelayed(hideRunnable, 5_000L)
    }

    private fun collapseToLeft() {
        pivotX = 0f
        animate()
            .alpha(0f)
            .scaleX(0f)
            .setDuration(350L)
            .withEndAction { visibility = View.GONE }
            .start()
    }

    private fun hideImmediately() {
        removeCallbacks(revealRunnable)
        removeCallbacks(hideRunnable)
        animate().cancel()
        alpha = 0f
        scaleX = 0f
        visibility = View.GONE
        text = null
        pendingRating = null
    }

    private fun updateSafeMargins() {
        val params = (layoutParams as? FrameLayout.LayoutParams)
            ?: FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
        params.gravity = Gravity.TOP or Gravity.START
        params.marginStart = baseStartMargin + systemInsets.left
        params.topMargin = baseTopMargin
        layoutParams = params
    }
}
