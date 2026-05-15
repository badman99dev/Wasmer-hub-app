package com.movie.app.best.ui.screens.library

import androidx.compose.animation.core.animate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp

private val PullToRefreshThreshold = 80.dp

@Composable
fun PullToRefreshLayout(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val thresholdPx = with(LocalDensity.current) { PullToRefreshThreshold.toPx() }
    var progress by remember { mutableFloatStateOf(0f) }
    var isTriggered by remember { mutableStateOf(false) }

    val nestedScrollConnection = remember(thresholdPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (isRefreshing) return Offset.Zero
                if (available.y < 0f && progress > 0f) {
                    val consumed = minOf(-available.y, progress)
                    progress -= consumed
                    return Offset(0f, -consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (isRefreshing) return Offset.Zero
                if (available.y > 0f) {
                    progress = minOf(progress + available.y * 0.5f, thresholdPx * 1.5f)
                    if (progress >= thresholdPx && !isTriggered) {
                        isTriggered = true
                        onRefresh()
                    }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (progress > 0f && !isRefreshing) {
                    animate(initialValue = progress, targetValue = 0f) { value, _ ->
                        progress = value
                    }
                }
                return Velocity.Zero
            }
        }
    }

    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) {
            isTriggered = false
            animate(initialValue = progress, targetValue = 0f) { value, _ ->
                progress = value
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        val indicatorOffset = with(LocalDensity.current) { progress.toDp() }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(indicatorOffset)
                .align(Alignment.TopCenter),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (progress > 0f || isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .size(28.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.5.dp
                )
            }
        }
        content()
    }
}
