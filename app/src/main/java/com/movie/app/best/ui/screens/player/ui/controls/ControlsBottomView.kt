package com.movie.app.best.ui.screens.player.ui.controls

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.movie.app.best.ui.screens.player.buttons.PlayerButton
import com.movie.app.best.ui.screens.player.extensions.nameRes
import com.movie.app.best.ui.screens.player.extensions.noRippleClickable
import com.movie.app.best.ui.screens.player.model.VideoContentScale
import com.movie.app.best.ui.screens.player.state.MediaPresentationState
import com.movie.app.best.ui.screens.player.state.durationFormatted
import com.movie.app.best.ui.screens.player.state.positionFormatted
import com.movie.app.best.ui.screens.player.state.pendingPositionFormatted

@Composable
fun ControlsBottomView(
    modifier: Modifier = Modifier,
    player: Player,
    mediaPresentationState: MediaPresentationState,
    controlsAlignment: Alignment.Horizontal,
    videoContentScale: VideoContentScale,
    isPipSupported: Boolean,
    isInline: Boolean = false,
    isRotationLocked: Boolean,
    onVideoContentScaleClick: () -> Unit,
    onVideoContentScaleLongClick: () -> Unit,
    onLockControlsClick: () -> Unit,
    onPictureInPictureClick: () -> Unit,
    onRotateClick: () -> Unit,
    onPlayInBackgroundClick: () -> Unit,
    onFullscreenClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekEnd: () -> Unit,
) {
    val systemBarsPadding = WindowInsets.systemBars.union(WindowInsets.displayCutout).asPaddingValues()
    val layoutDirection = androidx.compose.ui.platform.LocalLayoutDirection.current

    if (isInline) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 0.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                var showPendingPosition by rememberSaveable { mutableStateOf(false) }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.noRippleClickable { showPendingPosition = !showPendingPosition },
                ) {
                    Text(
                        text = when (showPendingPosition) {
                            true -> "-${mediaPresentationState.pendingPositionFormatted}"
                            false -> "${mediaPresentationState.positionFormatted} / ${mediaPresentationState.durationFormatted}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                PlayerButton(onClick = onFullscreenClick) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.White.copy(alpha = 0.85f),
                    )
                }
            }

            PlayerSeekbar(
                modifier = Modifier.offset(y = 2.dp),
                position = mediaPresentationState.position.toFloat(),
                duration = mediaPresentationState.duration.toFloat(),
                trackHeight = 2.dp,
                thumbWidth = 3.dp,
                trackThumbGapWidth = 0.dp,
                onSeek = { onSeek(it.toLong()) },
                onSeekFinished = { onSeekEnd() },
            )
        }
    } else {
        val bottomPad = if (systemBarsPadding.calculateBottomPadding() == 0.dp) 16.dp else systemBarsPadding.calculateBottomPadding()
        Column(
            modifier = modifier
                .padding(start = systemBarsPadding.calculateLeftPadding(layoutDirection), end = systemBarsPadding.calculateRightPadding(layoutDirection), top = 0.dp, bottom = bottomPad)
                .padding(horizontal = 8.dp)
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                var showPendingPosition by rememberSaveable { mutableStateOf(false) }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.noRippleClickable { showPendingPosition = !showPendingPosition },
                ) {
                    Text(
                        text = when (showPendingPosition) {
                            true -> "-${mediaPresentationState.pendingPositionFormatted}"
                            false -> mediaPresentationState.positionFormatted
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                    )
                    Text(text = " / ", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    Text(
                        text = mediaPresentationState.durationFormatted,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                    )
                }
            }

            PlayerSeekbar(
                position = mediaPresentationState.position.toFloat(),
                duration = mediaPresentationState.duration.toFloat(),
                onSeek = { onSeek(it.toLong()) },
                onSeekFinished = { onSeekEnd() },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlayerButton(onClick = onLockControlsClick) {
                    Icon(imageVector = Icons.Default.LockOpen, contentDescription = null)
                }
                PlayerButton(onClick = onRotateClick) {
                    Icon(
                        imageVector = Icons.Default.ScreenRotation,
                        contentDescription = null,
                        tint = if (isRotationLocked) MaterialTheme.colorScheme.primary else Color.White,
                    )
                }
                PlayerButton(
                    onClick = onVideoContentScaleClick,
                    onLongClick = onVideoContentScaleLongClick,
                ) {
                    Icon(imageVector = Icons.Default.FitScreen, contentDescription = null)
                }
                Spacer(modifier = Modifier.weight(1f))

                PlayerButton(onClick = onFullscreenClick) {
                    Icon(imageVector = Icons.Default.Fullscreen, contentDescription = null)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerSeekbar(
    modifier: Modifier = Modifier,
    position: Float,
    duration: Float,
    trackHeight: androidx.compose.ui.unit.Dp = 8.dp,
    thumbWidth: androidx.compose.ui.unit.Dp = 4.dp,
    trackThumbGapWidth: androidx.compose.ui.unit.Dp = 12.dp,
    onSeek: (Float) -> Unit,
    onSeekFinished: () -> Unit,
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Slider(
        value = position,
        valueRange = 0f..duration,
        onValueChange = onSeek,
        onValueChangeFinished = onSeekFinished,
        modifier = modifier.height(20.dp),
        thumb = {
            Box(
                modifier = Modifier.width(thumbWidth).height(20.dp).background(primaryColor, CircleShape),
            )
        },
        track = { sliderState ->
            val disabledAlpha = 0.15f
            Canvas(
                modifier = Modifier.fillMaxWidth().height(trackHeight),
            ) {
                val min = sliderState.valueRange.start
                val max = sliderState.valueRange.endInclusive
                val range = (max - min).takeIf { it > 0f } ?: 1f
                val playedFraction = ((sliderState.value - min) / range).coerceIn(0f, 1f)
                val playedPixels = size.width * playedFraction
                val endCornerRadius = size.height / 2f
                val insideCornerRadius = 2.dp.toPx()
                val gapHalf = trackThumbGapWidth.toPx() / 2f
                val leftEnd = (playedPixels - gapHalf).coerceIn(0f, size.width)
                val rightStart = (playedPixels + gapHalf).coerceIn(0f, size.width)

                if (leftEnd > 0f) {
                    drawRoundedRect(Offset(0f, 0f), Size(leftEnd, size.height), primaryColor.copy(alpha = disabledAlpha), endCornerRadius, insideCornerRadius)
                }
                if (rightStart < size.width) {
                    drawRoundedRect(Offset(rightStart, 0f), Size(size.width - rightStart, size.height), primaryColor.copy(alpha = disabledAlpha), insideCornerRadius, endCornerRadius)
                }
                if (leftEnd > 0f) {
                    drawRoundedRect(Offset(0f, 0f), Size(leftEnd, size.height), primaryColor, endCornerRadius, insideCornerRadius)
                }
            }
        },
    )
}

private fun DrawScope.drawRoundedRect(offset: Offset, size: Size, color: Color, startCornerRadius: Float, endCornerRadius: Float) {
    val startCorner = CornerRadius(startCornerRadius, startCornerRadius)
    val endCorner = CornerRadius(endCornerRadius, endCornerRadius)
    drawPath(
        path = Path().apply {
            addRoundRect(RoundRect(rect = Rect(Offset(offset.x, 0f), size = Size(size.width, size.height)), topLeft = startCorner, topRight = endCorner, bottomRight = endCorner, bottomLeft = startCorner))
        },
        color = color,
    )
}
