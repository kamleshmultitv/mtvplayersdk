package com.app.videosdk.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.MediaStyleNotificationHelper

class VideosdkAudioService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    companion object {
        var currentInstance: VideosdkAudioService? = null
            private set
        var activePlayer: Player? = null
            set(value) {
                field = value
                currentInstance?.updatePlayer(value)
            }

        private const val CHANNEL_ID = "videosdk_audio"
        private const val NOTIFICATION_ID = 1001
        const val EXTRA_FROM_APP = "extra_from_app"
        const val ACTION_TOGGLE_MUTE = "action_toggle_mute"
        const val ACTION_PLAY_PAUSE = "action_play_pause"
        const val ACTION_MUTE = ACTION_TOGGLE_MUTE
    }

    override fun onCreate() {
        super.onCreate()
        currentInstance = this
        activePlayer?.let { updatePlayer(it) }
    }

    private fun updatePlayer(player: Player?) {
        mediaSession?.let {
            removeSession(it)
            it.release()
            mediaSession = null
        }
        mediaSession = if (player != null) {
            MediaSession.Builder(this, player).build().also { addSession(it) }
        } else {
            null
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        val notification = buildNotification(session)
        if (startInForegroundRequired) {
            startForeground(NOTIFICATION_ID, notification)
        } else {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(session: MediaSession): android.app.Notification {
        val player = session.player
        val isPlaying = player.isPlaying
        val isMuted = player.volume == 0f

        val playPauseIntent = Intent(this, VideosdkAudioService::class.java).apply {
            action = ACTION_PLAY_PAUSE
        }
        val muteIntent = Intent(this, VideosdkAudioService::class.java).apply {
            action = ACTION_MUTE
        }
        val playPausePending = PendingIntent.getService(
            this, 0, playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val mutePending = PendingIntent.getService(
            this, 1, muteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseAction = NotificationCompat.Action(
            if (isPlaying) androidx.media3.session.R.drawable.media3_icon_pause else androidx.media3.session.R.drawable.media3_icon_play,
            if (isPlaying) "Pause" else "Play",
            playPausePending
        )
        val muteAction = NotificationCompat.Action(
            if (isMuted) androidx.media3.session.R.drawable.media3_icon_volume_off else androidx.media3.session.R.drawable.media3_icon_volume_up,
            if (isMuted) "Unmute" else "Mute",
            mutePending
        )

        val style = MediaStyleNotificationHelper.MediaStyle(session)
            .setShowActionsInCompactView(0, 1)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(androidx.media3.session.R.drawable.media3_icon_play)
            .setContentTitle(player.mediaMetadata.title ?: "Playing audio")
            .setContentText(player.mediaMetadata.artist)
            .setStyle(style)
            .addAction(playPauseAction)
            .addAction(muteAction)
            .setOngoing(true)
            .setOnlyAlertOnce(true)

        return builder.build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_PLAY_PAUSE -> {
                mediaSession?.player?.let { p ->
                    if (p.isPlaying) p.pause() else p.play()
                }
                triggerNotificationUpdate()
            }
            ACTION_MUTE -> {
                mediaSession?.player?.let { p ->
                    p.volume = if (p.volume == 0f) 1f else 0f
                }
                triggerNotificationUpdate()
            }
        }

        val fromApp = intent?.getBooleanExtra(EXTRA_FROM_APP, false) == true
        if (fromApp) {
            val session = mediaSession
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                    manager.createNotificationChannel(
                        NotificationChannel(CHANNEL_ID, "Audio playback", NotificationManager.IMPORTANCE_LOW)
                    )
                }
                if (session != null) {
                    startForeground(NOTIFICATION_ID, buildNotification(session))
                } else {
                    val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle("Playing audio")
            .setSmallIcon(androidx.media3.session.R.drawable.media3_icon_play)
                        .setOngoing(true)
                        .build()
                    startForeground(NOTIFICATION_ID, notification)
                }
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        mediaSession?.let {
            removeSession(it)
            it.release()
            mediaSession = null
        }
        currentInstance = null
        super.onDestroy()
    }
}
