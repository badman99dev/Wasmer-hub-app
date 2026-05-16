package com.movie.app.best.ui.screens.moviedetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.model.BookmarkItem
import com.movie.app.best.data.model.FirebaseHistoryItem
import com.movie.app.best.data.model.LikeItem
import com.movie.app.best.data.model.Resource
import com.movie.app.best.data.model.WasmerContentDetailResponse
import com.movie.app.best.data.model.WasmerDownloadLink
import com.movie.app.best.data.model.WasmerMovieDetails
import com.movie.app.best.data.repository.DownloadRepository
import com.movie.app.best.data.repository.FirebaseRepository
import com.movie.app.best.data.repository.MovieRepository
import com.movie.app.best.data.repository.MyListRefreshState
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class MovieDetailViewModel @Inject constructor(
    private val repository: MovieRepository,
    private val downloadRepository: DownloadRepository,
    private val firebaseRepository: FirebaseRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val slug: String = checkNotNull(savedStateHandle["slug"])

    private val _uiState = MutableStateFlow(MovieDetailUiState())
    val uiState: StateFlow<MovieDetailUiState> = _uiState.asStateFlow()

    init {
        MyListRefreshState.markStale()
        loadMovieDetails()
    }

    fun loadMovieDetails() {
        viewModelScope.launch {
            repository.getContentDetails(slug).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is Resource.Success -> {
                        val detailData = result.data
                        _uiState.update {
                            it.copy(
                                movie = detailData?.movie,
                                downloadLinks = detailData?.downloadLinks ?: emptyList(),
                                comments = detailData?.comments ?: emptyList(),
                                screenshots = detailData?.screenshots ?: emptyList(),
                                categories = emptyList(),
                                allMovies = emptyList(),
                                isLoading = false,
                                error = null
                            )
                        }
                        if (detailData?.movie != null) {
                            addToFirebaseHistory()
                            checkBookmarkAndLikeStatus()
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

    fun postComment(name: String, msg: String) {
        val movieId = _uiState.value.movie?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isCommentPosting = true) }
            repository.postComment(movieId, name, msg).collect { result ->
                when (result) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                isCommentPosting = false,
                                commentPosted = true,
                                commentError = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isCommentPosting = false,
                                commentPosted = false,
                                commentError = result.error
                            )
                        }
                    }
                }
            }
        }
    }

    fun postReport(issue: String, details: String) {
        val movieId = _uiState.value.movie?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isReportPosting = true) }
            repository.postReport(movieId, issue, details).collect { result ->
                when (result) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                isReportPosting = false,
                                reportPosted = true,
                                reportError = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isReportPosting = false,
                                reportPosted = false,
                                reportError = result.error
                            )
                        }
                    }
                }
            }
        }
    }

    fun requestStream() {
        val movieId = _uiState.value.movie?.id ?: return
        viewModelScope.launch {
            repository.postStreamRequest(movieId).collect { result ->
                when (result) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        _uiState.update { it.copy(streamRequested = true) }
                    }
                    is Resource.Error -> {}
                }
            }
        }
    }

    fun requestMovie(imdbId: String, title: String, message: String) {
        viewModelScope.launch {
            repository.postMovieRequest(imdbId, title, message).collect { result ->
                when (result) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        _uiState.update { it.copy(movieRequested = true) }
                    }
                    is Resource.Error -> {}
                }
            }
        }
    }

    fun startDownload(slug: String, linkId: Int) {
        val title = _uiState.value.movie?.title ?: "Movie"
        viewModelScope.launch {
            _uiState.update { it.copy(isDownloadLoading = true, downloadError = null, downloadUrl = "") }
            downloadRepository.resolveDownloadUrl(slug, linkId).collect { result ->
                when (result) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        val directUrl = result.data ?: return@collect
                        val fileName = extractFileName(directUrl, title)
                        downloadRepository.startDownload(directUrl, fileName, title)
                        _uiState.update {
                            it.copy(
                                downloadUrl = directUrl,
                                isDownloadLoading = false,
                                downloadStarted = true,
                                downloadError = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isDownloadLoading = false,
                                downloadError = result.error
                            )
                        }
                    }
                }
            }
        }
    }

    private fun extractFileName(url: String, fallback: String): String {
        return try {
            val path = android.net.Uri.parse(url).path ?: fallback
            val name = path.substringAfterLast("/")
            if (name.contains(".")) name else "$fallback.mkv"
        } catch (_: Exception) {
            "$fallback.mkv"
        }
    }

    fun resetCommentState() {
        _uiState.update { it.copy(commentPosted = false, commentError = null) }
    }

    fun resetReportState() {
        _uiState.update { it.copy(reportPosted = false, reportError = null) }
    }

    fun checkBookmarkAndLikeStatus() {
        viewModelScope.launch {
            val bookmarked = firebaseRepository.isBookmarked(slug)
            val liked = firebaseRepository.isLiked(slug)
            _uiState.update { it.copy(isBookmarked = bookmarked, isLiked = liked) }
        }
    }

    fun toggleBookmark() {
        val movie = _uiState.value.movie ?: return
        viewModelScope.launch {
            val currentlyBookmarked = _uiState.value.isBookmarked
            if (currentlyBookmarked) {
                firebaseRepository.removeBookmark(slug)
            } else {
                firebaseRepository.addBookmark(
                    BookmarkItem(
                        slug = movie.slug,
                        title = movie.title,
                        posterUrl = movie.posterUrl,
                        isSeries = movie.isSeries
                    )
                )
            }
            _uiState.update { it.copy(isBookmarked = !currentlyBookmarked) }
            MyListRefreshState.markStale()
        }
    }

    fun toggleLike() {
        val movie = _uiState.value.movie ?: return
        viewModelScope.launch {
            val currentlyLiked = _uiState.value.isLiked
            if (currentlyLiked) {
                firebaseRepository.removeLike(slug)
            } else {
                firebaseRepository.addLike(
                    LikeItem(
                        slug = movie.slug,
                        title = movie.title,
                        posterUrl = movie.posterUrl,
                        isSeries = movie.isSeries
                    )
                )
            }
            _uiState.update { it.copy(isLiked = !currentlyLiked) }
            MyListRefreshState.markStale()
        }
    }

    fun addToFirebaseHistory() {
        val movie = _uiState.value.movie ?: return
        viewModelScope.launch {
            firebaseRepository.addToHistory(
                FirebaseHistoryItem(
                    slug = movie.slug,
                    title = movie.title,
                    posterUrl = movie.posterUrl,
                    isSeries = movie.isSeries
                )
            )
        }
    }

    fun openReportDrawer() {
        _uiState.update { it.copy(showReportDrawer = true) }
    }

    fun closeReportDrawer() {
        _uiState.update { it.copy(showReportDrawer = false) }
    }

    fun submitContentModeration(movieId: Int, reportType: String, reason: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(showReportDrawer = false, showReportWaiting = true) }
            try {
                val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val tokenResult = user?.getIdToken(false)?.await()
                val authHeader = "Bearer ${tokenResult?.token ?: ""}"
                val response = repository.submitContentModeration(authHeader, movieId, reportType, reason)
                val modResult = response.moderation
                val hasAnyFlag = modResult?.poster == "sexual" || modResult?.screenshots == "sexual" || modResult?.storyline == "sexual"
                _uiState.update { state ->
                    state.copy(
                        showReportWaiting = false,
                        reportModerationResult = modResult,
                        showCelebration = hasAnyFlag
                    )
                }
                if (modResult != null) {
                    val newCm = com.movie.app.best.data.model.ContentModeration(
                        poster = modResult.poster,
                        screenshots = modResult.screenshots,
                        storyline = modResult.storyline
                    )
                    _uiState.update { it.copy(movie = it.movie?.copy(contentModeration = newCm)) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        showReportWaiting = false,
                        reportError = e.message
                    )
                }
            }
        }
    }

    fun dismissReportResult() {
        _uiState.update { it.copy(reportModerationResult = null) }
    }

    fun dismissCelebration() {
        _uiState.update { it.copy(showCelebration = false) }
    }
}

