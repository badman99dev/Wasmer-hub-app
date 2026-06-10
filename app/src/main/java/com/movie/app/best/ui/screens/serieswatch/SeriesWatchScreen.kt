package com.movie.app.best.ui.screens.serieswatch

import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import com.movie.app.best.data.debug.DebugInterceptor
import com.movie.app.best.data.model.ExtractionState
import com.movie.app.best.data.model.WatchEpisode
import com.movie.app.best.ui.screens.serieswatch.components.EpisodeCard
import com.movie.app.best.ui.screens.serieswatch.components.LanguageSelector
import com.movie.app.best.ui.screens.serieswatch.components.SeasonChips
import okhttp3.OkHttpClient

import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesWatchScreen(
    onBackClick: () -> Unit,
    viewModel: SeriesWatchViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    var showLangSheet by remember { mutableStateOf(false) }

    val trackSelector = remember { DefaultTrackSelector(context) }
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var lowestQualityApplied by remember { mutableStateOf(false) }

    LaunchedEffect(state.currentM3u8) {
        val m3u8 = state.currentM3u8
        if (m3u8 == null) {
            exoPlayer?.release()
            exoPlayer = null
            lowestQualityApplied = false
            return@LaunchedEffect
        }

        exoPlayer?.release()
        lowestQualityApplied = false

        val okClient = OkHttpClient.Builder()
            .followRedirects(true).followSslRedirects(true)
            .addInterceptor(DebugInterceptor()).build()

        val okFactory = OkHttpDataSource.Factory(okClient)
            .setUserAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/125.0.0.0 Mobile Safari/537.36")
            .setDefaultRequestProperties(mapOf(
                "Referer" to "https://gemma416okl.com/",
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
    }

    DisposableEffect(exoPlayer) {
        val player = exoPlayer ?: return@DisposableEffect onDispose {}
        val listener = object : Player.Listener {
            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                if (!lowestQualityApplied) {
                    val heights = mutableSetOf<Int>()
                    for (group in tracks.groups) {
                        if (group.type != C.TRACK_TYPE_VIDEO) continue
                        for (i in 0 until group.length) {
                            val fmt = group.getTrackFormat(i)
                            if (fmt.height > 0) heights.add(fmt.height)
                        }
                    }
                    if (heights.isNotEmpty()) {
                        lowestQualityApplied = true
                        val lowest = heights.min()
                        for (group in tracks.groups) {
                            if (group.type != C.TRACK_TYPE_VIDEO) continue
                            for (i in 0 until group.length) {
                                val fmt = group.getTrackFormat(i)
                                if (fmt.height == lowest) {
                                    player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                                        .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                                        .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, listOf(i)))
                                        .build()
                                    break
                                }
                            }
                        }
                    }
                }
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
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
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    BackHandler {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onBackClick()
    }

    if (showLangSheet) {
        LanguageSelector(
            availableLanguages = state.availableLanguages,
            selectedLanguage = state.selectedLanguage,
            onLanguageSelect = { viewModel.selectLanguage(it) },
            onDismiss = { showLangSheet = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111119))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = {
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        onBackClick()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(20.dp))
                }

                Text(
                    text = state.currentEpisode?.displayTitle ?: "Select Episode",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )

                if (state.availableLanguages.size > 1) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .clickable { showLangSheet = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Language, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        Text(
                            text = state.selectedLanguage,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black)
        ) {
            if (exoPlayer != null) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = exoPlayer
                            useController = true
                            controllerShowTimeoutMs = 4000
                            controllerHideOnTouch = true
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎬", fontSize = 40.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Select an episode to play", color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp)
                    }
                }
            }
        }

        if (state.seasonKeys.isNotEmpty()) {
            SeasonChips(
                seasons = state.seasonKeys,
                selectedSeason = state.selectedSeason,
                onSeasonSelect = { viewModel.selectSeason(it) }
            )
        }

        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFFE50914), modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Fetching episodes...", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp)
                }
            }
        } else if (state.error != null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚠️", fontSize = 40.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(state.error ?: "Unknown error", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    androidx.compose.material3.TextButton(onClick = { viewModel.selectSeason(state.selectedSeason) }) {
                        Text("Retry", color = Color(0xFFE50914))
                    }
                }
            }
        } else {
            val episodes = state.mergedEpisodes
            if (episodes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No episodes available for this season", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(episodes, key = { it.episodeNo }) { episode ->
                        EpisodeCard(
                            episode = episode,
                            isPlaying = state.currentEpisode?.episodeNo == episode.episodeNo && state.currentEpisode?.seasonNo == episode.seasonNo,
                            onPlayClick = { viewModel.onEpisodeClick(episode) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}
