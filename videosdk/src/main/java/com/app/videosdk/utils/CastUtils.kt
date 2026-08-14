package com.app.videosdk.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.exoplayer.ExoPlayer
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import com.app.videosdk.model.PlayerModel
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaSeekOptions
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.Session
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.images.WebImage

class CastUtils(context: Context, private val exoPlayer: ExoPlayer) {

    private val sessionManager = CastContext.getSharedInstance(context).sessionManager
    private var playerModel: PlayerModel? = null

    private data class CastMediaSource(
        val url: String,
        val contentType: String,
        val streamType: Int
    )

    private val sessionListener = object : SessionManagerListener<Session> {
        override fun onSessionStarted(session: Session, sessionId: String) {
            resumeCasting()
        }

        override fun onSessionResumed(session: Session, wasSuspended: Boolean) {
            resumeCasting()
        }

        override fun onSessionEnded(session: Session, error: Int) {
            stopCasting()
        }

        override fun onSessionSuspended(session: Session, reason: Int) {
            sessionManager.currentCastSession?.let {
                if (it.isConnected) {
                    resumeCasting()
                } else {
                    startCasting(false)
                }
            }
        }

        override fun onSessionStarting(session: Session) {}
        override fun onSessionResuming(session: Session, sessionId: String) {}
        override fun onSessionEnding(session: Session) {}
        override fun onSessionResumeFailed(session: Session, error: Int) {}
        override fun onSessionStartFailed(session: Session, error: Int) {}
    }

    private fun startOrRestartCastSession(model: PlayerModel, reset: Boolean) {
        val mediaClient = getRemoteMediaClient() ?: return
        val mediaSource = resolveCastMediaSource(model) ?: run {
            Log.e(TAG, "Cast load skipped: no playable URL for content id=${model.id}")
            return
        }

        // Get the current playback position from ExoPlayer if available
        var currentPosition: Long = exoPlayer.currentPosition

        // If a Cast session already exists, try getting the position from the Cast client
        if (mediaClient.hasMediaSession()) {
            currentPosition = mediaClient.approximateStreamPosition
        }

        pauseLocalPlayback()
        val mediaInfo = MediaInfo.Builder(mediaSource.url).apply {
            setStreamType(mediaSource.streamType)
            setContentType(mediaSource.contentType)
            setMetadata(buildMediaMetadata(model))
        }.build()

        val mediaLoadRequestData = MediaLoadRequestData.Builder().apply {
            setMediaInfo(mediaInfo)
            setAutoplay(true)
            setCurrentTime(
                if (reset) {
                    0L
                } else {
                    currentPosition
                }
            ) // Seek to the last known position
        }.build()

        Log.d(
            TAG,
            "Loading cast media: contentType=${mediaSource.contentType}, reset=$reset, position=$currentPosition, url=${mediaSource.url}"
        )

        mediaClient.load(mediaLoadRequestData).setResultCallback { result ->
            val status = result.status
            if (status.isSuccess) {
                Log.d(TAG, "Cast media load succeeded")
            } else {
                Log.e(
                    TAG,
                    "Cast media load failed: code=${status.statusCode}, message=${status.statusMessage}"
                )
            }
        }
    }


    /**
     * Sets up the Cast session only when an episode changes
     */
    fun setupCastSession(newPlayerModel: PlayerModel?) {
        if (newPlayerModel == null || newPlayerModel == playerModel) return // Avoid redundant setup

        this.playerModel = newPlayerModel

        sessionManager.apply {
            removeSessionManagerListener(sessionListener, Session::class.java)
            addSessionManagerListener(sessionListener, Session::class.java)
        }

        sessionManager.currentCastSession?.let {
            startCasting(true)
        }
    }

    fun release() {
        sessionManager.removeSessionManagerListener(sessionListener, Session::class.java)
    }

    private fun startCasting(reset: Boolean) {
        playerModel?.let { model ->
            startOrRestartCastSession(model, reset)
        }
    }

    private fun stopCasting() {
        stopMediaOnCast()
        resumeLocalPlayback()
    }

    private fun resumeCasting() {
        sessionManager.currentCastSession?.let {
            if (playerModel != null) {
                startCasting(false)
            }
        }
    }

