package com.flashshare.p2p.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AccentMint,
    onPrimary = AccentMintText,
    primaryContainer = SurfaceElevated,
    onPrimaryContainer = TextTitle,
    secondary = AccentAzure,
    onSecondary = TextTitle,
    background = AppBackground,
    onBackground = TextTitle,
    surface = SurfaceElevated,
    onSurface = TextTitle,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextBody,
    error = ErrorRose,
    onError = TextTitle
)

@Composable
fun FlashShareTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = AppBackground.toArgb()
                window.navigationBarColor = BottomBarBackground.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
