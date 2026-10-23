package com.apple.musik.data.repository

import android.content.Context
import com.apple.musik.data.datastore.SortOption
import com.apple.musik.data.datastore.SortPreferences
import com.apple.musik.data.local.AlbumEntity
import com.apple.musik.data.local.ArtistEntity
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
            SortOption.FILE_PATH -> db.songDao().getSongsSortedByPath()
        }
    }

    val recentlyAddedSongs: Flow<List<SongEntity>> = db.songDao().getRecentlyAddedSongs()

    val recentlyPlayedSongs: Flow<List<SongEntity>> = db.songDao().getRecentlyPlayedSongs()

    val albums: Flow<List<AlbumEntity>> = db.albumDao().getAllAlbums()

    val artists: Flow<List<ArtistEntity>> = db.artistDao().getAllArtists()

    val songCount: Flow<Int> = db.songDao().getSongCount()

    suspend fun setSortOption(option: SortOption) {
        sortPreferences.saveSortOrder(option)
    }

    suspend fun scanMedia(): Int {
        return mediaScanner.scanMedia()
    }

    fun searchSongs(query: String): Flow<List<SongEntity>> = db.songDao().searchSongs(query)

    fun searchAlbums(query: String): Flow<List<AlbumEntity>> = db.albumDao().searchAlbums(query)

    fun searchArtists(query: String): Flow<List<ArtistEntity>> = db.artistDao().searchArtists(query)

    fun getSongsForAlbum(albumName: String): Flow<List<SongEntity>> = db.songDao().getSongsForAlbum(albumName)

    fun getSongsForArtist(artistName: String): Flow<List<SongEntity>> = db.songDao().getSongsForArtist(artistName)

    fun getAlbumByName(albumName: String): Flow<AlbumEntity?> = db.albumDao().getAlbumByName(albumName)

    fun getAlbumById(albumId: Long): Flow<AlbumEntity?> = db.albumDao().getAlbumById(albumId)
}
