package com.movie.app.best.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.movie.app.best.BuildConfig
import com.movie.app.best.data.model.Resource
import com.movie.app.best.data.model.UpdateResponse
import com.movie.app.best.data.repository.ApkUpdateRepository
import com.movie.app.best.data.repository.ApkUpdateState
import com.movie.app.best.data.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val repository: MovieRepository,
    private val apkRepo: ApkUpdateRepository
) : ViewModel() {

    private val _state = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    private var currentDownloadId: Long = -1L

    fun checkForUpdate() {
        _state.value = UpdateUiState.Checking
        viewModelScope.launch {
            when (val result = repository.checkForUpdate(BuildConfig.VERSION_CODE)) {
                is Resource.Success -> {
                    val data = result.data!!
                    if (data.updateAvailable) {
                        _state.value = UpdateUiState.UpdateAvailable(data)
                    } else {
                        _state.value = UpdateUiState.UpToDate
                    }
                }
                is Resource.Error -> {
                    _state.value = UpdateUiState.Error(result.error ?: "Unknown error")
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun startDownload(context: Context, url: String) {
        apkRepo.cleanupOldApk(context)
        currentDownloadId = apkRepo.downloadApk(context, url)
        _state.value = UpdateUiState.Downloading(progress = 0)

        viewModelScope.launch {
            apkRepo.observeUpdateProgress(context, currentDownloadId).collect { state ->
                when (state) {
                    is ApkUpdateState.Downloading -> {
                        _state.value = UpdateUiState.Downloading(state.progress)
                    }
                    is ApkUpdateState.Completed -> {
                        _state.value = UpdateUiState.DownloadComplete(state.file)
                    }
                    is ApkUpdateState.Error -> {
                        _state.value = UpdateUiState.Error(state.message)
                    }
                    is ApkUpdateState.Cancelled -> {
                        _state.value = UpdateUiState.Error("Download cancelled")
                    }
                    else -> {}
                }
            }
        }
    }

    fun installApk(context: Context, file: java.io.File) {
        apkRepo.installApk(context, file)
    }

    fun resetState() {
        _state.value = UpdateUiState.Idle
    }
}

sealed class UpdateUiState {
    data object Idle : UpdateUiState()
    data object Checking : UpdateUiState()
    data object UpToDate : UpdateUiState()
    data class UpdateAvailable(val data: UpdateResponse) : UpdateUiState()
    data class Downloading(val progress: Int) : UpdateUiState()
    data class DownloadComplete(val file: java.io.File) : UpdateUiState()
    data class Error(val message: String) : UpdateUiState()
}
