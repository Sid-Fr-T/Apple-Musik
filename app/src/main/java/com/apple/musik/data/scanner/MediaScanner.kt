package com.apple.musik.data.scanner

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.apple.musik.data.local.AlbumEntity
import com.apple.musik.data.local.ArtistEntity
import com.apple.musik.data.local.MusicDatabase
import com.apple.musik.data.local.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaScanner(private val context: Context) {

    private val db = MusicDatabase.getInstance(context)

    suspend fun scanMedia(): Int = withContext(Dispatchers.IO) {
        val songsList = mutableListOf<SongEntity>()
        val albumsMap = mutableMapOf<Long, AlbumEntity>()
        val artistsMap = mutableMapOf<String, MutableList<SongEntity>>()

        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projectionList = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            projectionList.add(MediaStore.Audio.Media.GENRE)
        }
        val projection = projectionList.toTypedArray()

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
                val pathColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                val trackColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK)
                val yearColumn = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)
                val genreColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.GENRE)
                } else -1

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn) ?: "Unknown Title"
                    val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                    val album = cursor.getString(albumColumn) ?: "Unknown Album"
                    val albumId = cursor.getLong(albumIdColumn)
                    val duration = cursor.getLong(durationColumn)
                    val dateAdded = cursor.getLong(dateAddedColumn)
                    val dateModified = cursor.getLong(dateModifiedColumn)
                    val rawPath = if (pathColumn != -1) cursor.getString(pathColumn) else null
                    val rawTrack = if (trackColumn != -1) cursor.getInt(trackColumn) else 0
                    val trackNumber = if (rawTrack >= 1000) rawTrack % 1000 else rawTrack
                    val year = if (yearColumn != -1) cursor.getInt(yearColumn) else 0
                    val genre = if (genreColumn != -1) cursor.getString(genreColumn) ?: "" else ""

                    val uri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    ).toString()

                    // Unique artwork URI for each individual song to guarantee separate embedded artwork
                    val songArtworkUri = ContentUris.withAppendedId(
                        Uri.parse("content://com.apple.musik.artwork/song"),
                        id
                    ).toString()

                    val albumArtUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    ).toString()

                    val finalPath = rawPath?.ifEmpty { uri } ?: uri

                    val song = SongEntity(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        duration = duration,
                        uri = uri,
                        albumArtUri = songArtworkUri,
                        dateAdded = dateAdded,
                        dateModified = dateModified,
                        path = finalPath,
                        trackNumber = trackNumber,
                        year = year,
                        genre = genre,
                        albumId = albumId
                    )
                    songsList.add(song)

                    // Track albums
                    if (!albumsMap.containsKey(albumId)) {
                        albumsMap[albumId] = AlbumEntity(
                            id = albumId,
                            title = album,
                            artist = artist,
                            artworkUri = albumArtUri,
                            songCount = 1,
                            year = if (year > 0) year else 0,
                            genre = genre
                        )
                    } else {
                        val current = albumsMap[albumId]!!
                        albumsMap[albumId] = current.copy(
                            songCount = current.songCount + 1,
                            year = if (current.year == 0 && year > 0) year else current.year,
                            genre = if (current.genre.isEmpty() && genre.isNotEmpty()) genre else current.genre
                        )
                    }

                    // Track artists
                    artistsMap.getOrPut(artist) { mutableListOf() }.add(song)
                }
            }
        } catch (e: Exception) {
            Log.e("MediaScanner", "Error querying MediaStore", e)
        }

        // If no songs found on storage (e.g. emulator), seed high-fidelity PDF demo data
        if (songsList.isEmpty()) {
            val demoSongs = getPdfDemoSongs()
            songsList.addAll(demoSongs)

            demoSongs.forEach { song ->
                val albumId = song.albumId
                if (!albumsMap.containsKey(albumId)) {
                    albumsMap[albumId] = AlbumEntity(
                        id = albumId,
                        title = song.album,
                        artist = song.artist,
                        artworkUri = null,
                        songCount = 1,
                        year = song.year,
                        genre = song.genre
                    )
                } else {
                    val current = albumsMap[albumId]!!
                    albumsMap[albumId] = current.copy(songCount = current.songCount + 1)
                }
                artistsMap.getOrPut(song.artist) { mutableListOf() }.add(song)
            }
        }

        val artistEntities = artistsMap.entries.mapIndexed { index, entry ->
            val albumsForArtist = entry.value.map { it.album }.distinct().size
            ArtistEntity(
                id = (index + 1).toLong(),
                name = entry.key,
                trackCount = entry.value.size,
                albumCount = albumsForArtist
            )
        }

        if (songsList.isNotEmpty()) {
            db.songDao().deleteAllSongs()
            db.songDao().insertSongs(songsList)
            db.albumDao().deleteAllAlbums()
            db.albumDao().insertAlbums(albumsMap.values.toList())
            db.artistDao().deleteAllArtists()
            db.artistDao().insertArtists(artistEntities)
        }

        songsList.size
    }

    private fun getPdfDemoSongs(): List<SongEntity> {
        val now = System.currentTimeMillis() / 1000
        return listOf(
            SongEntity(
                id = 1,
                title = "Glass Hours",
                artist = "Aria Vale",
                album = "Slow Bloom",
                duration = 178_000L, // 2:58
                uri = "content://demo/1",
                albumArtUri = null,
                dateAdded = now - 5000,
                dateModified = now - 2000,
                path = "/storage/emulated/0/Music/Aria Vale/Slow Bloom/01 - Glass Hours.mp3",
                trackNumber = 1,
                year = 2024,
                genre = "Alternative",
                albumId = 1L
            ),
            SongEntity(
                id = 2,
                title = "Golden Static",
                artist = "Juno Hart",
                album = "Paper Planes",
                duration = 195_000L,
                uri = "content://demo/2",
                albumArtUri = null,
                dateAdded = now - 8000,
                dateModified = now - 4000,
                path = "/storage/emulated/0/Music/Juno Hart/Paper Planes/01 - Golden Static.mp3",
                trackNumber = 1,
                year = 2023,
                genre = "Pop",
                albumId = 2L
            ),
            SongEntity(
                id = 3,
                title = "Low Light",
                artist = "Mira Sol",
                album = "Low Light",
                duration = 222_000L, // 3:42
                uri = "content://demo/3",
                albumArtUri = null,
                dateAdded = now - 12000,
                dateModified = now - 6000,
                path = "/storage/emulated/0/Music/Mira Sol/Low Light/01 - Low Light.mp3",
                trackNumber = 1,
                year = 2023,
                genre = "R&B",
                albumId = 3L
            ),
            SongEntity(
                id = 4,
                title = "Neon Tides",
                artist = "The Parallels",
                album = "Neon Tides",
                duration = 210_000L,
                uri = "content://demo/4",
                albumArtUri = null,
                dateAdded = now - 15000,
                dateModified = now - 7000,
                path = "/storage/emulated/0/Music/The Parallels/Neon Tides/01 - Neon Tides.mp3",
                trackNumber = 1,
                year = 2024,
                genre = "Electronic",
                albumId = 4L
            ),
            SongEntity(
                id = 5,
                title = "Quiet Orbit",
                artist = "Ezra Kane",
                album = "Quiet Orbit",
                duration = 245_000L, // 4:05
                uri = "content://demo/5",
                albumArtUri = null,
                dateAdded = now - 20000,
                dateModified = now - 9000,
                path = "/storage/emulated/0/Music/Ezra Kane/Quiet Orbit/01 - Quiet Orbit.mp3",
                trackNumber = 1,
                year = 2022,
                genre = "Ambient",
                albumId = 5L
            ),
            SongEntity(
                id = 6,
                title = "Slow Bloom",
                artist = "Aria Vale",
                album = "Slow Bloom",
                duration = 201_000L, // 3:21
                uri = "content://demo/6",
                albumArtUri = null,
                dateAdded = now - 25000,
                dateModified = now - 1000,
                path = "/storage/emulated/0/Music/Aria Vale/Slow Bloom/02 - Slow Bloom.mp3",
                trackNumber = 2,
                year = 2024,
                genre = "Alternative",
                albumId = 1L
            ),
            SongEntity(
                id = 7,
                title = "Tidal",
                artist = "The Parallels",
                album = "Neon Tides",
                duration = 210_000L, // 3:30
                uri = "content://demo/7",
                albumArtUri = null,
                dateAdded = now - 30000,
                dateModified = now - 11000,
                path = "/storage/emulated/0/Music/The Parallels/Neon Tides/02 - Tidal.mp3",
                trackNumber = 2,
                year = 2024,
                genre = "Electronic",
                albumId = 4L
            ),
            SongEntity(
                id = 8,
                title = "Paper Light",
                artist = "Aria Vale",
                album = "Slow Bloom",
                duration = 224_000L, // 3:44
                uri = "content://demo/8",
                albumArtUri = null,
                dateAdded = now - 35000,
                dateModified = now - 15000,
                path = "/storage/emulated/0/Music/Aria Vale/Slow Bloom/03 - Paper Light.mp3",
                trackNumber = 3,
                year = 2024,
                genre = "Alternative",
                albumId = 1L
            ),
            SongEntity(
                id = 9,
                title = "Evening Static",
                artist = "Aria Vale",
                album = "Slow Bloom",
                duration = 250_000L, // 4:10
                uri = "content://demo/9",
                albumArtUri = null,
                dateAdded = now - 40000,
                dateModified = now - 18000,
                path = "/storage/emulated/0/Music/Aria Vale/Slow Bloom/04 - Evening Static.mp3",
                trackNumber = 4,
                year = 2024,
                genre = "Alternative",
                albumId = 1L
            )
        )
    }
}
