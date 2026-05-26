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

    init {
        viewModelScope.launch {
            try {
                val profile = firebaseRepository.getOrCreateUserProfile()
                val isMod = profile?.tier == "moderator"
                _uiState.update { it.copy(isModerator = isMod) }
            } catch (_: Exception) {}
        }
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
                            loadSimilarMovies(detailData.movie.imdbId)
                            val movie = detailData.movie
                            if (movie.hasStream || movie.playerUrl.isNotEmpty()) {
                                val streamUrl = if (movie.playerUrl.isNotEmpty()) movie.playerUrl
                                    else "https://sparkling-breeze-1ad6.badman993944.workers.dev/?id=${movie.id}"
                                warmCdnCache(streamUrl)
                            }
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

    private fun warmCdnCache(url: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/125.0.0.0 Mobile Safari/537.36")
                conn.setRequestProperty("Referer", "https://wasmer-jhns970ko-badals-projects-03fab3df.vercel.app/")
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.inputStream.bufferedReader().readText()
                conn.disconnect()
            } catch (_: Exception) {}
        }
    }

    fun postComment(name: String, msg: String) {
        val movieId = _uiState.value.movie?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isCommentPosting = true) }
            val authHeader = try {
                val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val tokenResult = user?.getIdToken(false)?.await()
                "Bearer ${tokenResult?.token ?: ""}"
            } catch (_: Exception) { "" }
            repository.postComment(authHeader, movieId, msg).collect { result ->
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

    fun requestStream() {
        val slug = _uiState.value.movie?.slug ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isStreamRequesting = true) }
            try {
                val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val tokenResult = user?.getIdToken(false)?.await()
                val authHeader = "Bearer ${tokenResult?.token ?: ""}"
                val response = repository.submitStreamRequest(authHeader, slug)
                _uiState.update {
                    it.copy(
                        isStreamRequesting = false,
                        streamRequested = !response.already_requested && !response.has_stream,
                        streamRequestResult = response,
                        showStreamRequestResult = true
                    )
                }
                if (response.already_requested) {
                    _uiState.update { it.copy(streamRequested = true) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isStreamRequesting = false,
                        streamRequestError = e.message
                    )
                }
            }
        }
    }

    fun dismissStreamRequestResult() {
        _uiState.update { it.copy(showStreamRequestResult = false) }
    }

    fun startDownload(linkUrl: String) {
        val title = _uiState.value.movie?.title ?: "Movie"
        viewModelScope.launch {
            _uiState.update { it.copy(isDownloadLoading = true, downloadError = null, downloadUrl = "") }
            downloadRepository.resolveDownloadUrl(linkUrl).collect { result ->
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
                    isSeries = movie.isSeries,
                    imdbId = movie.imdbId
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
        val isObjection = reportType == "objection"
        viewModelScope.launch {
            _uiState.update { it.copy(showReportDrawer = false, showReportWaiting = true, isObjectionReport = isObjection) }
            try {
                val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val tokenResult = user?.getIdToken(false)?.await()
                val authHeader = "Bearer ${tokenResult?.token ?: ""}"
                val response = repository.submitContentModeration(authHeader, movieId, reportType, reason)
                handleModerationResponse(response, isObjection)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        showReportWaiting = false,
                        moderationError = e.message
                    )
                }
            }
        }
    }

    fun submitModeratorVerdict(movieId: Int, poster: String, screenshots: String, storyline: String, reasoning: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(showReportDrawer = false) }
            try {
                val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val tokenResult = user?.getIdToken(false)?.await()
                val authHeader = "Bearer ${tokenResult?.token ?: ""}"
                val verdict = mapOf<String, @JvmSuppressWildcards Any>(
                    "poster" to poster,
                    "screenshots" to screenshots,
                    "storyline" to storyline,
                    "confidence" to "high",
                    "reasoning" to reasoning
                )
                val response = repository.submitModeratorVerdict(authHeader, movieId, "manual", reasoning, verdict)
                handleModerationResponse(response, false)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(moderationError = e.message)
                }
            }
        }
    }

    private suspend fun handleModerationResponse(response: com.movie.app.best.data.remote.ContentModerationApiResponse, isObjection: Boolean) {
        val modResult = response.moderation
        val prevResult = response.previous_moderation

        val anyChange = if (prevResult != null && modResult != null) {
            modResult.poster != prevResult.poster || modResult.screenshots != prevResult.screenshots || modResult.storyline != prevResult.storyline
        } else {
            modResult?.poster == "sexual" || modResult?.screenshots == "sexual" || modResult?.storyline == "sexual"
        }

        val showCeleb = if (isObjection) anyChange else (modResult?.poster == "sexual" || modResult?.screenshots == "sexual" || modResult?.storyline == "sexual")

        _uiState.update { state ->
            state.copy(
                showReportWaiting = false,
                reportModerationResult = modResult,
                previousModerationResult = prevResult,
                showCelebration = showCeleb
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
    }

    fun dismissReportResult() {
        _uiState.update { it.copy(reportModerationResult = null) }
    }

    fun dismissCelebration() {
        _uiState.update { it.copy(showCelebration = false) }
    }

    private fun loadSimilarMovies(imdbId: String) {
        if (!imdbId.startsWith("tt")) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSimilarLoading = true) }
            repository.getSimilar(imdbId).collect { result ->
                when (result) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                similarMovies = result.data?.items ?: emptyList(),
                                isSimilarLoading = false,
                                similarError = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isSimilarLoading = false,
                                similarError = result.error
                            )
                        }
                    }
                }
            }
        }
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

    val similarMovies: List<com.movie.app.best.data.model.WasmerMovie> = emptyList(),
    val isSimilarLoading: Boolean = false,
    val similarError: String? = null,

    val isCommentPosting: Boolean = false,
    val commentPosted: Boolean = false,
    val commentError: String? = null,

    val streamRequested: Boolean = false,
    val isStreamRequesting: Boolean = false,
    val streamRequestResult: com.movie.app.best.data.remote.StreamRequestApiResponse? = null,
    val showStreamRequestResult: Boolean = false,
    val streamRequestError: String? = null,

    val downloadUrl: String = "",
    val isDownloadLoading: Boolean = false,
    val downloadStarted: Boolean = false,
    val downloadError: String? = null,

    val isBookmarked: Boolean = false,
    val isLiked: Boolean = false,

    val showReportDrawer: Boolean = false,
    val showReportWaiting: Boolean = false,
    val isObjectionReport: Boolean = false,
    val isModerator: Boolean = false,
    val moderationError: String? = null,
    val reportModerationResult: com.movie.app.best.data.model.ContentModerationResponse? = null,
    val previousModerationResult: com.movie.app.best.data.model.ContentModerationResponse? = null,
    val showCelebration: Boolean = false
)
