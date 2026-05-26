package com.movie.app.best.ui.screens.library

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.movie.app.best.data.model.BookmarkItem
import com.movie.app.best.data.model.FirebaseHistoryItem
import com.movie.app.best.data.model.LikeItem
import com.movie.app.best.data.settings.ModerationSettings
import com.movie.app.best.data.repository.FirebaseRepository
import com.movie.app.best.data.repository.LibraryRepository
import com.movie.app.best.data.repository.MyListRefreshState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val history: List<FirebaseHistoryItem> = emptyList(),
    val likedPlaylist: List<LikeItem> = emptyList(),
    val watchLaterPlaylist: List<BookmarkItem> = emptyList(),
    val isOnline: Boolean = true,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: LibraryRepository,
    private val firebaseRepository: FirebaseRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            MyListRefreshState.isMyListRefreshed.collect { refreshed ->
                if (!refreshed) {
                    loadLibrary(showSkeleton = true)
                }
            }
        }
        val currentRefreshed = MyListRefreshState.isMyListRefreshed.value
        loadLibrary(showSkeleton = !currentRefreshed)
    }

    fun loadLibrary(showSkeleton: Boolean = false) {
        viewModelScope.launch {
            if (showSkeleton) {
                _uiState.update { it.copy(isLoading = true) }
            }
            val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
            if (isLoggedIn) {
                val bookmarks = firebaseRepository.getBookmarks()
                val history = firebaseRepository.getHistory()
                val likes = firebaseRepository.getLikes()
                _uiState.update {
                    it.copy(
                        history = applyModerationFilterHistory(history),
                        watchLaterPlaylist = applyModerationFilterBookmarks(bookmarks),
                        likedPlaylist = applyModerationFilterLikes(likes),
                        isOnline = isOnline(context),
                        isLoading = false,
                        isRefreshing = false
                    )
                }
                MyListRefreshState.markRefreshed()
            } else {
                _uiState.update {
                    it.copy(
                        history = repository.getHistory(),
                        likedPlaylist = repository.getPlaylist("liked"),
                        watchLaterPlaylist = repository.getPlaylist("watch_later"),
                        isOnline = isOnline(context),
                        isLoading = false,
                        isRefreshing = false
                    )
                }
                MyListRefreshState.markRefreshed()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            loadLibrary()
        }
    }

    fun removeFromHistory(slug: String) {
        viewModelScope.launch {
            val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
            if (isLoggedIn) {
                firebaseRepository.removeFromHistory(slug)
            } else {
                repository.removeFromHistory(slug)
            }
            loadLibrary()
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
            if (isLoggedIn) {
                firebaseRepository.clearHistory()
            } else {
                repository.clearHistory()
            }
            loadLibrary()
        }
    }

    fun removeFromLiked(slug: String) {
        viewModelScope.launch {
            val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
            if (isLoggedIn) {
                firebaseRepository.removeLike(slug)
            } else {
                repository.removeFromPlaylist("liked", slug)
            }
            loadLibrary()
        }
    }

    fun removeFromWatchLater(slug: String) {
        viewModelScope.launch {
            val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
            if (isLoggedIn) {
                firebaseRepository.removeBookmark(slug)
            } else {
                repository.removeFromPlaylist("watch_later", slug)
            }
            loadLibrary()
        }
    }

    private fun isOnline(context: Context): Boolean {

    private fun applyModerationFilterHistory(items: List<FirebaseHistoryItem>): List<FirebaseHistoryItem> {
        return items.filter { !ModerationSettings.shouldHide(context, it.contentModeration) }
    }

    private fun applyModerationFilterBookmarks(items: List<BookmarkItem>): List<BookmarkItem> {
        return items.filter { !ModerationSettings.shouldHide(context, it.contentModeration) }
    }

    private fun applyModerationFilterLikes(items: List<LikeItem>): List<LikeItem> {
        return items.filter { !ModerationSettings.shouldHide(context, it.contentModeration) }
    }
}
