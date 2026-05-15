package com.movie.app.best.ui.screens.moviedetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movie.app.best.ui.screens.moviedetail.components.DetailSectionTitle

/**
 * Cast & Director section — text-only (no cast images per requirement).
 */
@Composable
fun CastSection(
    director: String,
    cast: String,
    modifier: Modifier = Modifier
) {
    if (director.isEmpty() && cast.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        DetailSectionTitle("Cast & Crew")

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (director.isNotEmpty()) {
                CastRow(label = "Director", value = director)
            }
            if (cast.isNotEmpty()) {
                CastRow(label = "Starring", value = cast)
            }
        }
    }
}

@Composable
private fun CastRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text       = "$label:  ",
            color      = Color.White.copy(alpha = 0.45f),
            fontSize   = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier   = Modifier.width(72.dp)
        )
        Text(
            text       = value,
            color      = Color.White.copy(alpha = 0.85f),
            fontSize   = 13.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 19.sp,
            modifier   = Modifier.weight(1f)
        )
    }
}
