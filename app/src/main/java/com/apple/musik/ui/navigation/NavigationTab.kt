package com.apple.musik.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.ui.graphics.vector.ImageVector

enum class NavigationTab(
    val title: String,
    val icon: ImageVector
) {
    Home("Home", Icons.Rounded.Home),
    Library("Library", Icons.Rounded.LibraryMusic),
    Search("Search", Icons.Rounded.Search)
}
