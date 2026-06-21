package com.movie.app.best.ui.screens.downloads

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.data.model.DownloadMetadata
import com.movie.app.best.data.model.DownloadPhase
import com.movie.app.best.data.model.Resource
import com.movie.app.best.data.repository.DownloadRepository
import com.ketch.DownloadModel
import com.ketch.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DownloadsUiState(
    val activeDownloads: List<DownloadModel> = emptyList(),
    val completedDownloads: List<DownloadModel> = emptyList(),
    val downloadMetadata: List<DownloadMetadata> = emptyList(),
    val extractedPacks: List<DownloadMetadata> = emptyList(),
    val isResolving: Boolean = false,
    val resolveError: String? = null,
    val resolveSuccess: Boolean = false,
    val isRescanning: Boolean = false
)

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val repository: DownloadRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

    private val processedKetchIds = mutableSetOf<Int>()

    init {
        rescanDownloads()
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

                completed.forEach { dl ->
                    if (dl.id !in processedKetchIds) {
                        processedKetchIds.add(dl.id)
                        checkAndExtractZip(dl.id, dl.fileName)
                    }
                }

                val allMeta = repository.getAllMetadata()
                val packs = allMeta.filter { it.isZip && it.extractPath != null }
                _uiState.update {
                    it.copy(
                        activeDownloads = active,
                        completedDownloads = completed,
                        downloadMetadata = allMeta,
                        extractedPacks = packs
                    )
                }
            }
        }
    }

    private fun checkAndExtractZip(ketchId: Int, fileName: String) {
        viewModelScope.launch {
            val allMeta = repository.getAllMetadata()
            val meta = allMeta.find { it.ketchId == ketchId }

            if (meta != null && meta.isZip && meta.extractPath == null) {
                val metaKey = meta.slug + (meta.episodeLabel ?: "")
                repository.postProcessDownload(ketchId, metaKey)

                val updatedMeta = repository.getAllMetadata()
                val packs = updatedMeta.filter { it.isZip && it.extractPath != null }
                _uiState.update {
                    it.copy(
                        downloadMetadata = updatedMeta,
                        extractedPacks = packs
                    )
                }
            } else if (meta == null && isZipFile(fileName)) {
                val slug = fileName.substringBeforeLast(".").replace(Regex("[^a-zA-Z0-9-]"), "-").lowercase()
                val metaKey = slug
                val newMeta = DownloadMetadata(
                    slug = slug,
                    title = fileName.substringBeforeLast("."),
                    fileName = fileName,
                    filePath = android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOWNLOADS
                    ).path + "/WasmerHub/$fileName",
                    ketchId = ketchId,
                    isZip = true,
                    contentType = "series",
                    status = "extracting"
                )
                repository.saveMetadataDirect(metaKey, newMeta)
                repository.postProcessDownload(ketchId, metaKey)

                val updatedMeta = repository.getAllMetadata()
                val packs = updatedMeta.filter { it.isZip && it.extractPath != null }
                _uiState.update {
                    it.copy(
                        downloadMetadata = updatedMeta,
                        extractedPacks = packs
                    )
                }
            }
        }
    }

    private fun isZipFile(fileName: String): Boolean {
        val f = fileName.lowercase()
        return f.endsWith(".zip") || f.endsWith(".rar") || f.endsWith(".7z")
    }

    fun rescanDownloads() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRescanning = true) }
            repository.rescanDownloads()

            val allMeta = repository.getAllMetadata()
            allMeta.filter { it.isZip && it.extractPath == null && it.ketchId >= 0 }.forEach { meta ->
                repository.postProcessDownload(meta.ketchId, meta.slug + (meta.episodeLabel ?: ""))
            }

            val finalMeta = repository.getAllMetadata()
            val packs = finalMeta.filter { it.isZip && it.extractPath != null }
            _uiState.update {
                it.copy(
                    downloadMetadata = finalMeta,
                    extractedPacks = packs,
                    isRescanning = false
                )
            }
        }
    }

    fun getMetadataForKetchId(ketchId: Int): DownloadMetadata? {
        return repository.getAllMetadata().find { it.ketchId == ketchId }
    }

    fun resolveAndDownload(linkUrl: String, title: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isResolving = true, resolveError = null, resolveSuccess = false) }
            repository.resolveDownloadUrls(linkUrl).collect { result ->
                when (result) {
                    is Resource.Loading -> {}
                    is Resource.Success -> {
                        val mirrors = result.data ?: emptyList()
                        val first = mirrors.firstOrNull()
                        if (first != null) {
                            repository.startDownload(first.jackpot, first.fileName, title)
                            _uiState.update { it.copy(isResolving = false, resolveSuccess = true) }
                        } else {
                            _uiState.update { it.copy(isResolving = false, resolveError = "No mirrors found") }
                        }
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
}
