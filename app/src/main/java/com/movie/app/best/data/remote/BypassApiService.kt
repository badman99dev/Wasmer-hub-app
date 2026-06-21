package com.movie.app.best.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.POST

interface BypassApiService {
    @POST("api/bypass")
    suspend fun bypassUrl(@Body request: BypassRequest): BypassResponse
}

data class BypassRequest(
    val url: String,
    @SerializedName("fetch_info") val fetchInfo: Boolean = true
)

data class BypassResponse(
    val success: Boolean,
    val data: List<BypassResult>
)

data class BypassResult(
    val jackpot: String?,
    val status: String,
    @SerializedName("root_source") val rootSource: String?,
    @SerializedName("original_source") val originalSource: String?,
    @SerializedName("wrapper_resolved") val wrapperResolved: String?,
    @SerializedName("file_info") val fileInfo: BypassFileInfo?
)

data class BypassFileInfo(
    val filename: String?,
    val size: String?,
    @SerializedName("size_bytes") val sizeBytes: Long?,
    val resumable: Boolean?,
    @SerializedName("content_type") val contentType: String?
)
