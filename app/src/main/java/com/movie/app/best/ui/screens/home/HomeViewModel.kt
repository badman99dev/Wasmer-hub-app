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

    companion object {
        private const val PAGE_LIMIT = 45
    }

    init {
        loadAllContent()
    }

    fun loadAllContent() {
        loadSlider()
        loadAllTab()
        loadTrending()
        loadNotification()
    }

    fun loadTrending() {
        viewModelScope.launch {
            repository.getTrending(offset = 0, limit = 20).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isTrendingLoading = true) }
                    }
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                trendingMovies = result.data?.items ?: emptyList(),
                                isTrendingLoading = false,
                                trendingError = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isTrendingLoading = false,
                                trendingError = result.error
                            )
                        }
                    }
                }
            }
        }
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
            _uiState.update { it.copy(isAllTabLoading = true) }
            repository.getLatestUploads(offset = 0, limit = PAGE_LIMIT).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isAllTabLoading = true) }
                    }
                    is Resource.Success -> {
                        val data = result.data
                        _uiState.update {
                            it.copy(
                                allTabMovies = data?.items ?: emptyList(),
                                isAllTabLoading = false,
                                allTabError = null,
                                allTabOffset = data?.offset ?: 0,
                                allTabTotal = data?.total ?: 0,
                                canLoadMoreAllTab = (data?.items?.size ?: 0) >= PAGE_LIMIT && ((data?.offset ?: 0) + (data?.items?.size ?: 0)) < (data?.total ?: 0)
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

    fun loadMoreAllTab() {
        val current = _uiState.value
        if (current.isAllTabLoadingMore || !current.canLoadMoreAllTab) return

        val nextOffset = current.allTabOffset + current.allTabMovies.size
        if (nextOffset >= current.allTabTotal) return

        viewModelScope.launch {
            _uiState.update { it.copy(isAllTabLoadingMore = true) }
            repository.getLatestUploads(offset = nextOffset, limit = PAGE_LIMIT).collect { result ->
                when (result) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        val newItems = result.data?.items ?: emptyList()
                        _uiState.update {
                            it.copy(
                                allTabMovies = it.allTabMovies + newItems,
                                isAllTabLoadingMore = false,
                                allTabOffset = result.data?.offset ?: nextOffset,
                                canLoadMoreAllTab = newItems.size >= PAGE_LIMIT && (nextOffset + newItems.size) < (result.data?.total ?: 0)
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(isAllTabLoadingMore = false) }
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

    val trendingMovies: List<WasmerMovie> = emptyList(),
    val isTrendingLoading: Boolean = false,
    val trendingError: String? = null,

    val allTabMovies: List<WasmerMovie> = emptyList(),
    val isAllTabLoading: Boolean = false,
    val isAllTabLoadingMore: Boolean = false,
    val allTabError: String? = null,
    val allTabOffset: Int = 0,
    val allTabTotal: Int = 0,
    val canLoadMoreAllTab: Boolean = false,

    val notification: WasmerNotification? = null,
    val isNotificationLoading: Boolean = false,
    val notificationError: String? = null
)
