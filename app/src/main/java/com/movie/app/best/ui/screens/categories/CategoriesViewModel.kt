package com.movie.app.best.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.model.Resource
import com.movie.app.best.data.model.WasmerCategory
import com.movie.app.best.data.model.WasmerMovie
import com.movie.app.best.data.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val repository: MovieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    fun loadCategories() {
        viewModelScope.launch {
            repository.getCategories().collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                categories = (result.data ?: emptyList()).sortedByDescending { cat -> cat.count },
                                isLoading = false,
                                error = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = result.error
                            )
                        }
                    }
                }
            }
        }
    }
}

data class CategoriesUiState(
    val categories: List<WasmerCategory> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CategoryPageViewModel @Inject constructor(
    private val repository: MovieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryPageUiState())
    val uiState: StateFlow<CategoryPageUiState> = _uiState.asStateFlow()

    private var currentCategorySlug: String? = null

    fun loadMovies(categorySlug: String, page: Int = 1) {
        currentCategorySlug = categorySlug
        viewModelScope.launch {
            repository.getPage(categorySlug, null, page).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is Resource.Success -> {
                        val data = result.data
                        val movies = data?.movies ?: emptyList()
                        _uiState.update {
                            it.copy(
                                movies = if (page == 1) movies else it.movies + movies,
                                isLoading = false,
                                error = null,
                                currentPage = data?.page ?: 1,
                                totalPages = data?.totalPages ?: 1
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = result.error
                            )
                        }
                    }
                }
            }
        }
    }

    fun loadNextPage() {
        val current = _uiState.value
        if (current.currentPage < current.totalPages && !current.isLoading) {
            loadMovies(currentCategorySlug ?: return, current.currentPage + 1)
        }
    }
}

data class CategoryPageUiState(
    val movies: List<WasmerMovie> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 1,
    val totalPages: Int = 1
)
