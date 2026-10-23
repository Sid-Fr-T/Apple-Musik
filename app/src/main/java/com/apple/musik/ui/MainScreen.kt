package com.apple.musik.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.spring
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
import com.apple.musik.data.local.AlbumEntity
import com.apple.musik.ui.components.BottomTabBar
import com.apple.musik.ui.components.MiniPlayerBar
import com.apple.musik.ui.navigation.NavigationTab
import com.apple.musik.ui.screens.AlbumDetailScreen
import com.apple.musik.ui.screens.HomeScreen
import com.apple.musik.ui.screens.LibraryScreen
import com.apple.musik.ui.screens.NowPlayingScreen
import com.apple.musik.ui.screens.PermissionScreen
import com.apple.musik.ui.screens.QueueScreen
import com.apple.musik.ui.screens.SearchScreen
import com.apple.musik.ui.theme.AppleMusikTheme
import com.apple.musik.ui.viewmodel.MusicViewModel
import com.apple.musik.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.flow.flowOf

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
    var isQueueOpen by remember { mutableStateOf(false) }
    var selectedAlbum by remember { mutableStateOf<AlbumEntity?>(null) }

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
    val albums by musicViewModel.albums.collectAsState()
    val recentlyAdded by musicViewModel.recentlyAdded.collectAsState()
    val recentlyPlayed by musicViewModel.recentlyPlayed.collectAsState()
    val activeSort by musicViewModel.sortOption.collectAsState()

    val searchQuery by musicViewModel.searchQuery.collectAsState()
    val searchSongs by musicViewModel.searchSongs.collectAsState()
    val searchAlbums by musicViewModel.searchAlbums.collectAsState()
    val searchArtists by musicViewModel.searchArtists.collectAsState()

    val albumSongs by remember(selectedAlbum) {
        val title = selectedAlbum?.title
        if (title != null) musicViewModel.getSongsForAlbum(title)
        else flowOf(emptyList())
    }.collectAsState(initial = emptyList())

    val displayAlbumSongs = remember(albumSongs, selectedAlbum, songs) {
        if (albumSongs.isNotEmpty()) {
            albumSongs
        } else {
            songs.filter { it.album.equals(selectedAlbum?.title, ignoreCase = true) }
        }
    }

    val currentSong by playerViewModel.currentSong.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val currentPosition by playerViewModel.position.collectAsState()
    val totalDuration by playerViewModel.duration.collectAsState()
    val queue by playerViewModel.queue.collectAsState()
    val shuffleModeEnabled by playerViewModel.shuffleModeEnabled.collectAsState()
    val repeatMode by playerViewModel.repeatMode.collectAsState()

    // Default song fallback for preview / initial state
    val activeSong = currentSong ?: songs.firstOrNull { it.title == "Slow Bloom" } ?: songs.firstOrNull()

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* result handled */ }

    // Check system permission on launch
    LaunchedEffect(Unit) {
        if (hasSystemPermission) {
            musicViewModel.updatePermissionStatus(true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotificationPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasNotificationPermission) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Back handling: Queue collapses first, then Now Playing
    BackHandler(enabled = isQueueOpen) {
        isQueueOpen = false
    }

    BackHandler(enabled = isNowPlayingExpanded && !isQueueOpen) {
        isNowPlayingExpanded = false
    }

    BackHandler(enabled = selectedAlbum != null && !isNowPlayingExpanded && !isQueueOpen) {
        selectedAlbum = null
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
                        (slideInVertically(
                            animationSpec = spring(
                                dampingRatio = 0.82f,
                                stiffness = 380f
                            ),
                            initialOffsetY = { it }
                        ) + fadeIn(tween(280)))
                            .togetherWith(
                                fadeOut(tween(180))
                            )
                    } else {
                        fadeIn(tween(220))
                            .togetherWith(
                                slideOutVertically(
                                    animationSpec = spring(
                                        dampingRatio = 0.85f,
                                        stiffness = 380f
                                    ),
                                    targetOffsetY = { it }
                                ) + fadeOut(tween(220))
                            )
                    }
                },
                label = "NowPlayingHeroTransition"
            ) { expanded ->
                if (expanded) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        NowPlayingScreen(
                            song = activeSong,
                            isPlaying = isPlaying,
                            currentPosition = currentPosition,
                            totalDuration = totalDuration,
                            onPlayPauseClick = { playerViewModel.togglePlayPause() },
                            onPreviousClick = { playerViewModel.skipToPrevious() },
                            onNextClick = { playerViewModel.skipToNext() },
                            onSeek = { playerViewModel.seekTo(it) },
                            onCollapse = {
                                isQueueOpen = false
                                isNowPlayingExpanded = false
                            },
                            onQueueClick = { isQueueOpen = !isQueueOpen },
                            isQueueOpen = isQueueOpen,
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@AnimatedContent,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Animated Queue Screen overlay with smooth vertical spring slide
                        AnimatedVisibility(
                            visible = isQueueOpen,
                            enter = slideInVertically(
                                initialOffsetY = { it },
                                animationSpec = spring(
                                    dampingRatio = 0.85f,
                                    stiffness = 380f
                                )
                            ) + fadeIn(tween(200)),
                            exit = slideOutVertically(
                                targetOffsetY = { it },
                                animationSpec = spring(
                                    dampingRatio = 0.85f,
                                    stiffness = 380f
                                )
                            ) + fadeOut(tween(200))
                        ) {
                            QueueScreen(
                                currentSong = activeSong,
                                queue = queue,
                                isPlaying = isPlaying,
                                shuffleModeEnabled = shuffleModeEnabled,
                                repeatMode = repeatMode,
                                onSongClick = { song ->
                                    playerViewModel.playSong(song)
                                },
                                onRemoveFromQueue = { songId ->
                                    playerViewModel.removeFromQueue(songId)
                                },
                                onReorderQueue = { fromIndex, toIndex ->
                                    playerViewModel.reorderQueue(fromIndex, toIndex)
                                },
                                onToggleShuffle = { playerViewModel.toggleShuffle() },
                                onToggleRepeat = { playerViewModel.toggleRepeat() },
                                onDismiss = { isQueueOpen = false },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
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
                            if (selectedAlbum != null) {
                                val currentAlbum = selectedAlbum!!
                                AlbumDetailScreen(
                                    album = currentAlbum,
                                    tracks = displayAlbumSongs,
                                    currentSong = activeSong,
                                    onBackClick = { selectedAlbum = null },
                                    onTrackClick = { track, trackList ->
                                        playerViewModel.playSong(track, trackList)
                                    },
                                    onPlayAllClick = { trackList ->
                                        playerViewModel.playAll(trackList)
                                    },
                                    onShuffleClick = { trackList ->
                                        playerViewModel.shuffle(trackList)
                                    }
                                )
                            } else {
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
                                                },
                                                onAlbumClick = { song ->
                                                    val match = albums.firstOrNull { it.title.equals(song.album, ignoreCase = true) }
                                                        ?: AlbumEntity(
                                                            id = if (song.albumId != 0L) song.albumId else song.id,
                                                            title = song.album.ifEmpty { song.title },
                                                            artist = song.artist,
                                                            artworkUri = song.albumArtUri,
                                                            songCount = 1,
                                                            year = song.year,
                                                            genre = song.genre
                                                        )
                                                    selectedAlbum = match
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
                                                searchQuery = searchQuery,
                                                onQueryChange = { musicViewModel.onSearchQueryChanged(it) },
                                                songs = searchSongs,
                                                albums = searchAlbums,
                                                artists = searchArtists,
                                                onSongClick = { song, queueList ->
                                                    playerViewModel.playSong(song, queueList)
                                                },
                                                onAlbumClick = { album ->
                                                    selectedAlbum = album
                                                },
                                                onArtistClick = { artist ->
                                                    val artistTracks = songs.filter { it.artist.equals(artist.name, ignoreCase = true) }
                                                    if (artistTracks.isNotEmpty()) {
                                                        playerViewModel.playAll(artistTracks)
                                                    }
                                                }
                                            )
                                        }
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
                                onTabSelected = {
                                    selectedTab = it
                                    selectedAlbum = null
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
