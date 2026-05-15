package com.movie.app.best.ui.screens.player

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.media3.exoplayer.ExoPlayer
import kotlin.math.abs
import kotlinx.coroutines.withTimeout

// ── Pinch-to-zoom ─────────────────────────────────────────────────────────────
fun Modifier.pinchZoom(
    minScale: Float = 0.5f,
    maxScale: Float = 3.5f,
    currentScale: Float,
    onScaleChange: (Float) -> Unit
): Modifier = this.pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            if (event.changes.size >= 2) {
                val zoom = event.calculateZoom()
                val newScale = (currentScale * zoom).coerceIn(minScale, maxScale)
                onScaleChange(newScale)
                event.changes.forEach { it.consume() }
            }
        } while (event.changes.any { it.pressed })
    }
}

// ── Main player gesture handler ───────────────────────────────────────────────
fun Modifier.playerGestures(
    context: Context,
    activity: Activity?,
    exoPlayer: ExoPlayer,
    isLocked: Boolean,
    isSeekbarDragging: Boolean,          // ← NEW: seekbar drag active? ignore all screen gestures
    durationMs: Long,
    audioManager: AudioManager,
    maxVolume: Int,
    currentVolume: Float,
    currentBrightness: Float,
    swipeSeekState: SwipeSeekState,
    onTap: () -> Unit,
    onDoubleTap: (isRight: Boolean) -> Unit,
    onLongPressStart: () -> Unit,
    onLongPressEnd: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onShowVolumeIndicator: (Boolean) -> Unit,
    onShowBrightnessIndicator: (Boolean) -> Unit,
    onSwipeSeekUpdate: (SwipeSeekState) -> Unit,
    onScaleChange: (Float) -> Unit,
    currentScale: Float,
): Modifier {
    if (isLocked) return this

    return this
        // ── Tap / Double-tap / Long-press ──────────────────────────────────────
        .pointerInput(isLocked, isSeekbarDragging, durationMs) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)

                // Seekbar already consuming this gesture — bail out immediately
                if (isSeekbarDragging) return@awaitEachGesture

                var longPressTriggered = false
                var upOrCancel = false
                val startTime = System.currentTimeMillis()

                while (true) {
                    val event = awaitPointerEvent()
                    val elapsed = System.currentTimeMillis() - startTime
                    if (!event.changes.first().pressed) { upOrCancel = true; break }
                    if (elapsed > 500L && !longPressTriggered) {
                        longPressTriggered = true
                        onLongPressStart()
                    }
                }
                if (longPressTriggered) onLongPressEnd()
                if (!longPressTriggered && upOrCancel) {
                    var isDoubleTap = false
                    try {
                        withTimeout(200L) { awaitFirstDown(); isDoubleTap = true }
                    } catch (_: Exception) {}
                    if (isDoubleTap) onDoubleTap(down.position.x > size.width / 2)
                    else onTap()
                }
            }
        }

        // ── Horizontal seek + Vertical volume/brightness ───────────────────────
        .pointerInput(isLocked, isSeekbarDragging, durationMs) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)

                // Seekbar is consuming this touch — do not interfere at all
                if (isSeekbarDragging) return@awaitEachGesture

                var totalDragX = 0f
                var totalDragY = 0f
                // null = undecided, true = horizontal seek, false = vertical vol/bright
                var direction: Boolean? = null
                val startPositionMs = exoPlayer.currentPosition
                var localTargetMs = startPositionMs

                while (true) {
                    val event = awaitPointerEvent()
                    if (!event.changes.first().pressed) break
                    val change = event.changes.first()

                    val dx = change.position.x - change.previousPosition.x
                    val dy = change.position.y - change.previousPosition.y
                    totalDragX += dx
                    totalDragY += dy

                    // Direction lock:
                    // Use 20px threshold + require one axis to be clearly dominant (ratio > 1.8)
                    // This prevents accidental horizontal trigger while doing vertical swipe
                    if (direction == null) {
                        val totalMoved = abs(totalDragX) + abs(totalDragY)
                        if (totalMoved > 20f) {
                            val ratio = abs(totalDragX) / (abs(totalDragY) + 0.1f)
                            direction = ratio > 1.8f   // true=horizontal, false=vertical
                        }
                    }

                    when (direction) {
                        true -> {
                            // ── Horizontal seek ────────────────────────────────
                            change.consume()
                            val sensitivityMs = when {
                                durationMs <= 0          -> 60_000L
                                durationMs < 5 * 60_000  -> durationMs / 3
                                durationMs < 60 * 60_000 -> durationMs / 5
                                else                     -> durationMs / 10
                            }
                            val msPerPx = (sensitivityMs.toFloat() / size.width).coerceIn(50f, 3000f)
                            val deltaMs = (totalDragX * msPerPx).toLong()
                            localTargetMs = (startPositionMs + deltaMs)
                                .coerceIn(0L, if (durationMs > 0) durationMs else Long.MAX_VALUE)

                            onSwipeSeekUpdate(SwipeSeekState(
                                isActive         = true,
                                deltaMs          = deltaMs,
                                targetPositionMs = localTargetMs
                            ))
                            exoPlayer.seekTo(localTargetMs)
                        }

                        false -> {
                            // ── Vertical volume / brightness ───────────────────
                            change.consume()
                            val delta = -(dy / size.height)
                            if (down.position.x > size.width / 2) {
                                onShowVolumeIndicator(true)
                                val newVol = (currentVolume + delta * maxVolume * 2)
                                    .coerceIn(0f, maxVolume.toFloat())
                                onVolumeChange(newVol)
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol.toInt(), 0)
                            } else {
                                onShowBrightnessIndicator(true)
                                val newBrightness = (currentBrightness + delta * 2)
                                    .coerceIn(0.01f, 1f)
                                onBrightnessChange(newBrightness)
                                activity?.window?.attributes?.let { lp ->
                                    lp.screenBrightness = newBrightness
                                    activity.window.attributes = lp
                                }
                            }
                        }

                        null -> { /* still deciding direction — consume nothing */ }
                    }
                }

                // Gesture ended
                when (direction) {
                    true  -> { exoPlayer.seekTo(localTargetMs); onSwipeSeekUpdate(SwipeSeekState()) }
                    false -> { onShowVolumeIndicator(false); onShowBrightnessIndicator(false) }
                    null  -> {}
                }
            }
        }

        // ── Pinch zoom ─────────────────────────────────────────────────────────
        .pinchZoom(currentScale = currentScale, onScaleChange = onScaleChange)
}
