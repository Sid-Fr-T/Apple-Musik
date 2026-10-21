package com.apple.musik.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.apple.musik.ui.theme.AppleMusikTheme

/**
 * 50dp profile circle placed on the top right of Home and Library screens.
 * Currently a placeholder that does nothing.
 */
@Composable
fun ProfileCircle(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(AppleMusikTheme.colors.surface)
            .border(
                width = 1.dp,
                color = AppleMusikTheme.colors.card,
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Person,
            contentDescription = "Profile",
            tint = AppleMusikTheme.colors.secondaryText,
            modifier = Modifier.size(26.dp)
        )
    }
}
