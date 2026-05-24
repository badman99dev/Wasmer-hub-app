package com.movie.app.best.ui.screens.player.extensions

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi

fun Player.switchTrack(trackType: @C.TrackType Int, trackIndex: Int) {
    if (trackIndex < 0) {
        trackSelectionParameters = trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(trackType, true)
            .build()
    } else {
        val tracks = currentTracks.groups.filter { it.type == trackType }
        if (tracks.isEmpty() || trackIndex >= tracks.size) return
        val trackSelectionOverride = TrackSelectionOverride(tracks[trackIndex].mediaTrackGroup, 0)
        trackSelectionParameters = trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(trackType, false)
            .setOverrideForType(trackSelectionOverride)
            .build()
    }
}
