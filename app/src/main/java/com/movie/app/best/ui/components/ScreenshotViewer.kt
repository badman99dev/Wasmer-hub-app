package com.movie.app.best.ui.components

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun ScreenshotViewer(
    screenshots: List<String>,
    initialIndex: Int = 0,
    shouldBlur: Boolean = false,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { screenshots.size }, initialPage = initialPageCoerce(initialIndex, screenshots.size))
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
    ) {
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .zIndex(2f)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f))
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        Text(
            text = "${pagerState.currentPage + 1} / ${screenshots.size}",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 22.dp)
                .zIndex(2f)
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            var scale by remember { mutableFloatStateOf(1f) }
            var panX by remember { mutableFloatStateOf(0f) }
            var panY by remember { mutableFloatStateOf(0f) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            if (scale > 1f) {
                                panX += pan.x
                                panY += pan.y
                            } else {
                                panX = 0f
                                panY = 0f
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                BlurredContent(
                    shouldBlur = shouldBlur,
                    modifier = Modifier.fillMaxSize()
                ) {
                    AsyncImage(
                        model = screenshots[page],
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = panX
                                translationY = panY
                            }
                    )
                }
            }
        }

        if (screenshots.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (pagerState.currentPage > 0) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1, animationSpec = tween(300))
                            }
                        }
                    },
                    modifier = Modifier
                        .padding(start = 24.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color.White.copy(alpha = 0.2f), Color.White.copy(alpha = 0.08f))
                            )
                        ),
                    enabled = pagerState.currentPage > 0
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous",
                        tint = if (pagerState.currentPage > 0) Color.White else Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = {
                        if (pagerState.currentPage < screenshots.size - 1) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1, animationSpec = tween(300))
                            }
                        }
                    },
                    modifier = Modifier
                        .padding(end = 24.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color.White.copy(alpha = 0.2f), Color.White.copy(alpha = 0.08f))
                            )
                        ),
                    enabled = pagerState.currentPage < screenshots.size - 1
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next",
                        tint = if (pagerState.currentPage < screenshots.size - 1) Color.White else Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

private fun initialPageCoerce(index: Int, size: Int): Int {
    return if (size == 0) 0 else index.coerceIn(0, size - 1)
}
