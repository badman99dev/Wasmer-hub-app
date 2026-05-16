package com.movie.app.best.ui.screens.moviedetail.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movie.app.best.data.model.WasmerDownloadLink
import com.movie.app.best.ui.theme.WasmerGreen
import com.movie.app.best.ui.theme.WasmerPurple
import com.movie.app.best.ui.theme.WasmerRed

@Composable
fun DownloadSection(
    downloadLinks: List<WasmerDownloadLink>,
    isDownloadLoading: Boolean,
    downloadStarted: Boolean,
    downloadError: String?,
    onStartDownload: (linkUrl: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        DetailSectionTitle(
            title = "Download",
            icon  = Icons.Default.Download
        )

        Column(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (downloadLinks.isEmpty()) {
                EmptyDownloadCard()
            } else {
                downloadLinks.forEach { link ->
                    DownloadLinkCard(
                        link               = link,
                        isLoading          = isDownloadLoading,
                        onDownload         = { onStartDownload(link.linkUrl) }
                    )
                }
            }

            // Success banner
            AnimatedVisibility(
                visible = downloadStarted,
                enter   = expandVertically(tween(300)) + fadeIn(tween(250)),
                exit    = shrinkVertically(tween(250)) + fadeOut(tween(200))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(WasmerGreen.copy(alpha = 0.12f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = WasmerGreen, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text     = "Download started via DownloadManager",
                        color    = WasmerGreen,
                        fontSize = 13.sp
                    )
                }
            }

            // Error
            if (downloadError != null) {
                Text(
                    text     = downloadError,
                    color    = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun DownloadLinkCard(
    link: WasmerDownloadLink,
    isLoading: Boolean,
    onDownload: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDownload
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon box
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(WasmerRed.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(20.dp),
                    color       = WasmerRed,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    Icons.Default.Download, null,
                    tint     = WasmerRed,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = link.label.ifEmpty { "Download" },
                color      = Color.White,
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
            if (link.fileSize.isNotEmpty() || link.type.isNotEmpty()) {
                Row(
                    modifier           = Modifier.padding(top = 2.dp),
                    verticalAlignment  = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (link.fileSize.isNotEmpty()) {
                        Text(link.fileSize, color = Color.White.copy(0.5f), fontSize = 12.sp)
                    }
                    if (link.type.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(WasmerPurple.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                link.type.uppercase(),
                                color      = Color(0xFFB388FF),
                                fontSize   = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Icon(
            Icons.Default.Download, null,
            tint     = Color.White.copy(0.3f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun EmptyDownloadCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(16.dp)
    ) {
        Text(
            text  = "No download links available",
            color = Color.White.copy(alpha = 0.35f),
            fontSize = 13.sp
        )
    }
}
