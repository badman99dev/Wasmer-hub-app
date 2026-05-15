package com.movie.app.best.ui.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Volume indicator (right side, vertical swipe) ─────────────────────────────
@Composable
fun VolumeIndicator(volumePercent: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(end = 32.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Column(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = when {
                    volumePercent == 0 -> Icons.Default.VolumeOff
                    volumePercent < 50 -> Icons.Default.VolumeDown
                    else -> Icons.Default.VolumeUp
                },
                contentDescription = "Volume",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "$volumePercent%",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

// ── Brightness indicator (left side, vertical swipe) ──────────────────────────
@Composable
fun BrightnessIndicator(brightnessPercent: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 32.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.BrightnessHigh,
                contentDescription = "Brightness",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "$brightnessPercent%",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

// ── Swipe seek overlay (center, horizontal drag) ──────────────────────────────
@Composable
fun SwipeSeekOverlay(swipeSeekState: SwipeSeekState, currentPositionMs: Long, durationMs: Long) {
    if (!swipeSeekState.isActive) return
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(14.dp))
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Arrow direction + delta
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (swipeSeekState.deltaMs >= 0) Icons.Default.FastForward else Icons.Default.FastRewind,
                    contentDescription = null,
                    tint = if (swipeSeekState.deltaMs >= 0) Color(0xFF4FC3F7) else Color(0xFFFFB74D),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatSeekDelta(swipeSeekState.deltaMs),
                    color = if (swipeSeekState.deltaMs >= 0) Color(0xFF4FC3F7) else Color(0xFFFFB74D),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            // Target position
            Text(
                text = "${formatMs(swipeSeekState.targetPositionMs)} / ${formatMs(durationMs)}",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp
            )
        }
    }
}

// ── Speed overlay (long press) ─────────────────────────────────────────────────
@Composable
fun SpeedOverlay(speed: Float) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Row(
            modifier = Modifier
                .padding(top = 56.dp)
                .background(Color.Black.copy(alpha = 0.78f), RoundedCornerShape(20.dp))
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = "Speed",
                tint = Color(0xFFFFD600),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${speed}x",
                color = Color(0xFFFFD600),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

// ── Buffering spinner ──────────────────────────────────────────────────────────
@Composable
fun BufferingIndicator() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            color = Color(0xFFE50914),
            modifier = Modifier.size(48.dp),
            strokeWidth = 3.dp
        )
    }
}

// ── Error overlay ──────────────────────────────────────────────────────────────
@Composable
fun ErrorOverlay(error: String, onRetry: () -> Unit, onSwitchPlayer: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = Color(0xFFFF5252),
                modifier = Modifier.size(52.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Playback Error",
                color = Color(0xFFFF5252),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                error,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 32.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onRetry,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) { Text("Retry") }
                Button(
                    onClick = onSwitchPlayer,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
                ) { Text("Web Player") }
            }
        }
    }
}

// ── Double tap seek animation — YouTube wavefront style ──────────────────────
// Uses fireCount (Int) instead of Boolean so rapid successive double-taps
// always re-trigger the animation even when the previous one hasn't finished.
@Composable
fun DoubleTapSeekOverlay(isRight: Boolean, fireCount: Int, seconds: Int = 10) {

    var visible by remember { mutableStateOf(false) }

    // Multiple expanding ripple waves — YouTube feel
    val wave1Scale = remember { Animatable(0f) }
    val wave1Alpha = remember { Animatable(0f) }
    val wave2Scale = remember { Animatable(0f) }
    val wave2Alpha = remember { Animatable(0f) }
    val wave3Scale = remember { Animatable(0f) }
    val wave3Alpha = remember { Animatable(0f) }

    // Chevron wave — 3 chevrons light up sequentially
    var chevronStep by remember { mutableIntStateOf(0) }

    // Re-triggers every time fireCount changes (increments), so rapid taps work
    LaunchedEffect(fireCount) {
        if (fireCount <= 0) return@LaunchedEffect

        visible = true

        // Reset all wave animatables immediately
        wave1Scale.snapTo(0f); wave1Alpha.snapTo(0f)
        wave2Scale.snapTo(0f); wave2Alpha.snapTo(0f)
        wave3Scale.snapTo(0f); wave3Alpha.snapTo(0f)
        chevronStep = 0

        // Wave 1 — immediate, fast expand
        wave1Alpha.snapTo(0.55f)
        launch { wave1Scale.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) }
        launch { wave1Alpha.animateTo(0f, tween(500, easing = LinearEasing)) }

        delay(80)

        // Wave 2
        wave2Alpha.snapTo(0.40f)
        launch { wave2Scale.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) }
        launch { wave2Alpha.animateTo(0f, tween(500, easing = LinearEasing)) }

        delay(80)

        // Wave 3
        wave3Alpha.snapTo(0.28f)
        launch { wave3Scale.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) }
        launch { wave3Alpha.animateTo(0f, tween(500, easing = LinearEasing)) }

        // Chevron sequential light-up
        repeat(3) { i -> chevronStep = i + 1; delay(90) }

        // Hold for a moment then fade out
        delay(350)
        visible = false
        chevronStep = 0
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(60)),
        exit  = fadeOut(animationSpec = tween(300))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // Clipped half-screen canvas for waves
            val sideModifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.5f)
                .align(if (isRight) Alignment.CenterEnd else Alignment.CenterStart)
                .clip(if (isRight)
                    RoundedCornerShape(topStart = 200.dp, bottomStart = 200.dp)
                else
                    RoundedCornerShape(topEnd = 200.dp, bottomEnd = 200.dp)
                )

            // Wave circles drawn via Canvas — expands from edge
            Box(modifier = sideModifier) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = if (isRight) 0f else size.width
                    val cy = size.height / 2f
                    val maxR = size.height * 0.85f

                    // Wave 3 (back)
                    drawCircle(
                        color = Color.White.copy(alpha = wave3Alpha.value),
                        radius = maxR * wave3Scale.value,
                        center = Offset(cx, cy)
                    )
                    // Wave 2
                    drawCircle(
                        color = Color.White.copy(alpha = wave2Alpha.value),
                        radius = maxR * wave2Scale.value,
                        center = Offset(cx, cy)
                    )
                    // Wave 1 (front)
                    drawCircle(
                        color = Color.White.copy(alpha = wave1Alpha.value),
                        radius = maxR * wave1Scale.value,
                        center = Offset(cx, cy)
                    )
                }
            }

            // Chevrons + label centered on the half
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.5f)
                    .align(if (isRight) Alignment.CenterEnd else Alignment.CenterStart),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // 3 chevrons — sequential alpha + scale
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(3) { idx ->
                            val lit = chevronStep > idx
                            val iconAlpha = if (lit) 1f else 0.25f
                            val iconSize = if (lit) 30.dp else 24.dp
                            Icon(
                                imageVector = if (isRight)
                                    Icons.Default.KeyboardArrowRight
                                else
                                    Icons.Default.KeyboardArrowLeft,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = iconAlpha),
                                modifier = Modifier.size(iconSize)
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = if (isRight) "+$seconds seconds" else "-$seconds seconds",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
