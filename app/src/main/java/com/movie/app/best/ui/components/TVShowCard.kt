package com.movie.app.best.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.movie.app.best.data.model.WasmerMovie

@Composable
fun TVShowCard(
    movie: WasmerMovie,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    MovieCard(
        movie = movie,
        onClick = onClick,
        modifier = modifier
    )
}
