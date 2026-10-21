package com.apple.musik

import androidx.compose.ui.graphics.Color
import com.apple.musik.ui.navigation.NavigationTab
import com.apple.musik.ui.theme.Accent
import com.apple.musik.ui.theme.AppleMusikColors
import com.apple.musik.ui.theme.DarkBackground
import com.apple.musik.ui.theme.DarkCard
import com.apple.musik.ui.theme.DarkSurface
import com.apple.musik.ui.theme.MiniPlayerBackground
import com.apple.musik.ui.theme.SecondaryText
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeAndNavigationTest {

    @Test
    fun testThemeColorTokens() {
        val colors = AppleMusikColors()
        assertEquals(Color(0xFF0B0B0D), colors.background)
        assertEquals(Color(0xFF1E1E23), colors.surface)
        assertEquals(Color(0xFF17171B), colors.card)
        assertEquals(Color(0xFFFF5A3C), colors.accent)
        assertEquals(Color(0xFFA1A1AA), colors.secondaryText)
        assertEquals(Color(0xFF212126), colors.miniPlayer)

        assertEquals(Color(0xFF0B0B0D), DarkBackground)
        assertEquals(Color(0xFF1E1E23), DarkSurface)
        assertEquals(Color(0xFF17171B), DarkCard)
        assertEquals(Color(0xFFFF5A3C), Accent)
        assertEquals(Color(0xFFA1A1AA), SecondaryText)
        assertEquals(Color(0xFF212126), MiniPlayerBackground)
    }

    @Test
    fun testNavigationTabs() {
        val tabs = NavigationTab.entries
        assertEquals(3, tabs.size)
        assertEquals("Home", tabs[0].title)
        assertEquals("Library", tabs[1].title)
        assertEquals("Search", tabs[2].title)
    }
}
