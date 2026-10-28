package com.app.videosdk.player

import androidx.media3.common.util.UnstableApi
import org.junit.Assert.assertNotNull
import org.junit.Test

@OptIn(UnstableApi::class)
class PlayerFactoryTest {

    @Test
    fun `buildLoadControl creates valid DefaultLoadControl for VOD Video`() {
        val loadControl = PlayerFactory.buildLoadControl(isLive = false, isAudioOnly = false)
        assertNotNull(loadControl)
    }

    @Test
    fun `buildLoadControl creates valid DefaultLoadControl for Audio Only`() {
        val loadControl = PlayerFactory.buildLoadControl(isLive = false, isAudioOnly = true)
        assertNotNull(loadControl)
    }

    @Test
    fun `buildLoadControl creates valid DefaultLoadControl for Live Streaming`() {
        val loadControl = PlayerFactory.buildLoadControl(isLive = true, isAudioOnly = false)
        assertNotNull(loadControl)
    }
}
