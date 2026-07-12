package com.purehub.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD7F4EF),
    onPrimaryContainer = Color(0xFF083B37),
    secondary = Color(0xFF3B82F6),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDFECFF),
    onSecondaryContainer = Color(0xFF0E2A57),
    tertiary = Color(0xFF7C3AED),
    background = Color(0xFFF4F7FB),
    surface = Color(0xFFFCFDFF),
    surfaceContainerLow = Color(0xFFF0F4FA),
    outline = Color(0xFF7A8699),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5EEAD4),
    onPrimary = Color(0xFF032B28),
    primaryContainer = Color(0xFF0D4A46),
    onPrimaryContainer = Color(0xFFD7F4EF),
    secondary = Color(0xFF93C5FD),
    onSecondary = Color(0xFF0F2852),
    secondaryContainer = Color(0xFF183A6C),
    onSecondaryContainer = Color(0xFFDFECFF),
    tertiary = Color(0xFFC4B5FD),
    background = Color(0xFF0A1120),
    surface = Color(0xFF0F172A),
    surfaceContainerLow = Color(0xFF162033),
    outline = Color(0xFF92A0B5),
)

@Composable
@Suppress("DEPRECATION")
fun PureHubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors
        else -> LightColors
    }

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
