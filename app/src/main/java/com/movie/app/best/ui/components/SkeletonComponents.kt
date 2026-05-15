package com.movie.app.best.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Smooth shimmer brush — no lag, pure Compose, no external lib needed
@Composable
fun shimmerBrush(baseColor: Color = Color(0xFF1C1C1C)): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmerX by transition.animateFloat(
        initialValue = -600f,
        targetValue = 1400f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerX"
    )
    return Brush.linearGradient(
        colors = listOf(
            baseColor,
            Color(0xFF2E2E2E),
            Color(0xFF3A3A3A),
            Color(0xFF2E2E2E),
            baseColor
        ),
        start = Offset(shimmerX, 0f),
        end = Offset(shimmerX + 600f, 300f)
    )
}

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp)
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(shimmerBrush())
    )
}

@Composable
fun SkeletonLine(
    width: Dp = 120.dp,
    height: Dp = 14.dp
) {
    SkeletonBox(
        modifier = Modifier.width(width).height(height),
        shape = RoundedCornerShape(4.dp)
    )
}

@Composable
fun SkeletonCircle(size: Dp = 40.dp) {
    SkeletonBox(
        modifier = Modifier.size(size),
        shape = CircleShape
    )
}

@Composable
fun SkeletonPosterCard(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        SkeletonBox(
            modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f),
            shape = RoundedCornerShape(8.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        SkeletonLine(width = 90.dp, height = 11.dp)
        Spacer(modifier = Modifier.height(3.dp))
        SkeletonLine(width = 55.dp, height = 10.dp)
    }
}

@Composable
fun SkeletonHeroSlide() {
    Box(modifier = Modifier.fillMaxWidth().height(560.dp)) {
        SkeletonBox(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(0.dp))
        Column(
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.BottomStart)
                .padding(20.dp)
        ) {
            SkeletonLine(width = 210.dp, height = 26.dp)
            Spacer(modifier = Modifier.height(10.dp))
            SkeletonLine(width = 150.dp, height = 14.dp)
            Spacer(modifier = Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SkeletonBox(modifier = Modifier.width(130.dp).height(46.dp), shape = RoundedCornerShape(23.dp))
                SkeletonBox(modifier = Modifier.width(110.dp).height(46.dp), shape = RoundedCornerShape(23.dp))
            }
        }
    }
}

@Composable
fun SkeletonMovieRow(count: Int = 6) {
    Column {
        SkeletonLine(width = 90.dp, height = 18.dp)
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            repeat(count) {
                SkeletonPosterCard(modifier = Modifier.width(120.dp))
            }
        }
    }
}

@Composable
fun SkeletonMovieGrid(rows: Int = 3) {
    Column(
        modifier = Modifier.padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SkeletonLine(width = 90.dp, height = 18.dp)
        Spacer(modifier = Modifier.height(4.dp))
        repeat(rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                repeat(3) {
                    SkeletonPosterCard(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun SkeletonCategoryCard(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        SkeletonBox(
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            shape = RoundedCornerShape(10.dp)
        )
        Spacer(modifier = Modifier.height(5.dp))
        SkeletonLine(width = 100.dp, height = 12.dp)
    }
}

@Composable
fun SkeletonDetailPage() {
    Column {
        SkeletonHeroSlide()
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) {
                    SkeletonBox(modifier = Modifier.width(55.dp).height(24.dp), shape = RoundedCornerShape(12.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            repeat(3) {
                SkeletonLine(width = (220..290).random().dp, height = 13.dp)
                Spacer(modifier = Modifier.height(7.dp))
            }
            Spacer(modifier = Modifier.height(18.dp))
            SkeletonLine(width = 70.dp, height = 17.dp)
            Spacer(modifier = Modifier.height(7.dp))
            SkeletonLine(width = 210.dp, height = 13.dp)
            Spacer(modifier = Modifier.height(22.dp))
            repeat(2) {
                SkeletonBox(modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(12.dp))
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun SkeletonLibraryPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            SkeletonLine(width = 100.dp, height = 28.dp)
            Spacer(modifier = Modifier.weight(1f))
            SkeletonCircle(size = 28.dp)
        }
        SkeletonMovieRow(count = 6)
        Spacer(modifier = Modifier.height(20.dp))
        SkeletonMovieRow(count = 6)
        Spacer(modifier = Modifier.height(20.dp))
        SkeletonMovieRow(count = 6)
    }
}
