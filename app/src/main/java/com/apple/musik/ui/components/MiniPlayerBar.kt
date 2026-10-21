package com.apple.musik.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apple.musik.ui.theme.AppleMusikTheme

/**
 * Floating mini player bar (60dp high, 16dp side margins, radius 16dp, color #212126).
 * Sits directly above the bottom tab bar.
 * Controls: Previous, Play/Pause, Next (total 3 buttons).
 */
@Composable
fun MiniPlayerBar(
    modifier: Modifier = Modifier,
    title: String = "Slow Bloom",
    artist: String = "Aria Vale",
    albumArtUri: String? = null,
    onPreviousClick: () -> Unit = {},
    onPlayPauseClick: () -> Unit = {},
    onNextClick: () -> Unit = {}
) {
    var isPlaying by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .height(60.dp)
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color.Black.copy(alpha = 0.5f),
                ambientColor = Color.Black.copy(alpha = 0.3f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF212126))
            .padding(start = 10.dp, end = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Artwork matching the PDF
            AlbumArtThumbnail(
                albumArtUri = albumArtUri,
                title = title,
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Track Info: Title & Artist
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = AppleMusikTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    ),
                    color = AppleMusikTheme.colors.primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = artist,
                    style = AppleMusikTheme.typography.bodyMedium.copy(
                        fontSize = 12.sp
                    ),
                    color = AppleMusikTheme.colors.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Controls: Previous, Play/Pause, Next (Total 3 buttons)
            IconButton(
                onClick = onPreviousClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = "Previous Track",
                    tint = AppleMusikTheme.colors.primaryText,
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(
                onClick = {
                    isPlaying = !isPlaying
                    onPlayPauseClick()
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = AppleMusikTheme.colors.primaryText,
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(
                onClick = onNextClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = "Next Track",
                    tint = AppleMusikTheme.colors.primaryText,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
