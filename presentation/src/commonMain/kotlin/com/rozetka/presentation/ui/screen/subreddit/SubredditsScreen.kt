package com.rozetka.presentation.ui.screen.subreddit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.rozetka.domain.model.Subreddit
import com.rozetka.presentation.mvi.SubredditsEffect
import com.rozetka.presentation.mvi.SubredditsIntent
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubredditsScreen(
    viewModel: SubredditsViewModel,
    onBack: () -> Unit,
    onNavigateToSubreddit: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is SubredditsEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is SubredditsEffect.NavigateToSubreddit -> {
                    onNavigateToSubreddit(effect.name)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subscriptions") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Implement search if needed */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading && state.subreddits.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.subreddits.isEmpty() && !state.isLoading) {
                Text(
                    text = "No subscriptions found",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.subreddits, key = { it.id }) { subreddit ->
                        SubredditItem(
                            subreddit = subreddit,
                            onClick = { viewModel.handleIntent(SubredditsIntent.NavigateToSubreddit(subreddit.name)) },
                            onToggleSubscription = { viewModel.handleIntent(SubredditsIntent.ToggleSubscription(subreddit)) },
                            onToggleFavorite = { viewModel.handleIntent(SubredditsIntent.ToggleFavorite(subreddit)) }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
            
            if (state.isLoading && state.subreddits.isNotEmpty()) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
        }
    }
}

@Composable
fun SubredditItem(
    subreddit: Subreddit,
    onClick: () -> Unit,
    onToggleSubscription: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = if (subreddit.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = if (subreddit.isFavorite) "Remove from favorites" else "Add to favorites",
                tint = if (subreddit.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        AsyncImage(
            model = "https://www.redditstatic.com/icon_placeholder.png", // Reddit doesn't always provide icon in listing
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "r/${subreddit.displayName}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "${formatSubscribers(subreddit.subscribersCount)} subscribers",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Button(
            onClick = onToggleSubscription,
            colors = if (subreddit.isSubscribed) {
                ButtonDefaults.outlinedButtonColors()
            } else {
                ButtonDefaults.buttonColors()
            },
            border = if (subreddit.isSubscribed) {
                ButtonDefaults.outlinedButtonBorder(enabled = true)
            } else {
                null
            },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Text(
                text = if (subreddit.isSubscribed) "Leave" else "Join",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

private fun formatSubscribers(count: Int): String {
    return when {
        count >= 1_000_000 -> "${count / 1_000_000}.${(count % 1_000_000) / 100_000}M"
        count >= 1_000 -> "${count / 1_000}k"
        else -> count.toString()
    }
}
