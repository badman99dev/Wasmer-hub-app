package com.movie.app.best.ui.screens.moviedetail

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.movie.app.best.data.model.WasmerMovieDetails
import com.movie.app.best.ui.components.BlurOverlay
import com.movie.app.best.ui.components.CelebrationOverlay
import com.movie.app.best.ui.components.ErrorView
import com.movie.app.best.ui.components.SkeletonDetailPage
import com.movie.app.best.ui.components.StorylineWarningBadge
import com.movie.app.best.ui.screens.moviedetail.components.*

@Composable
fun MovieDetailScreen(
    slug: String,
    onBackClick: () -> Unit,
    onPlayClick: (playerUrl: String, streamUrl: String, title: String, youtubeId: String, movieId: String) -> Unit,
    onSeriesClick: (slug: String) -> Unit,
    onDownloadClick: (slug: String, linkId: Int) -> Unit = { _, _ -> },
    viewModel: MovieDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // ── Permission handling ────────────────────────────────
    var hasStoragePermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q)
                context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED
            else true
        )
    }
    var hasNotifPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= 33)
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED
            else true
        )
    }
    var pendingDownload by remember { mutableStateOf<Pair<String, Int>?>(null) }

    val storageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasStoragePermission = granted
        if (granted) pendingDownload?.let { viewModel.startDownload(it.first, it.second); pendingDownload = null }
    }
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
        pendingDownload?.let { viewModel.startDownload(it.first, it.second); pendingDownload = null }
    }

    fun requestDownload(slug: String, linkId: Int) {
        when {
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q && !hasStoragePermission -> {
                pendingDownload = slug to linkId
                storageLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            Build.VERSION.SDK_INT >= 33 && !hasNotifPermission -> {
                pendingDownload = slug to linkId
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            else -> viewModel.startDownload(slug, linkId)
        }
    }

    // ── Render ─────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        when {
            uiState.isLoading && uiState.movie == null -> SkeletonDetailPage()
            uiState.error != null && uiState.movie == null -> {
                ErrorView(
                    error   = uiState.error ?: "Failed to load details",
                    onRetry = viewModel::loadMovieDetails
                )
            }
            uiState.movie != null -> {
                MovieDetailContent(
                    movie              = uiState.movie!!,
                    uiState            = uiState,
                    onBackClick        = onBackClick,
                    onPlayClick        = onPlayClick,
                    onStartDownload    = { s, id -> requestDownload(s, id) },
                    onPostComment      = viewModel::postComment,
                    onPostReport       = viewModel::postReport,
                    onRequestStream    = viewModel::requestStream,
                    onResetCommentState = viewModel::resetCommentState,
                    onResetReportState  = viewModel::resetReportState,
                    onSeriesClick      = onSeriesClick,
                    onToggleBookmark   = viewModel::toggleBookmark,
                    onToggleLike       = viewModel::toggleLike,
                    onReportClick      = viewModel::openReportDrawer
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
                    movieId = uiState.movie?.id ?: 0,
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

// ─────────────────────────────────────────────────────────
//  Content (main layout)
// ─────────────────────────────────────────────────────────
@Composable
private fun MovieDetailContent(
    movie: WasmerMovieDetails,
    uiState: MovieDetailUiState,
    onBackClick: () -> Unit,
    onPlayClick: (String, String, String, String, String) -> Unit,
    onStartDownload: (String, Int) -> Unit,
    onPostComment: (String, String) -> Unit,
    onPostReport: (String, String) -> Unit,
    onRequestStream: () -> Unit,
    onResetCommentState: () -> Unit,
    onResetReportState: () -> Unit,
    onSeriesClick: (String) -> Unit,
    onToggleBookmark: () -> Unit,
    onToggleLike: () -> Unit,
    onReportClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 1. Full-bleed hero
        DetailHeroSection(
            movie       = movie,
            onBackClick = onBackClick,
            onReportClick = onReportClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
        ) {
            // 2. Play + Download + My List buttons
            DetailActionButtons(
                hasStream       = movie.hasStream || movie.playerUrl.isNotEmpty(),
                streamRequested = uiState.streamRequested,
                isInMyList      = uiState.isBookmarked,
                isLiked         = uiState.isLiked,
                onPlayClick     = {
                    val workerUrl = "https://sparkling-breeze-1ad6.badman993944.workers.dev/?id=${movie.id}"
                    onPlayClick("", workerUrl, movie.title, "", movie.id.toString())
                },
                onDownloadClick = { },
                onMyListClick   = onToggleBookmark,
                onLikeClick     = onToggleLike,
                onRequestStream = onRequestStream
            )

            // 3. Meta info chips
            MetaChipsRow(movie = movie)

            Divider(color = Color.White.copy(alpha = 0.07f), modifier = Modifier.padding(horizontal = 18.dp))

            // 4. Description
            if (movie.description.isNotEmpty()) {
                ExpandableDescription(text = movie.description)
                if (movie.contentModeration?.isStorylineSexual == true) {
                    StorylineWarningBadge(isSexual = true, modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp))
                }
            }

            Divider(color = Color.White.copy(alpha = 0.07f), modifier = Modifier.padding(horizontal = 18.dp))

            // 5. Cast & Crew (text-only, no cast images)
            CastSection(director = movie.director, cast = movie.cast)

            Divider(color = Color.White.copy(alpha = 0.07f), modifier = Modifier.padding(horizontal = 18.dp))

            // 6. Episodes row (series only)
            EpisodesRow(
                isSeries       = movie.isSeries,
                onViewEpisodes = { onSeriesClick(movie.slug) }
            )

            // 7. Trailers row
            TrailersRow(
                youtubeId      = movie.youtubeId,
                onTrailerClick = {
                    onPlayClick("", "", movie.title, movie.youtubeId, movie.id.toString())
                }
            )

            Divider(color = Color.White.copy(alpha = 0.07f), modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp))

            // 8. More Like This
            val similar = uiState.allMovies.shuffled().take(10)
            MoreLikeThisSection(
                movies       = similar,
                onMovieClick = { s, isSeries ->
                    if (isSeries) onSeriesClick(s)
                    else onPlayClick("", "", "", "", "")  // navigate via caller
                }
            )

            Divider(color = Color.White.copy(alpha = 0.07f), modifier = Modifier.padding(horizontal = 18.dp))

            // 9. Download links
            DownloadSection(
                downloadLinks     = uiState.downloadLinks,
                isDownloadLoading = uiState.isDownloadLoading,
                downloadStarted   = uiState.downloadStarted,
                downloadError     = uiState.downloadError,
                movieSlug         = movie.slug,
                onStartDownload   = onStartDownload
            )

            // 10. Screenshots
            if (uiState.screenshots.isNotEmpty()) {
                Divider(color = Color.White.copy(alpha = 0.07f), modifier = Modifier.padding(horizontal = 18.dp))
                ScreenshotsSection(
                    screenshots = uiState.screenshots,
                    shouldBlur = movie.contentModeration?.isScreenshotsSexual == true
                )
            }

            // 11. Comments
            Divider(color = Color.White.copy(alpha = 0.07f), modifier = Modifier.padding(horizontal = 18.dp))
            CommentsSection(
                comments          = uiState.comments,
                isPosting         = uiState.isCommentPosting,
                posted            = uiState.commentPosted,
                error             = uiState.commentError,
                onPost            = onPostComment,
                onReset           = onResetCommentState
            )

            // 12. Report
            Divider(color = Color.White.copy(alpha = 0.07f), modifier = Modifier.padding(horizontal = 18.dp))
            ReportSection(
                isPosting = uiState.isReportPosting,
                posted    = uiState.reportPosted,
                error     = uiState.reportError,
                onPost    = onPostReport,
                onReset   = onResetReportState
            )

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun Divider(color: Color, modifier: Modifier = Modifier) {
    HorizontalDivider(color = color, modifier = modifier)
}
