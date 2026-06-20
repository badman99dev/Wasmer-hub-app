package com.movie.app.best.data.remote

import com.movie.app.best.data.model.Zee5SuggestionRequest
import com.movie.app.best.data.model.Zee5SuggestionResponse
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST

interface Zee5SuggestionApiService {

    @POST("graphql")
    suspend fun getSuggestions(
        @HeaderMap headers: Map<String, String>,
        @Body body: Zee5SuggestionRequest
    ): Zee5SuggestionResponse
}
