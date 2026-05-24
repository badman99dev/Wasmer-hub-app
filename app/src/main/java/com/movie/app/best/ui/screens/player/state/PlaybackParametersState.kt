package com.movie.app.best.ui.screens.player.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import androidx.media3.common.listen
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.launch

@Composable
fun rememberPlaybackParametersState(player: Player): PlaybackParametersState {
    val scope = rememberCoroutineScope()
    val state = remember { PlaybackParametersState(player, scope) }
    LaunchedEffect(player) { state.observe() }
    return state
}

class PlaybackParametersState(private val player: Player, private val scope: kotlinx.coroutines.CoroutineScope) {
    var speed: Float by mutableFloatStateOf(1f)
        private set

    var skipSilenceEnabled: Boolean by mutableStateOf(false)
        private set

    fun setPlaybackSpeed(speed: Float) { player.setPlaybackSpeed(speed) }

    fun setIsSkipSilenceEnabled(enabled: Boolean) {
        scope.launch {
            if (player is ExoPlayer) player.skipSilenceEnabled = enabled
            updateSkipSilenceEnabled()
        }
    }

    suspend fun observe() {
        updateSpeed()
        updateSkipSilenceEnabled()
        player.listen { events ->
            if (events.contains(Player.EVENT_PLAYBACK_PARAMETERS_CHANGED)) updateSpeed()
        }
    }

    private fun updateSpeed() { speed = player.playbackParameters.speed }

    private fun updateSkipSilenceEnabled() {
        scope.launch {
            skipSilenceEnabled = (player as? ExoPlayer)?.skipSilenceEnabled ?: false
        }
    }
}
