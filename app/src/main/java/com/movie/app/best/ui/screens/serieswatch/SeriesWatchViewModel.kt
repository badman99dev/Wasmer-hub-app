package com.movie.app.best.ui.screens.serieswatch

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.model.ExtractionState
import com.movie.app.best.data.model.GemmaExtractionResult
import com.movie.app.best.data.model.ImdbEpisode
import com.movie.app.best.data.model.WatchEpisode
import com.movie.app.best.data.remote.GemmaExtractorService
import com.movie.app.best.data.remote.ImdbApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SeriesWatchViewModel @Inject constructor(
    private val gemmaExtractor: GemmaExtractorService,
    private val imdbApi: ImdbApiService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val imdbId: String = savedStateHandle["imdbId"] ?: ""
    private val seriesTitle: String = savedStateHandle["title"] ?: ""
    private val movieId: String = savedStateHandle["movieId"] ?: ""
    private val slug: String = savedStateHandle["slug"] ?: ""

    private val _state = MutableStateFlow(ExtractionState())
    val state: StateFlow<ExtractionState> = _state.asStateFlow()

    private val imdbCache = mutableMapOf<Int, List<ImdbEpisode>>()

    init {
        loadAll()
    }

    private fun loadAll() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val gemmaResult = gemmaExtractor.extract(imdbId)

            if (gemmaResult.error != null && gemmaResult.seasons.isEmpty()) {
                _state.update { it.copy(isLoading = false, error = gemmaResult.error) }
                return@launch
            }

            val firstSeason = gemmaResult.seasons.keys.minOrNull() ?: 1
            _state.update {
                it.copy(
                    isLoading = false,
                    result = gemmaResult,
                    selectedSeason = firstSeason,
                    error = null
                )
            }

            collectAvailableLanguages(gemmaResult)
            loadImdbEpisodes(firstSeason)
        }
    }

    private fun collectAvailableLanguages(result: GemmaExtractionResult) {
        val langs = mutableSetOf<String>()
        for (season in result.seasons.values) {
            for (ep in season.episodes.values) {
                langs.addAll(ep.languages.keys)
            }
        }
        val sorted = langs.sortedWith(compareBy { lang ->
            when {
                lang.contains("Hindi", ignoreCase = true) -> 0
                lang.contains("English", ignoreCase = true) -> 1
                else -> 2
            }.thenBy { it }
        })
        val default = sorted.firstOrNull { it.contains("Hindi", ignoreCase = true) } ?: sorted.firstOrNull() ?: "Hindi"
        _state.update { it.copy(availableLanguages = sorted, selectedLanguage = default) }
    }

    fun loadImdbEpisodes(seasonNo: Int) {
        if (imdbCache.containsKey(seasonNo)) {
            _state.update { it.copy(imdbEpisodes = it.imdbEpisodes.toMutableMap().apply { put(seasonNo, imdbCache[seasonNo]!! }) }
            return
        }
        viewModelScope.launch {
            try {
                val resp = imdbApi.getEpisodes(imdbId, seasonNo)
                val eps = resp.episodes
                imdbCache[seasonNo] = eps
                _state.update { it.copy(imdbEpisodes = it.imdbEpisodes.toMutableMap().apply { put(seasonNo, eps) }) }
            } catch (_: Exception) { }
        }
    }

    fun selectSeason(seasonNo: Int) {
        _state.update { it.copy(selectedSeason = seasonNo) }
        loadImdbEpisodes(seasonNo)
    }

    fun selectLanguage(lang: String) {
        _state.update { it.copy(selectedLanguage = lang) }
        val currentEp = _state.value.currentEpisode
        if (currentEp != null) {
            val m3u8 = currentEp.languages[lang]
            if (m3u8 != null) {
                resolveAndPlay(m3u8, currentEp)
            }
        }
    }

    fun onEpisodeClick(episode: WatchEpisode) {
        val lang = _state.value.selectedLanguage
        val file = episode.languages[lang] ?: episode.languages.values.firstOrNull() ?: return
        _state.update { it.copy(currentEpisode = episode) }
        resolveAndPlay(file, episode)
    }

    private fun resolveAndPlay(file: String, episode: WatchEpisode) {
        val csrfKey = _state.value.result?.csrfKey ?: return
        viewModelScope.launch {
            _state.update { it.copy(currentM3u8 = null) }
            val m3u8 = gemmaExtractor.resolveFile(file, csrfKey)
            if (m3u8 != null) {
                _state.update { it.copy(currentM3u8 = m3u8) }
            }
        }
    }
}
