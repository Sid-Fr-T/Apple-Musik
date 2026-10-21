package com.apple.musik.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import com.apple.musik.ui.components.BottomTabBar
import com.apple.musik.ui.components.MiniPlayerBar
import com.apple.musik.ui.navigation.NavigationTab
import com.apple.musik.ui.screens.HomeScreen
import com.apple.musik.ui.screens.LibraryScreen
import com.apple.musik.ui.screens.NowPlayingScreen
import com.apple.musik.ui.screens.PermissionScreen
import com.apple.musik.ui.screens.SearchScreen
import com.apple.musik.ui.theme.AppleMusikTheme
import com.apple.musik.ui.viewmodel.MusicViewModel
import com.apple.musik.ui.viewmodel.PlayerViewModel

/**
 * Main application navigation shell with:
 * - SharedTransitionLayout for Hero Artwork animation
 * - Media3 PlayerViewModel connection
 * - Live Mini Player expandable into Now Playing screen
 * - Storage permission check with friendly UI
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    musicViewModel: MusicViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(NavigationTab.Home) }
    var isNowPlayingExpanded by remember { mutableStateOf(false) }

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

    val songs by musicViewModel.songs.collectAsState()
    val recentlyAdded by musicViewModel.recentlyAdded.collectAsState()
    val recentlyPlayed by musicViewModel.recentlyPlayed.collectAsState()
    val activeSort by musicViewModel.sortOption.collectAsState()

    val currentSong by playerViewModel.currentSong.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val currentPosition by playerViewModel.position.collectAsState()
    val totalDuration by playerViewModel.duration.collectAsState()

    // Default song fallback for preview / initial state
    val activeSong = currentSong ?: songs.firstOrNull { it.title == "Slow Bloom" } ?: songs.firstOrNull()

    // Check system permission on launch
    LaunchedEffect(Unit) {
        if (hasSystemPermission) {
            musicViewModel.updatePermissionStatus(true)
        }
    }

    // Collapse Now Playing on system back button
    BackHandler(enabled = isNowPlayingExpanded) {
        isNowPlayingExpanded = false
    }

    if (!permissionDismissed && !hasSystemPermission) {
        PermissionScreen(
            onPermissionGranted = {
                permissionDismissed = true
                musicViewModel.updatePermissionStatus(true)
            },
            onSkip = {
                permissionDismissed = true
                musicViewModel.scanLocalMedia()
            }
        )
    } else {
        SharedTransitionLayout {
            AnimatedContent(
                targetState = isNowPlayingExpanded,
                transitionSpec = {
                    if (targetState) {
                        // Expanding: slide up from bottom + fade in
                        (slideInVertically(
                            animationSpec = tween(380),
                            initialOffsetY = { it }
                        ) + fadeIn(tween(320)))
                            .togetherWith(
                                fadeOut(tween(200))
                            )
                    } else {
                        // Collapsing: slide down to bottom + fade out
                        fadeIn(tween(250))
                            .togetherWith(
                                slideOutVertically(
                                    animationSpec = tween(320),
                                    targetOffsetY = { it }
                                ) + fadeOut(tween(250))
                            )
                    }
                },
                label = "NowPlayingHeroTransition"
            ) { expanded ->
                if (expanded) {
                    NowPlayingScreen(
                        song = activeSong,
                        isPlaying = isPlaying,
                        currentPosition = currentPosition,
                        totalDuration = totalDuration,
                        onPlayPauseClick = { playerViewModel.togglePlayPause() },
                        onPreviousClick = { playerViewModel.skipToPrevious() },
                        onNextClick = { playerViewModel.skipToNext() },
                        onSeek = { playerViewModel.seekTo(it) },
                        onCollapse = { isNowPlayingExpanded = false },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@AnimatedContent,
                        modifier = Modifier.fillMaxSize()
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
                                            onSongClick = { song ->
                                                playerViewModel.playSong(song, songs)
                                            }
                                        )
                                    }

                                    NavigationTab.Library -> {
                                        LibraryScreen(
                                            songs = songs,
                                            activeSort = activeSort,
                                            onSortOptionSelected = { musicViewModel.setSortOption(it) },
                                            onSongClick = { song ->
                                                playerViewModel.playSong(song, songs)
                                            },
                                            onPlayAllClick = {
                                                playerViewModel.playAll(songs)
                                            },
                                            onShuffleClick = {
                                                playerViewModel.shuffle(songs)
                                            }
                                        )
                                    }

                                    NavigationTab.Search -> {
                                        SearchScreen(
                                            songs = songs,
                                            onSongClick = { song ->
                                                playerViewModel.playSong(song, songs)
                                            }
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
                            MiniPlayerBar(
                                song = activeSong,
                                isPlaying = isPlaying,
                                onPreviousClick = { playerViewModel.skipToPrevious() },
                                onPlayPauseClick = { playerViewModel.togglePlayPause() },
                                onNextClick = { playerViewModel.skipToNext() },
                                onClick = { isNowPlayingExpanded = true },
                                sharedTransitionScope = this@SharedTransitionLayout,
                                animatedVisibilityScope = this@AnimatedContent,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            BottomTabBar(
                                selectedTab = selectedTab,
                                onTabSelected = { selectedTab = it }
                            )
                        }
                    }
                }
            }
        }
    }
}
