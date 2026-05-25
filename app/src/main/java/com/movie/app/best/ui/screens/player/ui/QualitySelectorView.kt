package com.movie.app.best.ui.screens.player.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer

@Composable
fun BoxScope.QualitySelectorView(
    modifier: Modifier = Modifier,
    show: Boolean,
    player: Player,
    onDismiss: () -> Unit,
) {
    var videoTracks by remember { mutableIntStateOf(0) }
    var qualities by remember { mutableIntStateOf(0) }
    var selectedHeight by remember { mutableIntStateOf(0) }

    LaunchedEffect(player.currentTracks) {
        val groups = player.currentTracks.groups.filter { it.type == C.TRACK_TYPE_VIDEO }
        videoTracks = groups.size
        val heights = mutableSetOf<Int>()
        for (group in groups) {
            for (i in 0 until group.length) {
                val fmt = group.getTrackFormat(i)
                if (fmt.height > 0) heights.add(fmt.height)
            }
        }
        qualities = heights.size
    }

    OverlayView(modifier = modifier, show = show, title = "Quality") {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
                .padding(horizontal = 24.dp)
                .selectableGroup(),
        ) {
            RadioButtonRow(
                selected = isAutoMode(player),
                text = "Auto",
                onClick = {
                    setAutoMode(player)
                    onDismiss()
                },
            )

            val groups = player.currentTracks.groups.filter { it.type == C.TRACK_TYPE_VIDEO }
            val seenHeights = mutableSetOf<Int>()
            for (group in groups) {
                for (i in 0 until group.length) {
                    val fmt = group.getTrackFormat(i)
                    if (fmt.height > 0 && seenHeights.add(fmt.height)) {
                        val isSelected = isHeightSelected(player, fmt.height)
                        RadioButtonRow(
                            selected = isSelected,
                            text = "${fmt.height}p",
                            onClick = {
                                setQuality(player, fmt.height)
                                onDismiss()
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun isAutoMode(player: Player): Boolean {
    return player.trackSelectionParameters.overrides.isEmpty() &&
        !player.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_VIDEO)
}

private fun isHeightSelected(player: Player, height: Int): Boolean {
    val overrides = player.trackSelectionParameters.overrides
    if (overrides.isEmpty()) return false
    val groups = player.currentTracks.groups.filter { it.type == C.TRACK_TYPE_VIDEO }
    for (group in groups) {
        val override = overrides.keys.find { it.type == C.TRACK_TYPE_VIDEO } ?: continue
        val overrideHeight = group.getTrackFormat(0).height
        if (overrideHeight == height) return true
    }
    return false
}

private fun setAutoMode(player: Player) {
    player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
        .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
        .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
        .setMinVideoSize(0, 0)
        .setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
        .build()
}

private fun setQuality(player: Player, height: Int) {
    player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
        .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
        .setMinVideoSize(0, height)
        .setMaxVideoSize(Int.MAX_VALUE, height)
        .build()
}
