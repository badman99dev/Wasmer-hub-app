package com.movie.app.best.data.remote

import com.movie.app.best.data.model.MeiliKeyResponse
import com.movie.app.best.data.model.MeiliSearchRequest
import com.movie.app.best.data.model.MeiliSearchResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface MeiliSearchService {

    @POST("indexes/movies/search")
    suspend fun search(
        @Header("Authorization") auth: String,
        @Body body: MeiliSearchRequest
    ): MeiliSearchResponse

    @GET(".")
    suspend fun ping(): Response<Unit>
}

interface MeiliKeyService {

    @GET(".")
    suspend fun getKey(): MeiliKeyResponse
}
