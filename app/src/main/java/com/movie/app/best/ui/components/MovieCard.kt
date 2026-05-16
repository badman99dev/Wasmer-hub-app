package com.movie.app.best.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.movie.app.best.data.model.WasmerMovie

@Composable
fun MovieCard(
    movie: WasmerMovie,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "card_scale"
    )

    Column(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick(movie.slug) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
        ) {
            SubcomposeAsyncImage(
                model = movie.posterUrl.ifBlank { null },
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp)),
                loading = {
                    Box(modifier = Modifier.fillMaxSize().aspectRatio(2f/3f).background(Color(0xFF1C1C1C)))
                },
                error = {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize().aspectRatio(2f/3f).background(Color(0xFF1C1C1C))
                    ) {
                        Icon(Icons.Default.BrokenImage, contentDescription = null, tint = Color(0xFF555555))
                    }
                }
            )

            if (movie.qualityLabel.isNotBlank()) {
                QualityBadge(
                    label = movie.qualityLabel,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(5.dp)
                )
            }

            BlurOverlay(
                shouldBlur = movie.shouldBlurPoster,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                        )
                    )
                    .padding(horizontal = 6.dp, vertical = 8.dp)
            ) {
                Text(
                    text = movie.title,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun QualityBadge(label: String, modifier: Modifier = Modifier) {
    val bgColor = when {
        label.contains("4K", ignoreCase = true) -> Color(0xFFD4A017).copy(alpha = 0.35f)
        label.contains("UHD", ignoreCase = true) -> Color(0xFFD4A017).copy(alpha = 0.3f)
        label.contains("HD", ignoreCase = true) -> Color(0xFF4FC3F7).copy(alpha = 0.2f)
        label.contains("CAM", ignoreCase = true) -> Color(0xFFFF5252).copy(alpha = 0.25f)
        else -> Color.White.copy(alpha = 0.18f)
    }
    val textColor = when {
        label.contains("4K", ignoreCase = true) -> Color(0xFFFFD700)
        label.contains("UHD", ignoreCase = true) -> Color(0xFFF0C040)
        label.contains("HD", ignoreCase = true) -> Color(0xFF4FC3F7)
        label.contains("CAM", ignoreCase = true) -> Color(0xFFFF5252)
        else -> Color.White
    }
    val borderColor = textColor.copy(alpha = 0.4f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        bgColor,
                        Color.White.copy(alpha = 0.14f),
                        bgColor
                    )
                )
            )
            .border(
                width = 0.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(borderColor, Color.White.copy(alpha = 0.18f), borderColor)
                ),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun ErrorView(error: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = error, color = Color(0xFFB3B3B3), fontSize = 14.sp)
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE50914))
                    .clickable { onRetry() }
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            ) {
                Text("Retry", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
