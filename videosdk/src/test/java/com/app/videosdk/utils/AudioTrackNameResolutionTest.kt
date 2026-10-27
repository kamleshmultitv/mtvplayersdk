package com.app.videosdk.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioTrackNameResolutionTest {

    @Test
    fun `language code is resolved first - ISO 639-2 three letter`() {
        assertEquals("Hindi",     PlayerUtils.resolveLanguageDisplayName("hin"))
        assertEquals("English",   PlayerUtils.resolveLanguageDisplayName("eng"))
        assertEquals("Bengali",   PlayerUtils.resolveLanguageDisplayName("ben"))
        assertEquals("Tamil",     PlayerUtils.resolveLanguageDisplayName("tam"))
        assertEquals("Telugu",    PlayerUtils.resolveLanguageDisplayName("tel"))
        assertEquals("Marathi",   PlayerUtils.resolveLanguageDisplayName("mar"))
        assertEquals("Gujarati",  PlayerUtils.resolveLanguageDisplayName("guj"))
        assertEquals("Kannada",   PlayerUtils.resolveLanguageDisplayName("kan"))
        assertEquals("Malayalam", PlayerUtils.resolveLanguageDisplayName("mal"))
        assertEquals("Urdu",      PlayerUtils.resolveLanguageDisplayName("urd"))
        val panjabi = PlayerUtils.resolveLanguageDisplayName("pan")
        assertTrue(panjabi == "Punjabi" || panjabi == "Panjabi")
    }

    @Test
    fun `language code is resolved first - ISO 639-1 two letter`() {
        assertEquals("Hindi",   PlayerUtils.resolveLanguageDisplayName("hi"))
        assertEquals("English", PlayerUtils.resolveLanguageDisplayName("en"))
        assertEquals("Spanish", PlayerUtils.resolveLanguageDisplayName("es"))
        assertEquals("French",  PlayerUtils.resolveLanguageDisplayName("fr"))
    }

    @Test
    fun `manifest id used when no language`() {
        val displayName = PlayerUtils.audioTrackDisplayName(
            language = null,
            label = null,
            id = "Commentary",
            fallbackIndex = 1,
            totalTrackCount = 2
        )
        assertEquals("Commentary", displayName)
    }

    @Test
    fun `fallback used when no language or id`() {
        val single = PlayerUtils.audioTrackDisplayName(
            language = null,
            label = null,
            id = null,
            fallbackIndex = 1,
            totalTrackCount = 1
        )
        assertEquals("Default Audio", single)

        val multiple = PlayerUtils.audioTrackDisplayName(
            language = null,
            label = null,
            id = null,
            fallbackIndex = 2,
            totalTrackCount = 3
        )
        assertEquals("Audio 2", multiple)
    }

    @Test
    fun `getAudioTrack resolves list of codes without asset file`() {
        val result = PlayerUtils.getAudioTrack(listOf("hin", "eng"))
        assertEquals(2, result.size)
        assertEquals("hin", result[0].id)
        assertEquals("Hindi", result[0].name)
        assertEquals("eng", result[1].id)
        assertEquals("English", result[1].name)
    }
}
