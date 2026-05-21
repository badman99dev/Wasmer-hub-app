package com.movie.app.best.ui.screens.moviedetail.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import coil.compose.AsyncImage
import com.movie.app.best.ui.components.BlurredContent
import com.movie.app.best.ui.components.ScreenshotViewer

@Composable
fun ScreenshotsSection(
    screenshots: List<String>,
    shouldBlur: Boolean = false,
    modifier: Modifier = Modifier
) {
    var viewerOpen by remember { mutableStateOf(false) }
    var viewerIndex by remember { mutableIntStateOf(0) }

    if (viewerOpen) {
        BackHandler(enabled = true) {
            viewerOpen = false
        }
        ScreenshotViewer(
            screenshots = screenshots,
            initialIndex = viewerIndex,
            shouldBlur = shouldBlur,
            onDismiss = { viewerOpen = false }
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        DetailSectionTitle(title = "Screenshots")
        LazyRow(
            contentPadding        = PaddingValues(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(screenshots) { index, url ->
                Card(
                    modifier = Modifier
                        .width(200.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            viewerIndex = index
                            viewerOpen = true
                        },
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
