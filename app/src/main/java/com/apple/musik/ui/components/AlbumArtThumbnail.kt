package com.apple.musik.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlin.math.absoluteValue

// PDF Art Palettes
val ArtPalettes = listOf(
    Pair(Color(0xFFD65022), Color(0xFFA83C15)), // Orange (Slow Bloom)
    Pair(Color(0xFF2357D6), Color(0xFF183EA0)), // Blue (Neon Tides)
    Pair(Color(0xFF5C28C6), Color(0xFF3F1991)), // Purple (Paper Planes)
    Pair(Color(0xFF169C56), Color(0xFF0F6B3B)), // Green (Low Light)
    Pair(Color(0xFFC41A68), Color(0xFF891147)), // Magenta (Quiet Orbit)
    Pair(Color(0xFFD98A19), Color(0xFF9B610F)), // Amber (Glass Hours)
    Pair(Color(0xFF1A84A1), Color(0xFF125D73)), // Teal (Tidal)
    Pair(Color(0xFF1886A8), Color(0xFF115E77))  // Cyan (Golden Static)
)

fun getArtPaletteFor(seed: String): Pair<Color, Color> {
    val index = (seed.hashCode().absoluteValue) % ArtPalettes.size
    return ArtPalettes[index]
}

/**
 * Highly optimized album art renderer:
 * - Uses ImageRequest with crossfade for smooth image loading.
 * - Draws abstract geometry directly on background via drawBehind,
 *   reducing Compose layout tree node count for every list item.
 */
@Composable
fun AlbumArtThumbnail(
    albumArtUri: String?,
    title: String,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(10.dp)
) {
    val context = LocalContext.current
    val (primaryColor, circleColor) = remember(title) { getArtPaletteFor(title) }
    val isRealImage = remember(albumArtUri) {
        !albumArtUri.isNullOrEmpty() && !albumArtUri.startsWith("content://demo/")
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(primaryColor)
            .drawBehind {
                if (!isRealImage) {
                    val radius = size.minDimension * 0.65f
                    val centerOffset = Offset(
                        x = size.width * 0.85f,
                        y = size.height * 0.85f
                    )
                    drawCircle(
                        color = circleColor,
                        radius = radius,
                        center = centerOffset
                    )
                }
            }
    ) {
        if (isRealImage) {
            val imageRequest = remember(albumArtUri) {
                ImageRequest.Builder(context)
                    .data(albumArtUri)
                    .crossfade(200)
                    .build()
            }
            AsyncImage(
                model = imageRequest,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
