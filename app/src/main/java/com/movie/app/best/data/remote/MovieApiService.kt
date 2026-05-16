package com.movie.app.best.data.remote

import com.movie.app.best.data.model.WasmerApiResponse
import com.movie.app.best.data.model.WasmerMovie
import com.movie.app.best.data.model.WasmerPageResult
import com.movie.app.best.data.model.WasmerSearchResult
import com.movie.app.best.data.model.WasmerSliderResult
import com.movie.app.best.data.model.ContentModerationResponse
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface MovieApiService {

    @GET("settings")
    suspend fun getSettings(): WasmerApiResponse<Map<String, String>>

    @GET("notification")
    suspend fun getNotification(): WasmerApiResponse<com.movie.app.best.data.model.WasmerNotification?>

    @GET("slider")
    suspend fun getSlider(): WasmerApiResponse<WasmerSliderResult>

    @POST("tabs/all")
    @FormUrlEncoded
    suspend fun getAllTab(
        @Field("ajax") ajax: String = "1"
    ): WasmerApiResponse<List<WasmerMovie>>

    @POST("tabs/popular")
    @FormUrlEncoded
    suspend fun getPopularTab(
        @Field("ajax") ajax: String = "1"
    ): WasmerApiResponse<List<WasmerMovie>>

    @GET("movie/{slug}")
    suspend fun getMovieDetails(@Path("slug") slug: String): WasmerApiResponse<com.movie.app.best.data.model.WasmerMovieDetailResponse>

    @GET("series/{slug}")
    suspend fun getSeriesDetails(@Path("slug") slug: String): WasmerApiResponse<com.movie.app.best.data.model.WasmerSeriesDetailResponse>

    @GET("search")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("page") page: Int = 1
    ): WasmerApiResponse<WasmerSearchResult>

    @GET("page")
    suspend fun getPage(
        @Query("category_slug") categorySlug: String? = null,
        @Query("s") search: String? = null,
        @Query("page") page: Int = 1
    ): WasmerApiResponse<WasmerPageResult>

    @GET("categories")
    suspend fun getCategories(): WasmerApiResponse<List<com.movie.app.best.data.model.WasmerCategory>>

    @GET("download/{slug}/{linkId}")
    suspend fun getDownloadInfo(
        @Path("slug") slug: String,
        @Path("linkId") linkId: Int
    ): WasmerApiResponse<com.movie.app.best.data.model.WasmerDownloadResult>

    @POST("comment")
    @FormUrlEncoded
    suspend fun postComment(
        @Field("movie_id") movieId: Int,
        @Field("name") name: String,
        @Field("msg") msg: String
    ): WasmerApiResponse<Map<String, String>>

    @POST("report")
    @FormUrlEncoded
    suspend fun postReport(
        @Field("movie_id") movieId: Int,
        @Field("issue") issue: String,
        @Field("details") details: String
    ): WasmerApiResponse<Any?>

    @POST("stream-request")
    @FormUrlEncoded
    suspend fun postStreamRequest(
        @Field("movie_id") movieId: Int
    ): WasmerApiResponse<Any?>

    @POST("movie-request")
    @FormUrlEncoded
    suspend fun postMovieRequest(
        @Field("imdb_id") imdbId: String,
        @Field("movie_title") movieTitle: String,
        @Field("user_message") userMessage: String
    ): WasmerApiResponse<Any?>

    @POST("bookmark/add")
    @FormUrlEncoded
    suspend fun addBookmark(
        @Header("Authorization") authHeader: String,
        @Field("movie_id") movieId: Int
    ): WasmerApiResponse<Map<String, String>>

    @POST("bookmark/remove")
    @FormUrlEncoded
    suspend fun removeBookmark(
        @Header("Authorization") authHeader: String,
        @Field("movie_id") movieId: Int
    ): WasmerApiResponse<Map<String, String>>

    @GET("bookmark/list")
    suspend fun getBookmarks(
        @Header("Authorization") authHeader: String
    ): WasmerApiResponse<List<WasmerMovie>>

    @GET("bookmark/check")
    suspend fun checkBookmark(
        @Header("Authorization") authHeader: String,
        @Query("movie_id") movieId: Int
    ): WasmerApiResponse<Map<String, Boolean>>

    @POST("content-moderation")
    suspend fun postContentModeration(
        @Header("Authorization") authHeader: String,
        @Body request: Map<String, @JvmSuppressWildcards Any>
    ): WasmerApiResponse<ContentModerationApiResponse>

    @GET("latest-uploads")
    suspend fun getLatestUploads(
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 20
    ): WasmerApiResponse<Map<String, Any>>
}

data class ContentModerationApiResponse(
    val verdict: String? = null,
    val moderation: ContentModerationResponse? = null,
    val previous_moderation: Any? = null,
    val images_analyzed: Int = 0,
    val debug: List<String> = emptyList()
)
