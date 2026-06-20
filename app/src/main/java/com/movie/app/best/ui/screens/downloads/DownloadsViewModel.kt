package com.movie.app.best.ui.screens.downloads

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.model.Resource
import com.movie.app.best.data.repository.DownloadRepository
import com.ketch.DownloadModel
import com.ketch.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DownloadsUiState(
    val activeDownloads: List<DownloadModel> = emptyList(),
    val completedDownloads: List<DownloadModel> = emptyList(),
    val isResolving: Boolean = false,
    val resolveError: String? = null,
    val resolveSuccess: Boolean = false
)

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val repository: DownloadRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

    init {
        observeDownloads()
    }

    private fun observeDownloads() {
        viewModelScope.launch {
            repository.observeDownloads().collect { downloads ->
                val active = downloads.filter {
                    it.status == Status.QUEUED || it.status == Status.STARTED ||
                    it.status == Status.PROGRESS || it.status == Status.PAUSED ||
                    it.status == Status.FAILED
                }
                val completed = downloads.filter { it.status == Status.SUCCESS }
                _uiState.update {
                    it.copy(activeDownloads = active, completedDownloads = completed)
                }
            }
        }
    }

    fun resolveAndDownload(linkUrl: String, title: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isResolving = true, resolveError = null, resolveSuccess = false) }
            repository.resolveDownloadUrl(linkUrl).collect { result ->
                when (result) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        val directUrl = result.data ?: return@collect
                        val fileName = extractFileName(directUrl, title)
                        repository.startDownload(directUrl, fileName, title)
                        _uiState.update { it.copy(isResolving = false, resolveSuccess = true) }
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(isResolving = false, resolveError = result.error) }
                    }
                }
            }
        }
    }

    fun pauseDownload(id: Int) { repository.pauseDownload(id) }
    fun resumeDownload(id: Int) { repository.resumeDownload(id) }
    fun cancelDownload(id: Int) { repository.cancelDownload(id) }
    fun retryDownload(id: Int) { repository.retryDownload(id) }
    fun deleteDownload(id: Int) { repository.deleteDownload(id) }

    private fun extractFileName(url: String, fallback: String): String {
        return try {
            val path = android.net.Uri.parse(url).path ?: fallback
            val name = path.substringAfterLast("/")
            if (name.contains(".")) name else "$fallback.mkv"
        } catch (_: Exception) {
            "$fallback.mkv"
        }
    }
}
