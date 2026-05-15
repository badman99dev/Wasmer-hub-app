package com.movie.app.best.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.model.Resource
import com.movie.app.best.data.model.WasmerMovie
import com.movie.app.best.data.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: MovieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun searchMovies(query: String, page: Int = 1) {
        if (query.isEmpty()) {
            _uiState.update {
                it.copy(
                    searchResults = emptyList(),
                    isLoading = false,
                    error = null,
                    searchQuery = query
                )
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, searchQuery = query) }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            repository.searchMovies(query, page).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is Resource.Success -> {
                        val results = result.data?.results ?: emptyList()
                        _uiState.update {
                            it.copy(
                                searchResults = if (page == 1) results else it.searchResults + results,
                                isLoading = false,
                                error = null,
                                currentPage = result.data?.page ?: 1,
                                totalPages = result.data?.totalPages ?: 1
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
        if (current.searchQuery.isNotEmpty() && current.currentPage < current.totalPages && !current.isLoading) {
            searchMovies(current.searchQuery, current.currentPage + 1)
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }

        searchJob?.cancel()

        if (query.length >= 3) {
            searchJob = viewModelScope.launch {
                delay(500)
                searchMovies(query)
            }
        } else if (query.isEmpty()) {
            _uiState.update {
                it.copy(
                    searchResults = emptyList(),
                    isLoading = false,
                    error = null
                )
            }
        }
    }
}

data class SearchUiState(
    val searchQuery: String = "",
    val searchResults: List<WasmerMovie> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 1,
    val totalPages: Int = 1
)
