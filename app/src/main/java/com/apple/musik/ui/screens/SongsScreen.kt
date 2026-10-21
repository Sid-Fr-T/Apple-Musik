package com.apple.musik.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.apple.musik.data.datastore.SortOption
import com.apple.musik.data.local.SongEntity
import com.apple.musik.ui.components.AlbumArtThumbnail
import com.apple.musik.ui.components.ProfileCircle
import com.apple.musik.ui.theme.AppleMusikTheme
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

/**
 * Library > Songs Screen (Page 3 & 4 of PDF):
 * - Large "Songs" title with subtitle "N songs · On device"
 * - Profile circle (50dp) on the top right
 * - Three equal pill buttons: Play, Shuffle, Sort (Surface #1E1E23, accent text/icon)
 * - Sort button opens dropdown menu (fade + scale in) with: Name (A-Z), Modified Date, Date Added
 * - Song rows: 48dp art, title, artist · album
 * - A-Z fast-scroll index on the right edge
 */
@Composable
fun SongsScreen(
    songs: List<SongEntity>,
    activeSort: SortOption,
    onSortOptionSelected: (SortOption) -> Unit,
    onSongClick: (SongEntity) -> Unit = {},
    onPlayAllClick: () -> Unit = {},
    onShuffleClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var isSortMenuVisible by remember { mutableStateOf(false) }

    // Alphabet fast scroll index mapping
    val alphabet = ('A'..'Z').toList()
    val letterPositions = remember(songs) {
        val map = mutableMapOf<Char, Int>()
        songs.forEachIndexed { index, song ->
            val firstChar = song.title.firstOrNull()?.uppercaseChar() ?: '#'
            if (firstChar in 'A'..'Z' && !map.containsKey(firstChar)) {
                map[firstChar] = index
            }
        }
        map
    }

    val formattedSongCount = remember(songs.size) {
        NumberFormat.getNumberInstance(Locale.US).format(songs.size)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppleMusikTheme.colors.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header: Title & Subtitle + Top-Right Profile Circle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Songs",
                        style = AppleMusikTheme.typography.displayLarge.copy(
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = AppleMusikTheme.colors.primaryText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$formattedSongCount songs · On device",
                        style = AppleMusikTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp
                        ),
                        color = AppleMusikTheme.colors.secondaryText
                    )
                }

                ProfileCircle()
            }

            // 3 Equal Pill Buttons in One Row: Play, Shuffle, Sort
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PillButton(
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Play",
                            tint = AppleMusikTheme.colors.accent,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    text = "Play",
                    onClick = onPlayAllClick,
                    modifier = Modifier.weight(1f)
                )

                PillButton(
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = "Shuffle",
                            tint = AppleMusikTheme.colors.accent,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    text = "Shuffle",
                    onClick = onShuffleClick,
                    modifier = Modifier.weight(1f)
                )

                Box(modifier = Modifier.weight(1f)) {
                    PillButton(
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.SwapVert,
                                contentDescription = "Sort",
                                tint = AppleMusikTheme.colors.accent,
                                modifier = Modifier.size(19.dp)
                            )
                        },
                        text = "Sort",
                        onClick = { isSortMenuVisible = !isSortMenuVisible },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Sorted Subtitle (matching Page 3: "Sorted by Title")
            Text(
                text = activeSort.headerText,
                style = AppleMusikTheme.typography.titleSmall.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = AppleMusikTheme.colors.accent,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 4.dp)
            )

            // Song List + Fast Scroll Alphabet Index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(
                        items = songs,
                        key = { _, song -> song.id }
                    ) { _, song ->
                        SongRowItem(
                            song = song,
                            onClick = { onSongClick(song) }
                        )
                    }

                    // Spacer at bottom so last item is not hidden
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // A-Z Fast-Scroll Index on the right edge
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    alphabet.forEach { letter ->
                        val hasLetter = letterPositions.containsKey(letter)
                        Text(
                            text = letter.toString(),
                            style = AppleMusikTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (hasLetter) AppleMusikTheme.colors.accent else AppleMusikTheme.colors.secondaryText.copy(alpha = 0.5f),
                            modifier = Modifier
                                .padding(vertical = 1.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    enabled = hasLetter
                                ) {
                                    val targetIndex = letterPositions[letter] ?: 0
                                    coroutineScope.launch {
                                        listState.scrollToItem(targetIndex)
                                    }
                                }
                        )
                    }
                }
            }
        }

        // Sort Dropdown Menu (Fade + Scale In from Sort button)
        AnimatedVisibility(
            visible = isSortMenuVisible,
            enter = fadeIn(tween(160)) + scaleIn(
                initialScale = 0.85f,
                transformOrigin = TransformOrigin(0.85f, 0.15f),
                animationSpec = tween(180)
            ),
            exit = fadeOut(tween(120)) + scaleOut(
                targetScale = 0.85f,
                transformOrigin = TransformOrigin(0.85f, 0.15f),
                animationSpec = tween(140)
            ),
            modifier = Modifier
                .zIndex(20f)
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 110.dp, end = 20.dp)
        ) {
            SortDropdownMenu(
                activeSort = activeSort,
                onSelect = { option ->
                    onSortOptionSelected(option)
                    isSortMenuVisible = false
                }
            )
        }
    }
}

/**
 * Three equal pill buttons in one row (Surface #1E1E23, accent text/icon).
 */
@Composable
fun PillButton(
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
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            icon()
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = AppleMusikTheme.typography.titleSmall.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = AppleMusikTheme.colors.accent
            )
        }
    }
}

/**
 * Sort Dropdown Menu matching Page 4 of PDF:
 * - Name (A-Z)
 * - Modified Date
 * - Date Added
 * With checkmark showing the active one.
 */
@Composable
fun SortDropdownMenu(
    activeSort: SortOption,
    onSelect: (SortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF222228),
        shadowElevation = 16.dp,
        modifier = modifier
            .width(200.dp)
            .border(
                width = 1.dp,
                color = Color(0xFF323238),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp)
        ) {
            SortOption.entries.forEach { option ->
                val isSelected = option == activeSort

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(),
                            onClick = { onSelect(option) }
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = option.title,
                        style = AppleMusikTheme.typography.bodyLarge.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 15.sp
                        ),
                        color = if (isSelected) AppleMusikTheme.colors.accent else AppleMusikTheme.colors.primaryText
                    )

                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Selected",
                            tint = AppleMusikTheme.colors.accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Song row item (48dp art, title, artist · album).
 */
@Composable
fun SongRowItem(
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
        // 48dp Album Artwork
        AlbumArtThumbnail(
            albumArtUri = song.albumArtUri,
            title = song.title,
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        // Title and Subtitle (Artist · Album)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 24.dp)
        ) {
            Text(
                text = song.title,
                style = AppleMusikTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                ),
                color = AppleMusikTheme.colors.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = "${song.artist} · ${song.album}",
                style = AppleMusikTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp
                ),
                color = AppleMusikTheme.colors.secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
