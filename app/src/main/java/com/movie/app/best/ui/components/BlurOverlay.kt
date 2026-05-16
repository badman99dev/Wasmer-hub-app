package com.movie.app.best.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BlurredContent(
    shouldBlur: Boolean,
    modifier: Modifier = Modifier,
    blurRadius: Int = 40,
    moderationTypes: List<String> = emptyList(),
    enableDoubleTap: Boolean = false,
    content: @Composable () -> Unit
) {
    var isRevealed by remember(shouldBlur) { mutableStateOf(false) }

    if (!shouldBlur) {
        content()
        return
    }

    Box(modifier = modifier) {
        Box(modifier = Modifier.then(if (!isRevealed) Modifier.blur(blurRadius.dp) else Modifier)) {
            content()
        }

        if (!isRevealed) {
            BlurOverlayContent(
                moderationTypes = moderationTypes,
                enableDoubleTap = enableDoubleTap,
                onReveal = { isRevealed = true }
            )
        }
    }
}

@Composable
private fun BlurOverlayContent(
    moderationTypes: List<String>,
    enableDoubleTap: Boolean,
    onReveal: () -> Unit
) {
    var showWarning by remember { mutableStateOf(false) }
    var pendingTapJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.25f))
            .then(
                if (enableDoubleTap) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                pendingTapJob?.cancel()
                                pendingTapJob = scope.launch {
                                    delay(400)
                                    showWarning = true
                                }
                            },
                            onDoubleTap = {
                                pendingTapJob?.cancel()
                                onReveal()
                            }
                        )
                    }
                } else {
                    Modifier.clickable { showWarning = true }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE50914).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.VisibilityOff,
                    contentDescription = null,
                    tint = Color(0xFFE50914).copy(alpha = 0.9f),
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFE50914).copy(alpha = 0.85f))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "18+",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }

    if (showWarning) {
        BlurWarningPopup(
            moderationTypes = moderationTypes,
            showDoubleTapTip = enableDoubleTap,
            onContinue = {
                showWarning = false
                onReveal()
            },
            onBack = {
                showWarning = false
            }
        )
    }
}

@Composable
private fun BlurWarningPopup(
    moderationTypes: List<String>,
    showDoubleTapTip: Boolean,
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    val typesText = moderationTypes.distinct().joinToString(" / ")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(enabled = false) { },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(300.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1A1A1A))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE50914).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.VisibilityOff,
                    contentDescription = null,
                    tint = Color(0xFFE50914),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Content Warning",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = if (typesText.isNotEmpty()) "This content contains $typesText content. Do you still want to proceed?"
                else "This content has been flagged. Do you still want to proceed?",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFE50914))
                    .clickable { onContinue() }
                    .padding(horizontal = 32.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Continue Anyway",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable { onBack() }
                    .padding(horizontal = 32.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Back",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (showDoubleTapTip) {
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "💡 Tips: Double tap on Blurred Poster to unblur just the poster",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun StorylineWarningBadge(
    isSexual: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isSexual) return

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFFF6D00).copy(alpha = 0.85f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = "⚠ Mature Theme",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
