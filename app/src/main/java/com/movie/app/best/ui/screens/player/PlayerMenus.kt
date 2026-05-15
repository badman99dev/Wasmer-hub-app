package com.movie.app.best.ui.screens.player

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.exoplayer.ExoPlayer

// ── Premium glass card overlay ────────────────────────────────────────────────
// Slides in from bottom-right with spring animation
@Composable
fun PlayerMenuOverlay(
    title: String,
    icon: ImageVector,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Dim scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable { onDismiss() }
        )

        // Slide in from bottom-end
        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(
                initialOffsetY = { it / 2 },
                animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f)
            ) + fadeIn(tween(180)),
            exit = slideOutVertically(
                targetOffsetY = { it / 2 },
                animationSpec = tween(180)
            ) + fadeOut(tween(150)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 88.dp)
        ) {
            Box(
                modifier = Modifier
                    .widthIn(min = 180.dp, max = 240.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF1C1C2E), Color(0xFF12121E))
                        )
                    )
                    // subtle border
                    .padding(1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(17.dp))
                        .background(Color(0xFF16162A).copy(alpha = 0.97f))
                        .padding(bottom = 6.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFFE50914).copy(alpha = 0.15f), Color.Transparent)
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, null, tint = Color(0xFFE50914), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    // Thin divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(Color.White.copy(alpha = 0.08f))
                    )
                    Spacer(Modifier.height(4.dp))
                    // Menu items
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp)
                    ) { content() }
                }
            }
        }
    }
}

// ── Menu row ──────────────────────────────────────────────────────────────────
@Composable
private fun MenuRow(label: String, isSelected: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) Color(0xFFE50914).copy(alpha = 0.14f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = if (isSelected) Color(0xFFFF6B6B) else Color.White.copy(alpha = 0.88f),
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
        if (isSelected) {
            Icon(
                Icons.Default.Check, null,
                tint = Color(0xFFFF6B6B),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ── Quality menu ──────────────────────────────────────────────────────────────
@Composable
fun QualityMenu(exoPlayer: ExoPlayer, onDismiss: () -> Unit) {
    PlayerMenuOverlay(title = "Quality", icon = Icons.Default.HighQuality, onDismiss = onDismiss) {
        data class QualityOption(val height: Int, val label: String)

        val qualityList = mutableListOf<QualityOption>()
        for (group in exoPlayer.currentTracks.groups) {
            for (i in 0 until group.length) {
                val fmt = group.getTrackFormat(i)
                if (fmt.height > 0) qualityList.add(QualityOption(fmt.height, "${fmt.height}p"))
            }
        }
        val sorted = qualityList.distinctBy { it.height }.sortedBy { it.height }

        if (sorted.isEmpty()) {
            Text("No quality options", color = Color.White.copy(alpha = 0.4f),
                fontSize = 13.sp, modifier = Modifier.padding(12.dp))
        } else {
            val params  = exoPlayer.trackSelectionParameters
            val isAuto  = params.maxVideoHeight == Int.MAX_VALUE && params.minVideoHeight == 0

            MenuRow("Auto", isSelected = isAuto) {
                exoPlayer.trackSelectionParameters = params.buildUpon()
                    .setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE).setMinVideoSize(0, 0).build()
                onDismiss()
            }
            sorted.forEach { opt ->
                val isSel = !isAuto && params.maxVideoHeight == opt.height && params.minVideoHeight == opt.height
                MenuRow(opt.label, isSelected = isSel) {
                    exoPlayer.trackSelectionParameters = params.buildUpon()
                        .setMaxVideoSize(Int.MAX_VALUE, opt.height).setMinVideoSize(0, opt.height).build()
                    onDismiss()
                }
            }
        }
    }
}

// ── Speed menu ────────────────────────────────────────────────────────────────
@Composable
fun SpeedMenu(
    currentSpeed: Float,
    exoPlayer: ExoPlayer,
    onSpeedChanged: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    PlayerMenuOverlay(title = "Speed", icon = Icons.Default.Speed, onDismiss = onDismiss) {
        listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f).forEach { speed ->
            MenuRow(
                label = if (speed == 1.0f) "Normal (1×)" else "${speed}×",
                isSelected = currentSpeed == speed
            ) {
                exoPlayer.setPlaybackSpeed(speed)
                onSpeedChanged(speed)
                onDismiss()
            }
        }
    }
}

// ── Audio menu ────────────────────────────────────────────────────────────────
@Composable
fun AudioMenu(exoPlayer: ExoPlayer, onDismiss: () -> Unit) {
    PlayerMenuOverlay(title = "Audio", icon = Icons.Default.AudioFile, onDismiss = onDismiss) {
        val audioTracks = mutableListOf<Triple<androidx.media3.common.Tracks.Group, Int, String>>()
        for (group in exoPlayer.currentTracks.groups) {
            if (group.type != C.TRACK_TYPE_AUDIO) continue
            for (i in 0 until group.length) {
                val fmt = group.getTrackFormat(i)
                val langName = fmt.language?.let {
                    try { java.util.Locale(it).getDisplayLanguage(java.util.Locale.ENGLISH)
                        .replaceFirstChar { c -> c.uppercase() }
                    } catch (_: Exception) { it }
                }
                audioTracks.add(Triple(group, i, fmt.label ?: langName ?: "Track ${audioTracks.size + 1}"))
            }
        }
        if (audioTracks.isEmpty()) {
            Text("No audio tracks", color = Color.White.copy(alpha = 0.4f),
                fontSize = 13.sp, modifier = Modifier.padding(12.dp))
        } else {
            audioTracks.forEach { (group, idx, label) ->
                MenuRow(label, isSelected = group.isTrackSelected(idx)) {
                    val builder = exoPlayer.trackSelectionParameters.buildUpon()
                        .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                    builder.addOverride(TrackSelectionOverride(group.mediaTrackGroup, listOf(idx)))
                    exoPlayer.trackSelectionParameters = builder.build()
                    onDismiss()
                }
            }
        }
    }
}

// ── 3-dot overflow menu ───────────────────────────────────────────────────────
@Composable
fun OverflowMenu(
    onViewLogs: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Transparent).clickable { onDismiss() })

        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(initialOffsetY = { -it / 3 }, animationSpec = spring(stiffness = 500f)) + fadeIn(tween(150)),
            exit  = slideOutVertically(targetOffsetY  = { -it / 3 }, animationSpec = tween(150)) + fadeOut(tween(120)),
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 52.dp, end = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = 160.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1A1A2E))
                    .padding(vertical = 4.dp)
            ) {
                OverflowItem(Icons.Default.BugReport,  "View Logs") { onViewLogs(); onDismiss() }
                Box(Modifier.fillMaxWidth().height(0.5.dp).background(Color.White.copy(alpha = 0.07f)))
                OverflowItem(Icons.Default.Share, "Share") { onShare(); onDismiss() }
            }
        }
    }
}

@Composable
private fun OverflowItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.75f), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
