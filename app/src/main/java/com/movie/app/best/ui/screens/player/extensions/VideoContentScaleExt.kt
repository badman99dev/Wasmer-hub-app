package com.movie.app.best.ui.screens.player.extensions

import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.FixedScale
import com.movie.app.best.ui.screens.player.model.VideoContentScale

fun VideoContentScale.nameRes(): String = when (this) {
    VideoContentScale.BEST_FIT -> "Best Fit"
    VideoContentScale.STRETCH -> "Stretch"
    VideoContentScale.CROP -> "Crop"
    VideoContentScale.HUNDRED_PERCENT -> "100%"
}

fun VideoContentScale.toContentScale(): ContentScale = when (this) {
    VideoContentScale.BEST_FIT -> ContentScale.Fit
    VideoContentScale.STRETCH -> ContentScale.FillBounds
    VideoContentScale.CROP -> ContentScale.Crop
    VideoContentScale.HUNDRED_PERCENT -> FixedScale(1.0f)
}

fun VideoContentScale.toResizeMode(): Int = when (this) {
    VideoContentScale.BEST_FIT -> 0
    VideoContentScale.STRETCH -> 2
    VideoContentScale.CROP -> 1
    VideoContentScale.HUNDRED_PERCENT -> 0
}
