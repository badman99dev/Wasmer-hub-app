package com.movie.app.best.data.repository

import com.movie.app.best.data.model.Resource
import com.movie.app.best.data.model.WasmerApiResponse
import com.movie.app.best.data.model.WasmerContentDetailResponse
import com.movie.app.best.data.model.WasmerNotification
import com.movie.app.best.data.model.WasmerCategoryOffsetResult
import com.movie.app.best.data.model.WasmerOffsetResult
import com.movie.app.best.data.model.WasmerSearchResult
import com.movie.app.best.data.model.WasmerSliderResult
import com.movie.app.best.data.remote.MovieApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MovieRepository @Inject constructor(
    private val apiService: MovieApiService
) {
    private suspend fun <T> safeApiCall(call: suspend () -> WasmerApiResponse<T>): Resource<T> {
        return try {
            val response = call()
            if (response.status == "success" && response.data != null) {
                Resource.Success(response.data)
            } else {
                Resource.Error(response.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    fun getNotification(): Flow<Resource<WasmerNotification?>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getNotification() })
    }

    fun getSlider(): Flow<Resource<WasmerSliderResult>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getSlider() })
    }

    fun getContentDetails(slug: String): Flow<Resource<WasmerContentDetailResponse>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getContentDetails(slug) })
    }

    fun searchMovies(query: String, page: Int = 1): Flow<Resource<WasmerSearchResult>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.searchMovies(query, page) })
    }

    fun getLatestUploads(offset: Int = 0, limit: Int = 45): Flow<Resource<WasmerOffsetResult>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getLatestUploads(offset, limit) })
    }

    fun getWatchableContent(offset: Int = 0, limit: Int = 45): Flow<Resource<WasmerOffsetResult>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getWatchableContent(offset, limit) })
    }

    fun getCategoryMovies(slug: String, offset: Int = 0, limit: Int = 45): Flow<Resource<WasmerCategoryOffsetResult>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getCategoryMovies(slug, offset, limit) })
    }

    fun postComment(authHeader: String, movieId: Int, msg: String): Flow<Resource<Map<String, String>>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.postComment(authHeader, movieId, msg) })
    }

    fun postStreamRequest(slug: String): Flow<Resource<Any?>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.postStreamRequest(slug) })
    }

    suspend fun submitContentModeration(authHeader: String, movieId: Int, reportType: String, reason: String): com.movie.app.best.data.remote.ContentModerationApiResponse {
        val response = apiService.postContentModeration(authHeader, mapOf(
            "movie_id" to movieId,
            "report_type" to reportType,
            "reason" to reason
        ))
        return response.data ?: com.movie.app.best.data.remote.ContentModerationApiResponse()
    }
}
