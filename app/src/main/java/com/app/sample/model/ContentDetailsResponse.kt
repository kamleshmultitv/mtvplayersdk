package com.app.sample.model

import com.google.gson.annotations.SerializedName

data class ContentDetailsResponse(
    @field:SerializedName("content")
    val content: ContentItem? = null
)


