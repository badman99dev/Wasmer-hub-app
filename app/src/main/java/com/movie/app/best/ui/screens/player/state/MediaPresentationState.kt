package com.movie.app.best.ui.screens.player.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import com.movie.app.best.ui.screens.player.extensions.formatted
import com.movie.app.best.ui.screens.player.extensions.listenEvents
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun rememberMediaPresentationState(player: Player): MediaPresentationState {
    val state = remember { MediaPresentationState(player) }
    LaunchedEffect(player) { state.observe() }
    return state
}

@Stable
class MediaPresentationState(private val player: Player) {
    var position: Long by mutableLongStateOf(0L)
        private set
    var duration: Long by mutableLongStateOf(0L)
        private set
    var isPlaying: Boolean by mutableStateOf(false)
        private set
    var isBuffering: Boolean by mutableStateOf(false)
        private set

    suspend fun observe() {
        updatePosition()
        updateDuration()
        isPlaying = player.isPlaying
        isBuffering = player.playbackState == Player.STATE_BUFFERING

        coroutineScope {
            launch {
                player.listenEvents().collect { events ->
                    if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) ||
                        events.contains(Player.EVENT_TIMELINE_CHANGED) ||
                        events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED)
                    ) updateDuration()

                    if (events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED))
                        isBuffering = player.playbackState == Player.STATE_BUFFERING

                    if (events.contains(Player.EVENT_IS_PLAYING_CHANGED))
                        isPlaying = player.isPlaying

                    if (events.contains(Player.EVENT_POSITION_DISCONTINUITY))
                        updatePosition()
                }
            }
            while (true) {
                delay(500)
                if (player.isPlaying) updatePosition()
            }
        }
    }

    private fun updatePosition() { position = player.currentPosition.coerceAtLeast(0L) }
    private fun updateDuration() { duration = player.duration.coerceAtLeast(0L) }
}

val MediaPresentationState.positionFormatted: String get() = position.milliseconds.formatted()
val MediaPresentationState.durationFormatted: String get() = duration.milliseconds.formatted()
val MediaPresentationState.pendingPositionFormatted: String get() = (duration - position).milliseconds.formatted()
