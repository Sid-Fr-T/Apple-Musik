package com.apple.musik.data.repository

import android.content.Context
import com.apple.musik.data.datastore.SortOption
import com.apple.musik.data.datastore.SortPreferences
import com.apple.musik.data.local.AlbumEntity
import com.apple.musik.data.local.MusicDatabase
import com.apple.musik.data.local.SongEntity
import com.apple.musik.data.scanner.MediaScanner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest

@OptIn(ExperimentalCoroutinesApi::class)
class MusicRepository(private val context: Context) {

    private val db = MusicDatabase.getInstance(context)
    private val sortPreferences = SortPreferences(context)
    private val mediaScanner = MediaScanner(context)

    val sortOption: Flow<SortOption> = sortPreferences.sortOrderFlow

    val songs: Flow<List<SongEntity>> = sortPreferences.sortOrderFlow.flatMapLatest { option ->
        when (option) {
            SortOption.NAME_ASC -> db.songDao().getSongsSortedByName()
            SortOption.MODIFIED_DATE -> db.songDao().getSongsSortedByModifiedDate()
            SortOption.DATE_ADDED -> db.songDao().getSongsSortedByDateAdded()
        }
    }

    val recentlyAddedSongs: Flow<List<SongEntity>> = db.songDao().getRecentlyAddedSongs()

    val recentlyPlayedSongs: Flow<List<SongEntity>> = db.songDao().getRecentlyPlayedSongs()

    val albums: Flow<List<AlbumEntity>> = db.albumDao().getAllAlbums()

    val songCount: Flow<Int> = db.songDao().getSongCount()

    suspend fun setSortOption(option: SortOption) {
        sortPreferences.saveSortOrder(option)
    }

    suspend fun scanMedia(): Int {
        return mediaScanner.scanMedia()
    }
}
