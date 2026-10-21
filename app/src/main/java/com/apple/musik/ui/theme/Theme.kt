package com.apple.musik.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    secondary = Accent,
    onSecondary = Color.White,
    tertiary = Accent,
    background = DarkBackground,
    onBackground = PrimaryText,
    surface = DarkSurface,
    onSurface = PrimaryText,
    surfaceVariant = DarkCard,
    onSurfaceVariant = SecondaryText,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkCard,
    outline = DividerColor
)

object AppleMusikTheme {
    val colors: AppleMusikColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppleMusikColors.current

    val typography
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography
}

@Composable
fun AppleMusikTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        }
    }

    val appColors = AppleMusikColors()

    CompositionLocalProvider(
        LocalAppleMusikColors provides appColors
    ) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = Typography,
            content = content
        )
    }
}
