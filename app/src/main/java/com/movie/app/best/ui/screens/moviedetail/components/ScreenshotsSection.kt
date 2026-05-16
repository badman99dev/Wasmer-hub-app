package com.movie.app.best.ui.screens.moviedetail.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.movie.app.best.ui.components.BlurredContent

@Composable
fun ScreenshotsSection(
    screenshots: List<String>,
    shouldBlur: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        DetailSectionTitle(title = "Screenshots")
        LazyRow(
            contentPadding        = PaddingValues(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(screenshots) { url ->
                Card(
                    modifier = Modifier
                        .width(200.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    shape  = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
                ) {
                    Box {
                        BlurredContent(
                            shouldBlur = shouldBlur,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 130.dp)
                        ) {
                            AsyncImage(
                                model              = url,
                                contentDescription = null,
                                contentScale       = ContentScale.Fit,
                                modifier           = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 130.dp)
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
