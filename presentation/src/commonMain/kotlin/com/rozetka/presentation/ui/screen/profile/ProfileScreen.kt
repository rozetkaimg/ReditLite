package com.rozetka.reditlite.presentation.screens

import io.github.aakira.napier.Napier
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rozetka.domain.model.SavedPost
import com.rozetka.domain.model.Trophy
import com.rozetka.presentation.ui.screen.profile.ProfileViewModel
import com.rozetka.presentation.ui.screen.profile.ProfileUiState

import org.jetbrains.compose.resources.stringResource
import com.rozetka.presentation.generated.resources.Res
import com.rozetka.presentation.generated.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = { Text(stringResource(Res.string.profile_title), style = MaterialTheme.typography.headlineMedium) },
                actions = {
                    IconButton(onClick = { 
                        Napier.e("Test error logged to Crashlytics via Napier", Throwable("Napier Test Error"))
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Test Napier Error",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { 
                        Napier.e("Non-fatal exception for Crashlytics test", Throwable("Test Non-Fatal Error"))
                    }) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Test Non-Fatal Error",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    IconButton(onClick = { viewModel.logout { onLogout() } }) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = stringResource(Res.string.logout),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.errorMessage != null -> {
                    Text(
                        text = stringResource(Res.string.error_loading, uiState.errorMessage ?: ""),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.profile != null -> {
                    ProfileContent(uiState = uiState)
                }
            }
        }
    }
}

@Composable
fun ProfileContent(uiState: ProfileUiState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            ProfileHeaderCard(
                name = uiState.profile?.name ?: stringResource(Res.string.unknown_user),
                karma = uiState.profile?.totalKarma ?: 0
            )
        }

        if (uiState.trophies.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(Res.string.trophies),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.trophies) { trophy ->
                        TrophyChip(trophy)
                    }
                }
            }
        }

        item {
            Text(
                text = stringResource(Res.string.saved_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        items(uiState.savedPosts) { post ->
            SavedPostCard(post)
        }
    }
}

@Composable
fun ProfileHeaderCard(name: String, karma: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.take(1).uppercase(),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Karma",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(Res.string.karma_count, karma),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun TrophyChip(trophy: Trophy) {
    AssistChip(
        onClick = { },
        label = { Text(trophy.name) },
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
            )
        },
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun SavedPostCard(post: SavedPost) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "r/${post.subreddit} • u/${post.author}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = post.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}