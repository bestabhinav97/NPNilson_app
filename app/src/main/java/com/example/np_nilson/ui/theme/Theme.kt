package com.example.np_nilson.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val NpLightColorScheme = lightColorScheme(
    primary = NpBluePrimary,
    onPrimary = NpSurfaceWhite,
    primaryContainer = NpBlueLight,
    onPrimaryContainer = NpSurfaceWhite,
    secondary = NpBlueDark,
    onSecondary = NpSurfaceWhite,
    background = NpBackgroundLight,
    onBackground = NpTextPrimary,
    surface = NpSurfaceWhite,
    onSurface = NpTextPrimary,
    error = NpErrorRed,
    onError = NpSurfaceWhite
)

@Composable
fun NP_NilsonTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = NpLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = NpBluePrimary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
