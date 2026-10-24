package com.apple.musik.data.artwork

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import java.util.Collections
import kotlin.math.max

/**
 * High-performance singleton helper for extracting and caching individual song artwork.
 * Prioritizes each song's individual embedded cover art (ID3/APIC/MP4 covr),
 * preventing different songs from sharing mismatched album art.
 */
object AudioArtworkHelper {

    // In-memory cache for raw artwork bytes (up to 32MB max / ~64-128 images)
    private val bytesCache = LruCache<Long, ByteArray>(64)

    // Set of song IDs confirmed to have no artwork to avoid repeated failed extraction
    private val noArtCache = Collections.synchronizedSet(HashSet<Long>())

    // Known generic/compilation tags where album art must NOT be shared across different artists
    private val genericAlbumKeywords = listOf(
        "noice", "download", "downloads", "music", "audio", "various", "unknown",
        "whatsapp", "telegram", "youtube", "sound", "track", "single", "compilation"
    )

    /**
     * Retrieves the raw JPEG/PNG byte array for an individual song.
     */
    fun getArtworkBytes(context: Context, songId: Long): ByteArray? {
        if (noArtCache.contains(songId)) return null

        val cached = bytesCache.get(songId)
        if (cached != null) return cached

        val songUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId)

        // 1. Primary: Extract embedded picture directly from the song file using MediaMetadataRetriever
        val embedded = extractEmbeddedPicture(context, songUri)
        if (embedded != null && embedded.isNotEmpty()) {
            bytesCache.put(songId, embedded)
            return embedded
        }

        // 2. Secondary fallback: Check if the song belongs to a legitimate single-artist studio album
        val albumArtBytes = loadLegitimateAlbumArt(context, songId)
        if (albumArtBytes != null && albumArtBytes.isNotEmpty()) {
            bytesCache.put(songId, albumArtBytes)
            return albumArtBytes
        }

        // No artwork available for this song
        noArtCache.add(songId)
        return null
    }

    /**
     * Extracts embedded artwork from the audio file using file descriptor or URI.
     */
    private fun extractEmbeddedPicture(context: Context, songUri: Uri): ByteArray? {
        val retriever = MediaMetadataRetriever()
        try {
            // Prefer openFileDescriptor for scoped storage safety and direct random access
            try {
                context.contentResolver.openFileDescriptor(songUri, "r")?.use { pfd ->
                    retriever.setDataSource(pfd.fileDescriptor)
                    val pic = retriever.embeddedPicture
                    if (pic != null && pic.isNotEmpty()) return pic
                }
            } catch (_: Exception) {
                // Fallback to direct content URI
                retriever.setDataSource(context, songUri)
                val pic = retriever.embeddedPicture
                if (pic != null && pic.isNotEmpty()) return pic
            }
        } catch (_: Exception) {
            // File might be unreadable or corrupt
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
        return null
    }

    /**
     * Only falls back to MediaStore's album art if the album is a real studio album
     * (i.e. not a shared generic folder/tag like "Noice", "Downloads", etc. and artist matches).
     */
    private fun loadLegitimateAlbumArt(context: Context, songId: Long): ByteArray? {
        try {
            val songUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId)
            val projection = arrayOf(
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ARTIST
            )

            var albumId: Long = -1
            var albumName = ""
            var artistName = ""

            context.contentResolver.query(songUri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                    val albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                    val artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)

                    if (albumIdCol != -1) albumId = cursor.getLong(albumIdCol)
                    if (albumCol != -1) albumName = cursor.getString(albumCol).orEmpty()
                    if (artistCol != -1) artistName = cursor.getString(artistCol).orEmpty()
                }
            }

            if (albumId <= 0 || albumName.isBlank()) return null

            // Do NOT inherit album art if the album name matches any generic tag or keyword
            val lowerAlbum = albumName.lowercase()
            for (keyword in genericAlbumKeywords) {
                if (lowerAlbum.contains(keyword)) {
                    return null
                }
            }

            // Verify album artist matches song artist to avoid cross-artist contamination
            val albumUri = ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumId)
            var albumArtist = ""
            try {
                context.contentResolver.query(
                    albumUri,
                    arrayOf(MediaStore.Audio.Albums.ARTIST),
                    null,
                    null,
                    null
                )?.use { albumCursor ->
                    if (albumCursor.moveToFirst()) {
                        val col = albumCursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST)
                        if (col != -1) albumArtist = albumCursor.getString(col).orEmpty()
                    }
                }
            } catch (_: Exception) {}

            if (albumArtist.isNotBlank() && !artistName.equals(albumArtist, ignoreCase = true)) {
                return null
            }

            // Read the album art from MediaStore
            val legacyAlbumArtUri = ContentUris.withAppendedId(
                Uri.parse("content://media/external/audio/albumart"),
                albumId
            )
            context.contentResolver.openInputStream(legacyAlbumArtUri)?.use { stream ->
                val bytes = stream.readBytes()
                if (bytes.isNotEmpty()) return bytes
            }
        } catch (_: Exception) {}

        return null
    }

    /**
     * Decodes and returns a Bitmap scaled to the requested maxDimension.
     */
    fun getArtworkBitmap(context: Context, songId: Long, maxDimension: Int): Bitmap? {
        val bytes = getArtworkBytes(context, songId) ?: return null

        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOpts)

        val sampleSize = calculateInSampleSize(
            boundsOpts.outWidth,
            boundsOpts.outHeight,
            maxDimension,
            maxDimension
        )
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
    }

    /**
     * Calculates the optimal inSampleSize for high-clarity decoding without excessive memory usage.
     */
    fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    fun clearCache() {
        bytesCache.evictAll()
        noArtCache.clear()
    }
}

