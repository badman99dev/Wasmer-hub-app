package com.movie.app.best.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * App-wide flag toggled on by screens that go truly edge-to-edge full screen
 * (the immersive / landscape video player). While this is true, the global
 * cutout-safe black strip added in MainActivity is suppressed so the player
 * can use the entire display — every other screen keeps the strip, the same
 * way YouTube keeps a permanent black status-bar zone everywhere except when
 * a video is expanded to full screen.
 */
object FullscreenPlayerState {
    var isActive by mutableStateOf(false)
}
