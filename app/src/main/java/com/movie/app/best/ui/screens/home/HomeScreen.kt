package com.movie.app.best.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.movie.app.best.ui.components.AppHeader
import com.movie.app.best.ui.components.SkeletonHeroSlide
import com.movie.app.best.ui.components.SkeletonMovieGrid
import com.movie.app.best.ui.screens.home.components.*

@Composable
fun HomeScreen(
    onContentClick: (String, Boolean) -> Unit,
    navController: NavController,
    onSearchClick: () -> Unit = {},
    onDownloadClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    val allMovies = uiState.allTabMovies
    val trending    = uiState.trendingMovies
    val myFeed      = uiState.myFeedMovies
    val newReleases = remember(allMovies) { allMovies.sortedByDescending { it.id }.take(12) }
    val series      = remember(allMovies) { allMovies.filter { it.isSeries }.take(12) }

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisible >= totalItems - 6 && uiState.canLoadMoreAllTab && !uiState.isAllTabLoadingMore
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMoreAllTab()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            if (uiState.isSliderLoading && uiState.sliderMovies.isEmpty()) {
                item { SkeletonHeroSlide() }
            } else {
                item {
                    HeroCarousel(
                        movies      = uiState.sliderMovies,
                        isLoading   = uiState.isSliderLoading,
                        onPlayClick = onContentClick,
                        onInfoClick = onContentClick
                    )
                }
            }

            if (uiState.isAllTabLoading && allMovies.isEmpty()) {
                item { SkeletonMovieGrid(rows = 3) }
            } else {
                if (trending.isNotEmpty()) {
                    item {
                        MovieRowSection(
                            title        = "Trending Now 🔥",
                            movies       = trending,
                            cardSize     = CardSize.NORMAL,
                            onMovieClick = onContentClick,
                            onSeeAllClick = { navController.navigate(com.movie.app.best.ui.navigation.Screen.Trending.route) }
                        )
                    }
                }

                if (newReleases.isNotEmpty()) {
                    item {
                        MovieRowSection(
                            title        = "New Releases",
                            movies       = newReleases,
                            cardSize     = CardSize.LARGE,
                            onMovieClick = onContentClick
                        )
                    }
                }

                if (series.isNotEmpty()) {
                    item {
                        MovieRowSection(
                            title        = "Binge-Worthy Series",
                            movies       = series,
                            cardSize     = CardSize.NORMAL,
                            onMovieClick = onContentClick
                        )
                    }
                }

                if (myFeed.isNotEmpty()) {
                    item {
                        MovieRowSection(
                            title        = "Because You Watched Similar",
                            movies       = myFeed,
                            cardSize     = CardSize.NORMAL,
                            onMovieClick = onContentClick,
                            onSeeAllClick = { navController.navigate(com.movie.app.best.ui.navigation.Screen.MyFeed.route) }
                        )
                    }
                }

                if (allMovies.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title      = "More to Explore",
                            showSeeAll = false
                        )
                    }
                    movieGridItems(
                        movies = allMovies,
                        onMovieClick = onContentClick
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }

            if (uiState.isAllTabLoadingMore) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                            modifier = Modifier.height(32.dp)
                        )
                    }
                }
            }
        }

        AppHeader(
            onMenuClick         = onMenuClick,
            onSearchClick       = onSearchClick,
            onDownloadClick     = onDownloadClick,
            onNotificationClick = { navController.navigate(com.movie.app.best.ui.navigation.Screen.Notifications.route) },
            hasNotification     = uiState.notification?.isActive == true,
            modifier            = Modifier.align(Alignment.TopCenter)
        )
    }
}
