package com.movie.app.best.ui.screens.home.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.movie.app.best.data.model.LiveChannel
import com.movie.app.best.data.model.LiveChannels

@Composable
fun LiveChannelsCarousel(
    onChannelClick: (LiveChannel) -> Unit,
) {
    val channels = remember { LiveChannels.all }
    val listState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        // ── Section Title: ● LIVE TV ────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Red dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color(0xFFFF0000), CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "LIVE TV",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        // ── Horizontal scrollable channel circles ───────
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(
                items = channels,
                key = { it.id }
            ) { channel ->
                LiveChannelCircle(
                    channel = channel,
                    onClick = { onChannelClick(channel) }
                )
            }
        }
    }
}

@Composable
private fun LiveChannelCircle(
    channel: LiveChannel,
    onClick: () -> Unit
) {
    val redGradient = Brush.linearGradient(
        colors = listOf(Color(0xFFFF0000), Color(0xFFFF4081)),
        start = Offset(0f, 0f),
        end = Offset(1f, 1f)
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        // ── Circle with red border + logo + LIVE badge ──
        Box(
            modifier = Modifier
                .size(72.dp)
                .drawWithCache {
                    // Red gradient border (like YouTube live ring)
                    onDrawWithContent {
                        drawCircle(
                            brush = redGradient,
                            radius = size.minDimension / 2,
                            center = center,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f * density)
                        )
                        drawContent()
                    }
                }
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            // Channel logo inside circle
            AsyncImage(
                model = channel.logoUrl,
                contentDescription = channel.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // LIVE badge — bottom center, overlapping border
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 8.dp)
                    .background(
                        color = Color(0xFFFF0000),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "LIVE",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.5f),
                            offset = Offset(0f, 1f),
                            blurRadius = 2f
                        )
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Channel name below circle ──────────────────
        Text(
            text = channel.name,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth()
        )
    }
}