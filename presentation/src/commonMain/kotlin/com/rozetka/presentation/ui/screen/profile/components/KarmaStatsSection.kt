package com.rozetka.presentation.ui.screen.profile.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rozetka.domain.model.UserProfile
import com.rozetka.presentation.generated.resources.Res
import com.rozetka.presentation.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun KarmaStatsSection(profile: UserProfile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(Res.string.total_karma),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = profile.totalKarma.toString(),
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold)
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), thickness = 0.5.dp)
            
            Row(modifier = Modifier.fillMaxWidth()) {
                KarmaStatItem(
                    label = stringResource(Res.string.link_karma),
                    value = profile.linkKarma,
                    icon = Icons.Default.TrendingUp,
                    modifier = Modifier.weight(1f)
                )
                KarmaStatItem(
                    label = stringResource(Res.string.comment_karma),
                    value = profile.commentKarma,
                    icon = Icons.Default.ChatBubbleOutline,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun KarmaStatItem(label: String, value: Int, icon: ImageVector, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = label, style = MaterialTheme.typography.labelMedium)
        }
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
    }
}