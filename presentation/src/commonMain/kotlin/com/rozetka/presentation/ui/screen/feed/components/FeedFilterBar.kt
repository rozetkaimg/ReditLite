package com.rozetka.presentation.ui.screen.feed.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rozetka.domain.model.FeedType
import com.rozetka.presentation.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun FeedFilterBar(
    selectedFeedType: FeedType,
    onFeedTypeSelected: (FeedType) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        LazyRow(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(FeedType.entries.size) { index ->
                val feedType = FeedType.entries[index]
                val isSelected = selectedFeedType == feedType
                val hashColor = generateColorFromHash(feedType.name)
                Surface(
                    onClick = { onFeedTypeSelected(feedType) },
                    shape = RoundedCornerShape(24.dp),
                    color = if (isSelected) hashColor.copy(alpha = 0.15f) else Color.Transparent,
                    modifier = Modifier.height(44.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = when (feedType) {
                                    FeedType.HOT -> Icons.Default.LocalFireDepartment
                                    FeedType.NEW -> Icons.Default.NewReleases
                                    FeedType.TOP -> Icons.Default.Star
                                    FeedType.RISING -> Icons.Default.TrendingUp
                                    FeedType.SAVED -> Icons.Outlined.Bookmark
                                    else -> Icons.Default.LocalFireDepartment
                                },
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = hashColor
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            text = stringResource(
                                when (feedType) {
                                    FeedType.HOT -> Res.string.hot
                                    FeedType.NEW -> Res.string.new_posts
                                    FeedType.TOP -> Res.string.top
                                    FeedType.RISING -> Res.string.rising
                                    FeedType.BEST -> Res.string.best
                                    FeedType.SAVED -> Res.string.saved
                                    else -> Res.string.hot
                                }
                            ),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp,
                            color = if (isSelected) hashColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun generateColorFromHash(str: String): Color {
    val hash = str.hashCode()
    val r = (hash shr 16 and 0xFF)
    val g = (hash shr 8 and 0xFF)
    val b = (hash and 0xFF)
    return Color(r, g, b)
}
