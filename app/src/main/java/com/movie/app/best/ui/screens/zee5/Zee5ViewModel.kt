package com.movie.app.best.ui.screens.zee5

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.model.Zee5Bucket
import com.movie.app.best.data.model.Zee5CollectionResponse
import com.movie.app.best.data.model.Zee5DetailResponse
import com.movie.app.best.data.model.Zee5Item
import com.movie.app.best.data.model.Zee5PlaybackResponse
import com.movie.app.best.data.model.Zee5Season
import com.movie.app.best.data.remote.Zee5ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class Zee5UiState {
    object Loading : Zee5UiState()
    data class Success(val buckets: List<Zee5Bucket>, val isLoadMore: Boolean = false) : Zee5UiState()
    data class Error(val message: String) : Zee5UiState()
}

sealed class Zee5DetailState {
    object Loading : Zee5DetailState()
    data class Success(val detail: Zee5DetailResponse) : Zee5DetailState()
    data class Error(val message: String) : Zee5DetailState()
}

sealed class Zee5EpisodesState {
    object Loading : Zee5EpisodesState()
    data class Success(val episodes: List<Zee5Item>, val hasMore: Boolean, val seasonId: String) : Zee5EpisodesState()
    data class Error(val message: String) : Zee5EpisodesState()
}

sealed class Zee5PlaybackState {
    object Loading : Zee5PlaybackState()
    data class Success(val playback: Zee5PlaybackResponse) : Zee5PlaybackState()
    data class Error(val message: String) : Zee5PlaybackState()
}

enum class Zee5Tab {
    ALL, TV_SHOWS, MOVIES, FREE
}