    private fun buildMediaMetadata(model: PlayerModel): MediaMetadata {
        return MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE).apply {
            putString(
                MediaMetadata.KEY_TITLE,
                model.episodeTitle
                    ?: model.title
                    ?: model.seasonTitle
                    ?: "Unknown Title"
            )
            val subtitle = model.episodeDescription
                ?: model.description
                ?: model.seasonDescription
            subtitle?.let { putString(MediaMetadata.KEY_SUBTITLE, it) }

            val imageUrl = model.imageUrl ?: model.thumbnail
            if (!imageUrl.isNullOrBlank()) {
                addImage(WebImage(Uri.parse(imageUrl)))
            }
        }
    }

    private fun resolveCastMediaSource(model: PlayerModel): CastMediaSource? {
        val streamType =
            if (model.isLive) MediaInfo.STREAM_TYPE_LIVE else MediaInfo.STREAM_TYPE_BUFFERED

        val urlAndType = when {
            model.isLive && !model.liveUrl.isNullOrBlank() ->
                model.liveUrl to HLS_MIME_TYPE

            model.drm == "1" && !model.mpdUrl.isNullOrBlank() ->
                model.mpdUrl to DASH_MIME_TYPE

            !model.hlsUrl.isNullOrBlank() ->
                model.hlsUrl to HLS_MIME_TYPE

            !model.mpdUrl.isNullOrBlank() ->
                model.mpdUrl to DASH_MIME_TYPE

            !model.videoUrl.isNullOrBlank() ->
                model.videoUrl to inferContentType(model.videoUrl)

            else -> null
        } ?: return null

        return CastMediaSource(
            url = urlAndType.first.trim(),
            contentType = urlAndType.second,
            streamType = streamType
        )
    }

    private fun inferContentType(url: String): String {
        val path = url.substringBefore("?").lowercase()
        return when {
            path.endsWith(".m3u8") -> HLS_MIME_TYPE
            path.endsWith(".mpd") -> DASH_MIME_TYPE
            else -> MP4_MIME_TYPE
        }
    }

    fun pauseCasting() {
        getRemoteMediaClient()?.pause()
    }

    fun playCasting() {
        getRemoteMediaClient()?.play()
    }

    private fun stopMediaOnCast() {
        getRemoteMediaClient()?.stop()
    }

    fun seekOnCast(position: Long) {
        val mediaClient = getRemoteMediaClient()
        if (mediaClient != null && mediaClient.hasMediaSession()) {
            mediaClient.seek(MediaSeekOptions.Builder().setPosition(position).build())
        }
    }
    fun muteOnCast(mute: Boolean) {
        getRemoteMediaClient()?.setStreamMute(mute)
    }

    private fun getRemoteMediaClient(): RemoteMediaClient? {
        return sessionManager.currentCastSession?.remoteMediaClient
    }

    private fun pauseLocalPlayback() {
        exoPlayer.pause()
    }

    private fun resumeLocalPlayback() {
        if (!isCasting()){
            exoPlayer.play()
        }
    }

    fun isCasting(): Boolean {
        val remoteMediaClient = getRemoteMediaClient()
        return remoteMediaClient?.hasMediaSession() == true
    }

    fun getCastPosition(): Long {
        val remoteMediaClient = getRemoteMediaClient()
        return remoteMediaClient?.approximateStreamPosition ?: 0L
    }

    fun getCastDuration(): Long {
        val remoteMediaClient = getRemoteMediaClient()
        return remoteMediaClient?.mediaInfo?.streamDuration ?: 0L
    }

    fun isCastTVAvailable(context: Context): Boolean {
        val mediaRouter = MediaRouter.getInstance(context)
        val selector = MediaRouteSelector.Builder()
            .addControlCategory(CastMediaControlIntent.categoryForCast(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID))
            .build()
        mediaRouter.addCallback(selector, object : MediaRouter.Callback() {}, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY)
        return mediaRouter.routes.any { route ->
            route.isEnabled && route.matchesSelector(selector)
        }
    }

    companion object {
        private const val TAG = "CastUtils"
        private const val HLS_MIME_TYPE = "application/x-mpegURL"
        private const val DASH_MIME_TYPE = "application/dash+xml"
        private const val MP4_MIME_TYPE = "video/mp4"
    }
}
