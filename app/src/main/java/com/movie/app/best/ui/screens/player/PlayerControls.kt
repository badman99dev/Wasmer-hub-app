package com.movie.app.best.ui.screens.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer

// ── Top bar ───────────────────────────────────────────────────────────────────
@Composable
fun PlayerTopBar(
    title: String,
    isMuted: Boolean,
    isRotationLocked: Boolean,
    isLandscape: Boolean,
    activity: Activity?,
    onBack: () -> Unit,
    onMuteToggle: () -> Unit,
    onRotationLockToggle: () -> Unit,
    onOverflowClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                )
            )
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Back — 48dp touch area
            TopBarIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, onClick = onBack)

            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Mute
            TopBarIconButton(
                icon = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                tint = if (isMuted) Color(0xFFFF5252) else Color.White,
                onClick = onMuteToggle
            )

            // Rotation lock
            TopBarIconButton(
                icon = if (isRotationLocked) Icons.Default.ScreenLockRotation else Icons.Default.ScreenRotation,
                tint = if (isRotationLocked) Color(0xFFFFD600) else Color.White,
                onClick = onRotationLockToggle
            )

            // 3-dot
            TopBarIconButton(icon = Icons.Default.MoreVert, onClick = onOverflowClick)
        }
    }
}

@Composable
private fun TopBarIconButton(
    icon: ImageVector,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Icon(imageVector = icon, contentDescription = null, tint = tint,
            modifier = Modifier.size(26.dp))
    }
}

// ── Center play controls ──────────────────────────────────────────────────────
@Composable
fun PlayerCenterControls(
    isPlaying: Boolean,
    isBuffering: Boolean,
    exoPlayer: ExoPlayer,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // -10s
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
                onClick = { exoPlayer.seekTo(maxOf(0, exoPlayer.currentPosition - 10_000)) },
                modifier = Modifier
                    .size(58.dp)
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(50))
            ) {
                Icon(
                    imageVector = Icons.Default.Replay10,
                    contentDescription = "-10s",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)   // bigger
                )
            }
            Text("-10s", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp,
                fontWeight = FontWeight.Medium)
        }

        // Play/Pause — prominent red circle
        IconButton(
            onClick = { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() },
            modifier = Modifier
                .size(76.dp)
                .background(Color(0xFFE50914).copy(alpha = 0.88f), RoundedCornerShape(50))
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Play/Pause",
                tint = Color.White,
                modifier = Modifier.size(46.dp)
            )
        }

        // +10s
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
                onClick = { exoPlayer.seekTo(minOf(exoPlayer.currentPosition + 10_000, exoPlayer.duration)) },
                modifier = Modifier
                    .size(58.dp)
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(50))
            ) {
                Icon(
                    imageVector = Icons.Default.Forward10,
                    contentDescription = "+10s",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }
            Text("+10s", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp,
                fontWeight = FontWeight.Medium)
        }
    }
}

// ── Bottom bar ────────────────────────────────────────────────────────────────
@Composable
fun PlayerBottomBar(
    exoPlayer: ExoPlayer,
    currentPositionMs: Long,
    durationMs: Long,
    bufferPercent: Int,
    seekDragState: SeekDragState,
    currentSpeed: Float,
    isLandscape: Boolean,
    useNativePlayer: Boolean,
    activity: Activity?,
    onSeekStart: (Float) -> Unit,
    onSeekChange: (Float) -> Unit,
    onSeekEnd: (Float) -> Unit,
    onQualityClick: () -> Unit,
    onSpeedClick: () -> Unit,
    onAudioClick: () -> Unit,
    onLockClick: () -> Unit,
    onPipClick: () -> Unit,
    onPlayerToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                )
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        PlayerSeekBar(
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            bufferPercent = bufferPercent,
            seekDragState = seekDragState,
            onSeekStart = onSeekStart,
            onSeekChange = onSeekChange,
            onSeekEnd = onSeekEnd,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(2.dp))

        // ── Single unified control row: LEFT = utility | RIGHT = playback options
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── LEFT: Lock · PiP · Player-switch ──────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Lock screen
                SmallControlButton(icon = Icons.Default.Lock, onClick = onLockClick)

                // Picture-in-Picture (Android 8+, native player only)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && useNativePlayer) {
                    SmallControlButton(
                        icon = Icons.Default.PictureInPicture,
                        onClick = onPipClick
                    )
                }

                // P1 / P2 toggle badge
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(50))
                        .clickable { onPlayerToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (useNativePlayer) "P2" else "P1",
                        color = Color(0xFFE50914),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // ── RIGHT: Quality · Audio · Speed · Fullscreen ───────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomBarIconButton(icon = Icons.Default.HighQuality, label = "Quality", onClick = onQualityClick)
                BottomBarIconButton(icon = Icons.Default.AudioFile,   label = "Audio",   onClick = onAudioClick)

                // Speed badge — shows value when not 1×
                IconButton(onClick = onSpeedClick, modifier = Modifier.size(48.dp)) {
                    if (currentSpeed == 1.0f) {
                        Icon(Icons.Default.Speed, "Speed", tint = Color.White,
                            modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = "${currentSpeed}x",
                            color = Color(0xFFFFD600),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // Fullscreen toggle
                IconButton(
                    onClick = {
                        activity?.requestedOrientation =
                            if (isLandscape) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            else             ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isLandscape) Icons.Default.FullscreenExit
                                      else             Icons.Default.Fullscreen,
                        contentDescription = "Fullscreen",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

// ── Shared small circular button (Lock, PiP) ──────────────────────────────────
@Composable
fun SmallControlButton(icon: ImageVector, tint: Color = Color.White, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(36.dp)
            .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(50))
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun BottomBarIconButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Icon(imageVector = icon, contentDescription = label,
            tint = Color.White, modifier = Modifier.size(24.dp))
    }
}
