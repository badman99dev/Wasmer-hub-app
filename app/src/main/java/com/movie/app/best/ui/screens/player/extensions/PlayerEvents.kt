package com.movie.app.best.ui.screens.player.extensions

import androidx.media3.common.Player
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

suspend fun Player.awaitEvent(check: (Player) -> Boolean) {
    suspendCancellableCoroutine<Unit> { cont ->
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) { if (check(this@awaitEvent)) cont.resume(Unit) }
            override fun onIsPlayingChanged(isPlaying: Boolean) { if (check(this@awaitEvent)) cont.resume(Unit) }
            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) { if (check(this@awaitEvent)) cont.resume(Unit) }
            override fun onPlaybackParametersChanged(playbackParameters: androidx.media3.common.PlaybackParameters) { if (check(this@awaitEvent)) cont.resume(Unit) }
            override fun onPositionDiscontinuity(reason: Int) { if (check(this@awaitEvent)) cont.resume(Unit) }
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) { if (check(this@awaitEvent)) cont.resume(Unit) }
            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) { if (check(this@awaitEvent)) cont.resume(Unit) }
        }
        addListener(listener)
        cont.invokeOnCancellation { removeListener(listener) }
    }
}
