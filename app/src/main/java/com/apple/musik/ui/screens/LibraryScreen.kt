package com.apple.musik.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.apple.musik.data.datastore.SortOption
import com.apple.musik.data.local.SongEntity

/**
 * Library screen displays the Songs view as specified in the PDF design (Page 3 & 4).
 */
@Composable
fun LibraryScreen(
    songs: List<SongEntity>,
    activeSort: SortOption,
    onSortOptionSelected: (SortOption) -> Unit,
    onSongClick: (SongEntity) -> Unit = {},
    onPlayAllClick: () -> Unit = {},
    onShuffleClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    SongsScreen(
        songs = songs,
        activeSort = activeSort,
        onSortOptionSelected = onSortOptionSelected,
        onSongClick = onSongClick,
        onPlayAllClick = onPlayAllClick,
        onShuffleClick = onShuffleClick,
        modifier = modifier
    )
}
