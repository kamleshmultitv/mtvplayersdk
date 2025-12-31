package com.app.videosdk.utils

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.ima.ImaAdsLoader
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView

object ImaPlayerHelper {

    fun createPlayerWithAds(
        context: Context,
        playerView: PlayerView,
        contentUrl: String,
        adTagUrl: String
    ): Pair<ExoPlayer, ImaAdsLoader> {

        val adsLoader = ImaAdsLoader.Builder(context).build()

        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setAdsLoaderProvider { adsLoader }
            .setAdViewProvider { playerView }

        val player = ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()

        // ✅ FIX: convert String → Uri
        val mediaItem = MediaItem.Builder()
            .setUri(Uri.parse(contentUrl))
            .setAdsConfiguration(
                MediaItem.AdsConfiguration.Builder(
                    Uri.parse(adTagUrl)
                ).build()
            )
            .build()

        adsLoader.setPlayer(player)
        player.setMediaItem(mediaItem)
        player.prepare()

        return player to adsLoader
    }
}
