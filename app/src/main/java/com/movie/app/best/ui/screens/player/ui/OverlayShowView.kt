package com.movie.app.best.ui.screens.player.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.media3.common.Player
import com.movie.app.best.ui.screens.player.extensions.noRippleClickable
import com.movie.app.best.ui.screens.player.model.VideoContentScale

@Composable
fun BoxScope.OverlayShowView(
    player: Player,
    overlayView: OverlayViewType?,
    videoContentScale: VideoContentScale,
    onDismiss: () -> Unit = {},
    onVideoContentScaleChanged: (VideoContentScale) -> Unit = {},
) {
    Box(
        modifier = Modifier
            .matchParentSize()
            .then(
                if (overlayView != null) Modifier.noRippleClickable(onClick = onDismiss) else Modifier,
            ),
    )

    AudioTrackSelectorView(
        show = overlayView == OverlayViewType.AUDIO_SELECTOR,
        player = player,
        onDismiss = onDismiss,
    )

    SubtitleSelectorView(
        show = overlayView == OverlayViewType.SUBTITLE_SELECTOR,
        player = player,
        onDismiss = onDismiss,
    )

    PlaybackSpeedSelectorView(
        show = overlayView == OverlayViewType.PLAYBACK_SPEED,
        player = player,
    )

    VideoContentScaleSelectorView(
        show = overlayView == OverlayViewType.VIDEO_CONTENT_SCALE,
        videoContentScale = videoContentScale,
        onVideoContentScaleChanged = onVideoContentScaleChanged,
        onDismiss = onDismiss,
    )
}
