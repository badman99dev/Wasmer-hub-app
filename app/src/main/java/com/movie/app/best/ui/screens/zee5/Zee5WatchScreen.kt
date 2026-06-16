package com.movie.app.best.ui.screens.zee5

import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import coil.compose.AsyncImage
import com.movie.app.best.data.debug.DebugInterceptor
import com.movie.app.best.data.debug.Zee5False404Interceptor
import com.movie.app.best.data.model.Zee5Item
import com.movie.app.best.data.settings.VideoQualitySettings
import com.movie.app.best.ui.components.GlassBadge
import com.movie.app.best.ui.screens.player.MediaPlayerScreen
import com.movie.app.best.ui.theme.WasmerRed
import com.movie.app.best.util.ImmersiveMode
import okhttp3.OkHttpClient

@Composable
fun Zee5WatchScreen(
    onBackClick: () -> Unit,
    viewModel: Zee5WatchViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    var isFullscreen by remember { mutableStateOf(false) }
    val episodeListState = rememberLazyListState()

    val shouldLoadMoreEpisodes = remember {
        derivedStateOf {
            val layoutInfo = episodeListState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItem >= totalItems - 3
        }
    }

    LaunchedEffect(shouldLoadMoreEpisodes.value) {
        if (shouldLoadMoreEpisodes.value) {
            viewModel.loadMoreEpisodes()
        }
    }

    val trackSelector = remember {
        DefaultTrackSelector(context).apply {
            val params = buildUponParameters()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            setParameters(VideoQualitySettings.applyTo(params).build())
        }
    }
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var playerError by remember { mutableStateOf<String?>(null) }

    var zee5RetryCount by remember { mutableStateOf(0) }

    LaunchedEffect(state.currentM3u8) {
        val m3u8 = state.currentM3u8
        if (m3u8 == null) {
            exoPlayer?.release()
            exoPlayer = null
            return@LaunchedEffect
        }

        exoPlayer?.release()
        zee5RetryCount = 0
        playerError = null

        val okClient = OkHttpClient.Builder()
            .followRedirects(true).followSslRedirects(true)
            .addInterceptor(Zee5False404Interceptor())
            .addInterceptor(DebugInterceptor()).build()

        val okFactory = OkHttpDataSource.Factory(okClient)
            .setUserAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/125.0.0.0 Mobile Safari/537.36")
            .setDefaultRequestProperties(mapOf(
                "Referer" to "https://zee5-no-ads.vercel.app/",
                "Accept" to "*/*"
            ))

        val dsFactory = DefaultDataSource.Factory(context, okFactory)
        val hlsFactory = HlsMediaSource.Factory(dsFactory)

        val player = ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(hlsFactory)
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(5000, 30000, DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS, DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS)
                    .setBackBuffer(300000, true).build()
            ).build()

        player.setMediaItem(MediaItem.fromUri(m3u8))
        player.prepare()
        player.playWhenReady = true
        exoPlayer = player
        viewModel.onPlaybackReady()
    }

    DisposableEffect(exoPlayer) {
        val player = exoPlayer ?: return@DisposableEffect onDispose {}
        val listener = object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                if (zee5RetryCount < 2) {
                    zee5RetryCount++
                    player.setMediaItem(MediaItem.fromUri(state.currentM3u8 ?: return))
                    player.prepare()
                    player.playWhenReady = true
                    return
                }
                playerError = error.message ?: "Playback error"
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) playerError = null
                activity?.let {
                    val pState = player.playbackState
                    ImmersiveMode.keepScreenOn(it, isPlaying || pState == Player.STATE_BUFFERING)
                }
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                activity?.let {
                    val playing = player.isPlaying
                    ImmersiveMode.keepScreenOn(it, playing || playbackState == Player.STATE_BUFFERING)
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            activity?.let { ImmersiveMode.keepScreenOn(it, false) }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer?.pause()
                Lifecycle.Event.ON_RESUME -> { if (exoPlayer?.playbackState == Player.STATE_READY) exoPlayer?.play() }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer?.release()
            exoPlayer = null
            activity?.let { ImmersiveMode.exit(it) }
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    val exitFullscreen = {
        isFullscreen = false
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        activity?.let { ImmersiveMode.exit(it) }
    }

    val enterFullscreen = {
        isFullscreen = true
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        activity?.let { ImmersiveMode.enter(it) }
    }

    BackHandler {
        if (isFullscreen) {
            exitFullscreen()
        } else {
            activity?.let { ImmersiveMode.exit(it) }
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            onBackClick()
        }
    }

    if (isFullscreen && exoPlayer != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            MediaPlayerScreen(
                player = exoPlayer,
                modifier = Modifier.fillMaxSize(),
                onBackClick = { exitFullscreen() },
                onPlayInBackgroundClick = {},
                onFullscreenClick = { exitFullscreen() },
                isInline = false,
                title = state.currentEpisode?.title ?: state.detail?.title ?: ""
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111119))
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    activity?.let { ImmersiveMode.exit(it) }
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    onBackClick()
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Text(
                text = state.detail?.title ?: "Loading...",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black)
                .zIndex(1f)
        ) {
            if (exoPlayer != null && playerError == null) {
                MediaPlayerScreen(
                    player = exoPlayer,
                    modifier = Modifier.fillMaxSize(),
                    onBackClick = {
                        activity?.let { ImmersiveMode.exit(it) }
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        onBackClick()
                    },
                    onPlayInBackgroundClick = {},
                    onFullscreenClick = { enterFullscreen() },
                    isInline = true,
                    title = state.currentEpisode?.title ?: state.detail?.title ?: ""
                )
            } else if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = WasmerRed, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Loading...", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
                    }
                }
            } else if (playerError != null || (state.error != null && state.currentM3u8 == null)) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            playerError ?: state.error ?: "Error",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = {
                            playerError = null
                            zee5RetryCount = 0
                            state.currentM3u8?.let { url ->
                                exoPlayer?.setMediaItem(MediaItem.fromUri(url))
                                exoPlayer?.prepare()
                                exoPlayer?.playWhenReady = true
                            } ?: state.currentEpisode?.let { viewModel.onEpisodeClick(it) }
                        }) { Text("Retry", color = WasmerRed) }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎬", fontSize = 40.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Select an episode to play", color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp)
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111119))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (state.currentEpisode != null) {
                Text(
                    text = state.currentEpisode?.title ?: "",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val epMeta = buildList {
                    state.currentEpisode?.episodeNumber?.let { add("Episode $it") }
                    state.currentEpisode?.duration?.let { if (it > 0) add("${it / 60} min") }
                }.joinToString(" · ")
                if (epMeta.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(epMeta, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                }
            } else {
                Text(
                    text = "Select an episode to play",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
            }
        }

        if (state.seasons.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF111119))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.seasons.forEach { season ->
                    val isSelected = season.id == state.selectedSeasonId
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) WasmerRed else Color.White.copy(alpha = 0.1f))
                            .clickable { season.id?.let { viewModel.selectSeason(it) } }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = season.title ?: "Season ${season.seasonNumber}",
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        if (state.episodes.isEmpty() && !state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No episodes available", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                state = episodeListState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(state.episodes, key = { it.id ?: it.episodeNumber?.toString() ?: it.title ?: "" }) { episode ->
                    Zee5WatchEpisodeCard(
                        episode = episode,
                        isPlaying = state.currentEpisode?.id == episode.id,
                        onPlayClick = { viewModel.onEpisodeClick(episode) }
                    )
                }

                if (state.isLoadingMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = WasmerRed, modifier = Modifier.size(24.dp))
                        }
                    }
                }

                if (!state.hasMoreEpisodes && state.episodes.isNotEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            GlassBadge(text = "Reached to bottom")
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun Zee5WatchEpisodeCard(
    episode: Zee5Item,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                val thumbUrl = episode.effectiveLandscapeUrl ?: episode.effectiveImageUrl
                if (thumbUrl != null) {
                    AsyncImage(
                        model = thumbUrl,
                        contentDescription = episode.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(colors = listOf(Color(0xFF1A1A2E), Color(0xFF16213E)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "E${episode.episodeNumber ?: "?"}",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.25f))
                        .clickable(onClick = onPlayClick),
                    contentAlignment = Alignment.Center
                ) {
                    val playBg = if (isPlaying) Color(0xFF4CAF50) else WasmerRed
                    IconButton(
                        onClick = onPlayClick,
                        modifier = Modifier
                            .size(44.dp)
                            .background(playBg.copy(alpha = 0.9f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .background(Color(0xFF4CAF50), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("NOW PLAYING", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = episode.title ?: "Untitled",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                val meta = buildList {
                    episode.episodeNumber?.let { add("Episode $it") }
                    episode.duration?.let { if (it > 0) add("${it / 60} min") }
                    episode.releaseDate?.let { try { add(java.text.SimpleDateFormat("yyyy", java.util.Locale.US).format(java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(it) ?: return@let)) } catch (_: Exception) {} }
                }.joinToString(" · ")

                if (meta.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(meta, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                }

                if (!episode.description.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = episode.description,
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 13.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = Color.White.copy(alpha = 0.06f),
            thickness = 0.5.dp
        )
    }
}
