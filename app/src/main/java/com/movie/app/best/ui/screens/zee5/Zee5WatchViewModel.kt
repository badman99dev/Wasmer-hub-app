package com.movie.app.best.ui.screens.zee5

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.model.Zee5DetailResponse
import com.movie.app.best.data.model.Zee5Item
import com.movie.app.best.data.model.Zee5Season
import com.movie.app.best.data.remote.Zee5ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class Zee5WatchState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val detail: Zee5DetailResponse? = null,
    val seasons: List<Zee5Season> = emptyList(),
    val selectedSeasonId: String? = null,
    val episodes: List<Zee5Item> = emptyList(),
    val hasMoreEpisodes: Boolean = true,
    val currentEpisode: Zee5Item? = null,
    val currentM3u8: String? = null,
    val isPlaying: Boolean = false
)

@HiltViewModel
class Zee5WatchViewModel @Inject constructor(
    val apiService: Zee5ApiService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val contentId: String = savedStateHandle["contentId"] ?: ""
    private val epId: String? = savedStateHandle["epId"]

    private val _state = MutableStateFlow(Zee5WatchState())
    private val seenEpisodeIds = mutableSetOf<String>()
    private var episodePage = 0
    val state: StateFlow<Zee5WatchState> = _state.asStateFlow()

    init {
        loadContent()
    }

    private fun loadContent() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            seenEpisodeIds.clear()
            episodePage = 0
            try {
                val detail = apiService.getDetails(contentId)
                _state.update { it.copy(detail = detail) }

                if (detail.isTvShow) {
                    val seasonData = apiService.getSeasons(contentId)
                    val seasons = seasonData.seasons ?: emptyList()
                    val latestSeason = seasons.lastOrNull()
                    val latestSeasonId = latestSeason?.id
                    val eps = latestSeason?.episodes ?: latestSeason?.episode ?: emptyList()
                    eps.mapNotNull { it.id }.forEach { seenEpisodeIds.add(it) }
                    val hasMore = eps.size < (latestSeason?.totalEpisodes ?: eps.size)

                    _state.update {
                        it.copy(
                            seasons = seasons,
                            selectedSeasonId = latestSeasonId,
                            episodes = eps,
                            hasMoreEpisodes = hasMore,
                            isLoading = false
                        )
                    }

                    if (epId != null) {
                        val ep = _state.value.episodes.find { it.id == epId }
                        if (ep != null) {
                            onEpisodeClick(ep)
                        } else {
                            for (season in seasons) {
                                val found = (season.episodes ?: season.episode ?: emptyList()).find { it.id == epId }
                                if (found != null) {
                                    selectSeason(season.id ?: latestSeasonId ?: "")
                                    onEpisodeClick(found)
                                    break
                                }
                            }
                        }
                    } else {
                        val firstEp = _state.value.episodes.firstOrNull()
                        if (firstEp != null) onEpisodeClick(firstEp)
                    }
                } else {
                    _state.update { it.copy(isLoading = false) }
                    playContent(contentId)
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Failed to load") }
            }
        }
    }

    fun selectSeason(seasonId: String) {
        val season = _state.value.seasons.find { it.id == seasonId } ?: return
        val eps = season.episodes ?: season.episode ?: emptyList()
        seenEpisodeIds.clear()
        episodePage = 0
        eps.mapNotNull { it.id }.forEach { seenEpisodeIds.add(it) }
        val hasMore = eps.size < (season.totalEpisodes ?: eps.size)
        _state.update { it.copy(selectedSeasonId = seasonId, episodes = eps, hasMoreEpisodes = hasMore, isLoadingMore = false) }
    }

    fun loadMoreEpisodes() {
        val seasonId = _state.value.selectedSeasonId ?: return
        if (_state.value.isLoadingMore || !_state.value.hasMoreEpisodes) return

        viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true) }
            try {
                episodePage++
                val response = apiService.getEpisodes(
                    seasonId = seasonId,
                    limit = 25,
                    page = episodePage
                )
                val eps = response.items
                    ?: response.episode
                    ?: response.buckets?.flatMap { it.items ?: emptyList() }
                    ?: emptyList()
                val total = response.total ?: 0

                val newEpisodes = eps.filter { item ->
                    val id = item.id
                    if (id == null || seenEpisodeIds.contains(id)) {
                        false
                    } else {
                        seenEpisodeIds.add(id)
                        true
                    }
                }

                val combinedEpisodes = _state.value.episodes + newEpisodes
                val hasMore = newEpisodes.isNotEmpty() && combinedEpisodes.size < total
                _state.update {
                    it.copy(
                        episodes = combinedEpisodes,
                        hasMoreEpisodes = hasMore,
                        isLoadingMore = false
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoadingMore = false, hasMoreEpisodes = false) }
            }
        }
    }

    fun onEpisodeClick(episode: Zee5Item) {
        val epId = episode.id ?: return
        _state.update { it.copy(currentEpisode = episode, isPlaying = false) }
        playContent(epId)
    }

    private fun playContent(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(currentM3u8 = null) }
            try {
                val playback = apiService.getPlayback(id)
                val url = playback.effectiveStreamUrl
                if (url != null) {
                    _state.update { it.copy(currentM3u8 = url, isPlaying = true) }
                } else {
                    _state.update { it.copy(error = "No stream URL available") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Failed to load stream") }
            }
        }
    }

    fun onPlaybackReady() {
        _state.update { it.copy(error = null) }
    }
}
