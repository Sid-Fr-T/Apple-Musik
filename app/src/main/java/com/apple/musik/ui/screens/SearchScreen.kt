package com.apple.musik.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apple.musik.data.local.AlbumEntity
import com.apple.musik.data.local.ArtistEntity
import com.apple.musik.data.local.SongEntity
import com.apple.musik.ui.components.AlbumArtThumbnail
import com.apple.musik.ui.components.getArtPaletteFor
import com.apple.musik.ui.theme.AppleMusikTheme

sealed interface SearchTopResult {
    data class Song(val song: SongEntity) : SearchTopResult
    data class Album(val album: AlbumEntity) : SearchTopResult
    data class Artist(val artist: ArtistEntity) : SearchTopResult
}

/**
 * Search Screen:
 * - Large "Search" title
 * - Search field with Cancel
 * - Live results as you type from Room:
 *   - Top Result
 *   - Songs
 *   - Albums
 *   - Artists
 * - Tapping a result plays or opens it.
 */
@Composable
fun SearchScreen(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    songs: List<SongEntity>,
    albums: List<AlbumEntity>,
    artists: List<ArtistEntity>,
    onSongClick: (SongEntity, List<SongEntity>) -> Unit,
    onAlbumClick: (AlbumEntity) -> Unit,
    onArtistClick: (ArtistEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val trimmedQuery = searchQuery.trim()

    // Determine Top Result with priority: Exact match -> Prefix match -> First available item
    val topResult: SearchTopResult? = remember(trimmedQuery, songs, albums, artists) {
        if (trimmedQuery.isBlank()) return@remember null

        // 1. Exact match check
        artists.firstOrNull { it.name.equals(trimmedQuery, ignoreCase = true) }?.let {
            return@remember SearchTopResult.Artist(it)
        }
        albums.firstOrNull { it.title.equals(trimmedQuery, ignoreCase = true) }?.let {
            return@remember SearchTopResult.Album(it)
        }
        songs.firstOrNull { it.title.equals(trimmedQuery, ignoreCase = true) }?.let {
            return@remember SearchTopResult.Song(it)
        }

        // 2. Prefix match check
        artists.firstOrNull { it.name.startsWith(trimmedQuery, ignoreCase = true) }?.let {
            return@remember SearchTopResult.Artist(it)
        }
        albums.firstOrNull { it.title.startsWith(trimmedQuery, ignoreCase = true) }?.let {
            return@remember SearchTopResult.Album(it)
        }
        songs.firstOrNull { it.title.startsWith(trimmedQuery, ignoreCase = true) }?.let {
            return@remember SearchTopResult.Song(it)
        }

        // 3. Fallback to first available result
        when {
            songs.isNotEmpty() -> SearchTopResult.Song(songs.first())
            albums.isNotEmpty() -> SearchTopResult.Album(albums.first())
            artists.isNotEmpty() -> SearchTopResult.Artist(artists.first())
            else -> null
        }
    }

    val hasAnyResults = topResult != null || songs.isNotEmpty() || albums.isNotEmpty() || artists.isNotEmpty()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppleMusikTheme.colors.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 140.dp)
        ) {
            // Header with Large "Search" title + Search Field with Cancel
            item(key = "search_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp)
                ) {
                    Text(
                        text = "Search",
                        style = AppleMusikTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 34.sp
                        ),
                        color = AppleMusikTheme.colors.primaryText
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Search Input Box
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppleMusikTheme.colors.surface)
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Search",
                                tint = AppleMusikTheme.colors.secondaryText,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))

                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Artists, Songs, Lyrics, and More",
                                        style = AppleMusikTheme.typography.bodyMedium.copy(
                                            fontSize = 15.sp
                                        ),
                                        color = AppleMusikTheme.colors.secondaryText
                                    )
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = onQueryChange,
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = AppleMusikTheme.colors.primaryText,
                                        fontSize = 15.sp,
                                        fontFamily = AppleMusikTheme.typography.bodyLarge.fontFamily
                                    ),
                                    cursorBrush = SolidColor(AppleMusikTheme.colors.accent),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { onQueryChange("") },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Clear",
                                        tint = AppleMusikTheme.colors.secondaryText,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        if (searchQuery.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                onClick = { onQueryChange("") },
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = "Cancel",
                                    style = AppleMusikTheme.typography.bodyLarge.copy(
                                        color = AppleMusikTheme.colors.accent,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Empty state when no query entered
            if (trimmedQuery.isBlank()) {
                item(key = "search_placeholder") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = AppleMusikTheme.colors.secondaryText.copy(alpha = 0.4f),
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Search your music library",
                                style = AppleMusikTheme.typography.bodyLarge.copy(
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = AppleMusikTheme.colors.secondaryText
                            )
                        }
                    }
                }
            } else if (!hasAnyResults) {
                // No results state
                item(key = "no_results") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No Results",
                                style = AppleMusikTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = AppleMusikTheme.colors.primaryText
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Couldn't find any results for \"$trimmedQuery\"",
                                style = AppleMusikTheme.typography.bodyMedium,
                                color = AppleMusikTheme.colors.secondaryText
                            )
                        }
                    }
                }
            } else {
                // 1. Top Result Section
                if (topResult != null) {
                    item(key = "header_top_result") {
                        Text(
                            text = "Top Result",
                            style = AppleMusikTheme.typography.headlineMedium.copy(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = AppleMusikTheme.colors.primaryText,
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 10.dp)
                        )

                        when (topResult) {
                            is SearchTopResult.Song -> {
                                val song = topResult.song
                                TopResultRow(
                                    title = song.title,
                                    subtitle = "Song · ${song.artist}",
                                    artworkUri = song.albumArtUri,
                                    artSeed = song.album.ifEmpty { song.title },
                                    isArtist = false,
                                    onClick = { onSongClick(song, songs) }
                                )
                            }
                            is SearchTopResult.Album -> {
                                val album = topResult.album
                                TopResultRow(
                                    title = album.title,
                                    subtitle = "Album · ${album.artist}",
                                    artworkUri = album.artworkUri,
                                    artSeed = album.title,
                                    isArtist = false,
                                    onClick = { onAlbumClick(album) }
                                )
                            }
                            is SearchTopResult.Artist -> {
                                val artist = topResult.artist
                                TopResultRow(
                                    title = artist.name,
                                    subtitle = "Artist",
                                    artworkUri = null,
                                    artSeed = artist.name,
                                    isArtist = true,
                                    onClick = { onArtistClick(artist) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // 2. Songs Section
                if (songs.isNotEmpty()) {
                    item(key = "header_songs") {
                        Text(
                            text = "Songs",
                            style = AppleMusikTheme.typography.headlineMedium.copy(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = AppleMusikTheme.colors.primaryText,
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp)
                        )
                    }

                    items(
                        items = songs,
                        key = { "song_${it.id}" },
                        contentType = { "song_row" }
                    ) { song ->
                        RecentlyPlayedRowItem(
                            song = song,
                            onClick = { onSongClick(song, songs) }
                        )
                    }

                    item(key = "spacer_after_songs") {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // 3. Albums Section
                if (albums.isNotEmpty()) {
                    item(key = "header_albums") {
                        Text(
                            text = "Albums",
                            style = AppleMusikTheme.typography.headlineMedium.copy(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = AppleMusikTheme.colors.primaryText,
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp)
                        )
                    }

                    items(
                        items = albums,
                        key = { "album_${it.id}" },
                        contentType = { "album_row" }
                    ) { album ->
                        AlbumSearchRowItem(
                            album = album,
                            onClick = { onAlbumClick(album) }
                        )
                    }

                    item(key = "spacer_after_albums") {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // 4. Artists Section
                if (artists.isNotEmpty()) {
                    item(key = "header_artists") {
                        Text(
                            text = "Artists",
                            style = AppleMusikTheme.typography.headlineMedium.copy(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = AppleMusikTheme.colors.primaryText,
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp)
                        )
                    }

                    items(
                        items = artists,
                        key = { "artist_${it.id}" },
                        contentType = { "artist_row" }
                    ) { artist ->
                        ArtistSearchRowItem(
                            artist = artist,
                            onClick = { onArtistClick(artist) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopResultRow(
    title: String,
    subtitle: String,
    artworkUri: String?,
    artSeed: String,
    isArtist: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isArtist) {
            ArtistAvatar(
                name = title,
                size = 64.dp
            )
        } else {
            AlbumArtThumbnail(
                albumArtUri = artworkUri,
                title = artSeed,
                modifier = Modifier.size(64.dp),
                shape = RoundedCornerShape(12.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = AppleMusikTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                ),
                color = AppleMusikTheme.colors.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = subtitle,
                style = AppleMusikTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp
                ),
                color = AppleMusikTheme.colors.secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = AppleMusikTheme.colors.secondaryText,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun AlbumSearchRowItem(
    album: AlbumEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumArtThumbnail(
            albumArtUri = album.artworkUri,
            title = album.title,
            modifier = Modifier.size(54.dp),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.title,
                style = AppleMusikTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = AppleMusikTheme.colors.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            val subtitle = buildString {
                append("Album · ")
                append(album.artist)
                if (album.year > 0) {
                    append(" · ")
                    append(album.year)
                }
            }

            Text(
                text = subtitle,
                style = AppleMusikTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp
                ),
                color = AppleMusikTheme.colors.secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = AppleMusikTheme.colors.secondaryText,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun ArtistSearchRowItem(
    artist: ArtistEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArtistAvatar(
            name = artist.name,
            size = 50.dp
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artist.name,
                style = AppleMusikTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = AppleMusikTheme.colors.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            val countText = if (artist.trackCount > 0) "${artist.trackCount} songs" else "Artist"
            Text(
                text = "Artist · $countText",
                style = AppleMusikTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp
                ),
                color = AppleMusikTheme.colors.secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = AppleMusikTheme.colors.secondaryText,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun ArtistAvatar(
    name: String,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val (primaryColor, _) = remember(name) { getArtPaletteFor(name) }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(primaryColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Person,
            contentDescription = name,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(size * 0.55f)
        )
    }
}
