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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtists(artists: List<ArtistEntity>): List<Long>

    @Query("DELETE FROM artists")
    suspend fun deleteAllArtists(): Int
}