data class MovieDetailUiState(
    val movie: WasmerMovieDetails? = null,
    val isLoading: Boolean = false,
    val error: String? = null,

    val downloadLinks: List<WasmerDownloadLink> = emptyList(),
    val comments: List<com.movie.app.best.data.model.WasmerComment> = emptyList(),
    val screenshots: List<String> = emptyList(),
    val categories: List<com.movie.app.best.data.model.WasmerCategorySimple> = emptyList(),
    val allMovies: List<com.movie.app.best.data.model.WasmerMovie> = emptyList(),

    val isCommentPosting: Boolean = false,
    val commentPosted: Boolean = false,
    val commentError: String? = null,

    val isReportPosting: Boolean = false,
    val reportPosted: Boolean = false,
    val reportError: String? = null,

    val streamRequested: Boolean = false,
    val movieRequested: Boolean = false,

    val downloadUrl: String = "",
    val isDownloadLoading: Boolean = false,
    val downloadStarted: Boolean = false,
    val downloadError: String? = null,

    val isBookmarked: Boolean = false,
    val isLiked: Boolean = false,

    val showReportDrawer: Boolean = false,
    val showReportWaiting: Boolean = false,
    val reportModerationResult: com.movie.app.best.data.model.ContentModerationResponse? = null,
    val showCelebration: Boolean = false
)
