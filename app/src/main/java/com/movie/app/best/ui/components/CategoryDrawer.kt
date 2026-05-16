package com.movie.app.best.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.SubcomposeAsyncImage
import com.movie.app.best.data.model.WasmerCategory
import com.movie.app.best.ui.screens.categories.CategoriesViewModel
import com.movie.app.best.ui.theme.WasmerBg
import com.movie.app.best.ui.theme.WasmerCardDark
import com.movie.app.best.ui.theme.WasmerDivider
import com.movie.app.best.ui.theme.WasmerRed
import com.movie.app.best.ui.theme.WasmerSubText

@Composable
fun CategoryDrawerContent(
    onCategoryClick: (slug: String, name: String) -> Unit,
    onAllCategoriesClick: () -> Unit = {},
    viewModel: CategoriesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(WasmerBg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(WasmerRed.copy(alpha = 0.12f))
                .padding(horizontal = 20.dp, vertical = 28.dp)
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = WasmerRed, fontWeight = FontWeight.Black)) { append("W") }
                    withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.ExtraBold)) { append("ASMER ") }
                    withStyle(SpanStyle(color = WasmerRed, fontWeight = FontWeight.Black)) { append("HUB") }
                },
                fontSize = 22.sp,
                letterSpacing = (-0.5).sp
            )
        }

        HorizontalDivider(color = WasmerDivider, thickness = 1.dp)

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onAllCategoriesClick() }
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Category,
                contentDescription = null,
                tint = WasmerRed,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = "All Categories",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        HorizontalDivider(color = WasmerDivider, thickness = 0.5.dp)

        Spacer(modifier = Modifier.height(4.dp))

        if (uiState.isLoading && uiState.categories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = WasmerRed, modifier = Modifier.size(28.dp))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(uiState.categories, key = { it.slug }) { category ->
                    DrawerCategoryItem(
                        category = category,
                        onClick = { onCategoryClick(category.slug, category.categoryName) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerCategoryItem(
    category: WasmerCategory,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (category.bannerUrl.isNotBlank()) {
            SubcomposeAsyncImage(
                model = category.bannerUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp)),
                loading = {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier
                        .size(40.dp)
                        .background(WasmerCardDark)) {
                        Icon(
                            imageVector = if (category.categoryName.contains("series", ignoreCase = true) ||
                                category.categoryName.contains("tv", ignoreCase = true))
                                Icons.Default.Tv else Icons.Default.Movie,
                            contentDescription = null,
                            tint = WasmerSubText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                error = {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier
                        .size(40.dp)
                        .background(WasmerCardDark)) {
                        Icon(
                            imageVector = Icons.Default.BrokenImage,
                            contentDescription = null,
                            tint = WasmerSubText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            )
        } else {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(WasmerCardDark),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (category.categoryName.contains("series", ignoreCase = true) ||
                        category.categoryName.contains("tv", ignoreCase = true))
                        Icons.Default.Tv else Icons.Default.Movie,
                    contentDescription = null,
                    tint = WasmerSubText,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category.categoryName,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                text = "${category.count} items",
                color = WasmerSubText,
                fontSize = 11.sp
            )
        }
    }
}
