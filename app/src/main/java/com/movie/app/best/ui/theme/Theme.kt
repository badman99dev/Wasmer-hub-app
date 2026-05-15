package com.movie.app.best.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val WasmerHubColorScheme = darkColorScheme(
    primary = WasmerRed,
    onPrimary = Color.White,
    secondary = WasmerDarkRed,
    onSecondary = Color.White,
    tertiary = WasmerGreen,
    background = WasmerBlack,
    surface = WasmerCardDark,
    surfaceVariant = WasmerSurface,
    onBackground = WasmerText,
    onSurface = WasmerText,
    onSurfaceVariant = WasmerSubText,
    outline = Color(0xFF3D3D3D),
    error = WasmerRed
)

@Composable
fun MovieAppTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = WasmerHubColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}