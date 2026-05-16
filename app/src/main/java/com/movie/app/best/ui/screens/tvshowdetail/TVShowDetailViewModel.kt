package com.movie.app.best.ui.screens.tvshowdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.model.BookmarkItem
import com.movie.app.best.data.model.FirebaseHistoryItem
import com.movie.app.best.data.model.LikeItem
import com.movie.app.best.data.model.Resource
import com.movie.app.best.data.model.WasmerComment
import com.movie.app.best.data.model.WasmerDownloadLink
import com.movie.app.best.data.model.WasmerEpisode
import com.movie.app.best.data.model.WasmerSeason
import com.movie.app.best.data.model.WasmerSeriesDetailResponse
import com.movie.app.best.data.model.WasmerSeriesDetails
import com.movie.app.best.data.repository.DownloadRepository
import com.movie.app.best.data.repository.FirebaseRepository
import com.movie.app.best.data.repository.MovieRepository
import com.movie.app.best.data.repository.MyListRefreshState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class TVShowDetailViewModel @Inject constructor(
    private val repository: MovieRepository,
    private val downloadRepository: DownloadRepository,
    private val firebaseRepository: FirebaseRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val slug: String = checkNotNull(savedStateHandle["slug"])

    private val _uiState = MutableStateFlow(TVShowDetailUiState())
    val uiState: StateFlow<TVShowDetailUiState> = _uiState.asStateFlow()

    init {
        MyListRefreshState.markStale()
        loadSeriesDetails()
    }

    fun loadSeriesDetails() {
        viewModelScope.launch {
            repository.getSeriesDetails(slug).collect { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is Resource.Success -> {
                        val data = result.data
                        if (data != null) {
                            _uiState.update {
                                it.copy(
                                    series = data.movie,
                                    episodesBySeason = data.episodes_by_season.mapKeys { it.key.toIntOrNull() ?: 0 },
                                    linksByEpisode = data.links_by_episode.mapKeys { it.key.toIntOrNull() ?: 0 },
                                    comments = data.comments,
                                    screenshots = data.screenshots,
                                    moreSeasons = data.more_seasons,
                                    downloadLinks = data.links_no_episode,
                                    isLoading = false,
                                    error = null
                                )
                            }
                            addToFirebaseHistory()
                            checkBookmarkAndLikeStatus()
                        } else {
                            _uiState.update { it.copy(isLoading = false, error = "No data") }
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(isLoading = false, error = result.error)
                        }
                    }
                }
            }
        }
    }

    fun postComment(name: String, msg: String) {
        val seriesId = _uiState.value.series?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isCommentPosting = true) }
            repository.postComment(seriesId, name, msg).collect { result ->
                when (result) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(isCommentPosting = false, commentPosted = true, commentError = null)
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(isCommentPosting = false, commentPosted = false, commentError = result.error)
                        }
                    }
                }
            }
        }
    }

    fun postReport(issue: String, details: String) {
        val seriesId = _uiState.value.series?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isReportPosting = true) }
            repository.postReport(seriesId, issue, details).collect { result ->
                when (result) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(isReportPosting = false, reportPosted = true, reportError = null)
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(isReportPosting = false, reportPosted = false, reportError = result.error)
                        }
                    }
                }
            }
        }
    }

    fun requestStream() {
        val seriesId = _uiState.value.series?.id ?: return
        viewModelScope.launch {
            repository.postStreamRequest(seriesId).collect { result ->
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

    fun startDownload(slug: String, linkId: Int) {
        val title = _uiState.value.series?.title ?: "Series"
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
                            it.copy(downloadUrl = directUrl, isDownloadLoading = false, downloadStarted = true, downloadError = null)
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(isDownloadLoading = false, downloadError = result.error) }
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
        val series = _uiState.value.series ?: return
        viewModelScope.launch {
            val currentlyBookmarked = _uiState.value.isBookmarked
            if (currentlyBookmarked) {
                firebaseRepository.removeBookmark(slug)
            } else {
                firebaseRepository.addBookmark(
                    BookmarkItem(
                        slug = series.slug,
                        title = series.title,
                        posterUrl = series.posterUrl,
                        isSeries = true
                    )
                )
            }
            _uiState.update { it.copy(isBookmarked = !currentlyBookmarked) }
            MyListRefreshState.markStale()
        }
    }

    fun toggleLike() {
        val series = _uiState.value.series ?: return
        viewModelScope.launch {
            val currentlyLiked = _uiState.value.isLiked
            if (currentlyLiked) {
                firebaseRepository.removeLike(slug)
            } else {
                firebaseRepository.addLike(
                    LikeItem(
                        slug = series.slug,
                        title = series.title,
                        posterUrl = series.posterUrl,
                        isSeries = true
                    )
                )
            }
            _uiState.update { it.copy(isLiked = !currentlyLiked) }
            MyListRefreshState.markStale()
        }
    }

    fun addToFirebaseHistory() {
        val series = _uiState.value.series ?: return
        viewModelScope.launch {
            firebaseRepository.addToHistory(
                FirebaseHistoryItem(
                    slug = series.slug,
                    title = series.title,
                    posterUrl = series.posterUrl,
                    isSeries = true
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
                    _uiState.update { it.copy(series = it.series?.copy(contentModeration = newCm)) }
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

data class TVShowDetailUiState(
    val series: WasmerSeriesDetails? = null,
    val isLoading: Boolean = false,
    val error: String? = null,

    val episodesBySeason: Map<Int, List<WasmerEpisode>> = emptyMap(),
    val linksByEpisode: Map<Int, List<WasmerDownloadLink>> = emptyMap(),
    val downloadLinks: List<WasmerDownloadLink> = emptyList(),
    val comments: List<WasmerComment> = emptyList(),
    val screenshots: List<String> = emptyList(),
    val moreSeasons: List<WasmerSeason> = emptyList(),

    val isCommentPosting: Boolean = false,
    val commentPosted: Boolean = false,
    val commentError: String? = null,

    val isReportPosting: Boolean = false,
    val reportPosted: Boolean = false,
    val reportError: String? = null,

    val streamRequested: Boolean = false,

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
