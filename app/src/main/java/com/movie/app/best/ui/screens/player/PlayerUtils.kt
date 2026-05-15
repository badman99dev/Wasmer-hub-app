package com.movie.app.best.ui.screens.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.os.Build
import android.util.Rational
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

fun formatMs(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0)
        "%d:%02d:%02d".format(hours, minutes, seconds)
    else
        "%d:%02d".format(minutes, seconds)
}

fun formatSeekDelta(ms: Long): String {
    val sign = if (ms >= 0) "+" else "-"
    val abs = kotlin.math.abs(ms)
    val totalSeconds = abs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0)
        "$sign${minutes}m${seconds}s"
    else
        "$sign${seconds}s"
}

fun hideSystemUI(activity: Activity?) {
    activity ?: return
    val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    controller.hide(WindowInsetsCompat.Type.systemBars())
}

fun showSystemUI(activity: Activity?) {
    activity ?: return
    val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
    controller.show(WindowInsetsCompat.Type.systemBars())
}

fun enterPipMode(activity: Activity?) {
    activity ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .build()
        activity.enterPictureInPictureMode(params)
    }
}
