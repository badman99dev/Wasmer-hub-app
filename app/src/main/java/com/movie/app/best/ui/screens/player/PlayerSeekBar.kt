package com.movie.app.best.ui.screens.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.abs

@Composable
fun PlayerSeekBar(
    currentPositionMs: Long,
    durationMs: Long,
    bufferPercent: Int,
    seekDragState: SeekDragState,
    onSeekStart: (fraction: Float) -> Unit,
    onSeekChange: (fraction: Float) -> Unit,
    onSeekEnd: (fraction: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var trackWidthPx by remember { mutableFloatStateOf(0f) }

    val displayFraction = if (seekDragState.isDragging)
        seekDragState.dragFraction
    else
        if (durationMs > 0) currentPositionMs.toFloat() / durationMs else 0f

    val bufferFraction = bufferPercent / 100f

    Column(modifier = modifier) {

        // ── Seekbar hit area + track ───────────────────────────────────────────
        // zIndex(1f) — always above parent Box gestures so touch is consumed here first
        // Height 48dp normal, 56dp while dragging — fat hit area prevents mis-touch
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (seekDragState.isDragging) 56.dp else 48.dp)
                .zIndex(1f)                          // ← above parent playerGestures
                .onGloballyPositioned { trackWidthPx = it.size.width.toFloat() }
                .pointerInput(durationMs) {
                    awaitEachGesture {
                        // Grab DOWN immediately and consume it —
                        // parent pointerInput blocks will NOT see this touch at all
                        val down = awaitFirstDown(requireUnconsumed = false)
                        if (trackWidthPx <= 0f) return@awaitEachGesture

                        down.consume()                // ← lock out parent gestures NOW

                        val startFraction = (down.position.x / trackWidthPx).coerceIn(0f, 1f)
                        onSeekStart(startFraction)

                        var lastFraction = startFraction
                        val pointerId: PointerId = down.id

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.find { it.id == pointerId }
                                ?: event.changes.firstOrNull()
                                ?: break

                            if (!change.pressed) {
                                onSeekEnd(lastFraction)
                                break
                            }

                            change.consume()           // consume every move event too
                            val newFraction = (change.position.x / trackWidthPx).coerceIn(0f, 1f)
                            if (abs(newFraction - lastFraction) > 0.001f) {
                                lastFraction = newFraction
                                onSeekChange(newFraction)
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center       // track drawn in vertical center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val trackHeight = if (seekDragState.isDragging) 6.dp.toPx() else 3.dp.toPx()
                val yCenter = size.height / 2f
                val cr = CornerRadius(trackHeight / 2, trackHeight / 2)
                val top = yCenter - trackHeight / 2

                // Layer 1 — background (unloaded)
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.12f),
                    topLeft = Offset(0f, top),
                    size = Size(size.width, trackHeight),
                    cornerRadius = cr
                )

                // Layer 2 — buffer (loaded, grey-white)
                val bufferWidth = (size.width * bufferFraction).coerceIn(0f, size.width)
                if (bufferWidth > 0f) {
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.55f),
                        topLeft = Offset(0f, top),
                        size = Size(bufferWidth, trackHeight),
                        cornerRadius = cr
                    )
                }

                // Layer 3 — progress (played, red)
                val progressWidth = (size.width * displayFraction).coerceIn(0f, size.width)
                if (progressWidth > 0f) {
                    drawRoundRect(
                        color = Color(0xFFE50914),
                        topLeft = Offset(0f, top),
                        size = Size(progressWidth, trackHeight),
                        cornerRadius = cr
                    )
                }

                // Thumb
                val thumbRadius = if (seekDragState.isDragging) 9.dp.toPx() else 6.dp.toPx()
                val thumbX = progressWidth.coerceIn(thumbRadius, size.width - thumbRadius)
                drawCircle(color = Color(0xFFE50914), radius = thumbRadius, center = Offset(thumbX, yCenter))
                drawCircle(color = Color.White, radius = thumbRadius * 0.4f, center = Offset(thumbX, yCenter))
            }
        }

        // ── Time labels ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatMs(
                    if (seekDragState.isDragging)
                        (seekDragState.dragFraction * durationMs).toLong()
                    else currentPositionMs
                ),
                color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium
            )
            Text(
                text = formatMs(durationMs),
                color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp
            )
        }
    }
}
