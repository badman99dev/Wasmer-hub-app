package com.movie.app.best.ui.screens.zee5

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.model.Zee5Item
import com.movie.app.best.data.remote.Zee5ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class Zee5CollectionUiState(
    val items: List<Zee5Item> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    val total: Int = 0,
    val error: String? = null
)

@HiltViewModel
class Zee5CollectionViewModel @Inject constructor(
    private val apiService: Zee5ApiService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val collectionId: String = savedStateHandle.get<String>("collectionId") ?: ""
    val title: String = savedStateHandle.get<String>("title") ?: ""

    private val _uiState = MutableStateFlow(Zee5CollectionUiState())
    val uiState: StateFlow<Zee5CollectionUiState> = _uiState.asStateFlow()

    private var currentPage = 0
    private var isLoadingMore = false
    private val seenItemIds = mutableSetOf<String>()

    init {
        loadCollection()
    }

    fun loadCollection() {
        if (collectionId.isBlank()) {
            _uiState.value = Zee5CollectionUiState(error = "Invalid collection")
            return
        }
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                currentPage = 0
                seenItemIds.clear()
                val response = apiService.getCollection(
                    id = collectionId,
                    page = 0,
                    limit = PAGE_LIMIT
                )
                val items = extractItems(response).filter { addSeen(it) }
                val total = response.total ?: 0
                _uiState.value = Zee5CollectionUiState(
                    items = items,
                    hasMore = hasMoreItems(items, total),
                    total = total
                )
            } catch (e: Exception) {
                _uiState.value = Zee5CollectionUiState(error = e.message ?: "Failed to load collection")
            }
        }
    }

    fun loadMore() {
        if (isLoadingMore || !_uiState.value.hasMore || collectionId.isBlank()) return

        viewModelScope.launch {
            isLoadingMore = true
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            try {
                currentPage++
                val response = apiService.getCollection(
                    id = collectionId,
                    page = currentPage,
                    limit = PAGE_LIMIT
                )
                val newItems = extractItems(response).filter { addSeen(it) }
                val total = response.total ?: _uiState.value.total
                val combinedItems = _uiState.value.items + newItems
                _uiState.value = _uiState.value.copy(
                    items = combinedItems,
                    hasMore = hasMoreItems(combinedItems, total),
                    isLoadingMore = false,
                    total = total
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    hasMore = false
                )
            } finally {
                isLoadingMore = false
            }
        }
    }

    private fun extractItems(response: com.movie.app.best.data.model.Zee5CollectionResponse): List<Zee5Item> {
        val buckets = response.buckets
        return if (!buckets.isNullOrEmpty()) {
            buckets.flatMap { it.items ?: emptyList() }
        } else {
            response.items ?: emptyList()
        }
    }

    private fun addSeen(item: Zee5Item): Boolean {
        val id = item.id ?: return true
        if (seenItemIds.contains(id)) return false
        seenItemIds.add(id)
        return true
    }

    private fun hasMoreItems(items: List<Zee5Item>, total: Int): Boolean {
        if (total <= 0) return false
        return items.size < total
    }

    companion object {
        private const val PAGE_LIMIT = 25
    }
}
