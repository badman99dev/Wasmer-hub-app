package com.movie.app.best.ui.screens.player.extensions

import androidx.media3.common.Player
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

fun Player.listenEvents(): Flow<Player.Events> = callbackFlow {
    val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            trySend(player.currentEvents)
        }
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            trySend(player.currentEvents)
        }
        override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
            trySend(player.currentEvents)
        }
        override fun onPlaybackParametersChanged(playbackParameters: androidx.media3.common.PlaybackParameters) {
            trySend(player.currentEvents)
        }
        override fun onPositionDiscontinuity(reason: Int) {
            trySend(player.currentEvents)
        }
        override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
            trySend(player.currentEvents)
        }
        override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
            trySend(player.currentEvents)
        }
    }
    addListener(listener)
    awaitClose { removeListener(listener) }
}
