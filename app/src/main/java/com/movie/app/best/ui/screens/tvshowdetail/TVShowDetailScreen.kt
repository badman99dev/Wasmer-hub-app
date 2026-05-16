package com.movie.app.best.ui.screens.tvshowdetail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.movie.app.best.data.model.WasmerComment
import com.movie.app.best.data.model.WasmerDownloadLink
import com.movie.app.best.data.model.WasmerEpisode
import com.movie.app.best.data.model.WasmerSeason
import com.movie.app.best.data.model.WasmerMovieDetails
import com.movie.app.best.ui.components.BlurredContent
import com.movie.app.best.ui.components.CelebrationOverlay
import com.movie.app.best.ui.components.SkeletonDetailPage
import com.movie.app.best.ui.components.ErrorView
import com.movie.app.best.ui.components.StorylineWarningBadge
import com.movie.app.best.ui.screens.moviedetail.components.DetailActionButtons
import com.movie.app.best.ui.screens.moviedetail.components.ReportDrawer
import com.movie.app.best.ui.screens.moviedetail.components.ReportWaitingPopup
import com.movie.app.best.ui.screens.moviedetail.components.ReportResultModal
import kotlinx.coroutines.delay

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun TVShowDetailScreen(
    slug: String,
    onBackClick: () -> Unit,
    onPlayClick: (playerUrl: String, streamUrl: String, title: String, youtubeId: String, movieId: String) -> Unit,
    viewModel: TVShowDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        when {
            uiState.isLoading && uiState.series == null -> {
                SkeletonDetailPage()
            }
            uiState.error != null && uiState.series == null -> {
                ErrorView(
                    error = uiState.error ?: "Failed to load series details",
                    onRetry = viewModel::loadSeriesDetails
                )
            }
            uiState.series != null -> {
                TVShowDetailContent(
                    series = uiState.series!!,
                    uiState = uiState,
                    onBackClick = onBackClick,
                    onPlayClick = onPlayClick,
                    onPostComment = viewModel::postComment,
                    onRequestStream = viewModel::requestStream,
                    onStartDownload = viewModel::startDownload,
                    onResetCommentState = viewModel::resetCommentState,
                    onToggleBookmark = viewModel::toggleBookmark,
                    onToggleLike = viewModel::toggleLike,
                    onReportClick = viewModel::openReportDrawer
                )
            }
        }

        CelebrationOverlay(
            play = uiState.showCelebration,
            onFinished = viewModel::dismissCelebration
        )

        if (uiState.showReportDrawer) {
            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = viewModel::closeReportDrawer,
                containerColor = Color(0xFF1A1A1A)
            ) {
                ReportDrawer(
                    movieId = uiState.series?.id ?: 0,
                    onSubmit = { movieId, reportType, reason ->
                        viewModel.submitContentModeration(movieId, reportType, reason)
                    },
                    onDismiss = viewModel::closeReportDrawer
                )
            }
        }

        if (uiState.showReportWaiting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                ReportWaitingPopup()
            }
        }

        if (uiState.reportModerationResult != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                ReportResultModal(
                    moderation = uiState.reportModerationResult!!,
                    onDismiss = viewModel::dismissReportResult
                )
            }
        }
    }
}

