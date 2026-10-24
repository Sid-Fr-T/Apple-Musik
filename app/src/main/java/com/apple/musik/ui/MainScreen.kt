package com.apple.musik.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
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
import kotlin.math.roundToInt

/**
 * Main application navigation shell:
 * - Fluid iOS-style bottom sheet modal transition to Now Playing.
 * - Opened page (Home/Library/Search/AlbumDetail) stays permanently visible underneath,
 *   scaling down (1.0 -> 0.93) with rounded corners and gentle dimming (no white background).
 * - Tactile spring-based physics for all transitions and controls.
 * - Zero-jitter album art loading and memory caching.
 */
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

    // Back handling: Queue collapses first, then Now Playing, then Album Details
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
        // Continuous, bouncy spring animation progress for Now Playing sheet
        val nowPlayingProgress by animateFloatAsState(
            targetValue = if (isNowPlayingExpanded) 1f else 0f,
            animationSpec = spring(
                dampingRatio = 0.78f,
                stiffness = 330f
            ),
            label = "NowPlayingProgress"
        )

        // Clamped progress to prevent underdamped spring overshoot from producing negative corner sizes or scales
        val clampedProgress = nowPlayingProgress.coerceIn(0f, 1f)
        val baseScale = (1f - (0.07f * clampedProgress)).coerceIn(0.9f, 1f)
        val baseCorners = (28f * clampedProgress).coerceAtLeast(0f).dp
        val scrimAlpha = (0.52f * clampedProgress).coerceIn(0f, 1f)

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(AppleMusikTheme.colors.background)
        ) {
            // 1. BASE SCREEN LAYER: Always mounted & rendered underneath
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = baseScale
                        scaleY = baseScale
                        transformOrigin = TransformOrigin(0.5f, 0.35f)
                        clip = clampedProgress > 0.005f
                        shape = RoundedCornerShape(baseCorners)
                    }
            ) {
                // Active Screen Content (Home, Library, Search, Album Detail)
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
                            animationSpec = spring(
                                dampingRatio = 0.82f,
                                stiffness = 380f
                            ),
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
                        .graphicsLayer {
                            alpha = (1f - clampedProgress * 2.5f).coerceIn(0f, 1f)
                            translationY = (60f * clampedProgress).coerceAtLeast(0f)
                        }
                ) {
                    MiniPlayerBar(
                        song = activeSong,
                        isPlaying = isPlaying,
                        onPreviousClick = { playerViewModel.skipToPrevious() },
                        onPlayPauseClick = { playerViewModel.togglePlayPause() },
                        onNextClick = { playerViewModel.skipToNext() },
                        onClick = { isNowPlayingExpanded = true },
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

            // 2. DIM SCRIM: Softly dims the opened page behind the expanding sheet
            if (scrimAlpha > 0.001f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = scrimAlpha }
                        .background(Color.Black)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = isNowPlayingExpanded,
                            onClick = { isNowPlayingExpanded = false }
                        )
                )
            }

            // 3. NOW PLAYING SHEET LAYER: Springs up smoothly from bottom over the opened page
            if (clampedProgress > 0.001f || isNowPlayingExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationY = ((1f - clampedProgress) * size.height).coerceAtLeast(0f)
                            alpha = (clampedProgress * 3.5f).coerceIn(0f, 1f)
                            clip = true
                            shape = RoundedCornerShape(
                                topStart = 28.dp,
                                topEnd = 28.dp
                            )
                            shadowElevation = (24 * clampedProgress).coerceAtLeast(0f).dp.toPx()
                            spotShadowColor = Color.Black.copy(alpha = 0.7f * clampedProgress)
                        }
                ) {
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
                        modifier = Modifier.fillMaxSize()
                    )

                    // Animated Queue Screen overlay with smooth vertical spring slide
                    AnimatedVisibility(
                        visible = isQueueOpen,
                        enter = slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = spring(
                                dampingRatio = 0.78f,
                                stiffness = 340f
                            )
                        ) + fadeIn(tween(180)),
                        exit = slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = spring(
                                dampingRatio = 0.85f,
                                stiffness = 380f
                            )
                        ) + fadeOut(tween(160))
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
            }
        }
    }
}
