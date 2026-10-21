package com.apple.musik.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.apple.musik.ui.components.BottomTabBar
import com.apple.musik.ui.components.MiniPlayerBar
import com.apple.musik.ui.navigation.NavigationTab
import com.apple.musik.ui.screens.HomeScreen
import com.apple.musik.ui.screens.LibraryScreen
import com.apple.musik.ui.screens.SearchScreen
import com.apple.musik.ui.theme.AppleMusikTheme

/**
 * Main application navigation shell containing:
 * - Placeholder screens (Home, Library, Search)
 * - Floating mini player bar sitting above the tab bar
 * - Bottom tab bar with active accent tinting
 */
@Composable
fun MainScreen(
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(NavigationTab.Home) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppleMusikTheme.colors.background)
    ) {
        // Active Screen Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 136.dp)
        ) {
            Crossfade(
                targetState = selectedTab,
                label = "ScreenCrossfade"
            ) { tab ->
                when (tab) {
                    NavigationTab.Home -> HomeScreen()
                    NavigationTab.Library -> LibraryScreen()
                    NavigationTab.Search -> SearchScreen()
                }
            }
        }

        // Floating Mini Player & Bottom Navigation Tab Bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            // Floating Mini Player Bar (60dp high, 16dp side margins, radius 16dp, color #212126)
            MiniPlayerBar(
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Bottom Navigation Tab Bar (3 tabs: Home, Library, Search)
            BottomTabBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    }
}
