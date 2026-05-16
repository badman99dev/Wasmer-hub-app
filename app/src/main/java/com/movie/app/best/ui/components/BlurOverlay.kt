package com.movie.app.best.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text

@Composable
fun BlurredContent(
    shouldBlur: Boolean,
    modifier: Modifier = Modifier,
    blurRadius: Int = 40,
    content: @Composable () -> Unit
) {
    if (!shouldBlur) {
        content()
        return
    }
    Box(modifier = modifier) {
        Box(modifier = Modifier.blur(blurRadius.dp)) {
            content()
        }
        BlurBadge(modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
fun BlurOverlay(
    shouldBlur: Boolean,
    modifier: Modifier = Modifier,
    blurRadius: Int = 40,
    label: String = "18+"
) {
    if (!shouldBlur) return
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        BlurBadge()
    }
}

@Composable
private fun BlurBadge(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFFE50914).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.VisibilityOff,
                contentDescription = null,
                tint = Color(0xFFE50914).copy(alpha = 0.9f),
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFE50914).copy(alpha = 0.85f))
                .padding(horizontal = 10.dp, vertical = 3.dp)
        ) {
            Text(
                text = "18+",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun StorylineWarningBadge(
    isSexual: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isSexual) return

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFFF6D00).copy(alpha = 0.85f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = "⚠ Mature Theme",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
