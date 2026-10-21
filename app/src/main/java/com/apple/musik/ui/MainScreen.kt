package com.apple.musik.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apple.musik.data.local.SongEntity
import com.apple.musik.ui.components.BottomTabBar
import com.apple.musik.ui.components.MiniPlayerBar
import com.apple.musik.ui.navigation.NavigationTab
import com.apple.musik.ui.screens.HomeScreen
import com.apple.musik.ui.screens.LibraryScreen
import com.apple.musik.ui.screens.PermissionScreen
import com.apple.musik.ui.screens.SearchScreen
import com.apple.musik.ui.theme.AppleMusikTheme
import com.apple.musik.ui.viewmodel.MusicViewModel

/**
 * Main application navigation shell containing:
 * - Storage permission gatekeeper with friendly permission screen
 * - Real MediaStore / Room powered screens (Home, Library > Songs, Search)
 * - Floating mini player bar with 3 buttons (Previous, Play/Pause, Next)
 * - Bottom tab bar with active accent tinting
 */
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MusicViewModel = viewModel()
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(NavigationTab.Home) }

    val permissionToTest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val hasSystemPermission = ContextCompat.checkSelfPermission(
        context,
        permissionToTest
    ) == PackageManager.PERMISSION_GRANTED

    var permissionDismissed by remember { mutableStateOf(hasSystemPermission) }

    val songs by viewModel.songs.collectAsState()
    val recentlyAdded by viewModel.recentlyAdded.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    val activeSort by viewModel.sortOption.collectAsState()

    var activeSong by remember { mutableStateOf<SongEntity?>(null) }

    // Set initial active song if none selected
    LaunchedEffect(songs) {
        if (activeSong == null && songs.isNotEmpty()) {
            activeSong = songs.firstOrNull { it.title == "Slow Bloom" } ?: songs.first()
        }
    }

    // Check system permission on launch
    LaunchedEffect(Unit) {
        if (hasSystemPermission) {
            viewModel.updatePermissionStatus(true)
        }
    }

    if (!permissionDismissed && !hasSystemPermission) {
        PermissionScreen(
            onPermissionGranted = {
                permissionDismissed = true
                viewModel.updatePermissionStatus(true)
            },
            onSkip = {
                permissionDismissed = true
                viewModel.scanLocalMedia() // Seeds demo data
            }
        )
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(AppleMusikTheme.colors.background)
        ) {
            // Active Screen Content (padded at bottom to avoid overlapping mini player + tab bar)
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
                        NavigationTab.Home -> {
                            HomeScreen(
                                recentlyAdded = if (recentlyAdded.isNotEmpty()) recentlyAdded else songs.take(6),
                                recentlyPlayed = if (recentlyPlayed.isNotEmpty()) recentlyPlayed else songs.take(6),
                                onSongClick = { activeSong = it }
                            )
                        }

                        NavigationTab.Library -> {
                            LibraryScreen(
                                songs = songs,
                                activeSort = activeSort,
                                onSortOptionSelected = { viewModel.setSortOption(it) },
                                onSongClick = { activeSong = it }
                            )
                        }

                        NavigationTab.Search -> {
                            SearchScreen(
                                songs = songs,
                                onSongClick = { activeSong = it }
                            )
                        }
                    }
                }
            }

            // Floating Mini Player & Bottom Navigation Tab Bar
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                // Floating Mini Player Bar with 3 controls: Previous, Play/Pause, Next
                val displaySong = activeSong ?: songs.firstOrNull()
                MiniPlayerBar(
                    title = displaySong?.title ?: "Slow Bloom",
                    artist = displaySong?.artist ?: "Aria Vale",
                    albumArtUri = displaySong?.albumArtUri,
                    modifier = Modifier.padding(bottom = 8.dp),
                    onPreviousClick = {
                        val currentIndex = songs.indexOf(activeSong)
                        if (currentIndex > 0) {
                            activeSong = songs[currentIndex - 1]
                        } else if (songs.isNotEmpty()) {
                            activeSong = songs.last()
                        }
                    },
                    onNextClick = {
                        val currentIndex = songs.indexOf(activeSong)
                        if (currentIndex in 0 until songs.size - 1) {
                            activeSong = songs[currentIndex + 1]
                        } else if (songs.isNotEmpty()) {
                            activeSong = songs.first()
                        }
                    }
                )

                // Bottom Navigation Tab Bar (3 tabs: Home, Library, Search)
                BottomTabBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }
        }
    }
}
