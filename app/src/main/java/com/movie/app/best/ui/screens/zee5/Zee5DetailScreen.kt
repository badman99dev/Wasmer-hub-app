package com.movie.app.best.ui.screens.zee5

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.movie.app.best.data.model.Zee5DetailResponse
import com.movie.app.best.data.model.Zee5Item
import com.movie.app.best.data.model.Zee5Season
import com.movie.app.best.ui.theme.WasmerBlack
import com.movie.app.best.ui.theme.WasmerCardDark
import com.movie.app.best.ui.theme.WasmerRed
import com.movie.app.best.ui.theme.WasmerSubText

@Composable
fun Zee5DetailScreen(
    contentId: String,
    navController: NavController,
    viewModel: Zee5ViewModel = hiltViewModel(),
    onPlayClick: (String, String, String, String, String, String, Boolean, String) -> Unit
) {
    val detailState by viewModel.detailState.collectAsState()
    val episodesState by viewModel.episodesState.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()

    val detail = (detailState as? Zee5DetailState.Success)?.detail

    var selectedSeasonId by remember { mutableStateOf<String?>(null) }
    val seasonsList = remember { mutableStateOf<List<com.movie.app.best.data.model.Zee5Season>>(emptyList()) }

    LaunchedEffect(detail) {
        detail?.let {
            if (it.isTvShow) {
                val seasonId = it.firstSeasonId
                if (seasonId != null) {
                    selectedSeasonId = seasonId
                    viewModel.loadEpisodesFromSeasons(it.id ?: "")
                }
                try {
                    val seasonData = viewModel.apiService.getSeasons(it.id ?: "")
                    seasonsList.value = seasonData.seasons ?: emptyList()
                    if (selectedSeasonId == null && seasonData.seasons?.isNotEmpty() == true) {
                        selectedSeasonId = seasonData.seasons.last()?.id
                    }
                } catch(_: Exception) {}
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WasmerBlack)
    ) {
        when (detailState) {
            is Zee5DetailState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = WasmerRed)
                }
            }
            is Zee5DetailState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = (detailState as Zee5DetailState.Error).message,
                            color = WasmerSubText
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.loadDetails(contentId) },
                            colors = ButtonDefaults.buttonColors(containerColor = WasmerRed)
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
            is Zee5DetailState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Hero/Poster Section
                    item {
                        Zee5DetailHero(
                            detail = detail!!,
                            onPlayClick = {
                                if (detail?.isTvShow == true) {
                                    val firstEpisode = (episodesState as? Zee5EpisodesState.Success)?.episodes?.firstOrNull()
                                    if (firstEpisode?.id != null) {
                                        viewModel.loadPlayback(firstEpisode.id!!)
                                    }
                                } else {
                                    viewModel.loadPlayback(contentId)
                                }
                            },
                            onBackClick = { navController.popBackStack() }
                        )
                    }

                    // Meta Info
                    item {
                        Zee5DetailMeta(detail = detail!!)
                    }

                    // Description
                    item {
                        Zee5DetailDescription(detail = detail!!)
                    }

                    // Season Selector (for TV shows)
                    if (detail?.isTvShow == true && seasonsList.value.isNotEmpty()) {
                        item {
                            Zee5SeasonSelector(
                                seasons = seasonsList.value,
                                    selectedSeasonId = selectedSeasonId,
                                    onSeasonSelect = { seasonId ->
                                        selectedSeasonId = seasonId
                                        viewModel.loadEpisodesForSeason(seasonId)
                                    }
                                )
                            }
                        }
                    }

                    // Episodes List (always show for TV shows)
                    if (detail?.isTvShow == true) {
                        item {
                            when (episodesState) {
                                is Zee5EpisodesState.Loading -> {
                                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = WasmerRed, modifier = Modifier.size(24.dp))
                                    }
                                }
                                is Zee5EpisodesState.Error -> {
                                    Text(text = (episodesState as Zee5EpisodesState.Error).message, color = WasmerSubText, fontSize = 12.sp, modifier = Modifier.padding(16.dp))
                                }
                                is Zee5EpisodesState.Success -> {
                                    val episodes = (episodesState as Zee5EpisodesState.Success).episodes
                                    if (episodes.isNotEmpty()) {
                                        Zee5EpisodesList(
                                            episodes = episodes,
                                            onEpisodeClick = { episode ->
                                                episode.id?.let { id ->
                                                    viewModel.loadPlayback(id)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Related Content
                    if (detail?.related?.isNotEmpty() == true) {
                        item {
                            Zee5RelatedSection(
                                related = detail.related!!,
                                onItemClick = { item ->
                                    item.id?.let { id ->
                                        navController.navigate("zee5_detail/${id}")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Playback overlay (when playback is ready)
        AnimatedVisibility(
            visible = playbackState is Zee5PlaybackState.Success,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val playback = (playbackState as? Zee5PlaybackState.Success)?.playback
            playback?.let {
                LaunchedEffect(it) {
                    val streamUrl = it.effectiveStreamUrl ?: return@LaunchedEffect
                    val title = detail?.title ?: ""
                        onPlayClick(
                            streamUrl,
                            streamUrl,
                            title,
                            "",
                            contentId,
                            "",
                            false,
                            "zee5"
                        )
                    viewModel.resetPlayback()
                }
        }
    }
}

@Composable
fun Zee5DetailHero(
    detail: Zee5DetailResponse,
    onPlayClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
    ) {
        // Background image
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(detail.effectiveLandscapeUrl ?: detail.effectiveImageUrl)
                .crossfade(true)
                .build(),
            contentDescription = detail.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            WasmerBlack.copy(alpha = 0.3f),
                            WasmerBlack.copy(alpha = 0.6f),
                            WasmerBlack.copy(alpha = 0.95f)
                        )
                    )
                )
        )

        // Back button
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .padding(16.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        // Play button (centered)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(WasmerRed.copy(alpha = 0.9f))
                    .clickable { onPlayClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Title at bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(
                text = detail.title ?: "",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Age rating
                detail.ageRating?.let {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = it,
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Duration
                detail.duration?.let {
                    if (it > 0) {
                        Text(
                            text = "${it}m",
                            color = WasmerSubText,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }

                // Release year
                detail.releaseDate?.let {
                    Text(
                        text = it.take(4),
                        color = WasmerSubText,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun Zee5DetailMeta(detail: Zee5DetailResponse) {
    Column(modifier = Modifier.padding(16.dp)) {
        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { /* Play action handled by hero */ },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = WasmerRed),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, "Play", tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Watch Now", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Genres
        detail.genres?.let { genres ->
            if (genres.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(genres) { genre ->
                        genre.value?.let { value ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(WasmerCardDark)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = value,
                                    color = WasmerSubText,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Cast
        detail.actors?.let { actors ->
            if (actors.isNotEmpty()) {
                Text(
                    text = "Cast: ${actors.joinToString(", ")}",
                    color = WasmerSubText,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        // Directors
        detail.directors?.let { directors ->
            if (directors.isNotEmpty()) {
                Text(
                    text = "Director: ${directors.joinToString(", ")}",
                    color = WasmerSubText,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun Zee5DetailDescription(detail: Zee5DetailResponse) {
    var expanded by remember { mutableStateOf(false) }

    detail.description?.let { desc ->
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = desc,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 13.sp,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis
            )

            if (desc.length > 150) {
                TextButton(
                    onClick = { expanded = !expanded },
                    colors = ButtonDefaults.textButtonColors(contentColor = WasmerRed)
                ) {
                    Text(
                        text = if (expanded) "Show Less" else "Show More",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun Zee5SeasonSelector(
    seasons: List<Zee5Season>,
    selectedSeasonId: String?,
    onSeasonSelect: (String) -> Unit
) {
    val selectedSeason = selectedSeasonId ?: seasons.lastOrNull()?.id ?: ""

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Seasons",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(seasons) { season ->
                val isSelected = selectedSeason == season.id
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isSelected) WasmerRed
                            else WasmerCardDark
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            season.id?.let { onSeasonSelect(it) }
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = season.title ?: "Season ${season.seasonNumber}",
                        color = if (isSelected) Color.White else WasmerSubText,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun Zee5EpisodesList(
    episodes: List<Zee5Item>,
    onEpisodeClick: (Zee5Item) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Episodes",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.heightIn(max = 600.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(episodes) { episode ->
                Zee5EpisodeCard(
                    episode = episode,
                    onClick = { onEpisodeClick(episode) }
                )
            }
        }
    }
}

@Composable
fun Zee5EpisodeCard(
    episode: Zee5Item,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(WasmerCardDark)
            .clickable { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Episode thumbnail (16:9 aspect ratio)
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(68.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(WasmerBlack)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(episode.effectiveLandscapeUrl ?: episode.effectiveImageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = episode.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Play icon overlay
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(24.dp)
                )
            }

            // Episode number badge
            episode.episodeNumber?.let { epNum ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Ep $epNum",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Episode info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = episode.title ?: "",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            episode.duration?.let { duration ->
                if (duration > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${duration}m",
                        color = WasmerSubText,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun Zee5RelatedSection(
    related: List<Zee5Item>,
    onItemClick: (Zee5Item) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = "More Like This",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(related) { item ->
                Zee5Card(item = item, onClick = { onItemClick(item) })
            }
        }
    }
}
