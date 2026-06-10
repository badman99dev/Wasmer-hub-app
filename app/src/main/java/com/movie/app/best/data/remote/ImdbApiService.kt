package com.movie.app.best.data.remote

import com.movie.app.best.data.model.ImdbEpisodeResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ImdbApiService {
    @GET("titles/{titleId}/episodes")
    suspend fun getEpisodes(
        @Path("titleId") titleId: String,
        @Query("season") season: Int? = null
    ): ImdbEpisodeResponse
}
