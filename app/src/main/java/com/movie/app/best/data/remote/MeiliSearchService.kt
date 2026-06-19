package com.movie.app.best.data.remote

import com.movie.app.best.data.model.MeiliSearchRequest
import com.movie.app.best.data.model.MeiliSearchResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

interface MeiliSearchService {

    @POST
    @Headers("Content-Type: application/json")
    suspend fun search(
        @Url url: String = "indexes/movies/search",
        @Header("Authorization") auth: String,
        @Body body: MeiliSearchRequest
    ): MeiliSearchResponse

    @GET
    suspend fun ping(@Url url: String = "https://meilisearch-rs25.onrender.com/"): Response<Unit>
}

interface MeiliKeyService {

    @GET
    suspend fun getKey(@Url url: String = "https://meilisearch.badman993944.workers.dev/"): com.movie.app.best.data.model.MeiliKeyResponse
}
