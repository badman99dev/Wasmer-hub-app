package com.movie.app.best.ui.screens.player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.movie.app.best.data.debug.DebugInterceptor
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VideoPlayerScreen(
    onBackClick: () -> Unit,
    playerUrl: String,
    streamUrl: String,
    title: String,
    youtubeId: String,
    movieId: String = "",
    viewModel: VideoPlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var isControlsVisible by remember { mutableStateOf(true) }
    var isLocked         by remember { mutableStateOf(false) }
    var isPlaying        by remember { mutableStateOf(false) }
    var isBuffering      by remember { mutableStateOf(true) }
    var showStartOverlay by remember { mutableStateOf(true) }
    var useNativePlayer  by remember { mutableStateOf(true) }
    var playerError      by remember { mutableStateOf<String?>(null) }
    var isMuted          by remember { mutableStateOf(false) }
    var isRotationLocked by remember { mutableStateOf(false) }
    var currentSpeed     by remember { mutableFloatStateOf(1.0f) }
    var videoScale       by remember { mutableFloatStateOf(1.0f) }

    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs        by remember { mutableLongStateOf(0L) }
    var bufferPercent     by remember { mutableIntStateOf(0) }

    var seekDragState  by remember { mutableStateOf(SeekDragState()) }
    var swipeSeekState by remember { mutableStateOf(SwipeSeekState()) }
    var showSpeedOverlay by remember { mutableStateOf(false) }
    var gestureIndicator by remember { mutableStateOf(GestureIndicatorState()) }

    // Double tap animation state
    var doubleTapRight   by remember { mutableStateOf(false) }
    var doubleTapLeft    by remember { mutableStateOf(false) }

    var showQualityMenu  by remember { mutableStateOf(false) }
    var showSpeedMenu    by remember { mutableStateOf(false) }
    var showAudioMenu    by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showDebugPanel   by remember { mutableStateOf(false) }

    val debugLogs = remember { mutableStateListOf<DebugLogEntry>() }
    fun addLog(type: String, message: String) {
        val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
        debugLogs.add(DebugLogEntry(time, type, message))
        if (debugLogs.size > 300) debugLogs.removeAt(0)
    }

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume    = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
    var currentVolume by remember {
        mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat())
    }
    var currentBrightness by remember {
        mutableFloatStateOf(
            (activity?.window?.attributes?.screenBrightness ?: -1f).let { if (it >= 0f) it else 0.5f }
        )
    }

    val effectiveUrl = remember(playerUrl, streamUrl) {
        when {
            playerUrl.isNotEmpty() -> playerUrl
            streamUrl.isNotEmpty() -> streamUrl
            else -> ""
        }.let { url ->
            if (url.startsWith("/")) "file://$url" else url
        }.let { url ->
            if (url.startsWith("file://")) {
                try { java.net.URLDecoder.decode(url, "UTF-8") } catch (_: Exception) { url }
            } else url
        }
    }

    val isLocalFile = effectiveUrl.startsWith("file://") || effectiveUrl.startsWith("content://")
    val isHls = !isLocalFile && (
        effectiveUrl.contains(".m3u8", ignoreCase = true) ||
        effectiveUrl.contains("sparkling-breeze", ignoreCase = true) ||
        effectiveUrl.contains("/?id=", ignoreCase = true)
    )

    val trackSelector = remember { DefaultTrackSelector(context) }

    val exoPlayer = remember {
        val debugInterceptor = Interceptor { chain ->
            val request = chain.request()
            addLog("REQ", "${request.method} ${request.url}")
            request.headers.forEach { (n, v) -> addLog("HDR", "-> $n: $v") }
            val response: Response
            try {
                response = chain.proceed(request)
                addLog("RESP", "${response.code} ${response.message}")
                response.headers.forEach { (n, v) -> addLog("HDR", "<- $n: $v") }
                val body = response.peekBody(2048).string()
                addLog("BODY", if (body.length > 400) body.take(400) + "..." else body)
            } catch (e: Exception) {
                addLog("ERR", "Network: ${e.javaClass.simpleName} ${e.message}")
                throw e
            }
            response
        }

        val okHttpClient = OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor(debugInterceptor)
            .addInterceptor(DebugInterceptor())
            .build()

        val okHttpFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/125.0.0.0 Mobile Safari/537.36")
            .setDefaultRequestProperties(mapOf(
                "Referer" to "https://wasmer-jhns970ko-badals-projects-03fab3df.vercel.app/",
                "Accept"  to "*/*"
            ))

        val dataSourceFactory = DefaultDataSource.Factory(context, okHttpFactory)
        val mediaSourceFactory = if (isHls)
            HlsMediaSource.Factory(dataSourceFactory)
        else
            DefaultMediaSourceFactory(dataSourceFactory)

        ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                playWhenReady = false
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        isPlaying = playing
                        if (playing) { showStartOverlay = false; playerError = null }
                        addLog("PLAYER", "isPlaying=$playing")
                    }
                    override fun onPlaybackStateChanged(state: Int) {
                        isBuffering = state == Player.STATE_BUFFERING
                        addLog("PLAYER", "state=${when(state){Player.STATE_IDLE->"IDLE";Player.STATE_BUFFERING->"BUFFERING";Player.STATE_READY->"READY";Player.STATE_ENDED->"ENDED";else->"UNKNOWN"}}")
                    }
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        playerError = error.message ?: "Unknown error"
                        addLog("ERROR", "code=${error.errorCodeName} msg=${error.message}")
                        error.cause?.let { addLog("ERROR", "cause=${it.javaClass.simpleName}: ${it.message}") }
                    }
                    override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
                        for (group in tracks.groups) {
                            val typeName = when(group.type){C.TRACK_TYPE_VIDEO->"VIDEO";C.TRACK_TYPE_AUDIO->"AUDIO";C.TRACK_TYPE_TEXT->"TEXT";else->"TYPE_${group.type}"}
                            for (i in 0 until group.length) {
                                val fmt = group.getTrackFormat(i)
                                addLog("TRACK", "[$typeName] ${fmt.height}p lang=${fmt.language} selected=${group.isTrackSelected(i)}")
                            }
                        }
                    }
                })
            }
    }

    LaunchedEffect(effectiveUrl) {
        if (effectiveUrl.isNotEmpty()) {
            addLog("EXO", "load uri=$effectiveUrl isHls=$isHls")
            exoPlayer.setMediaItem(MediaItem.fromUri(effectiveUrl))
            exoPlayer.prepare()
        }
    }

    LaunchedEffect(Unit) {
        val default = viewModel.getDefaultPlayer()
        useNativePlayer = default != 2
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            currentPositionMs = exoPlayer.currentPosition
            durationMs        = exoPlayer.duration.coerceAtLeast(0L)
            bufferPercent     = exoPlayer.bufferedPercentage
            delay(250)
        }
    }

    // Auto-reset double tap animation after 600ms
    LaunchedEffect(doubleTapRight) {
        if (doubleTapRight) { delay(600); doubleTapRight = false }
    }
    LaunchedEffect(doubleTapLeft) {
        if (doubleTapLeft) { delay(600); doubleTapLeft = false }
    }

    LaunchedEffect(isControlsVisible, isLocked, showQualityMenu, showSpeedMenu, showAudioMenu, showOverflowMenu, showDebugPanel) {
        if (isControlsVisible && !isLocked && !showQualityMenu && !showSpeedMenu && !showAudioMenu && !showOverflowMenu && !showDebugPanel) {
            delay(4000)
            isControlsVisible = false
        }
    }

    LaunchedEffect(Unit) {
        if (!isRotationLocked)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        hideSystemUI(activity)
    }

    // ── Lifecycle: pause on background, resume on foreground ──────────────────
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    // App went to background — stop instantly, no background audio
                    exoPlayer.pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    // App came back — do nothing, user decides when to play
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            showSystemUI(activity)
        }
    }

    BackHandler {
        if (isLandscape && !isRotationLocked)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        else { showSystemUI(activity); onBackClick() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .playerGestures(
                context               = context,
                activity              = activity,
                exoPlayer             = exoPlayer,
                isLocked              = isLocked,
                isSeekbarDragging     = seekDragState.isDragging,
                durationMs            = durationMs,
                audioManager          = audioManager,
                maxVolume             = maxVolume,
                currentVolume         = currentVolume,
                currentBrightness     = currentBrightness,
                swipeSeekState        = swipeSeekState,
                onTap = {
                    when {
                        showQualityMenu || showSpeedMenu || showAudioMenu || showOverflowMenu -> {
                            showQualityMenu = false; showSpeedMenu = false
                            showAudioMenu = false; showOverflowMenu = false
                        }
                        else -> isControlsVisible = !isControlsVisible
                    }
                },
                onDoubleTap = { isRight ->
                    if (isRight) {
                        exoPlayer.seekTo(minOf(exoPlayer.currentPosition + 10_000, durationMs))
                        doubleTapRight = true
                        doubleTapLeft  = false
                    } else {
                        exoPlayer.seekTo(maxOf(0L, exoPlayer.currentPosition - 10_000))
                        doubleTapLeft  = true
                        doubleTapRight = false
                    }
                },
                onLongPressStart = {
                    showSpeedOverlay = true
                    exoPlayer.setPlaybackSpeed(2.0f)
                    currentSpeed = 2.0f
                },
                onLongPressEnd = {
                    showSpeedOverlay = false
                    exoPlayer.setPlaybackSpeed(1.0f)
                    currentSpeed = 1.0f
                },
                onVolumeChange    = { currentVolume = it },
                onBrightnessChange = { currentBrightness = it },
                onShowVolumeIndicator     = { gestureIndicator = gestureIndicator.copy(showVolume = it, volumePercent = (currentVolume / maxVolume * 100).toInt()) },
                onShowBrightnessIndicator = { gestureIndicator = gestureIndicator.copy(showBrightness = it, brightnessPercent = (currentBrightness * 100).toInt()) },
                onSwipeSeekUpdate = { swipeSeekState = it },
                onScaleChange     = { videoScale = it },
                currentScale      = videoScale
            )
    ) {
        // Video surface
        if (useNativePlayer && effectiveUrl.isNotEmpty() && playerError == null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        layoutParams = android.widget.FrameLayout.LayoutParams(
                            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                            android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { scaleX = videoScale; scaleY = videoScale }
            )
        } else if (!useNativePlayer && movieId.isNotEmpty()) {
            AndroidView(
                factory = { ctx ->
                    android.webkit.WebView(ctx).apply {
                        layoutParams = android.widget.FrameLayout.LayoutParams(
                            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                            android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                        )
                        settings.apply {
                            javaScriptEnabled = true; domStorageEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        }
                        webViewClient = android.webkit.WebViewClient()
                        webChromeClient = android.webkit.WebChromeClient()
                        loadUrl("https://player-swart-two.vercel.app/?id=$movieId")
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else if (playerError == null && effectiveUrl.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("No playback URL available", color = Color.White, fontSize = 16.sp)
            }
        }

        // Buffering
        if (useNativePlayer && isBuffering && !showStartOverlay && effectiveUrl.isNotEmpty() && playerError == null)
            BufferingIndicator()

        // Error
        if (useNativePlayer && playerError != null)
            ErrorOverlay(
                error = playerError!!,
                onRetry = { playerError = null; exoPlayer.prepare() },
                onSwitchPlayer = { useNativePlayer = false; playerError = null }
            )

        // Start overlay — premium feel
        if (useNativePlayer && showStartOverlay && effectiveUrl.isNotEmpty()) {
            // Clean title: URL decode + replace + with space
            val cleanTitle = try {
                java.net.URLDecoder.decode(title, "UTF-8")
            } catch (_: Exception) { title }
                .replace("+", " ").replace("  ", " ").trim()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .clickable { showStartOverlay = false; exoPlayer.playWhenReady = true },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 32.dp)
                ) {
                    // Play button circle
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .background(
                                Color(0xFFE50914).copy(alpha = 0.92f),
                                androidx.compose.foundation.shape.CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(52.dp)
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    // Clean title
                    Text(
                        text = cleanTitle,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Tap to play",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Gesture indicators
        if (gestureIndicator.showVolume)
            VolumeIndicator(gestureIndicator.volumePercent)
        if (gestureIndicator.showBrightness)
            BrightnessIndicator(gestureIndicator.brightnessPercent)
        SwipeSeekOverlay(swipeSeekState, currentPositionMs, durationMs)

        // Double tap seek animations
        DoubleTapSeekOverlay(isRight = true,  visible = doubleTapRight)
        DoubleTapSeekOverlay(isRight = false, visible = doubleTapLeft)

        if (showSpeedOverlay)
            SpeedOverlay(2.0f)

        // Zoom reset
        if (videoScale != 1.0f) {
            Box(Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 100.dp)) {
                Text(
                    text = "%.1fx  x".format(videoScale),
                    color = Color.White, fontSize = 11.sp,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        .clickable { videoScale = 1.0f }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Lock icon
        if (isLocked) {
            Box(Modifier.fillMaxSize().padding(16.dp), Alignment.TopEnd) {
                IconButton(
                    onClick = { isLocked = false; isControlsVisible = true },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(50))
                ) {
                    Icon(Icons.Default.LockOpen, null, tint = Color.White)
                }
            }
        }

        // Controls
        if (!isLocked && !showStartOverlay) {
            AnimatedVisibility(visible = isControlsVisible, enter = fadeIn(), exit = fadeOut()) {
                Box(Modifier.fillMaxSize()) {
                    PlayerTopBar(
                        title = title, isMuted = isMuted, isRotationLocked = isRotationLocked,
                        isLandscape = isLandscape, activity = activity,
                        onBack = {
                            if (isLandscape && !isRotationLocked)
                                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            else { showSystemUI(activity); onBackClick() }
                        },
                        onMuteToggle = {
                            isMuted = !isMuted
                            exoPlayer.volume = if (isMuted) 0f else 1f
                        },
                        onRotationLockToggle = {
                            isRotationLocked = !isRotationLocked
                            // Read orientation real-time from activity — avoids stale closure
                            val currentlyLandscape = activity?.resources?.configuration?.orientation ==
                                android.content.res.Configuration.ORIENTATION_LANDSCAPE
                            activity?.requestedOrientation = if (isRotationLocked)
                                if (currentlyLandscape) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            else ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
                        },
                        onOverflowClick = { showOverflowMenu = true },
                        modifier = Modifier.align(Alignment.TopStart)
                    )

                    if (useNativePlayer) {
                        PlayerCenterControls(
                            isPlaying = isPlaying, isBuffering = isBuffering,
                            exoPlayer = exoPlayer,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    PlayerBottomBar(
                        exoPlayer = exoPlayer, currentPositionMs = currentPositionMs,
                        durationMs = durationMs, bufferPercent = bufferPercent,
                        seekDragState = seekDragState, currentSpeed = currentSpeed,
                        isLandscape = isLandscape, activity = activity,
                        onSeekStart  = { seekDragState = SeekDragState(isDragging = true, dragFraction = it) },
                        onSeekChange = { seekDragState = seekDragState.copy(dragFraction = it) },
                        onSeekEnd    = { exoPlayer.seekTo((it * durationMs).toLong()); seekDragState = SeekDragState() },
                        onQualityClick = { showQualityMenu = true },
                        onSpeedClick   = { showSpeedMenu   = true },
                        onAudioClick   = { showAudioMenu   = true },
                        modifier = Modifier.align(Alignment.BottomStart)
                    )

                    // Lock + PiP + P1/P2 small buttons above seekbar
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 16.dp, bottom = 76.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SmallIconButton(icon = Icons.Default.Lock, onClick = { isLocked = true })
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && useNativePlayer) {
                            SmallIconButton(icon = Icons.Default.PictureInPicture, onClick = { enterPipMode(activity) })
                        }
                        SmallTextButton(
                            text = if (useNativePlayer) "P2" else "P1",
                            onClick = { useNativePlayer = !useNativePlayer; if (useNativePlayer) showStartOverlay = true }
                        )
                    }
                }
            }
        }

        // Menus (always on top)
        if (showQualityMenu)
            QualityMenu(exoPlayer, onDismiss = { showQualityMenu = false })
        if (showSpeedMenu)
            SpeedMenu(currentSpeed, exoPlayer, onSpeedChanged = { currentSpeed = it }, onDismiss = { showSpeedMenu = false })
        if (showAudioMenu)
            AudioMenu(exoPlayer, onDismiss = { showAudioMenu = false })
        if (showOverflowMenu)
            OverflowMenu(
                onViewLogs = { showDebugPanel = true },
                onShare = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, "Watch $title on Wasmer Hub")
                    }
                    context.startActivity(android.content.Intent.createChooser(intent, "Share"))
                },
                onDismiss = { showOverflowMenu = false }
            )
        if (showDebugPanel)
            PlayerDebugPanel(logs = debugLogs, onDismiss = { showDebugPanel = false })
    }
}

@Composable
private fun SmallIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(36.dp)
            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(50))
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SmallTextButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(50))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color(0xFFE50914), fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
