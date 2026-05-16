package com.movie.app.best.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition

@Composable
fun CelebrationOverlay(
    play: Boolean,
    onFinished: () -> Unit
) {
    if (!play) return

    var hasPlayed by remember { mutableStateOf(false) }

    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(com.movie.app.best.R.raw.celebration)
    )

    if (!hasPlayed) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .zIndex(999f)
        ) {
            LottieAnimation(
                composition = composition,
                iterations = 1,
                isPlaying = true,
                restartOnPlay = true
            )
        }

        LaunchedEffect(composition) {
            if (composition != null) {
                val durationMs = (composition!!.durationMs / composition!!.speed).toLong()
                kotlinx.coroutines.delay(durationMs.coerceAtMost(5000))
                hasPlayed = true
                onFinished()
            }
        }
    }
}
