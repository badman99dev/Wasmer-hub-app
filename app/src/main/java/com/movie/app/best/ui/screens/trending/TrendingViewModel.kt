package com.movie.app.best.ui.screens.trending

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.model.Resource
import com.movie.app.best.data.model.WasmerMovie
import com.movie.app.best.data.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrendingUiState(
    val popularMovies: List<WasmerMovie> = emptyList(),
    val isPopularLoading: Boolean = false,
    val popularError: String? = null
)

@HiltViewModel
class TrendingViewModel @Inject constructor(
    private val repository: MovieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrendingUiState())
    val uiState: StateFlow<TrendingUiState> = _uiState.asStateFlow()

    init {
        loadPopular()
    }

    fun loadPopular() {
        viewModelScope.launch {
            _uiState.update { it.copy(isPopularLoading = true) }
            repository.getPopularTab().collect { result ->
                when (result) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                popularMovies = result.data ?: emptyList(),
                                isPopularLoading = false,
                                popularError = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isPopularLoading = false,
                                popularError = result.error
                            )
                        }
                    }
                }
            }
        }
    }
}
