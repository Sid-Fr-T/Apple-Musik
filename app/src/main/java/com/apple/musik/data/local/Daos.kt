package com.apple.musik.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE ASC")
    fun getSongsSortedByName(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY dateModified DESC")
    fun getSongsSortedByModifiedDate(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY dateAdded DESC")
    fun getSongsSortedByDateAdded(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY path COLLATE NOCASE ASC")
    fun getSongsSortedByPath(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR album LIKE '%' || :query || '%' ORDER BY title COLLATE NOCASE ASC")
    fun searchSongs(query: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE album = :albumName ORDER BY CASE WHEN trackNumber > 0 THEN trackNumber ELSE id END ASC")
    fun getSongsForAlbum(albumName: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE artist = :artistName ORDER BY title COLLATE NOCASE ASC")
    fun getSongsForArtist(artistName: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY dateAdded DESC LIMIT 15")
    fun getRecentlyAddedSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY dateModified DESC LIMIT 15")
    fun getRecentlyPlayedSongs(): Flow<List<SongEntity>>

    @Query("SELECT COUNT(*) FROM songs")
    fun getSongCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getSongCountSync(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>): List<Long>

    @Query("DELETE FROM songs")
    suspend fun deleteAllSongs(): Int
}

@Dao
interface AlbumDao {
    @Query("SELECT * FROM albums ORDER BY title COLLATE NOCASE ASC")
    fun getAllAlbums(): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM albums WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' ORDER BY title COLLATE NOCASE ASC")
    fun searchAlbums(query: String): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM albums WHERE title = :albumName LIMIT 1")
    fun getAlbumByName(albumName: String): Flow<AlbumEntity?>

    @Query("SELECT * FROM albums WHERE id = :albumId LIMIT 1")
    fun getAlbumById(albumId: Long): Flow<AlbumEntity?>

    @Query("SELECT * FROM albums LIMIT 10")
    fun getRecentAlbums(): Flow<List<AlbumEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbums(albums: List<AlbumEntity>): List<Long>

    @Query("DELETE FROM albums")
    suspend fun deleteAllAlbums(): Int
}

@Dao
interface ArtistDao {
    @Query("SELECT * FROM artists ORDER BY name COLLATE NOCASE ASC")
    fun getAllArtists(): Flow<List<ArtistEntity>>

    @Query("SELECT * FROM artists WHERE name LIKE '%' || :query || '%' ORDER BY name COLLATE NOCASE ASC")
    fun searchArtists(query: String): Flow<List<ArtistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtists(artists: List<ArtistEntity>): List<Long>

    @Query("DELETE FROM artists")
    suspend fun deleteAllArtists(): Int
}
