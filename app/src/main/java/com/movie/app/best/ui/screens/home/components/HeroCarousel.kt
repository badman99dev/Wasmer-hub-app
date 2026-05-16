package com.movie.app.best.ui.screens.home.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import coil.compose.AsyncImage
import com.movie.app.best.data.model.WasmerMovie
import com.movie.app.best.ui.components.BlurredContent
import com.movie.app.best.ui.theme.WasmerAmber
import com.movie.app.best.ui.theme.WasmerRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.seconds

/**
 * Full-screen hero carousel with auto-scroll, parallax effect, and pager dots.
 * Uses slider movies from the API.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeroCarousel(
    movies: List<WasmerMovie>,
    isLoading: Boolean,
    onPlayClick: (slug: String, isSeries: Boolean) -> Unit,
    onInfoClick: (slug: String, isSeries: Boolean) -> Unit
) {
    if (isLoading && movies.isEmpty()) {
        HeroSkeleton()
        return
    }
    if (movies.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { movies.size })
    val scope = rememberCoroutineScope()

    // Auto-scroll every 6 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(6.seconds)
            val next = (pagerState.currentPage + 1) % movies.size
            pagerState.animateScrollToPage(
                page = next,
                animationSpec = tween(700, easing = FastOutSlowInEasing)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(580.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val pageOffset = ((pagerState.currentPage - page) +
                    pagerState.currentPageOffsetFraction).absoluteValue

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Slight scale + alpha parallax
                        val scale = lerp(0.92f, 1f, 1f - pageOffset.coerceIn(0f, 1f))
                        scaleX = scale; scaleY = scale
                        alpha = lerp(0.7f, 1f, 1f - pageOffset.coerceIn(0f, 0.6f))
                    }
            ) {
                HeroSlide(
                    movie = movies[page],
                    onPlayClick = onPlayClick,
                    onInfoClick = onInfoClick
                )
            }
        }

        // ── Pager Dots ────────────────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(movies.size) { index ->
                val selected = pagerState.currentPage == index
                val width by animateDpAsState(
                    targetValue = if (selected) 20.dp else 6.dp,
                    animationSpec = tween(250),
                    label = "dot_width_$index"
                )
                Box(
                    modifier = Modifier
                        .size(width = width, height = 6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (selected) WasmerRed else Color.White.copy(alpha = 0.4f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            scope.launch {
                                pagerState.animateScrollToPage(
                                    page = index,
                                    animationSpec = tween(400)
                                )
                            }
                        }
                )
            }
        }
    }
}

// ── Single hero slide ─────────────────────────────────────
@Composable
private fun HeroSlide(
    movie: WasmerMovie,
    onPlayClick: (String, Boolean) -> Unit,
    onInfoClick: (String, Boolean) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        BlurredContent(
            shouldBlur = movie.shouldBlurPoster,
            modifier = Modifier.fillMaxSize(),
            moderationTypes = movie.flaggedModerationTypes,
            enableDoubleTap = true
        ) {
            AsyncImage(
                model = movie.posterUrl,
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Multi-stop gradient for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to Color.Black.copy(alpha = 0.25f),
                            0.40f to Color.Transparent,
                            0.65f to Color.Black.copy(alpha = 0.55f),
                            0.85f to Color.Black.copy(alpha = 0.85f),
                            1.00f to Color.Black
                        )
                    )
                )
        )

        // Content overlay at bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text(
                text = movie.title.uppercase(),
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 30.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Meta row
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (movie.releaseYear.isNotBlank()) {
                    Text(movie.releaseYear, color = Color.White.copy(0.75f), fontSize = 13.sp)
                    MetaDot()
                }
                if (movie.rating.isNotBlank()) {
                    Icon(
                        Icons.Default.Star, null,
                        tint = WasmerAmber,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(movie.rating, color = Color.White.copy(0.75f), fontSize = 13.sp)
                    MetaDot()
                }
                if (movie.qualityLabel.isNotBlank()) {
                    Text(
                        movie.qualityLabel,
                        color = when {
                            movie.qualityLabel.contains("4K", ignoreCase = true) -> Color(0xFFFFD700)
                            movie.qualityLabel.contains("UHD", ignoreCase = true) -> Color(0xFF00E5FF)
                            else -> Color(0xFF4FC3F7)
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (movie.isSeries) {
                    MetaDot()
                    Text("Series", color = Color(0xFFB388FF), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Buttons row
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                // Play button
                Button(
                    onClick = { onPlayClick(movie.slug, movie.isSeries) },
                    modifier = Modifier
                        .weight(1.3f)
                        .height(46.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WasmerRed,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Play", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                // Info button
                OutlinedButton(
                    onClick = { onInfoClick(movie.slug, movie.isSeries) },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.8f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Info, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Info", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }

        // Top-right badge
        if (movie.qualityLabel.isNotBlank()) {
            val qLabel = if (movie.isSeries) "SERIES" else movie.qualityLabel
            val bgColor = when {
                qLabel.contains("4K", ignoreCase = true) -> Color(0xFFFFD700).copy(alpha = 0.25f)
                qLabel.contains("UHD", ignoreCase = true) -> Color(0xFF00E5FF).copy(alpha = 0.2f)
                qLabel.contains("HD", ignoreCase = true) -> Color(0xFF4FC3F7).copy(alpha = 0.15f)
                qLabel == "SERIES" -> Color(0xFF7C4DFF).copy(alpha = 0.2f)
                else -> Color.White.copy(alpha = 0.15f)
            }
            val textColor = when {
                qLabel.contains("4K", ignoreCase = true) -> Color(0xFFFFD700)
                qLabel.contains("UHD", ignoreCase = true) -> Color(0xFF00E5FF)
                qLabel.contains("HD", ignoreCase = true) -> Color(0xFF4FC3F7)
                qLabel == "SERIES" -> Color(0xFFB388FF)
                else -> Color.White
            }
            val borderColor = textColor.copy(alpha = 0.3f)

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(bgColor, Color.White.copy(alpha = 0.08f), bgColor)
                        )
                    )
                    .border(
                        width = 0.5.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(borderColor, Color.White.copy(alpha = 0.1f), borderColor)
                        ),
                        shape = RoundedCornerShape(50)
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = qLabel,
                    color = textColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
private fun MetaDot() {
    Text("  •  ", color = Color.White.copy(0.4f), fontSize = 13.sp)
}

// ── Hero skeleton ─────────────────────────────────────────
@Composable
private fun HeroSkeleton() {
    val shimmer = rememberInfiniteTransition(label = "hero_shimmer")
    val alpha by shimmer.animateFloat(
        initialValue = 0.2f, targetValue = 0.5f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "alpha"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(580.dp)
            .background(Color.White.copy(alpha = alpha))
    )
}
