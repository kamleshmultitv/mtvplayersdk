package com.app.videosdk.service

import android.content.Context
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import com.google.common.collect.ImmutableList

class MuteMediaNotificationProvider(context: Context) : DefaultMediaNotificationProvider(context) {

    override fun getMediaButtons(
        session: MediaSession,
        playerCommands: androidx.media3.common.Player.Commands,
        mediaButtonPreferences: ImmutableList<CommandButton>,
        showPauseButton: Boolean
    ): ImmutableList<CommandButton> {
        val buttons = super.getMediaButtons(session, playerCommands, mediaButtonPreferences, showPauseButton)
        val player = session.player
        val isMuted = player.volume == 0f
        val icon = if (isMuted) CommandButton.ICON_VOLUME_OFF else CommandButton.ICON_VOLUME_UP
        val displayName = if (isMuted) "Unmute" else "Mute"
        val muteButton = CommandButton.Builder(icon)
            .setSessionCommand(androidx.media3.session.SessionCommand(VideosdkAudioService.ACTION_TOGGLE_MUTE, android.os.Bundle.EMPTY))
            .setDisplayName(displayName)
            .build()
        return ImmutableList.builder<CommandButton>().addAll(buttons).add(muteButton).build()
    }
}
