package com.movie.app.best.data.remote

import com.movie.app.best.data.model.MeiliSearchRequest
import com.movie.app.best.data.model.MeiliSearchResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface MeiliSearchService {

    @POST("indexes/movies/search")
    suspend fun search(
        @Header("Authorization") auth: String,
        @Body body: MeiliSearchRequest
    ): MeiliSearchResponse
}
