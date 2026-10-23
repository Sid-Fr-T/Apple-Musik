package com.apple.musik.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val uri: String,
    val albumArtUri: String?,
    val dateAdded: Long,
    val dateModified: Long,
    val path: String = "",
    val trackNumber: Int = 0,
    val year: Int = 0,
    val genre: String = "",
    val albumId: Long = 0L
)

@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val artworkUri: String?,
    val songCount: Int,
    val year: Int = 0,
    val genre: String = ""
)

@Entity(tableName = "artists")
data class ArtistEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val trackCount: Int,
    val albumCount: Int
)
