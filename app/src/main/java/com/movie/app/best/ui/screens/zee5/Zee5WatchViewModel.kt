package com.movie.app.best.ui.screens.zee5

import android.util.Log
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
    private var isOnAir = false
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
                isOnAir = detail.onAir?.toString()?.equals("true", ignoreCase = true) ?: false
                _state.update { it.copy(detail = detail) }
                Log.d("Zee5Watch", "loadContent contentId=$contentId isTvShow=${detail.isTvShow} onAir=$isOnAir")

                if (detail.isTvShow) {
                    val seasonData = apiService.getSeasons(contentId)
                    val seasons = seasonData.seasons ?: emptyList()
                    val latestSeason = seasons.lastOrNull()
                    val latestSeasonId = latestSeason?.id
                    val eps = latestSeason?.episodes ?: latestSeason?.episode ?: emptyList()
                    eps.mapNotNull { it.id }.forEach { seenEpisodeIds.add(it) }
                    val hasMore = eps.size < (latestSeason?.totalEpisodes ?: eps.size)
                    val sortedEps = sortEpisodes(eps)

                    _state.update {
                        it.copy(
                            seasons = seasons,
                            selectedSeasonId = latestSeasonId,
                            episodes = sortedEps,
                            hasMoreEpisodes = hasMore,
                            isLoading = false
                        )
                    }
                    Log.d("Zee5Watch", "episodes loaded: ${sortedEps.size}, hasMore=$hasMore, first=${sortedEps.firstOrNull()?.episodeNumber}, last=${sortedEps.lastOrNull()?.episodeNumber}")

                    if (epId != null) {
                        val ep = _state.value.episodes.find { it.id == epId }
                        if (ep != null) {
                            onEpisodeClick(ep)
                        } else {
                            var foundInSeason: com.movie.app.best.data.model.Zee5Season? = null
                            var foundEpisode: Zee5Item? = null
                            for (season in seasons) {
                                foundEpisode = (season.episodes ?: season.episode ?: emptyList()).find { it.id == epId }
                                if (foundEpisode != null) {
                                    foundInSeason = season
                                    break
                                }
                            }
                            if (foundEpisode != null && foundInSeason != null) {
                                selectSeason(foundInSeason.id ?: latestSeasonId ?: "")
                                onEpisodeClick(foundEpisode)
                            } else {
                                Log.w("Zee5Watch", "epId=$epId not found in any season, falling back to first")
                                val firstEp = _state.value.episodes.firstOrNull()
                                if (firstEp != null) onEpisodeClick(firstEp)
                            }
                        }
                    } else {
                        val firstEp = _state.value.episodes.firstOrNull()
                        Log.d("Zee5Watch", "auto-play firstEp=${firstEp?.id} epNum=${firstEp?.episodeNumber}")
                        if (firstEp != null) onEpisodeClick(firstEp)
                    }
                } else {
                    _state.update { it.copy(isLoading = false) }
                    playContent(contentId)
                }
            } catch (e: Exception) {
                Log.e("Zee5Watch", "loadContent failed", e)
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
        val sortedEps = sortEpisodes(eps)
        _state.update { it.copy(selectedSeasonId = seasonId, episodes = sortedEps, hasMoreEpisodes = hasMore, isLoadingMore = false) }
    }

    fun loadMoreEpisodes() {
        val seasonId = _state.value.selectedSeasonId ?: return
        if (_state.value.isLoadingMore || !_state.value.hasMoreEpisodes) return

        viewModelScope.launch {
            _state.update { it.copy(isLoadingMore = true) }
            try {
                episodePage = nextEpisodePage(episodePage)
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

                val combinedEpisodes = sortEpisodes(_state.value.episodes + newEpisodes)
                val effectiveTotal = if (total > 0) total else if (newEpisodes.size >= 25) Int.MAX_VALUE else combinedEpisodes.size
                val hasMore = newEpisodes.isNotEmpty() && combinedEpisodes.size < effectiveTotal
                _state.update {
                    it.copy(
                        episodes = combinedEpisodes,
                        hasMoreEpisodes = hasMore,
                        isLoadingMore = false
                    )
                }
                Log.d("Zee5Watch", "loadMoreEpisodes page=$episodePage new=${newEpisodes.size} totalLoaded=${combinedEpisodes.size} hasMore=$hasMore")
            } catch (e: Exception) {
                Log.e("Zee5Watch", "loadMoreEpisodes failed page=$episodePage", e)
                _state.update { it.copy(isLoadingMore = false, hasMoreEpisodes = false) }
            }
        }
    }

    private fun nextEpisodePage(current: Int): Int {
        return if (current == 0) 2 else current + 1
    }

    private fun sortEpisodes(episodes: List<Zee5Item>): List<Zee5Item> {
        return if (isOnAir) {
            episodes.sortedByDescending { it.episodeNumber ?: 0 }
        } else {
            episodes.sortedBy { it.episodeNumber ?: 0 }
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
                Log.d("Zee5Watch", "playContent id=$id")
                val playback = apiService.getPlayback(id)
                val url = playback.effectiveStreamUrl
                Log.d("Zee5Watch", "playback url=${url != null} isDrm=${playback.isDrm}")
                if (url != null) {
                    _state.update { it.copy(currentM3u8 = url, isPlaying = true) }
                } else {
                    _state.update { it.copy(error = "No stream URL available") }
                }
            } catch (e: Exception) {
                Log.e("Zee5Watch", "playContent failed id=$id", e)
                _state.update { it.copy(error = e.message ?: "Failed to load stream") }
            }
        }
    }

    fun onPlaybackReady() {
        _state.update { it.copy(error = null) }
    }
}
