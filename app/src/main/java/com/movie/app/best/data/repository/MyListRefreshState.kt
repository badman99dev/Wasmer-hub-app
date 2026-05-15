package com.movie.app.best.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object MyListRefreshState {
    private val _isMyListRefreshed = MutableStateFlow(false)
    val isMyListRefreshed: StateFlow<Boolean> = _isMyListRefreshed.asStateFlow()

    fun markStale() {
        _isMyListRefreshed.value = false
    }

    fun markRefreshed() {
        _isMyListRefreshed.value = true
    }
}
