package com.apple.musik.data.artwork

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import coil.size.pxOrElse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Custom Coil Fetcher that handles "content://com.apple.musik.artwork/song/{songId}" URIs.
 * Retrieves each song's individual embedded artwork at the exact requested target resolution.
 */
class AudioArtworkFetcher(
    private val context: Context,
    private val uri: Uri,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        val songId = uri.lastPathSegment?.toLongOrNull() ?: return@withContext null

        val reqWidth = options.size.width.pxOrElse { 512 }
        val reqHeight = options.size.height.pxOrElse { 512 }
        val maxDim = maxOf(reqWidth, reqHeight).coerceIn(128, 1400)

        val bytes = AudioArtworkHelper.getArtworkBytes(context, songId) ?: return@withContext null

        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOpts)

        val sampleSize = AudioArtworkHelper.calculateInSampleSize(
            boundsOpts.outWidth,
            boundsOpts.outHeight,
            maxDim,
            maxDim
        )
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
            ?: return@withContext null

        DrawableResult(
            drawable = BitmapDrawable(context.resources, bitmap),
            isSampled = sampleSize > 1,
            dataSource = DataSource.DISK
        )
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (data.scheme == "content" && data.authority == "com.apple.musik.artwork") {
                return AudioArtworkFetcher(context, data, options)
            }
            return null
        }
    }
}

