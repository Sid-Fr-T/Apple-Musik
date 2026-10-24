package com.apple.musik.data.artwork

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.FileNotFoundException

/**
 * Standard ContentProvider exposing song artwork via "content://com.apple.musik.artwork/song/{songId}".
 * Allows Media3 System Notifications, Lock Screen controllers, and Palette extraction
 * to read song artwork streams via standard ContentResolver.openInputStream.
 */
class ArtworkContentProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val songId = uri.lastPathSegment?.toLongOrNull()
            ?: throw FileNotFoundException("Invalid artwork URI: $uri")
        val ctx = context ?: throw FileNotFoundException("Context unavailable")

        val bytes = AudioArtworkHelper.getArtworkBytes(ctx, songId)
            ?: throw FileNotFoundException("No artwork for song id: $songId")

        return openPipeHelper(uri, "image/*", null, bytes) { output, _, _, _, data ->
            try {
                ParcelFileDescriptor.AutoCloseOutputStream(output).use { stream ->
                    stream.write(data)
                }
            } catch (_: Exception) {}
        }
    }

    override fun getType(uri: Uri): String = "image/jpeg"

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
