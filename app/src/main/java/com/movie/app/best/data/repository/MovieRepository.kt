package com.movie.app.best.data.repository

import com.movie.app.best.data.model.Resource
import com.movie.app.best.data.model.WasmerApiResponse
import com.movie.app.best.data.model.WasmerCategory
import com.movie.app.best.data.model.WasmerDownloadResult
import com.movie.app.best.data.model.WasmerMovie
import com.movie.app.best.data.model.WasmerMovieDetailResponse
import com.movie.app.best.data.model.WasmerSeriesDetailResponse
import com.movie.app.best.data.model.WasmerMovieDetails
import com.movie.app.best.data.model.WasmerNotification
import com.movie.app.best.data.model.WasmerPageResult
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

    fun getSettings(): Flow<Resource<Map<String, String>>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getSettings() })
    }

    fun getNotification(): Flow<Resource<WasmerNotification?>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getNotification() })
    }

    fun getSlider(): Flow<Resource<WasmerSliderResult>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getSlider() })
    }

    fun getAllTab(): Flow<Resource<List<WasmerMovie>>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getAllTab() })
    }

    fun getPopularTab(): Flow<Resource<List<WasmerMovie>>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getPopularTab() })
    }

    fun getMovieDetails(slug: String): Flow<Resource<WasmerMovieDetailResponse>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getMovieDetails(slug) })
    }

    fun getSeriesDetails(slug: String): Flow<Resource<WasmerSeriesDetailResponse>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getSeriesDetails(slug) })
    }

    fun searchMovies(query: String, page: Int = 1): Flow<Resource<WasmerSearchResult>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.searchMovies(query, page) })
    }

    fun getPage(categorySlug: String? = null, search: String? = null, page: Int = 1): Flow<Resource<WasmerPageResult>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getPage(categorySlug, search, page) })
    }

    fun getCategories(): Flow<Resource<List<WasmerCategory>>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getCategories() })
    }

    fun getDownloadInfo(slug: String, linkId: Int): Flow<Resource<WasmerDownloadResult>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.getDownloadInfo(slug, linkId) })
    }

    fun postComment(movieId: Int, name: String, msg: String): Flow<Resource<Map<String, String>>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.postComment(movieId, name, msg) })
    }

    fun postReport(movieId: Int, issue: String, details: String): Flow<Resource<Any?>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.postReport(movieId, issue, details) })
    }

    fun postStreamRequest(movieId: Int): Flow<Resource<Any?>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.postStreamRequest(movieId) })
    }

    fun postMovieRequest(imdbId: String, title: String, message: String): Flow<Resource<Any?>> = flow {
        emit(Resource.Loading())
        emit(safeApiCall { apiService.postMovieRequest(imdbId, title, message) })
    }
}
