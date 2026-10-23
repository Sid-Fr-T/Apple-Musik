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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apple.musik.data.local.SongEntity
import com.apple.musik.ui.components.AlbumArtThumbnail
import com.apple.musik.ui.components.ProfileCircle
import com.apple.musik.ui.theme.AppleMusikTheme
import java.util.concurrent.TimeUnit

/**
 * Home Screen strictly matching Page 1 of the PDF design:
 * - Top header with "Home" title and 50dp profile circle in top right
 * - "Recently Added" horizontal scroll row of album cards
 * - "Recently Played" vertical list of song items with duration
 */
@Composable
fun HomeScreen(
    recentlyAdded: List<SongEntity>,
    recentlyPlayed: List<SongEntity>,
    onSongClick: (SongEntity) -> Unit = {},
    onAlbumClick: (SongEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppleMusikTheme.colors.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Header Row: "Home" Title + Top-Right Profile Circle
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Home",
                        style = AppleMusikTheme.typography.displayLarge.copy(
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = AppleMusikTheme.colors.primaryText
                    )
                    ProfileCircle()
                }
            }

            // "Recently Added" Section Header
            item {
                Text(
                    text = "Recently Added",
                    style = AppleMusikTheme.typography.headlineMedium.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = AppleMusikTheme.colors.primaryText,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 14.dp)
                )
            }

            // "Recently Added" Horizontal Row of Album Cards
            item(key = "header_recently_added", contentType = "header") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(
                        items = recentlyAdded,
                        key = { "recent_added_${it.id}" },
                        contentType = { "album_card" }
                    ) { song ->
                        AlbumCardItem(
                            song = song,
                            onClick = { onAlbumClick(song) }
                        )
                    }
                }
            }

            // "Recently Played" Section Header
            item(key = "header_recently_played", contentType = "header") {
                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    text = "Recently Played",
                    style = AppleMusikTheme.typography.headlineMedium.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = AppleMusikTheme.colors.primaryText,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp)
                )
            }

            // "Recently Played" Vertical List
            items(
                items = recentlyPlayed,
                key = { "recent_played_${it.id}" },
                contentType = { "song_row" }
            ) { song ->
                RecentlyPlayedRowItem(
                    song = song,
                    onClick = { onSongClick(song) }
                )
            }
        }
    }
}

/**
 * Album card matching Page 1 horizontal scroll cards:
 * ~160dp width artwork, title (bold), artist (secondary text).
 */
@Composable
fun AlbumCardItem(
    song: SongEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(160.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick
            )
    ) {
        AlbumArtThumbnail(
            albumArtUri = song.albumArtUri,
            title = song.album.ifEmpty { song.title },
            modifier = Modifier
                .size(160.dp),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = song.album.ifEmpty { song.title },
            style = AppleMusikTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            ),
            color = AppleMusikTheme.colors.primaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = song.artist,
            style = AppleMusikTheme.typography.bodyMedium.copy(
                fontSize = 13.sp
            ),
            color = AppleMusikTheme.colors.secondaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Recently Played item matching Page 1:
 * 48dp artwork, title (bold), artist (secondary), and duration (right aligned).
 */
@Composable
fun RecentlyPlayedRowItem(
    song: SongEntity,
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
            albumArtUri = song.albumArtUri,
            title = song.title,
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                style = AppleMusikTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = AppleMusikTheme.colors.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = song.artist,
                style = AppleMusikTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp
                ),
                color = AppleMusikTheme.colors.secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = formatDuration(song.duration),
            style = AppleMusikTheme.typography.bodyMedium.copy(
                fontSize = 13.sp
            ),
            color = AppleMusikTheme.colors.secondaryText
        )
    }
}

fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0) return "0:00"
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
    return String.format("%d:%02d", minutes, seconds)
}
