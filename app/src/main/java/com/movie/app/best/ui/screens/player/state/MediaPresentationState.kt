package com.movie.app.best.ui.screens.player.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import com.movie.app.best.ui.screens.player.extensions.formatted
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

@Composable
fun rememberMediaPresentationState(player: Player): MediaPresentationState {
    val state = remember { MediaPresentationState(player) }
    LaunchedEffect(player) {
        state.updatePosition()
        state.updateDuration()
        state.isPlaying = player.isPlaying
        state.isBuffering = player.playbackState == Player.STATE_BUFFERING

        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                state.isBuffering = playbackState == Player.STATE_BUFFERING
                state.updateDuration()
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                state.isPlaying = isPlaying
            }
            override fun onPositionDiscontinuity(reason: Int) {
                state.updatePosition()
            }
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                state.updateDuration()
                state.updatePosition()
            }
            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                state.updateDuration()
            }
        }
        player.addListener(listener)

        try {
            while (true) {
                delay(500)
                if (player.isPlaying) state.updatePosition()
            }
        } finally {
            player.removeListener(listener)
        }
    }
    return state
}

@Stable
class MediaPresentationState(private val player: Player) {
    var position: Long by mutableLongStateOf(0L)
        internal set
    var duration: Long by mutableLongStateOf(0L)
        internal set
    var isPlaying: Boolean by mutableStateOf(false)
        internal set
    var isBuffering: Boolean by mutableStateOf(false)
        internal set

    fun updatePosition() { position = player.currentPosition.coerceAtLeast(0L) }
    fun updateDuration() { duration = player.duration.coerceAtLeast(0L) }
}

val MediaPresentationState.positionFormatted: String get() = position.milliseconds.formatted()
val MediaPresentationState.durationFormatted: String get() = duration.milliseconds.formatted()
val MediaPresentationState.pendingPositionFormatted: String get() = (duration - position).milliseconds.formatted()
