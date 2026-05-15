package com.movie.app.best.ui.screens.player

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PlayerDebugPanel(
    logs: List<DebugLogEntry>,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val listState = rememberLazyListState()
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) listState.animateScrollToItem(logs.size - 1)
    }
    LaunchedEffect(copied) {
        if (copied) { kotlinx.coroutines.delay(1500); copied = false }
    }

    // Slide in from right
    AnimatedVisibility(
        visible = true,
        enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(260)) + fadeIn(tween(200)),
        exit  = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(220)) + fadeOut(tween(180))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable { onDismiss() }
            )

            // Panel
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(280.dp)
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF0A0A14), Color(0xFF080810))
                        )
                    )
                    .clickable { /* consume */ }
            ) {
                Column(modifier = Modifier.fillMaxSize()) {

                    // ── Header ─────────────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F0F1E))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.BugReport, null,
                                tint = Color(0xFF00FF88),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "DEBUG",
                                color = Color(0xFF00FF88),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 2.sp
                            )
                            Spacer(Modifier.width(8.dp))
                            // Log count badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF1A2A1A))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "${logs.size}",
                                    color = Color(0xFF00FF88).copy(alpha = 0.7f),
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Copy button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (copied) Color(0xFF1A3A2A) else Color(0xFF151520)
                                    )
                                    .clickable {
                                        val text = logs.joinToString("\n") {
                                            "[${it.timestamp}][${it.type}] ${it.message}"
                                        }
                                        clipboardManager.setText(AnnotatedString(text))
                                        copied = true
                                    }
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                                        null,
                                        tint = if (copied) Color(0xFF00FF88) else Color(0xFF00CCFF),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        if (copied) "COPIED!" else "COPY",
                                        color = if (copied) Color(0xFF00FF88) else Color(0xFF00CCFF),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Spacer(Modifier.width(8.dp))

                            // Close
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF2A1515))
                                    .clickable { onDismiss() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Close, null,
                                    tint = Color(0xFFFF5252), modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    // Thin accent line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF00FF88).copy(alpha = 0.5f), Color.Transparent)
                                )
                            )
                    )

                    // ── Log list ───────────────────────────────────────────────
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        items(logs) { entry ->
                            LogRow(entry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogRow(entry: DebugLogEntry) {
    val typeColor = when (entry.type) {
        "ERROR", "ERR" -> Color(0xFFFF5252)
        "RESP"         -> Color(0xFF69F0AE)
        "REQ"          -> Color(0xFF40C4FF)
        "BODY"         -> Color(0xFFFFD740)
        "PLAYER"       -> Color(0xFF82B1FF)
        "TRACK"        -> Color(0xFFFF80AB)
        "EXO"          -> Color(0xFF80D8FF)
        "HDR"          -> Color(0xFFCCFF90)
        "URL","CONFIG" -> Color(0xFFB9F6CA)
        else           -> Color.White.copy(alpha = 0.55f)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(3.dp))
            .background(typeColor.copy(alpha = 0.04f))
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        // Type badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .background(typeColor.copy(alpha = 0.15f))
                .padding(horizontal = 4.dp, vertical = 1.dp)
                .widthIn(min = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                entry.type,
                color = typeColor,
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(Modifier.width(4.dp))
        Column {
            Text(
                entry.timestamp,
                color = Color.White.copy(alpha = 0.25f),
                fontSize = 6.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                entry.message,
                color = typeColor.copy(alpha = 0.85f),
                fontSize = 7.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 9.sp
            )
        }
    }
}
