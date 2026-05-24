package com.movie.app.best.ui.screens.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import okhttp3.OkHttpClient

@Composable
fun VideoPlayerScreen(
    onBackClick: () -> Unit,
    playerUrl: String,
    streamUrl: String,
    title: String,
    youtubeId: String,
    movieId: String,
    viewModel: VideoPlayerViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val effectiveUrl = remember(streamUrl, playerUrl) {
        val s = streamUrl.trim()
        val p = playerUrl.trim()
        when {
            s.isNotEmpty() && s.startsWith("http") -> s
            p.isNotEmpty() && p.startsWith("http") -> p
            s.isNotEmpty() -> s
            p.isNotEmpty() -> p
            else -> ""
        }
    }

    val trackSelector = remember { DefaultTrackSelector(context) }

    val exoPlayer = remember(effectiveUrl) {
        if (effectiveUrl.isEmpty()) return@remember null

        val okHttpClient = OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        val okHttpFactory = OkHttpDataSource.Factory(okHttpClient)
        val defaultFactory = DefaultDataSource.Factory(context, okHttpFactory)

        val mediaSourceFactory = if (effectiveUrl.contains(".m3u8", ignoreCase = true)) {
            HlsMediaSource.Factory(defaultFactory)
        } else {
            DefaultMediaSourceFactory(defaultFactory)
        }

        ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(DefaultLoadControl.Builder().build())
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(effectiveUrl))
                prepare()
                playWhenReady = true
            }
    }

    var playerError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(exoPlayer) {
        exoPlayer?.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                playerError = error.message ?: "Playback error"
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) playerError = null
            }
        })
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer?.pause()
                Lifecycle.Event.ON_RESUME -> {
                    if (exoPlayer?.playbackState == Player.STATE_READY) exoPlayer.play()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer?.release()
        }
    }

    BackHandler { onBackClick() }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (exoPlayer != null && playerError == null) {
            MediaPlayerScreen(
                player = exoPlayer,
                onBackClick = onBackClick,
                onPlayInBackgroundClick = { },
                title = title,
            )
        } else if (playerError != null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = playerError ?: "Unknown error",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = {
                    playerError = null
                    exoPlayer?.let { player ->
                        player.setMediaItem(MediaItem.fromUri(effectiveUrl))
                        player.prepare()
                        player.playWhenReady = true
                    }
                }) {
                    Text("Retry", color = Color.White)
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onBackClick) {
                    Text("Exit", color = Color.White)
                }
            }
        }
    }
}
