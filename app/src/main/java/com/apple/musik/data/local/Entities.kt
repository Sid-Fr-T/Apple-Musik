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
    val dateModified: Long
)

@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val artworkUri: String?,
    val songCount: Int,
    val year: Int = 0
)

@Entity(tableName = "artists")
data class ArtistEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val trackCount: Int,
    val albumCount: Int
)
