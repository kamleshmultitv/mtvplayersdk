package com.app.reelssdk.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AgeRatingResolverTest {

    @Test
    fun `returns age rating exactly as supplied`() {
        assertEquals(" U/A 13+ ", AgeRatingResolver.resolve(ReelsPlayerModel(ageRating = " U/A 13+ ")))
    }

    @Test
    fun `supports content rating backend field`() {
        assertEquals("A 18+", AgeRatingResolver.resolve(ReelsPlayerModel(contentRating = "A 18+")))
    }

    @Test
    fun `age rating takes precedence without mapping`() {
        val video = ReelsPlayerModel(ageRating = "U", contentRating = "A 18+")
        assertEquals("U", AgeRatingResolver.resolve(video))
    }

    @Test
    fun `null empty and blank ratings are absent`() {
        assertNull(AgeRatingResolver.resolve(null))
        assertNull(AgeRatingResolver.resolve(ReelsPlayerModel(ageRating = "")))
        assertNull(AgeRatingResolver.resolve(ReelsPlayerModel(ageRating = "   ")))
    }
}