@HiltViewModel
class Zee5ViewModel @Inject constructor(
    private val apiService: Zee5ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow<Zee5UiState>(Zee5UiState.Loading)
    val uiState: StateFlow<Zee5UiState> = _uiState.asStateFlow()

    private val _detailState = MutableStateFlow<Zee5DetailState>(Zee5DetailState.Loading)
    val detailState: StateFlow<Zee5DetailState> = _detailState.asStateFlow()

    private val _episodesState = MutableStateFlow<Zee5EpisodesState>(Zee5EpisodesState.Loading)
    val episodesState: StateFlow<Zee5EpisodesState> = _episodesState.asStateFlow()

    private val _playbackState = MutableStateFlow<Zee5PlaybackState>(Zee5PlaybackState.Loading)
    val playbackState: StateFlow<Zee5PlaybackState> = _playbackState.asStateFlow()

    private val _currentTab = MutableStateFlow(Zee5Tab.ALL)
    val currentTab: StateFlow<Zee5Tab> = _currentTab.asStateFlow()

    // Pagination state
    private var free5Page = 0
    private var isLoadingMore = false
    private var hasMoreFree5 = true
    private val loadedBuckets = mutableListOf<Zee5Bucket>()
    private val seenItemIds = mutableSetOf<String>()

    // Collection IDs for different tabs
    companion object {
        const val FREE5_COLLECTION = "0-8-5011"
        const val MOVIES_COLLECTION = "0-8-5016"
        const val TV_SHOWS_COLLECTION = "0-8-5794"
        const val HOME_COLLECTION = "0-8-homepage"
    }

    init {
        loadTab(Zee5Tab.ALL)
    }

    fun switchTab(tab: Zee5Tab) {
        if (_currentTab.value == tab) return
        _currentTab.value = tab
        free5Page = 0
        hasMoreFree5 = true
        isLoadingMore = false
        loadedBuckets.clear()
        seenItemIds.clear()
        loadTab(tab)
    }

    fun loadTab(tab: Zee5Tab) {
        viewModelScope.launch {
            _uiState.value = Zee5UiState.Loading
            try {
                val result = when (tab) {
                    Zee5Tab.ALL -> loadAllTab()
                    Zee5Tab.TV_SHOWS -> loadCollection(TV_SHOWS_COLLECTION)
                    Zee5Tab.MOVIES -> loadCollection(MOVIES_COLLECTION)
                    Zee5Tab.FREE -> loadFree5()
                }
                loadedBuckets.clear()
                loadedBuckets.addAll(result)
                _uiState.value = Zee5UiState.Success(result)
            } catch (e: Exception) {
                _uiState.value = Zee5UiState.Error(e.message ?: "Failed to load")
            }
        }
    }

    fun loadMore() {
        if (isLoadingMore || !hasMoreFree5) return
        if (_currentTab.value != Zee5Tab.FREE) return

        viewModelScope.launch {
            isLoadingMore = true
            try {
                free5Page++
                val response = apiService.getFree5(page = free5Page)
                val buckets = response.buckets ?: emptyList()

                // Filter out duplicate items using seen IDs
                val newBuckets = buckets.map { bucket ->
                    val newItems = bucket.items?.filter { item ->
                        val id = item.id
                        if (id == null || seenItemIds.contains(id)) {
                            false
                        } else {
                            seenItemIds.add(id)
                            true
                        }
                    } ?: emptyList()
                    bucket.copy(items = newItems)
                }.filter { it.items?.isNotEmpty() == true }

                if (newBuckets.isEmpty()) {
                    hasMoreFree5 = false
                } else {
                    loadedBuckets.addAll(newBuckets)
                    _uiState.value = Zee5UiState.Success(loadedBuckets.toList(), isLoadMore = true)
                }
            } catch (e: Exception) {
                hasMoreFree5 = false
            } finally {
                isLoadingMore = false
            }
        }
    }

    private suspend fun loadAllTab(): List<Zee5Bucket> {
        // Load homepage collection for mixed content
        val homepage = apiService.getCollection(HOME_COLLECTION, page = 0, limit = 20)
        val buckets = mutableListOf<Zee5Bucket>()

        homepage.buckets?.let { buckets.addAll(it) }

        // Also add FREE5 buckets as a rail
        val free5 = apiService.getFree5(page = 0)
        free5.buckets?.firstOrNull()?.let { firstBucket ->
            buckets.add(0, firstBucket)
        }

        // Track seen items
        buckets.forEach { bucket ->
            bucket.items?.forEach { item ->
                item.id?.let { seenItemIds.add(it) }
            }
        }

        return buckets
    }

    private suspend fun loadCollection(id: String): List<Zee5Bucket> {
        val response = apiService.getCollection(id, page = 0, limit = 20)
        val buckets = response.buckets ?: emptyList()

        // Track seen items
        buckets.forEach { bucket ->
            bucket.items?.forEach { item ->
                item.id?.let { seenItemIds.add(it) }
            }
        }

        return buckets
    }

    private suspend fun loadFree5(): List<Zee5Bucket> {
        free5Page = 0
        val response = apiService.getFree5(page = 0)
        val buckets = response.buckets ?: emptyList()

        // Track seen items
        buckets.forEach { bucket ->
            bucket.items?.forEach { item ->
                item.id?.let { seenItemIds.add(it) }
            }
        }

        return buckets
    }

    fun loadDetails(contentId: String) {
        viewModelScope.launch {
            _detailState.value = Zee5DetailState.Loading
            seenItemIds.clear()
            loadedEpisodes.clear()
            try {
                val detail = apiService.getDetails(contentId)
                _detailState.value = Zee5DetailState.Success(detail)
            } catch (e: Exception) {
                _detailState.value = Zee5DetailState.Error(e.message ?: "Failed to load details")
            }
        }
    }

    private val loadedEpisodes = mutableListOf<Zee5Item>()

    fun loadEpisodes(seasonId: String, page: Int = 0) {
        viewModelScope.launch {
            if (page == 0) {
                loadedEpisodes.clear()
                seenItemIds.clear()
                _episodesState.value = Zee5EpisodesState.Loading
            }
            try {
                val response = apiService.getEpisodes(seasonId, page = page, limit = 25)
                val items = response.buckets?.firstOrNull()?.items ?: response.items ?: emptyList()

                val newItems = items.filter { item ->
                    val id = item.id
                    if (id == null || seenItemIds.contains(id)) {
                        false
                    } else {
                        seenItemIds.add(id)
                        true
                    }
                }

                loadedEpisodes.addAll(newItems)
                val hasMore = newItems.size == 25
                _episodesState.value = Zee5EpisodesState.Success(loadedEpisodes.toList(), hasMore, seasonId)
            } catch (e: Exception) {
                _episodesState.value = Zee5EpisodesState.Error(e.message ?: "Failed to load episodes")
            }
        }
    }

    fun loadPlayback(contentId: String) {
        viewModelScope.launch {
            _playbackState.value = Zee5PlaybackState.Loading
            try {
                val playback = apiService.getPlayback(contentId)
                _playbackState.value = Zee5PlaybackState.Success(playback)
            } catch (e: Exception) {
                _playbackState.value = Zee5PlaybackState.Error(e.message ?: "Failed to load playback")
            }
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            _uiState.value = Zee5UiState.Loading
            try {
                val response = apiService.search(query, limit = 20)
                val rails = response.data?.hybridSearchResults?.rails ?: emptyList()

                val buckets = rails.map { rail ->
                    Zee5Bucket(
                        id = rail.id,
                        title = rail.title ?: rail.originalTitle,
                        items = rail.contents?.mapNotNull { it.effectiveItem }
                    )
                }.filter { it.items?.isNotEmpty() == true }

                loadedBuckets.clear()
                loadedBuckets.addAll(buckets)
                _uiState.value = Zee5UiState.Success(buckets)
            } catch (e: Exception) {
                _uiState.value = Zee5UiState.Error(e.message ?: "Search failed")
            }
        }
    }

    fun resetDetail() {
        _detailState.value = Zee5DetailState.Loading
        _episodesState.value = Zee5EpisodesState.Loading
        _playbackState.value = Zee5PlaybackState.Loading
    }

    fun resetPlayback() {
        _playbackState.value = Zee5PlaybackState.Loading
    }
}
