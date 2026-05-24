package com.movie.app.best.ui.screens.player.extensions

import android.os.Build
import androidx.media3.common.C
import androidx.media3.common.TrackGroup
import java.util.Locale

fun TrackGroup.getName(trackType: @C.TrackType Int, index: Int): String {
    val format = this.getFormat(0)
    val language = format.language
    val label = format.label
    return buildString {
        if (label != null) {
            append(label)
        }
        if (isEmpty()) {
            if (trackType == C.TRACK_TYPE_TEXT) {
                append("Subtitle Track #${index + 1}")
            } else {
                append("Audio Track #${index + 1}")
            }
        }
        @Suppress("DEPRECATION")
        if (language != null && language != "und") {
            append(" - ")
            append(Locale(language).displayLanguage)
        }
    }
}
