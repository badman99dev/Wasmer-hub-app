package com.movie.app.best.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

interface BypassApiService {
    @POST("api/bypass")
    suspend fun bypassUrl(@Body request: BypassRequest): BypassResponse
}

data class BypassRequest(val url: String)

data class BypassResponse(val success: Boolean, val data: List<BypassResult>)

data class BypassResult(val jackpot: String, val status: String)
