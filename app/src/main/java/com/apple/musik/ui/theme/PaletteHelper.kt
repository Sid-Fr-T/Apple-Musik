package com.apple.musik.ui.theme

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import com.apple.musik.ui.components.getArtPaletteFor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Extracts the dominant/vibrant artwork color using Palette for real artwork,
 * or the PDF's signature art palette color for fallback/demo tracks.
 */
@Composable
fun rememberArtworkDominantColor(
    context: Context,
    title: String,
    albumArtUri: String?
): Color {
    val defaultColor = remember(title) { getArtPaletteFor(title).first }
    val dominantColorState = remember(title, albumArtUri) { mutableStateOf(defaultColor) }

    LaunchedEffect(title, albumArtUri) {
        dominantColorState.value = getArtPaletteFor(title).first

        if (!albumArtUri.isNullOrEmpty() && !albumArtUri.startsWith("content://demo")) {
            withContext(Dispatchers.IO) {
                try {
                    val uri = Uri.parse(albumArtUri)
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val options = BitmapFactory.Options().apply { inSampleSize = 4 }
                        val bitmap = BitmapFactory.decodeStream(stream, null, options)
                        if (bitmap != null) {
                            val palette = Palette.from(bitmap).generate()
                            val swatch = palette.vibrantSwatch
                                ?: palette.dominantSwatch
                                ?: palette.mutedSwatch
                            if (swatch != null) {
                                dominantColorState.value = Color(swatch.rgb)
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Retain default color
                }
            }
        }
    }

    return dominantColorState.value
}
