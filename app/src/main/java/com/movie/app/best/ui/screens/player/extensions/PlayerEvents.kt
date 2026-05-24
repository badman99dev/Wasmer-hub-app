package com.movie.app.best.ui.screens.player.extensions

import androidx.media3.common.Player
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

fun Player.listenEvents(): Flow<Player.Events> = callbackFlow {
    val listener = object : Player.Listener {
        override fun onEvents(events: Player.Events) {
            trySend(events)
        }
    }
    addListener(listener)
    awaitClose { removeListener(listener) }
}
