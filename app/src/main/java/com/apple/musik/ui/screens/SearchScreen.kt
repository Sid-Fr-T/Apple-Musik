package com.apple.musik.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apple.musik.data.local.SongEntity
import com.apple.musik.ui.components.AlbumArtThumbnail
import com.apple.musik.ui.theme.AppleMusikTheme

/**
 * Search Screen matching Page 5 of the PDF design:
 * - Top header with "Search" title
 * - Search query input with "Cancel"
 * - Sections for Top Result, Songs, Artists
 */
@Composable
fun SearchScreen(
    songs: List<SongEntity>,
    onSongClick: (SongEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredSongs = remember(searchQuery, songs) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            songs.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                    it.artist.contains(searchQuery, ignoreCase = true) ||
                    it.album.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val topResult = filteredSongs.firstOrNull()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppleMusikTheme.colors.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            item {
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

                    // Search Input Box matching Page 5
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
                                    onValueChange = { searchQuery = it },
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
                        }

                        if (searchQuery.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = { searchQuery = "" }) {
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

            if (searchQuery.isNotBlank() && topResult != null) {
                // Top Result Section
                item {
                    Text(
                        text = "Top Result",
                        style = AppleMusikTheme.typography.headlineMedium.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = AppleMusikTheme.colors.primaryText,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(),
                                onClick = { onSongClick(topResult) }
                            )
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AlbumArtThumbnail(
                            albumArtUri = topResult.albumArtUri,
                            title = topResult.album.ifEmpty { topResult.title },
                            modifier = Modifier.size(64.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = topResult.album.ifEmpty { topResult.title },
                                style = AppleMusikTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                ),
                                color = AppleMusikTheme.colors.primaryText
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Album · ${topResult.artist}",
                                style = AppleMusikTheme.typography.bodyMedium.copy(
                                    fontSize = 13.sp
                                ),
                                color = AppleMusikTheme.colors.secondaryText
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

                // Songs Section
                item {
                    Spacer(modifier = Modifier.height(16.dp))
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

                items(filteredSongs) { song ->
                    RecentlyPlayedRowItem(
                        song = song,
                        onClick = { onSongClick(song) }
                    )
                }
            } else if (searchQuery.isBlank()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = AppleMusikTheme.colors.secondaryText.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Search your music library",
                                style = AppleMusikTheme.typography.bodyLarge,
                                color = AppleMusikTheme.colors.secondaryText
                            )
                        }
                    }
                }
            }
        }
    }
}
