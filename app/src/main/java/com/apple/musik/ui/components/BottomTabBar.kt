package com.apple.musik.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apple.musik.ui.navigation.NavigationTab
import com.apple.musik.ui.theme.AppleMusikTheme

/**
 * Custom bottom tab bar with 3 tabs: Home, Library, Search.
 * Strictly adheres to the PDF visual design:
 * - Active tab: accent color (#FF5A3C)
 * - Inactive tab: secondary text (#A1A1AA)
 * - Custom Apple Music style library icon (|| \)
 */
@Composable
fun BottomTabBar(
    selectedTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val dividerColor = AppleMusikTheme.colors.divider

    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = dividerColor,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .background(AppleMusikTheme.colors.surface)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavigationTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                val tabColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        AppleMusikTheme.colors.accent
                    } else {
                        AppleMusikTheme.colors.secondaryText
                    },
                    label = "tabColor"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false, radius = 28.dp),
                            onClick = { onTabSelected(tab) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        when (tab) {
                            NavigationTab.Home -> {
                                Icon(
                                    imageVector = Icons.Rounded.Home,
                                    contentDescription = tab.title,
                                    tint = tabColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            NavigationTab.Library -> {
                                LibraryTabIcon(
                                    tint = tabColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            NavigationTab.Search -> {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = tab.title,
                                    tint = tabColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tab.title,
                            style = AppleMusikTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            ),
                            color = tabColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LibraryTabIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 2.4.dp.toPx()
        // Line 1: vertical
        drawLine(
            color = tint,
            start = Offset(size.width * 0.32f, size.height * 0.20f),
            end = Offset(size.width * 0.32f, size.height * 0.78f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        // Line 2: vertical
        drawLine(
            color = tint,
            start = Offset(size.width * 0.50f, size.height * 0.20f),
            end = Offset(size.width * 0.50f, size.height * 0.78f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        // Line 3: slanted / tilted right
        drawLine(
            color = tint,
            start = Offset(size.width * 0.65f, size.height * 0.22f),
            end = Offset(size.width * 0.80f, size.height * 0.78f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}
