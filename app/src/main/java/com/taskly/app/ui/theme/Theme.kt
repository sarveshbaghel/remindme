package com.taskly.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = Accent.copy(alpha = 0.08f),
    onPrimaryContainer = Accent,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnBackground,
    surfaceVariant = LightBackground,
    onSurfaceVariant = LightOnSurfaceVariant,
    error = Error,
    onError = Color.White,
    outline = Color(0xFFE0E0E0),
)

private val DarkColors = darkColorScheme(
    primary = AccentDark,
    onPrimary = Color.White,
    primaryContainer = AccentDark.copy(alpha = 0.15f),
    onPrimaryContainer = AccentDark,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnBackground,
    surfaceVariant = Color(0xFF252525),
    onSurfaceVariant = DarkOnSurfaceVariant,
    error = Error,
    onError = Color.White,
    outline = Color(0xFF333333),
)

@Composable
fun TasklyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = TasklyTypography,
        content = content,
    )
}

