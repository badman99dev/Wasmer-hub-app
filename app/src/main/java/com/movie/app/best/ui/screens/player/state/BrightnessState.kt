package com.movie.app.best.ui.screens.player.state

import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.DisposableEffectResult
import androidx.compose.runtime.DisposableEffectScope
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.util.Consumer
import com.movie.app.best.ui.screens.player.extensions.brightnessPercentage
import com.movie.app.best.ui.screens.player.extensions.currentBrightness

@Composable
fun rememberBrightnessState(): BrightnessState {
    val activity = LocalActivity.current ?: return remember { BrightnessState(null) }
    val brightnessState = remember { BrightnessState(activity) }
    DisposableEffect(activity) { brightnessState.handleListeners(this) }
    return brightnessState
}

@Stable
class BrightnessState(private val activity: android.app.Activity?) {
    val maxBrightness: Float = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL

    var currentBrightness: Float by mutableFloatStateOf(activity?.currentBrightness ?: 0.5f)
        private set

    var brightnessPercentage: Int by mutableIntStateOf(activity?.brightnessPercentage ?: 50)
        private set

    fun updateBrightnessPercentage(percentage: Int) {
        setBrightness(brightness = percentage.coerceIn(0, 100) * maxBrightness / 100)
    }

    fun setBrightness(brightness: Float) {
        val a = activity ?: return
        val windowAttributes = a.window.attributes
        windowAttributes.screenBrightness = brightness.coerceIn(0f, maxBrightness)
        a.window.attributes = windowAttributes
    }

    fun handleListeners(disposableEffectScope: DisposableEffectScope): DisposableEffectResult =
        with(disposableEffectScope) {
            val a = activity ?: return onDispose { }
            val listener: Consumer<WindowManager.LayoutParams?> = Consumer {
                currentBrightness = a.currentBrightness
                brightnessPercentage = a.brightnessPercentage
            }
            a.addOnWindowAttributesChangedListener(listener)
            onDispose { a.removeOnWindowAttributesChangedListener(listener) }
        }
}
