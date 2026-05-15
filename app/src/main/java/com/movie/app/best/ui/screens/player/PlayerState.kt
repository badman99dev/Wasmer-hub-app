package com.movie.app.best.ui.screens.player

// Debug log entry used in debug panel
data class DebugLogEntry(
    val timestamp: String,
    val type: String,
    val message: String
)

// Seekbar drag state - holds info while user is dragging seekbar
data class SeekDragState(
    val isDragging: Boolean = false,
    val dragFraction: Float = 0f   // 0.0 to 1.0
)

// Horizontal swipe seek overlay state
data class SwipeSeekState(
    val isActive: Boolean = false,
    val deltaMs: Long = 0L,          // positive = forward, negative = backward
    val targetPositionMs: Long = 0L
)

// Speed overlay - shown during long press
data class SpeedOverlayState(
    val isVisible: Boolean = false,
    val speed: Float = 2.0f
)

// Volume / Brightness indicator state
data class GestureIndicatorState(
    val showVolume: Boolean = false,
    val volumePercent: Int = 0,
    val showBrightness: Boolean = false,
    val brightnessPercent: Int = 0
)
