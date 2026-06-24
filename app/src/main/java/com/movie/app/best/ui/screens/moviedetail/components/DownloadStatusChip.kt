package com.movie.app.best.ui.screens.moviedetail.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.movie.app.best.data.model.DownloadPhase
import com.movie.app.best.ui.theme.WasmerGreen
import com.movie.app.best.ui.theme.WasmerRed

@Composable
fun DownloadStatusChip(
    phase: DownloadPhase,
    progress: Int,
    isZip: Boolean,
    failureReason: String?,
    onPlay: () -> Unit,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = phase != DownloadPhase.NONE,
        enter = slideInVertically(tween(300)) + fadeIn(tween(200)),
        exit = slideOutVertically(tween(200)) + fadeOut(tween(150))
    ) {
        val accentColor = when (phase) {
            DownloadPhase.COMPLETE -> WasmerGreen
            DownloadPhase.FAILED -> WasmerRed
            DownloadPhase.CANCELLED -> Color(0xFFFB923C)
            else -> if (isZip) Color(0xFFB388FF) else WasmerGreen
        }

        val icon = when (phase) {
            DownloadPhase.COMPLETE -> Icons.Default.CheckCircle
            DownloadPhase.FAILED, DownloadPhase.CANCELLED -> Icons.Default.Close
            DownloadPhase.EXTRACTING -> Icons.Default.FolderZip
            else -> if (isZip) Icons.Default.FolderZip else Icons.Default.Download
        }

        val title = when (phase) {
            DownloadPhase.INITIALIZING -> "Initializing..."
            DownloadPhase.DOWNLOADING -> if (isZip) "Downloading ZIP... $progress%" else "Downloading... $progress%"
            DownloadPhase.EXTRACTING -> "Extracting episodes..."
            DownloadPhase.COMPLETE -> if (isZip) "Ready to Play!" else "Download Complete!"
            DownloadPhase.CANCELLED -> "Download Cancelled"
            DownloadPhase.FAILED -> "Download Failed"
            else -> ""
        }

        val subtitle = when (phase) {
            DownloadPhase.INITIALIZING -> "Preparing poster & metadata"
            DownloadPhase.DOWNLOADING -> if (isZip) "ZIP pack downloading" else "File downloading in background"
            DownloadPhase.EXTRACTING -> "Unpacking episodes from archive"
            DownloadPhase.COMPLETE -> if (isZip) "Episodes extracted & ready" else "Saved to Downloads/WasmerHub"
            DownloadPhase.CANCELLED -> "Download was cancelled"
            DownloadPhase.FAILED -> failureReason ?: "An error occurred"
            else -> ""
        }

        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.15f),
                            accentColor.copy(alpha = 0.06f)
                        )
                    )
                )
                .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (phase) {
                        DownloadPhase.COMPLETE -> Icon(
                            icon, null,
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                        DownloadPhase.FAILED, DownloadPhase.CANCELLED -> Icon(
                            icon, null,
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                        else -> CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = accentColor,
                            strokeWidth = 2.5.dp
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            color = accentColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = subtitle,
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    if (phase == DownloadPhase.COMPLETE) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(accentColor.copy(alpha = 0.2f))
                                .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                .clickable(onClick = onPlay)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    null,
                                    tint = accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Play",
                                    color = accentColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else if (phase == DownloadPhase.DOWNLOADING || phase == DownloadPhase.EXTRACTING || phase == DownloadPhase.INITIALIZING) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier
                                .size(20.dp)
                                .clickable(onClick = onDismiss)
                        )
                    }
                }

                if (phase == DownloadPhase.DOWNLOADING && progress > 0) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = accentColor,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                }
            }
        }
    }
}
