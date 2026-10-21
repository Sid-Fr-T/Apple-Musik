package com.apple.musik.ui.screens

import android.content.Context
import android.media.AudioManager
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apple.musik.data.local.SongEntity
import com.apple.musik.ui.components.AlbumArtThumbnail
import com.apple.musik.ui.theme.AppleMusikTheme
import com.apple.musik.ui.theme.rememberArtworkDominantColor
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Now Playing Screen strictly adhering to Page 2 of the PDF design:
 * - Fluid spring-based pull-down handle gesture
 * - Cached ambient background glow via Palette
 * - Large rounded album art with soft shadow & Hero animation
 * - Title, artist, and "..." menu button
 * - Sleek thin scrubber with elapsed and negative remaining time (-mm:ss)
 * - Big playback controls (Previous, Play/Pause, Next)
 * - Volume slider with speaker low & high icons
 * - Bottom row: Lyrics button, "On device · FLAC" chip, Queue button
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NowPlayingScreen(
    song: SongEntity?,
    isPlaying: Boolean,
    currentPosition: Long,
    totalDuration: Long,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onCollapse: () -> Unit,
    onQueueClick: () -> Unit = {},
    isQueueOpen: Boolean = false,
    modifier: Modifier = Modifier,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val title = song?.title ?: "Slow Bloom"
    val artist = song?.artist ?: "Aria Vale"
    val albumArtUri = song?.albumArtUri

    val dominantColor = rememberArtworkDominantColor(
        context = context,
        title = title,
        albumArtUri = albumArtUri
    )

    // Cached ambient glow brush
    val radialGlowBrush = remember(dominantColor) {
        Brush.radialGradient(
            colors = listOf(
                dominantColor.copy(alpha = 0.55f),
                dominantColor.copy(alpha = 0.22f),
                Color.Transparent
            )
        )
    }

    // Spring-animated vertical drag-to-dismiss gesture
    val dragAnimatable = remember { Animatable(0f) }

    // Volume state via AudioManager
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat() }
    val currentVolume = remember { audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() }
    var volumeFraction by remember { mutableFloatStateOf((currentVolume / maxVolume).coerceIn(0f, 1f)) }

    // Scrubber dragging state
    var isDraggingScrubber by remember { mutableStateOf(false) }
    var dragPositionMs by remember { mutableFloatStateOf(0f) }

    val duration = totalDuration.takeIf { it > 0 } ?: (song?.duration?.takeIf { it > 0 } ?: 201_000L)
    val effectivePosition = if (isDraggingScrubber) dragPositionMs.toLong() else currentPosition
    val progressFraction = (effectivePosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    val remainingMs = (duration - effectivePosition).coerceAtLeast(0L)

    Box(
        modifier = modifier
            .fillMaxSize()
            .offset { IntOffset(0, dragAnimatable.value.roundToInt().coerceAtLeast(0)) }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dragAnimatable.value > 130f) {
                            onCollapse()
                        } else {
                            coroutineScope.launch {
                                dragAnimatable.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = 0.8f,
                                        stiffness = 450f
                                    )
                                )
                            }
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            dragAnimatable.animateTo(0f, spring(0.8f, 450f))
                        }
                    },
                    onVerticalDrag = { _, dragAmount ->
                        if (dragAmount > 0 || dragAnimatable.value > 0) {
                            coroutineScope.launch {
                                dragAnimatable.snapTo((dragAnimatable.value + dragAmount).coerceAtLeast(0f))
                            }
                        }
                    }
                )
            }
            .background(AppleMusikTheme.colors.background)
            .drawBehind {
                drawCircle(
                    brush = radialGlowBrush,
                    center = Offset(size.width * 0.5f, size.height * 0.28f),
                    radius = size.width * 0.95f
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Grab / Pull-Down Handle
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 18.dp)
                    .width(38.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.35f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onCollapse
                    )
            )

            // Large Rounded Artwork with Soft Shadow & Hero Animation
            val heroArtModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                with(sharedTransitionScope) {
                    Modifier.sharedElement(
                        sharedContentState = rememberSharedContentState(key = "now_playing_art_${song?.id ?: 0}"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                }
            } else {
                Modifier
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .aspectRatio(1f)
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(28.dp),
                        spotColor = Color.Black.copy(alpha = 0.6f),
                        ambientColor = Color.Black.copy(alpha = 0.3f)
                    )
                    .then(heroArtModifier)
            ) {
                AlbumArtThumbnail(
                    albumArtUri = albumArtUri,
                    title = title,
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Track Info Row: Title, Artist, and "..." Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = AppleMusikTheme.typography.headlineLarge.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = artist,
                        style = AppleMusikTheme.typography.titleMedium.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = AppleMusikTheme.colors.secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Options "..." Button
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreHoriz,
                            contentDescription = "Options",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Custom Smooth Thin Scrubber Progress Bar
            CustomThinProgressBar(
                progressFraction = progressFraction,
                onSeekFraction = { frac ->
                    val newMs = (frac * duration).toLong()
                    onSeek(newMs)
                },
                onDragFraction = { frac ->
                    isDraggingScrubber = true
                    dragPositionMs = frac * duration
                },
                onDragFinished = {
                    onSeek(dragPositionMs.toLong())
                    isDraggingScrubber = false
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Scrubber Time Labels: Elapsed (left) and Remaining (right with negative sign)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(effectivePosition),
                    style = AppleMusikTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    color = AppleMusikTheme.colors.secondaryText
                )
                Text(
                    text = "-${formatTime(remainingMs)}",
                    style = AppleMusikTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    color = AppleMusikTheme.colors.secondaryText
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Big Playback Controls (Previous, Play/Pause, Next)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPreviousClick,
                    modifier = Modifier.size(54.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }

                IconButton(
                    onClick = onPlayPauseClick,
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(54.dp)
                    )
                }

                IconButton(
                    onClick = onNextClick,
                    modifier = Modifier.size(54.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Volume Slider with Speaker Icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.VolumeDown,
                    contentDescription = "Volume Down",
                    tint = AppleMusikTheme.colors.secondaryText,
                    modifier = Modifier.size(20.dp)
                )

                CustomThinProgressBar(
                    progressFraction = volumeFraction,
                    onSeekFraction = { frac ->
                        volumeFraction = frac
                        val targetVol = (frac * maxVolume).roundToInt()
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                    },
                    onDragFraction = { frac ->
                        volumeFraction = frac
                        val targetVol = (frac * maxVolume).roundToInt()
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                    },
                    onDragFinished = {},
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp)
                )

                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                    contentDescription = "Volume Up",
                    tint = AppleMusikTheme.colors.secondaryText,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom Action Row: Lyrics Button, "On device · FLAC" Chip, Queue Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Rounded.ChatBubbleOutline,
                        contentDescription = "Lyrics",
                        tint = AppleMusikTheme.colors.secondaryText,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Format Chip
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier.height(34.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "On device · FLAC",
                            style = AppleMusikTheme.typography.bodyMedium.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = Color.White
                        )
                    }
                }

                IconButton(onClick = onQueueClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                        contentDescription = "Queue",
                        tint = if (isQueueOpen) AppleMusikTheme.colors.accent else AppleMusikTheme.colors.secondaryText,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/**
 * Custom thin progress bar (Apple Music style: thin, tactile, smooth dragging).
 */
@Composable
fun CustomThinProgressBar(
    progressFraction: Float,
    onSeekFraction: (Float) -> Unit,
    onDragFraction: (Float) -> Unit,
    onDragFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var widthPx by remember { mutableFloatStateOf(1f) }

    Box(
        modifier = modifier
            .height(28.dp)
            .onGloballyPositioned { widthPx = it.size.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val frac = (offset.x / widthPx).coerceIn(0f, 1f)
                    onSeekFraction(frac)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        val frac = (offset.x / widthPx).coerceIn(0f, 1f)
                        onDragFraction(frac)
                    },
                    onDragEnd = { onDragFinished() },
                    onDragCancel = { onDragFinished() },
                    onHorizontalDrag = { change, _ ->
                        val frac = (change.position.x / widthPx).coerceIn(0f, 1f)
                        onDragFraction(frac)
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // Track Background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.22f))
        )

        // Active Track
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = progressFraction)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White)
        )
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
