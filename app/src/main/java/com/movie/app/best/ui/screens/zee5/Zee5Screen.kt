package com.movie.app.best.ui.screens.zee5

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
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
import com.movie.app.best.data.model.Zee5Bucket
import com.movie.app.best.data.model.Zee5Item
import com.movie.app.best.ui.navigation.Screen
import com.movie.app.best.ui.theme.WasmerRed
import com.movie.app.best.ui.theme.WasmerBlack
import com.movie.app.best.ui.theme.WasmerCardDark
import com.movie.app.best.ui.theme.WasmerSubText
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@Composable
fun Zee5Screen(
    navController: NavController,
    viewModel: Zee5ViewModel = hiltViewModel(),
    onSearchClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val listState = rememberLazyListState()
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    // Infinite scroll detection
    val shouldLoadMore = remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItem >= totalItems - 3
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            viewModel.loadMore()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WasmerBlack)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with ZEE5 branding
            Zee5Header(
                onSearchClick = { isSearching = true },
                onBackClick = { isSearching = false }
            )

            // Search bar (when searching)
            if (isSearching) {
                Zee5SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = {
                        if (searchQuery.isNotBlank()) {
                            viewModel.search(searchQuery)
                            isSearching = false
                        }
                    },
                    onClose = { isSearching = false }
                )
            }

            // Tab bar
            Zee5TabBar(
                currentTab = currentTab,
                onTabClick = { viewModel.switchTab(it) }
            )

            // Content
            when (val state = uiState) {
                is Zee5UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = WasmerRed)
                    }
                }
                is Zee5UiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.message,
                                color = WasmerSubText,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.loadTab(currentTab) },
                                colors = ButtonDefaults.buttonColors(containerColor = WasmerRed)
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
                is Zee5UiState.Success -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        // Hero carousel for ALL tab (first bucket items)
                        if (currentTab == Zee5Tab.ALL && state.buckets.isNotEmpty()) {
                            val heroItems = state.buckets.firstOrNull()?.items?.take(5) ?: emptyList()
                            if (heroItems.isNotEmpty()) {
                                item {
                                    Zee5HeroCarousel(
                                        items = heroItems,
                                        onItemClick = { item ->
                                            item.id?.let { id ->
                                                navController.navigate("zee5_detail/${id}")
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // Content rails
                        items(state.buckets) { bucket ->
                            Zee5Rail(
                                bucket = bucket,
                                onItemClick = { item ->
                                    item.id?.let { id ->
                                        navController.navigate("zee5_detail/${id}")
                                    }
                                }
                            )
                        }

                        // Load more indicator at bottom
                        item {
                            if (currentTab == Zee5Tab.FREE && state.isLoadMore) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = WasmerRed,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Zee5Header(
    onSearchClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "ZEE5",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = WasmerRed,
            letterSpacing = 2.sp
        )

        IconButton(onClick = onSearchClick) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Search",
                tint = Color.White
            )
        }
    }
}

@Composable
fun Zee5SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClose: () -> Unit
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(WasmerCardDark, RoundedCornerShape(8.dp)),
        placeholder = { Text("Search ZEE5...", color = WasmerSubText) },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = WasmerCardDark,
            unfocusedContainerColor = WasmerCardDark,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedIndicatorColor = WasmerRed,
            unfocusedIndicatorColor = Color.Transparent
        ),
        trailingIcon = {
            IconButton(onClick = onSearch) {
                Icon(Icons.Filled.Search, "Search", tint = WasmerRed)
            }
        }
    )
}

@Composable
fun Zee5TabBar(
    currentTab: Zee5Tab,
    onTabClick: (Zee5Tab) -> Unit
) {
    val tabs = listOf(
        Zee5Tab.ALL to "All",
        Zee5Tab.TV_SHOWS to "TV Shows",
        Zee5Tab.MOVIES to "Movies",
        Zee5Tab.FREE to "FREE"
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tabs) { (tab, label) ->
            val isSelected = currentTab == tab
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
                    ) { onTabClick(tab) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = label,
                    color = if (isSelected) Color.White else WasmerSubText,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun Zee5HeroCarousel(
    items: List<Zee5Item>,
    onItemClick: (Zee5Item) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { items.size })

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(5000)
            if (items.size > 1) {
                val nextPage = (pagerState.currentPage + 1) % items.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) { page ->
        val item = items[page]
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onItemClick(item) }
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.effectiveLandscapeUrl ?: item.effectiveImageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = item.title,
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
                                Color.Transparent,
                                WasmerBlack.copy(alpha = 0.7f),
                                WasmerBlack.copy(alpha = 0.95f)
                            )
                        )
                    )
            )

            // Content info at bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = item.title ?: "",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = WasmerRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Watch Now",
                        color = WasmerRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun Zee5Rail(
    bucket: Zee5Bucket,
    onItemClick: (Zee5Item) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        // Rail title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = bucket.title ?: "",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Horizontal rail
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(bucket.items ?: emptyList()) { item ->
                Zee5Card(item = item, onClick = { onItemClick(item) })
            }
        }
    }
}

@Composable
fun Zee5Card(
    item: Zee5Item,
    onClick: () -> Unit
) {
    val isMovie = item.isMovie
    val cardWidth = if (isMovie) 140.dp else 120.dp
    val cardHeight = if (isMovie) 210.dp else 180.dp
    val aspectRatio = if (isMovie) 2f / 3f else 2f / 3f

    Column(
        modifier = Modifier
            .width(cardWidth)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .width(cardWidth)
                .height(cardHeight)
                .clip(RoundedCornerShape(8.dp))
                .background(WasmerCardDark)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(
                        if (isMovie) item.effectiveLandscapeUrl ?: item.effectiveImageUrl
                        else item.effectiveImageUrl ?: item.effectiveLandscapeUrl
                    )
                    .crossfade(true)
                    .build(),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // FREE badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(WasmerRed)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "FREE",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Duration badge for movies
            if (isMovie && item.duration != null && item.duration > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${item.duration}m",
                        color = Color.White,
                        fontSize = 9.sp
                    )
                }
            }

            // Play overlay on hover/press
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.0f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White.copy(alpha = 0.0f),
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = item.title ?: "",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(cardWidth)
        )

        // Genre/Language info
        val genreText = item.genres?.firstOrNull()?.value
            ?: item.genre?.firstOrNull()
            ?: item.languages?.firstOrNull()?.uppercase()
        if (genreText != null) {
            Text(
                text = genreText,
                color = WasmerSubText,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(cardWidth)
            )
        }
    }
}
