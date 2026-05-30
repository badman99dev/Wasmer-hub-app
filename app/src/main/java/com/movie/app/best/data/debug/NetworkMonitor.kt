package com.movie.app.best.data.debug

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NetworkMonitor {
    private val _hasNetworkError = MutableStateFlow(false)
    val hasNetworkError: StateFlow<Boolean> = _hasNetworkError.asStateFlow()

    private val _refreshCounter = MutableStateFlow(0)
    val refreshCounter: StateFlow<Int> = _refreshCounter.asStateFlow()

    fun reportFailure() {
        _hasNetworkError.value = true
    }

    fun clearError() {
        _hasNetworkError.value = false
    }

    fun triggerRefresh() {
        _refreshCounter.value += 1
    }

    fun onBackOnline() {
        clearError()
        triggerRefresh()
    }
}
