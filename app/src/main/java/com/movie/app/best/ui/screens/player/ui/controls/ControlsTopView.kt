package com.movie.app.best.ui.screens.player.ui.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.movie.app.best.ui.screens.player.buttons.PlayerButton

@Composable
fun ControlsTopView(
    modifier: Modifier = Modifier,
    title: String,
    onAudioClick: () -> Unit = {},
    onSubtitleClick: () -> Unit = {},
    onPlaybackSpeedClick: () -> Unit = {},
    onBackClick: () -> Unit,
) {
    val systemBarsPadding = WindowInsets.systemBars.union(WindowInsets.displayCutout).asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current
    Row(
        modifier = modifier
            .padding(start = systemBarsPadding.calculateLeftPadding(layoutDirection), end = systemBarsPadding.calculateRightPadding(layoutDirection), top = systemBarsPadding.calculateTopPadding(), bottom = 0.dp)
            .padding(horizontal = 8.dp)
            .padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PlayerButton(onClick = onBackClick) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            PlayerButton(onClick = onPlaybackSpeedClick) {
                Icon(imageVector = Icons.Default.Speed, contentDescription = null)
            }
            PlayerButton(onClick = onAudioClick) {
                Icon(imageVector = Icons.Default.Audiotrack, contentDescription = null)
            }
            PlayerButton(onClick = onSubtitleClick) {
                Icon(imageVector = Icons.Default.ClosedCaption, contentDescription = null)
            }
        }
    }
}
