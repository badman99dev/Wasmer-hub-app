package com.movie.app.best.ui.screens.tvshows

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
class TVShowsViewModel @Inject constructor(
    private val repository: MovieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TVShowsUiState())
    val uiState: StateFlow<TVShowsUiState> = _uiState.asStateFlow()

    init {
        loadTVShows()
    }

    fun loadTVShows(page: Int = 1) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getPage(categorySlug = "web-series", page = page).collect { result ->
                when (result) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        val data = result.data
                        val movies = data?.movies ?: emptyList()
                        _uiState.update {
                            it.copy(
                                tvShows = if (page == 1) movies else it.tvShows + movies,
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
            loadTVShows(current.currentPage + 1)
        }
    }
}

data class TVShowsUiState(
    val tvShows: List<WasmerMovie> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 1,
    val totalPages: Int = 1
)
