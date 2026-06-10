package com.movie.app.best.ui.screens.serieswatch.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SeasonChips(
    seasons: List<Int>,
    selectedSeason: Int,
    onSeasonSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(seasons) { seasonNo ->
            val isSelected = seasonNo == selectedSeason
            val shape = RoundedCornerShape(50)
            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(
                        if (isSelected) Brush.linearGradient(colors = listOf(Color(0xFFE50914), Color(0xFFB71C1C)))
                        else Brush.linearGradient(colors = listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.04f)))
                    )
                    .border(
                        width = if (isSelected) 1.dp else 0.5.dp,
                        brush = if (isSelected) Brush.linearGradient(colors = listOf(Color(0xFFFF5252), Color(0xFFFFD700), Color(0xFFFF5252)))
                        else BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)).brush ?: Brush.linearGradient(colors = listOf(Color.White.copy(alpha = 0.15f))),
                        shape = shape
                    )
                    .clickable { onSeasonSelect(seasonNo) }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Season $seasonNo",
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}
