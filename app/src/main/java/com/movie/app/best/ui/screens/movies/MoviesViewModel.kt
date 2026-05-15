package com.movie.app.best.ui.screens.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.model.Resource
import com.movie.app.best.data.model.WasmerCategory
import com.movie.app.best.data.model.WasmerMovie
import com.movie.app.best.data.model.WasmerPageResult
import com.movie.app.best.data.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MoviesViewModel @Inject constructor(
    private val repository: MovieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MoviesUiState())
    val uiState: StateFlow<MoviesUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
        loadMovies()
    }

    fun loadCategories() {
        viewModelScope.launch {
            repository.getCategories().collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isCategoriesLoading = true) }
                    }
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                categories = result.data ?: emptyList(),
                                isCategoriesLoading = false,
                                categoriesError = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isCategoriesLoading = false,
                                categoriesError = result.error
                            )
                        }
                    }
                }
            }
        }
    }

    fun loadMovies(categorySlug: String? = null, search: String? = null, page: Int = 1) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMoviesLoading = true) }
            repository.getPage(categorySlug, search, page).collect { result ->
                when (result) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        val data = result.data
                        val movies = data?.movies ?: emptyList()
                        _uiState.update {
                            it.copy(
                                movies = if (page == 1) movies else it.movies + movies,
                                isMoviesLoading = false,
                                moviesError = null,
                                currentPage = data?.page ?: 1,
                                totalPages = data?.totalPages ?: 1,
                                currentCategorySlug = categorySlug,
                                currentSearch = search
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isMoviesLoading = false,
                                moviesError = result.error
                            )
                        }
                    }
                }
            }
        }
    }

    fun loadNextPage() {
        val current = _uiState.value
        if (current.currentPage < current.totalPages && !current.isMoviesLoading) {
            loadMovies(current.currentCategorySlug, current.currentSearch, current.currentPage + 1)
        }
    }

    fun selectCategory(slug: String?) {
        loadMovies(categorySlug = slug, page = 1)
    }

    fun searchMovies(query: String) {
        loadMovies(search = query.ifBlank { null }, page = 1)
    }
}

data class MoviesUiState(
    val categories: List<WasmerCategory> = emptyList(),
    val isCategoriesLoading: Boolean = false,
    val categoriesError: String? = null,

    val movies: List<WasmerMovie> = emptyList(),
    val isMoviesLoading: Boolean = false,
    val moviesError: String? = null,

    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val currentCategorySlug: String? = null,
    val currentSearch: String? = null
)
