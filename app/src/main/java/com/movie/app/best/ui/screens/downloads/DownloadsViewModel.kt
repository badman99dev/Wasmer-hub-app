package com.movie.app.best.ui.screens.downloads

import android.app.DownloadManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.model.Resource
import com.movie.app.best.data.repository.DownloadProgress
import com.movie.app.best.data.repository.DownloadRepository
import com.movie.app.best.data.debug.NetworkLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ActiveDownload(
    val downloadId: Long,
    val title: String,
    val fileName: String,
    val progress: DownloadProgress?,
    val speedBytesPerSec: Double,
    val eta: String,
    val isResumable: Boolean
)

data class CompletedDownload(
    val downloadId: Long,
    val fileName: String,
    val fileSize: String,
    val fileUri: String?
)

data class DownloadsUiState(
    val activeDownloads: List<ActiveDownload> = emptyList(),
    val completedDownloads: List<CompletedDownload> = emptyList(),
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

    private val trackedDownloads = mutableMapOf<Long, String>()
    private var progressJob: Job? = null

    init {
        startProgressTracking()
        loadCompletedDownloads()
        loadActiveDownloads()
    }

    fun loadActiveDownloads() {
        viewModelScope.launch {
            val query = DownloadManager.Query().setFilterByStatus(
                DownloadManager.STATUS_RUNNING or
                DownloadManager.STATUS_PENDING or
                DownloadManager.STATUS_PAUSED
            )
            val cursor = (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).query(query)
            cursor?.use {
                while (it.moveToNext()) {
                    val id = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_ID))
                    val uri = it.getString(it.getColumnIndexOrThrow(DownloadManager.COLUMN_URI))
                    if (uri != null && (uri.contains("wasmer") || uri.contains("mkv") || uri.contains("mp4") || uri.contains("WasmerHub") || uri.contains("diskcdn"))) {
                        val title = it.getString(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE)) ?: "Download"
                        trackedDownloads[id] = title
                    }
                }
            }
        }
    }

    fun resolveAndDownload(slug: String, linkId: Int, title: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isResolving = true, resolveError = null, resolveSuccess = false) }
            repository.resolveDownloadUrl(slug, linkId).collect { result ->
                when (result) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        val directUrl = result.data ?: return@collect
                        val fileName = extractFileName(directUrl, title)
                        val downloadId = repository.startDownload(directUrl, fileName, title)
                        trackedDownloads[downloadId] = title
                        _uiState.update { it.copy(isResolving = false, resolveSuccess = true) }
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(isResolving = false, resolveError = result.error) }
                    }
                }
            }
        }
    }

    fun cancelDownload(downloadId: Long) {
        repository.cancelDownload(downloadId)
        trackedDownloads.remove(downloadId)
        _uiState.update { state ->
            state.copy(activeDownloads = state.activeDownloads.filter { it.downloadId != downloadId })
        }
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                updateProgress()
                delay(1000)
            }
        }
    }

    private suspend fun updateProgress() {
        val activeList = mutableListOf<ActiveDownload>()
        val completedList = mutableListOf<CompletedDownload>()
        val toRemove = mutableListOf<Long>()

        for ((downloadId, title) in trackedDownloads) {
            val progress = repository.getDownloadProgress(downloadId)
            if (progress == null) {
                toRemove.add(downloadId)
                continue
            }

            if (progress.isCompleted) {
                completedList.add(
                    CompletedDownload(
                        downloadId = downloadId,
                        fileName = progress.fileName.ifEmpty { title },
                        fileSize = formatFileSize(progress.totalBytes),
                        fileUri = null
                    )
                )
                toRemove.add(downloadId)
            } else if (progress.isFailed) {
                NetworkLogger.logAction("DOWNLOAD_FAILED", "id=$downloadId reason=${progress.reason}")
                toRemove.add(downloadId)
            } else {
                val speed = repository.calculateSpeed(downloadId)
                val eta = repository.calculateEta(downloadId)
                val isResumable = progress.isPaused || progress.isRunning
                activeList.add(
                    ActiveDownload(
                        downloadId = downloadId,
                        title = title,
                        fileName = progress.fileName.ifEmpty { title },
                        progress = progress,
                        speedBytesPerSec = speed,
                        eta = eta,
                        isResumable = isResumable
                    )
                )
            }
        }

        toRemove.forEach { trackedDownloads.remove(it) }

        val existingCompleted = _uiState.value.completedDownloads.toMutableList()
        completedList.forEach { newCompleted ->
            if (!existingCompleted.any { it.downloadId == newCompleted.downloadId }) {
                existingCompleted.add(newCompleted)
            }
        }

        _uiState.update {
            it.copy(
                activeDownloads = activeList,
                completedDownloads = existingCompleted
            )
        }
    }

    fun loadCompletedDownloads() {
        viewModelScope.launch {
            val completed = repository.getCompletedDownloads()
            val completedList = completed.map { progress ->
                CompletedDownload(
                    downloadId = progress.downloadId,
                    fileName = progress.fileName,
                    fileSize = formatFileSize(progress.totalBytes),
                    fileUri = null
                )
            }
            _uiState.update { it.copy(completedDownloads = completedList) }
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

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024L * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    override fun onCleared() {
        super.onCleared()
        progressJob?.cancel()
    }
}
