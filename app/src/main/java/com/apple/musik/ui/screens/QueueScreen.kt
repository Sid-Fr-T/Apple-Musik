package com.apple.musik.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.apple.musik.data.local.SongEntity
import com.apple.musik.ui.theme.AppleMusikTheme
import com.apple.musik.ui.theme.rememberArtworkDominantColor
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Queue Screen with:
 * - Same Palette tinted blurred background as Now Playing
 * - Header with small album art + track title & artist
 * - Shuffle and Repeat toggle buttons top right
 * - "Playing Next" header with song count
 * - Current track highlighted with Animated Equalizer Bars in accent color
 * - Drag-to-reorder handles on queue items
 * - Swipe to remove items from queue
 * - Tap to play any song in queue
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    currentSong: SongEntity?,
    queue: List<SongEntity>,
    isPlaying: Boolean,
    shuffleModeEnabled: Boolean,
    repeatMode: Int,
    onSongClick: (SongEntity) -> Unit,
    onRemoveFromQueue: (Long) -> Unit,
    onReorderQueue: (fromIndex: Int, toIndex: Int) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Palette extraction for identical Now Playing background tint
    val dominantColor = rememberArtworkDominantColor(
        context = context,
        title = currentSong?.title ?: "Apple Musik",
        albumArtUri = currentSong?.albumArtUri
    )

    val glowBrush = remember(dominantColor) {
        Brush.radialGradient(
            colors = listOf(
                dominantColor.copy(alpha = 0.55f),
                dominantColor.copy(alpha = 0.22f),
                Color.Transparent
            ),
            center = Offset(540f, 250f),
            radius = 950f
        )
    }

    BackHandler(onBack = onDismiss)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppleMusikTheme.colors.background)
            .drawBehind {
                drawRect(brush = glowBrush)
            }
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // Drag handle pill for pull down / dismiss
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 6.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount > 20f) {
                                onDismiss()
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(38.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Color.White.copy(alpha = 0.32f))
                        .clickable(onClick = onDismiss)
                )
            }

            // Top Header: Mini Art + Song Title & Artist + Shuffle/Repeat buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Collapse Queue",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Mini Album Art
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppleMusikTheme.colors.card)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(currentSong?.albumArtUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = currentSong?.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentSong?.title ?: "No Song Playing",
                            style = AppleMusikTheme.typography.titleMedium.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentSong?.artist ?: "Apple Musik",
                            style = AppleMusikTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                            color = AppleMusikTheme.colors.secondaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Top Right Action Buttons: Shuffle and Repeat
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle Toggle Button
                    Surface(
                        shape = CircleShape,
                        color = if (shuffleModeEnabled) {
                            AppleMusikTheme.colors.accent.copy(alpha = 0.20f)
                        } else {
                            Color.White.copy(alpha = 0.08f)
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clickable(onClick = onToggleShuffle)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (shuffleModeEnabled) {
                                    AppleMusikTheme.colors.accent
                                } else {
                                    AppleMusikTheme.colors.secondaryText
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Repeat Toggle Button
                    val isRepeatActive = repeatMode != Player.REPEAT_MODE_OFF
                    Surface(
                        shape = CircleShape,
                        color = if (isRepeatActive) {
                            AppleMusikTheme.colors.accent.copy(alpha = 0.20f)
                        } else {
                            Color.White.copy(alpha = 0.08f)
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clickable(onClick = onToggleRepeat)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (repeatMode == Player.REPEAT_MODE_ONE) {
                                    Icons.Rounded.RepeatOne
                                } else {
                                    Icons.Rounded.Repeat
                                },
                                contentDescription = "Repeat",
                                tint = if (isRepeatActive) {
                                    AppleMusikTheme.colors.accent
                                } else {
                                    AppleMusikTheme.colors.secondaryText
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Section Header: "Playing Next" + Queue Count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Playing Next",
                    style = AppleMusikTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )

                Text(
                    text = "${queue.size} songs",
                    style = AppleMusikTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = AppleMusikTheme.colors.secondaryText
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Queue List with Swipe to Remove and Drag to Reorder
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(
                    items = queue,
                    key = { _, song -> song.id },
                    contentType = { _, _ -> "queue_item" }
                ) { index, song ->
                    val isCurrentTrack = song.id == currentSong?.id

                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart || value == SwipeToDismissBoxValue.StartToEnd) {
                                onRemoveFromQueue(song.id)
                                true
                            } else {
                                false
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        enableDismissFromEndToStart = true,
                        backgroundContent = {
                            val color = Color(0xFFE53935)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(color)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = "Remove from Queue",
                                    tint = Color.White
                                )
                            }
                        },
                        content = {
                            QueueItemRow(
                                song = song,
                                isCurrentTrack = isCurrentTrack,
                                isPlaying = isPlaying,
                                index = index,
                                queueSize = queue.size,
                                onClick = { onSongClick(song) },
                                onReorder = onReorderQueue
                            )
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

/**
 * Individual Queue item row with:
 * - 44dp album art
 * - Track Title (highlighted in accent color if active)
 * - Artist & Album info
 * - Animated Equalizer bars icon when playing
 * - Interactive Drag-to-reorder handle
 */
@Composable
private fun QueueItemRow(
    song: SongEntity,
    isCurrentTrack: Boolean,
    isPlaying: Boolean,
    index: Int,
    queueSize: Int,
    onClick: () -> Unit,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }
    val itemHeightPx = 180f // Approximate threshold per row swap in pixels

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isCurrentTrack) {
            AppleMusikTheme.colors.surface.copy(alpha = 0.85f)
        } else {
            Color.White.copy(alpha = 0.05f)
        },
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Art Thumbnail
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppleMusikTheme.colors.card)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(song.albumArtUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = song.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title & Artist
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = AppleMusikTheme.typography.titleMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = if (isCurrentTrack) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isCurrentTrack) {
                        AppleMusikTheme.colors.accent
                    } else {
                        Color.White
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${song.artist} · ${song.album}",
                    style = AppleMusikTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    color = AppleMusikTheme.colors.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Animated Equalizer Bars when current song is active
            if (isCurrentTrack) {
                AnimatedEqualizerBars(
                    isPlaying = isPlaying,
                    barColor = AppleMusikTheme.colors.accent,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            // Drag-to-reorder handle
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .pointerInput(index, queueSize) {
                        detectVerticalDragGestures(
                            onDragStart = { accumulatedDrag = 0f },
                            onDragEnd = { accumulatedDrag = 0f },
                            onDragCancel = { accumulatedDrag = 0f },
                            onVerticalDrag = { _, dragAmount ->
                                accumulatedDrag += dragAmount
                                if (accumulatedDrag > itemHeightPx && index < queueSize - 1) {
                                    onReorder(index, index + 1)
                                    accumulatedDrag = 0f
                                } else if (accumulatedDrag < -itemHeightPx && index > 0) {
                                    onReorder(index, index - 1)
                                    accumulatedDrag = 0f
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.DragHandle,
                    contentDescription = "Reorder",
                    tint = if (isCurrentTrack) {
                        AppleMusikTheme.colors.accent
                    } else {
                        AppleMusikTheme.colors.secondaryText
                    },
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * Apple Music style Animated Equalizer bars icon (3 rhythmically dancing bars in accent color).
 */
@Composable
fun AnimatedEqualizerBars(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = AppleMusikTheme.colors.accent
) {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer")

    val h1 by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 440, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )

    val h2 by infiniteTransition.animateFloat(
        initialValue = 14f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 360, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )

    val h3 by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )

    Row(
        modifier = modifier.height(18.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val bar1Height = if (isPlaying) h1.dp else 5.dp
        val bar2Height = if (isPlaying) h2.dp else 10.dp
        val bar3Height = if (isPlaying) h3.dp else 6.dp

        Box(
            modifier = Modifier
                .width(3.dp)
                .height(bar1Height)
                .clip(RoundedCornerShape(1.5.dp))
                .background(barColor)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(bar2Height)
                .clip(RoundedCornerShape(1.5.dp))
                .background(barColor)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(bar3Height)
                .clip(RoundedCornerShape(1.5.dp))
                .background(barColor)
        )
    }
}
