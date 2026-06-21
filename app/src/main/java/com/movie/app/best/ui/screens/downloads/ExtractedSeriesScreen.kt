package com.movie.app.best.ui.screens.downloads

import android.os.Environment
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import java.io.File

data class ExtractedEpisode(
    val fileName: String,
    val filePath: String,
    val size: Long
)

fun scanExtractedVideos(extractPath: String): List<ExtractedEpisode> {
    val dir = File(extractPath)
    if (!dir.exists() || !dir.isDirectory) return emptyList()

    val videoExts = setOf("mp4", "mkv", "avi", "webm", "mov", "flv", "3gp", "ts", "m4v")
    return dir.listFiles()
        ?.filter { it.isFile && it.extension.lowercase() in videoExts }
        ?.sortedBy { it.name }
        ?.map { file ->
            ExtractedEpisode(
                fileName = file.name,
                filePath = file.absolutePath,
                size = file.length()
            )
        } ?: emptyList()
}

@Composable
fun ExtractedSeriesScreen(
    extractPath: String,
    slug: String,
    posterPath: String,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val episodes = remember(extractPath) { scanExtractedVideos(extractPath) }
    var currentEpisode by remember { mutableStateOf<ExtractedEpisode?>(null) }
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }

    DisposableEffect(Unit) {
        val player = ExoPlayer.Builder(context).build()
        exoPlayer = player
        onDispose {
            player.release()
            exoPlayer = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = "Extracted Series",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
        }

        currentEpisode?.let { ep ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                        }
                    },
                    update = { view ->
                        exoPlayer?.setMediaItem(MediaItem.fromUri("file://${ep.filePath}"))
                        exoPlayer?.prepare()
                        exoPlayer?.playWhenReady = true
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } ?: run {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (posterPath.isNotEmpty() && File(posterPath).exists()) {
                    AsyncImage(
                        model = File(posterPath),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        "Select an episode to play",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
            }
        }

        if (episodes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No episodes found in extracted folder",
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
            ) {
                item {
                    Text(
                        text = "Episodes (${episodes.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                items(episodes, key = { it.filePath }) { episode ->
                    ExtractedEpisodeItem(
                        episode = episode,
                        posterPath = posterPath,
                        isPlaying = currentEpisode?.filePath == episode.filePath,
                        onPlay = {
                            currentEpisode = episode
                            exoPlayer?.setMediaItem(MediaItem.fromUri("file://${episode.filePath}"))
                            exoPlayer?.prepare()
                            exoPlayer?.playWhenReady = true
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ExtractedEpisodeItem(
    episode: ExtractedEpisode,
    posterPath: String,
    isPlaying: Boolean,
    onPlay: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.06f))
            .clickable(onClick = onPlay)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(64.dp, 36.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            if (posterPath.isNotEmpty() && File(posterPath).exists()) {
                AsyncImage(
                    model = File(posterPath),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = episode.fileName,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatFileSize(episode.size),
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        if (isPlaying) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}
