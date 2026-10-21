package com.apple.musik.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Color Tokens
val DarkBackground = Color(0xFF0B0B0D)
val DarkSurface = Color(0xFF1E1E23)
val DarkCard = Color(0xFF17171B)
val Accent = Color(0xFFFF5A3C)
val SecondaryText = Color(0xFFA1A1AA)
val PrimaryText = Color(0xFFFFFFFF)
val MiniPlayerBackground = Color(0xFF212126)
val DividerColor = Color(0xFF26262B)

@Immutable
data class AppleMusikColors(
    val background: Color = DarkBackground,
    val surface: Color = DarkSurface,
    val card: Color = DarkCard,
    val accent: Color = Accent,
    val secondaryText: Color = SecondaryText,
    val primaryText: Color = PrimaryText,
    val miniPlayer: Color = MiniPlayerBackground,
    val divider: Color = DividerColor
)

val LocalAppleMusikColors = staticCompositionLocalOf { AppleMusikColors() }