@Composable
private fun TVShowDetailContent(
    series: WasmerMovieDetails,
    uiState: TVShowDetailUiState,
    onBackClick: () -> Unit,
    onPlayClick: (playerUrl: String, streamUrl: String, title: String, youtubeId: String, movieId: String) -> Unit,
    onPostComment: (name: String, msg: String) -> Unit,
    onRequestStream: () -> Unit,
    onStartDownload: (linkUrl: String) -> Unit,
    onResetCommentState: () -> Unit,
    onToggleBookmark: () -> Unit,
    onToggleLike: () -> Unit,
    onReportClick: () -> Unit = {}
) {
    val seasonKeys = uiState.episodesBySeason.keys.sorted()
    var selectedSeason by remember { mutableStateOf(seasonKeys.firstOrNull() ?: 1) }
    val shouldBlurPoster = series.contentModeration?.isPosterSexual == true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(550.dp)
        ) {
            BlurredContent(
                shouldBlur = shouldBlurPoster,
                modifier = Modifier.fillMaxSize()
            ) {
                AsyncImage(
                    model = series.backdropUrl.ifEmpty { series.posterUrl },
                    contentDescription = series.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.1f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                                MaterialTheme.colorScheme.background
                            ),
                            startY = 0f,
                            endY = 1000f
                        )
                    )
            )

            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            IconButton(
                onClick = onReportClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(end = 16.dp, bottom = 16.dp)
                    .background(
                        color = Color(0xFFE50914).copy(alpha = 0.6f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = "Report",
                    tint = Color.White
                )
            }

            if (series.qualityLabel.isNotEmpty()) {
                val qLabel = series.qualityLabel
                val bgColor = when {
                    qLabel.contains("4K", ignoreCase = true) -> Color(0xFFFFD700).copy(alpha = 0.25f)
                    qLabel.contains("UHD", ignoreCase = true) -> Color(0xFF00E5FF).copy(alpha = 0.2f)
                    qLabel.contains("HD", ignoreCase = true) -> Color(0xFF4FC3F7).copy(alpha = 0.15f)
                    qLabel.contains("CAM", ignoreCase = true) -> Color(0xFFFF5252).copy(alpha = 0.2f)
                    else -> Color.White.copy(alpha = 0.15f)
                }
                val textColor = when {
                    qLabel.contains("4K", ignoreCase = true) -> Color(0xFFFFD700)
                    qLabel.contains("UHD", ignoreCase = true) -> Color(0xFF00E5FF)
                    qLabel.contains("HD", ignoreCase = true) -> Color(0xFF4FC3F7)
                    qLabel.contains("CAM", ignoreCase = true) -> Color(0xFFFF5252)
                    else -> Color.White
                }
                val borderColor = textColor.copy(alpha = 0.3f)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Brush.linearGradient(colors = listOf(bgColor, Color.White.copy(alpha = 0.08f), bgColor)))
                        .border(0.5.dp, Brush.linearGradient(colors = listOf(borderColor, Color.White.copy(alpha = 0.1f), borderColor)), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = qLabel,
                        color = textColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = series.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                if (series.originalTitle.isNotEmpty() && series.originalTitle != series.title) {
                    Text(
                        text = series.originalTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (series.releaseYear.isNotEmpty()) {
                        Text(
                            text = series.releaseYear,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(text = " • ", color = Color.White.copy(alpha = 0.7f))
                    }

                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFFFC107)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = series.rating,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )

                    if (series.seasonLabel.isNotEmpty()) {
                        Text(text = " • ", color = Color.White.copy(alpha = 0.7f))
                        Text(
                            text = series.seasonLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    if (series.totalEpisodes > 0) {
                        Text(text = " • ", color = Color.White.copy(alpha = 0.7f))
                        Text(
                            text = "${series.totalEpisodes} episodes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    if (series.status.isNotEmpty()) {
                        Text(text = " • ", color = Color.White.copy(alpha = 0.7f))
                        Text(
                            text = series.status,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                if (series.runtime.isNotEmpty()) {
                    Text(
                        text = series.runtime,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }

                if (series.audioLabel.isNotEmpty() || series.language.isNotEmpty()) {
                    Text(
                        text = listOfNotNull(
                            if (series.audioLabel.isNotEmpty()) series.audioLabel else null,
                            if (series.language.isNotEmpty()) series.language else null
                        ).joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }

                if (series.country.isNotEmpty()) {
                    Text(
                        text = series.country,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                DetailActionButtons(
                    hasStream = series.hasStream,
                    streamRequested = uiState.streamRequested,
                    isInMyList = uiState.isBookmarked,
                    isLiked = uiState.isLiked,
                    onPlayClick = {
                        val workerUrl = "https://sparkling-breeze-1ad6.badman993944.workers.dev/?id=${series.id}"
                        onPlayClick("", workerUrl, series.title, "", series.id.toString())
                    },
                    onDownloadClick = { },
                    onMyListClick = onToggleBookmark,
                    onLikeClick = onToggleLike,
                    onRequestStream = onRequestStream
                )
            }
        }

        if (series.description.isNotEmpty()) {
            Text(
                text = series.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(16.dp)
            )
            if (series.contentModeration?.isStorylineSexual == true) {
                StorylineWarningBadge(isSexual = true, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }
        }

        if (series.director.isNotEmpty()) {
            SectionTitle("Director")
            Text(
                text = series.director,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )
        }

        if (series.cast.isNotEmpty()) {
            SectionTitle("Cast")
            Text(
                text = series.cast,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )
        }

        if (seasonKeys.isNotEmpty()) {
            SectionTitle("Episodes")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                seasonKeys.forEach { seasonNo ->
                    val isSelected = selectedSeason == seasonNo
                    Card(
                        modifier = Modifier.clickable { selectedSeason = seasonNo },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary
                            else Color.DarkGray.copy(alpha = 0.4f)
                        )
                    ) {
                        Text(
                            text = "Season $seasonNo",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            val episodes = uiState.episodesBySeason[selectedSeason] ?: emptyList()
            episodes.forEach { episode ->
                EpisodeItem(
                    episode = episode,
                    downloadLinks = uiState.linksByEpisode[episode.id] ?: emptyList(),
                    onPlayClick = {
                        val workerUrl = "https://sparkling-breeze-1ad6.badman993944.workers.dev/?id=${series.id}"
                        onPlayClick("", workerUrl, "${series.title} - S${episode.seasonNo}:E${episode.episodeNo}", "", series.id.toString())
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = Color.DarkGray.copy(alpha = 0.5f)
                )
            }

            if (episodes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No episodes found for this season",
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        if (uiState.screenshots.isNotEmpty()) {
            val shouldBlurScreenshots = series.contentModeration?.isScreenshotsSexual == true
            SectionTitle("Screenshots")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.screenshots) { url ->
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.width(240.dp)
                    ) {
                        Box {
                            BlurredContent(
                                shouldBlur = shouldBlurScreenshots,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = "Screenshot",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(16f / 9f)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (uiState.comments.isNotEmpty()) {
            SectionTitle("Comments")
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                uiState.comments.forEach { comment ->
                    CommentItem(comment)
                    HorizontalDivider(
                        color = Color.DarkGray.copy(alpha = 0.3f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }

        if (uiState.moreSeasons.isNotEmpty()) {
            SectionTitle("More Seasons")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.moreSeasons, key = { it.seasonLabel }) { season ->
                    MoreSeasonCard(season = season)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        TVDownloadSection(
            uiState = uiState,
            onStartDownload = onStartDownload
        )

        Spacer(modifier = Modifier.height(8.dp))
        TVCommentFormSection(
            isPosting = uiState.isCommentPosting,
            posted = uiState.commentPosted,
            error = uiState.commentError,
            onPost = onPostComment,
            onReset = onResetCommentState
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun EpisodeItem(
    episode: WasmerEpisode,
    downloadLinks: List<WasmerDownloadLink>,
    onPlayClick: () -> Unit
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .width(160.dp)
                .aspectRatio(16f / 9f)
        ) {
            Card(shape = RoundedCornerShape(8.dp)) {
                AsyncImage(
                    model = episode.stillPath,
                    contentDescription = episode.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable(onClick = onPlayClick),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${episode.episodeNo}. ${episode.title}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (episode.airDate.isNotEmpty()) {
                Text(
                    text = episode.airDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (episode.overview.isNotEmpty()) {
                Text(
                    text = episode.overview,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (episode.voteAverage > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = Color(0xFFFFC107)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = String.format("%.1f", episode.voteAverage),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            if (downloadLinks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    downloadLinks.forEach { link ->
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link.linkUrl))
                                context.startActivity(intent)
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(
                                text = link.label,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentItem(comment: WasmerComment) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            text = comment.userName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = comment.comment,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f)
        )
        Text(
            text = comment.createdAt,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun MoreSeasonCard(
    season: WasmerSeason
) {
    Card(
        modifier = Modifier.width(130.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            AsyncImage(
                model = season.posterUrl,
                contentDescription = season.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = season.seasonLabel,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (season.qualityLabel.isNotEmpty()) {
                    val sLabel = season.qualityLabel
                    val sBgColor = when {
                        sLabel.contains("4K", ignoreCase = true) -> Color(0xFFFFD700).copy(alpha = 0.25f)
                        sLabel.contains("UHD", ignoreCase = true) -> Color(0xFF00E5FF).copy(alpha = 0.2f)
                        sLabel.contains("HD", ignoreCase = true) -> Color(0xFF4FC3F7).copy(alpha = 0.15f)
                        sLabel.contains("CAM", ignoreCase = true) -> Color(0xFFFF5252).copy(alpha = 0.2f)
                        else -> Color.White.copy(alpha = 0.15f)
                    }
                    val sTextColor = when {
                        sLabel.contains("4K", ignoreCase = true) -> Color(0xFFFFD700)
                        sLabel.contains("UHD", ignoreCase = true) -> Color(0xFF00E5FF)
                        sLabel.contains("HD", ignoreCase = true) -> Color(0xFF4FC3F7)
                        sLabel.contains("CAM", ignoreCase = true) -> Color(0xFFFF5252)
                        else -> Color.White
                    }
                    val sBorderColor = sTextColor.copy(alpha = 0.3f)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Brush.linearGradient(colors = listOf(sBgColor, Color.White.copy(alpha = 0.08f), sBgColor)))
                            .border(0.5.dp, Brush.linearGradient(colors = listOf(sBorderColor, Color.White.copy(alpha = 0.1f), sBorderColor)), RoundedCornerShape(50))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = sLabel,
                            color = sTextColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
                Text(
                    text = "${season.totalEpisodes} episodes",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun TVDownloadSection(
    uiState: TVShowDetailUiState,
    onStartDownload: (linkUrl: String) -> Unit
) {
    val context = LocalContext.current

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionTitle("Download")

        if (uiState.isDownloadLoading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (uiState.downloadError != null) {
            Text(
                text = uiState.downloadError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (uiState.downloadStarted) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20).copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = Color(0xFF69F0AE),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Download started! Check Downloads tab",
                        fontSize = 13.sp,
                        color = Color(0xFF69F0AE)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TVCommentFormSection(
    isPosting: Boolean,
    posted: Boolean,
    error: String?,
    onPost: (name: String, msg: String) -> Unit,
    onReset: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionTitle("Post a Comment")

        if (posted) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20))
            ) {
                Text(
                    text = "Comment posted successfully!",
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
            }
            LaunchedEffect(posted) {
                if (posted) {
                    delay(2000)
                    onReset()
                }
            }
        }

        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Your Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color.DarkGray.copy(alpha = 0.3f),
                focusedContainerColor = Color.DarkGray.copy(alpha = 0.3f)
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text("Comment") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5,
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color.DarkGray.copy(alpha = 0.3f),
                focusedContainerColor = Color.DarkGray.copy(alpha = 0.3f)
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (name.isNotBlank() && message.isNotBlank()) {
                    onPost(name, message)
                }
            },
            enabled = !isPosting && name.isNotBlank() && message.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isPosting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
            } else {
                Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Post Comment")
            }
        }
    }
}
