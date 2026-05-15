package com.movie.app.best.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.model.Resource
import com.movie.app.best.data.model.WasmerMovie
import com.movie.app.best.data.model.WasmerNotification
import com.movie.app.best.data.model.WasmerSliderResult
import com.movie.app.best.data.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MovieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadAllContent()
    }

    fun loadAllContent() {
        loadSlider()
        loadAllTab()
        loadNotification()
    }

    fun loadSlider() {
        viewModelScope.launch {
            repository.getSlider().collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isSliderLoading = true) }
                    }
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                sliderMovies = result.data?.movies ?: emptyList(),
                                isSliderLoading = false,
                                sliderError = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isSliderLoading = false,
                                sliderError = result.error
                            )
                        }
                    }
                }
            }
        }
    }

    fun loadAllTab() {
        viewModelScope.launch {
            repository.getAllTab().collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isAllTabLoading = true) }
                    }
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                allTabMovies = result.data ?: emptyList(),
                                isAllTabLoading = false,
                                allTabError = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isAllTabLoading = false,
                                allTabError = result.error
                            )
                        }
                    }
                }
            }
        }
    }

    fun loadNotification() {
        viewModelScope.launch {
            repository.getNotification().collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isNotificationLoading = true) }
                    }
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                notification = result.data,
                                isNotificationLoading = false,
                                notificationError = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isNotificationLoading = false,
                                notificationError = result.error
                            )
                        }
                    }
                }
            }
        }
    }

    fun dismissNotification() {
        _uiState.update { it.copy(notification = null) }
    }
}

data class HomeUiState(
    val sliderMovies: List<WasmerMovie> = emptyList(),
    val isSliderLoading: Boolean = false,
    val sliderError: String? = null,

    val allTabMovies: List<WasmerMovie> = emptyList(),
    val isAllTabLoading: Boolean = false,
    val allTabError: String? = null,

    val notification: WasmerNotification? = null,
    val isNotificationLoading: Boolean = false,
    val notificationError: String? = null
)
