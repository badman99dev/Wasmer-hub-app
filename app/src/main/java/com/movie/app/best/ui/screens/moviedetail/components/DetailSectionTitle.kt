package com.movie.app.best.ui.screens.moviedetail.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DetailSectionTitle(
    title: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(start = 18.dp, top = 18.dp, bottom = 8.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector  = icon,
                contentDescription = null,
                tint         = Color.White.copy(alpha = 0.7f),
                modifier     = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(7.dp))
        }
        Text(
            text       = title,
            color      = Color.White,
            fontSize   = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.2).sp
        )
    }
}
