package com.movie.app.best.data.remote

import com.movie.app.best.data.model.Zee5SuggestionResponse
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.Query

interface Zee5SuggestionApiService {

    @GET("artemis/apq/web_app/IN/SSQ")
    suspend fun getSuggestions(
        @HeaderMap headers: Map<String, String>,
        @Query("operationName") operationName: String = "SSQ",
        @Query("variables", encoded = true) variables: String,
        @Query("extensions", encoded = true) extensions: String
    ): Zee5SuggestionResponse
}
