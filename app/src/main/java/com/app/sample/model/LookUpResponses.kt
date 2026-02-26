package com.app.sample.model

import com.google.gson.annotations.SerializedName

data class LookUpResponses(
    @field:SerializedName("result")
    val result: String? = null,

    @field:SerializedName("code")
    val code: Int? = null
)