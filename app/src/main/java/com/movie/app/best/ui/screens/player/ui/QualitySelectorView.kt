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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride

@Composable
fun BoxScope.QualitySelectorView(
    modifier: Modifier = Modifier,
    show: Boolean,
    player: Player,
    onDismiss: () -> Unit,
) {
    var selectedHeight by remember { mutableIntStateOf(0) }
    var lowestQualityApplied by remember { mutableStateOf(false) }

    LaunchedEffect(player.currentTracks) {
        val groups = player.currentTracks.groups.filter { it.type == C.TRACK_TYPE_VIDEO }
        val heights = mutableSetOf<Int>()
        for (group in groups) {
            for (i in 0 until group.length) {
                val fmt = group.getTrackFormat(i)
                if (fmt.height > 0) heights.add(fmt.height)
            }
        }
        if (heights.isNotEmpty() && !lowestQualityApplied) {
            lowestQualityApplied = true
            val lowest = heights.min()
            selectedHeight = lowest
            setQualityByOverride(player, lowest)
        }
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
                selected = selectedHeight == 0,
                text = "Auto",
                onClick = {
                    selectedHeight = 0
                    setAutoMode(player)
                    onDismiss()
                },
            )

            val groups = player.currentTracks.groups.filter { it.type == C.TRACK_TYPE_VIDEO }
            val seenHeights = mutableSetOf<Int>()
            val sortedHeights = mutableListOf<Int>()
            for (group in groups) {
                for (i in 0 until group.length) {
                    val fmt = group.getTrackFormat(i)
                    if (fmt.height > 0 && seenHeights.add(fmt.height)) {
                        sortedHeights.add(fmt.height)
                    }
                }
            }
            sortedHeights.sortedDescending().forEach { height ->
                RadioButtonRow(
                    selected = selectedHeight == height,
                    text = "${height}p",
                    onClick = {
                        selectedHeight = height
                        setQualityByOverride(player, height)
                        onDismiss()
                    },
                )
            }
        }
    }
}

private fun setAutoMode(player: Player) {
    player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
        .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
        .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
        .build()
}

private fun setQualityByOverride(player: Player, height: Int) {
    val videoGroups = player.currentTracks.groups.filter { it.type == C.TRACK_TYPE_VIDEO }
    for (group in videoGroups) {
        for (i in 0 until group.length) {
            val fmt = group.getTrackFormat(i)
            if (fmt.height == height) {
                player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                    .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                    .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, listOf(i)))
                    .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
                    .build()
                return
            }
        }
    }
}
