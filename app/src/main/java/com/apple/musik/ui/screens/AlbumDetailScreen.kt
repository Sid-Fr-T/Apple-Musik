package com.apple.musik.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apple.musik.data.local.AlbumEntity
import com.apple.musik.data.local.SongEntity
import com.apple.musik.ui.components.AlbumArtThumbnail
import com.apple.musik.ui.theme.AppleMusikTheme

/**
 * Album details screen:
 * - Back button
 * - 230dp art with shadow
 * - Title
 * - Artist in accent color
 * - Genre · year
 * - Play and Shuffle pills
 * - Numbered track list with durations
 * - Current track highlighted
 */
@Composable
fun AlbumDetailScreen(
    album: AlbumEntity,
    tracks: List<SongEntity>,
    currentSong: SongEntity?,
    onBackClick: () -> Unit,
    onTrackClick: (SongEntity, List<SongEntity>) -> Unit,
    onPlayAllClick: (List<SongEntity>) -> Unit,
    onShuffleClick: (List<SongEntity>) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)

    val genreYearText = remember(album, tracks) {
        val genre = album.genre.ifBlank { tracks.firstOrNull { it.genre.isNotBlank() }?.genre ?: "" }
        val year = if (album.year > 0) album.year else tracks.firstOrNull { it.year > 0 }?.year ?: 0
        buildString {
            if (genre.isNotBlank()) append(genre)
            if (genre.isNotBlank() && year > 0) append(" · ")
            if (year > 0) append(year)
            if (isEmpty()) append("${tracks.size} songs")
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppleMusikTheme.colors.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 140.dp)
        ) {
            // Top App Bar with Back Button
            item(key = "album_header_bar") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = AppleMusikTheme.colors.accent,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Album Artwork (230dp with shadow)
            item(key = "album_art") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .shadow(
                                elevation = 24.dp,
                                shape = RoundedCornerShape(20.dp),
                                spotColor = Color.Black.copy(alpha = 0.65f),
                                ambientColor = Color.Black.copy(alpha = 0.35f)
                            )
                    ) {
                        AlbumArtThumbnail(
                            albumArtUri = album.artworkUri ?: tracks.firstOrNull()?.albumArtUri,
                            title = album.title,
                            modifier = Modifier.size(230.dp),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }

            // Title, Artist in accent color, Genre · Year
            item(key = "album_metadata") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = album.title,
                        style = AppleMusikTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        ),
                        color = AppleMusikTheme.colors.primaryText,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = album.artist,
                        style = AppleMusikTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        ),
                        color = AppleMusikTheme.colors.accent,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (genreYearText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = genreYearText,
                            style = AppleMusikTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp
                            ),
                            color = AppleMusikTheme.colors.secondaryText,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // Play and Shuffle Pills
            item(key = "album_action_pills") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AlbumPillButton(
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Play",
                                tint = AppleMusikTheme.colors.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        text = "Play",
                        onClick = { onPlayAllClick(tracks) },
                        modifier = Modifier.weight(1f)
                    )

                    AlbumPillButton(
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.Shuffle,
                                contentDescription = "Shuffle",
                                tint = AppleMusikTheme.colors.accent,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        text = "Shuffle",
                        onClick = { onShuffleClick(tracks) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Numbered Track List with Durations and Current Track Highlighted
            itemsIndexed(
                items = tracks,
                key = { _, song -> song.id },
                contentType = { _, _ -> "track_row" }
            ) { index, track ->
                val isCurrent = currentSong?.id == track.id
                val trackNum = if (track.trackNumber > 0) track.trackNumber else index + 1

                TrackRowItem(
                    trackNumber = trackNum,
                    song = track,
                    isCurrent = isCurrent,
                    onClick = { onTrackClick(track, tracks) }
                )

                if (index < tracks.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 58.dp, end = 20.dp),
                        thickness = 0.5.dp,
                        color = AppleMusikTheme.colors.divider.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumPillButton(
    icon: @Composable () -> Unit,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = AppleMusikTheme.colors.surface,
        modifier = modifier.height(44.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            icon()
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = AppleMusikTheme.typography.titleSmall.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = AppleMusikTheme.colors.accent
            )
        }
    }
}

@Composable
private fun TrackRowItem(
    trackNumber: Int,
    song: SongEntity,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isCurrent) AppleMusikTheme.colors.surface.copy(alpha = 0.6f)
                else Color.Transparent
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track number or playing equalizer indicator
        Box(
            modifier = Modifier.width(28.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (isCurrent) {
                Icon(
                    imageVector = Icons.Rounded.GraphicEq,
                    contentDescription = "Playing",
                    tint = AppleMusikTheme.colors.accent,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Text(
                    text = "$trackNumber",
                    style = AppleMusikTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = AppleMusikTheme.colors.secondaryText
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Track title
        Text(
            text = song.title,
            style = AppleMusikTheme.typography.bodyLarge.copy(
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                fontSize = 16.sp
            ),
            color = if (isCurrent) AppleMusikTheme.colors.accent else AppleMusikTheme.colors.primaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Duration
        Text(
            text = formatDuration(song.duration),
            style = AppleMusikTheme.typography.bodyMedium.copy(
                fontSize = 13.sp
            ),
            color = if (isCurrent) AppleMusikTheme.colors.accent else AppleMusikTheme.colors.secondaryText
        )
    }
}

