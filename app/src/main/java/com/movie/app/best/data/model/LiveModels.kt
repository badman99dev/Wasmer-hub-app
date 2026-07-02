package com.movie.app.best.data.model

import com.google.gson.annotations.SerializedName

data class LiveChannel(
    val id: Int,
    val name: String,
    @SerializedName("logoUrl") val logoUrl: String,
    val category: String,
    @SerializedName("streamUrl") val streamUrl: String,
)

data class BroadcastResponse(
    val channels: List<LiveChannel>
)
